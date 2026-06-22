# ProGuard rules for LocalMusicPlayer

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room entities
-keep class com.musicplayer.localmusicplayer.data.local.db.entity.** { *; }

# Keep domain models
-keep class com.musicplayer.localmusicplayer.domain.model.** { *; }
