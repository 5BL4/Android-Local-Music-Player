package com.musicplayer.localmusicplayer.domain.model

sealed class EditResult {
    object Success : EditResult()
    data class NeedsConfirmation(val requestId: Long) : EditResult()
    object Error : EditResult()
}
