package com.musicplayer.localmusicplayer.domain.model

sealed class DeleteResult {
    object Success : DeleteResult()
    data class NeedsConfirmation(val requestId: Long) : DeleteResult()
    object Error : DeleteResult()
}
