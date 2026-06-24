package com.musicplayer.localmusicplayer.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * 10-band software graphic equalizer implemented as a Media3 [BaseAudioProcessor].
 *
 * Each band is a [BiquadFilter] peaking-EQ filter. Filters are chained per channel
 * and applied to 16-bit interleaved PCM data in [queueInput].
 *
 * Thread-safety: gain updates arrive from the UI thread via [setBandGain] / [setAllGains]
 * and are read on the audio thread via @Volatile fields. Coefficient rebuild happens
 * inside [queueInput] (audio thread) to avoid concurrent access to filter state.
 */
@UnstableApi
class EqualizerAudioProcessor : BaseAudioProcessor() {

    companion object {
        /** Fixed 10-band center frequencies in Hz. */
        val BAND_FREQUENCIES = floatArrayOf(
            32f, 100f, 250f, 500f, 1000f,
            2000f, 4000f, 8000f, 12000f, 16000f
        )

        private const val NUM_BANDS = 10
        private const val BAND_Q = 1.41f
    }

    private var sampleRate = 0
    private var channels = 0

    /** filters[channel][band] — created in onConfigure. */
    private var filters: Array<Array<BiquadFilter>>? = null

    @Volatile
    private var targetGains = FloatArray(NUM_BANDS) { 0f }

    @Volatile
    private var enabled = false

    private var appliedGains = FloatArray(NUM_BANDS) { 0f }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channels = inputAudioFormat.channelCount
        filters = Array(channels) { Array(NUM_BANDS) { BiquadFilter() } }
        // Apply any gains that were set before playback started.
        appliedGains = if (enabled) targetGains.copyOf() else FloatArray(NUM_BANDS) { 0f }
        rebuildCoefficients(appliedGains)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val gains = if (enabled) targetGains else FloatArray(NUM_BANDS) { 0f }

        if (!gains.contentEquals(appliedGains)) {
            rebuildCoefficients(gains)
            appliedGains = gains.copyOf()
        }

        val isBypass = gains.all { it == 0f }
        val out = replaceOutputBuffer(inputBuffer.remaining())
        val filterArray = filters

        if (isBypass || filterArray == null) {
            // Passthrough — no processing needed.
            while (inputBuffer.hasRemaining()) out.put(inputBuffer.get())
        } else {
            // Process interleaved 16-bit PCM, frame by frame.
            while (inputBuffer.remaining() >= 2 * channels) {
                for (ch in 0 until channels) {
                    var sample = inputBuffer.short.toFloat() / 32768f
                    val bandFilters = filterArray[ch]
                    for (band in bandFilters.indices) {
                        sample = bandFilters[band].process(sample)
                    }
                    val clamped = sample.coerceIn(-1f, 1f)
                    out.putShort((clamped * 32767f).toInt().toShort())
                }
            }
            // Copy any trailing bytes that don't form a complete frame.
            while (inputBuffer.hasRemaining()) out.put(inputBuffer.get())
        }
        out.flip()
    }

    // ── Public control API (called from EqualizerRepository on UI thread) ──

    fun setBandGain(band: Int, gainDb: Float) {
        if (band in targetGains.indices && targetGains[band] != gainDb) {
            val newGains = targetGains.copyOf()
            newGains[band] = gainDb
            targetGains = newGains
        }
    }

    fun setAllGains(gains: FloatArray) {
        if (gains.size == NUM_BANDS && !gains.contentEquals(targetGains)) {
            targetGains = gains.copyOf()
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    // ── Lifecycle ──

    override fun onFlush() {
        // Reset delay lines on seek / stream change to avoid artifacts.
        filters?.forEach { row -> row.forEach { it.reset() } }
    }

    override fun onReset() {
        filters = null
        appliedGains = FloatArray(NUM_BANDS) { 0f }
        targetGains = FloatArray(NUM_BANDS) { 0f }
        enabled = false
    }

    // ── Internal ──

    private fun rebuildCoefficients(gains: FloatArray) {
        filters?.let { filterArray ->
            for (ch in 0 until channels) {
                for (b in 0 until NUM_BANDS) {
                    filterArray[ch][b].setPeakingEq(
                        freqHz = BAND_FREQUENCIES[b],
                        gainDb = gains[b],
                        q = BAND_Q,
                        sampleRate = sampleRate
                    )
                }
            }
        }
    }
}
