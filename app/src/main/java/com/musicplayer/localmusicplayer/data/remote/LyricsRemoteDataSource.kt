package com.musicplayer.localmusicplayer.data.remote

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRemoteDataSource @Inject constructor(
    private val api: LyricApiService
) {
    suspend fun search(source: String, name: String): Result<List<SearchResult>> = runCatching {
        api.search(source, name)
    }

    suspend fun getLyric(source: String, id: String): Result<LyricResponse> = runCatching {
        api.getLyric(source, id)
    }
}
