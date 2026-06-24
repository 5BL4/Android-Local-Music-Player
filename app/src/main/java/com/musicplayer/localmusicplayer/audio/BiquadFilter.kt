package com.musicplayer.localmusicplayer.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Single-channel biquad peaking EQ filter using Direct Form II Transposed.
 *
 * Coefficients based on Robert Bristow-Johnson's Audio EQ Cookbook:
 * https://www.w3.org/TR/audio-eq-cookbook/
 */
class BiquadFilter {

    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    private var z1 = 0f
    private var z2 = 0f

    /** Configure as a peaking EQ band with the given center frequency, gain, and Q. */
    fun setPeakingEq(freqHz: Float, gainDb: Float, q: Float, sampleRate: Int) {
        val a = 10.0.pow((gainDb / 40.0)).toFloat()
        val w0 = (2.0 * PI * freqHz / sampleRate).toFloat()
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2f * q)

        val nb0 = 1f + alpha * a
        val nb1 = -2f * cosW0
        val nb2 = 1f - alpha * a
        val na0 = 1f + alpha / a
        val na1 = -2f * cosW0
        val na2 = 1f - alpha / a

        b0 = nb0 / na0
        b1 = nb1 / na0
        b2 = nb2 / na0
        a1 = na1 / na0
        a2 = na2 / na0
    }

    /** Process one sample through the filter (Direct Form II Transposed). */
    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    /** Reset the filter state (delay lines). Call on flush/seek. */
    fun reset() {
        z1 = 0f
        z2 = 0f
    }
}
