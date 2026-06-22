package com.musicplayer.localmusicplayer.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface LyricApiService {
    @GET("api.php?types=search")
    suspend fun search(
        @Query("source") source: String,
        @Query("name") name: String
    ): List<SearchResult>

    @GET("api.php?types=lyric")
    suspend fun getLyric(
        @Query("source") source: String,
        @Query("id") id: String
    ): LyricResponse
}
