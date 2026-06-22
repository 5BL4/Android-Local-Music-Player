# LocalMusicPlayer 技术文档

## 1. 项目概览

| 属性 | 值 |
|------|-----|
| **项目名称** | LocalMusicPlayer |
| **当前版本** | v1.3.0 (versionCode: 7) |
| **包名** | `com.musicplayer.localmusicplayer` |
| **最低 SDK** | Android 8.0 (API 26) |
| **目标 SDK** | Android 14 (API 35) |
| **编译 SDK** | Android 14 (API 35) |
| **Kotlin 文件数** | 106 |
| **架构模式** | MVVM + Clean Architecture |
| **依赖注入** | Hilt (Dagger) |

---

## 2. 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| **语言** | Kotlin | 2.1.0 |
| **构建工具** | Gradle + AGP | Gradle 8.11.1 / AGP 8.7.3 |
| **UI 框架** | Jetpack Compose + Material Design 3 | BOM 2025.04.00 |
| **导航** | Navigation Compose | 2.9.0 |
| **播放引擎** | Media3 ExoPlayer | 1.6.1 |
| **数据库** | Room | 2.7.0 |
| **图片加载** | Coil 3 | 3.0.4 |
| **偏好存储** | DataStore Preferences | 1.1.3 |
| **桌面小部件** | Glance | 1.1.1 |
| **网络请求** | Retrofit + OkHttp + Gson | 2.11.0 / 4.12.0 / 2.11.0 |
| **依赖注入** | Hilt | 2.53.1 |
| **KSP** | KSP | 2.1.0-1.0.29 |

---

## 3. 项目结构

```
app/src/main/java/com/musicplayer/localmusicplayer/
├── MusicPlayerApplication.kt          # Application 入口
├── MainActivity.kt                    # 单 Activity（Compose 宿主）
│
├── domain/                            # 领域层（纯 Kotlin，无 Android 依赖）
│   ├── model/                         # 数据模型
│   │   ├── Song.kt                    # 歌曲
│   │   ├── Album.kt                   # 专辑
│   │   ├── Artist.kt                  # 艺术家
│   │   ├── Playlist.kt                # 播放列表
│   │   ├── LyricLine.kt               # 歌词行
│   │   ├── PlaybackState.kt           # 播放状态（密封类）
│   │   ├── RepeatMode.kt              # 循环模式枚举
│   │   ├── SortOption.kt              # 排序选项枚举
│   │   ├── ThemeMode.kt               # 主题模式枚举
│   │   ├── ThemeColor.kt              # 主题颜色枚举（10 色）
│   │   ├── Language.kt                # 语言枚举
│   │   └── EqualizerPreset.kt         # 均衡器预设
│   ├── repository/                    # 仓库接口
│   │   ├── MusicRepository.kt
│   │   ├── PlaylistRepository.kt
│   │   ├── EqualizerRepository.kt
│   │   └── ThemeRepository.kt
│   └── usecase/                       # 用例（业务逻辑）
│       ├── ScanMusicFilesUseCase.kt
│       ├── PlaySongUseCase.kt
│       ├── ControlPlaybackUseCase.kt
│       ├── CreatePlaylistUseCase.kt
│       ├── UpdatePlaylistUseCase.kt
│       ├── DeletePlaylistUseCase.kt
│       ├── ManagePlaylistSongsUseCase.kt
│       ├── ParseLyricsUseCase.kt
│       ├── SleepTimerUseCase.kt
│       ├── GetAlbumsUseCase.kt
│       ├── GetArtistsUseCase.kt
│       ├── GetSongsUseCase.kt
│       └── GetEqualizerPresetsUseCase.kt
│
├── data/                              # 数据层
│   ├── local/db/                      # Room 数据库
│   │   ├── AppDatabase.kt             # 数据库定义（version=3）
│   │   ├── entity/                    # 数据库实体
│   │   │   ├── SongEntity.kt
│   │   │   ├── PlaylistEntity.kt
│   │   │   └── PlaylistSongCrossRef.kt
│   │   └── dao/                       # 数据访问对象
│   │       ├── SongDao.kt
│   │       ├── PlaylistDao.kt
│   │       └── PlaylistSongCrossRefDao.kt
│   ├── local/datasource/              # 本地数据源
│   │   ├── MediaStoreDataSource.kt    # 媒体库扫描
│   │   └── LyricsFileDataSource.kt    # LRC 文件查找
│   ├── remote/                        # 远程数据源
│   │   ├── LyricApiService.kt         # Retrofit API 接口
│   │   ├── LyricApiModels.kt          # API 数据模型
│   │   └── LyricsRemoteDataSource.kt  # 远程歌词搜索
│   ├── repository/                    # 仓库实现
│   │   ├── MusicRepositoryImpl.kt
│   │   ├── PlaylistRepositoryImpl.kt
│   │   ├── EqualizerRepositoryImpl.kt
│   │   └── ThemeRepositoryImpl.kt
│   ├── mapper/                        # 映射器
│   │   ├── SongMapper.kt
│   │   └── PlaylistMapper.kt
│   └── preference/
│       └── AppPreferences.kt          # DataStore 封装
│
├── di/                                # 依赖注入模块
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   ├── ServiceModule.kt
│   └── NetworkModule.kt
│
├── service/                           # 播放服务
│   ├── PlaybackManager.kt             # 核心：ExoPlayer + StateFlow
│   ├── MusicPlaybackService.kt        # 前台 MediaSessionService
│   ├── MediaNotificationManager.kt    # 通知栏控制
│   └── AudioFocusManager.kt           # 音频焦点管理
│
├── presentation/                      # 表现层（Compose UI）
│   ├── theme/                         # 主题
│   │   ├── Theme.kt                   # MusicPlayerTheme
│   │   ├── Color.kt                   # 色彩定义
│   │   └── Type.kt                    # 字体排版
│   ├── navigation/                    # 导航
│   │   ├── Screen.kt                  # 路由定义
│   │   └── NavGraph.kt                # 导航图
│   ├── library/                       # 音乐库主页
│   │   ├── LibraryScreen.kt           # 标签页（歌曲/专辑/艺术家/播放列表）
│   │   ├── LibraryViewModel.kt
│   │   └── components/                # 子组件
│   │       ├── SongList.kt / SongItem.kt
│   │       ├── AlbumGrid.kt / AlbumCard.kt
│   │       ├── ArtistList.kt / ArtistItem.kt
│   │       ├── PlaylistGrid.kt / PlaylistCard.kt
│   │       ├── SongBottomSheet.kt
│   │       ├── AlbumBottomSheet.kt
│   │       ├── EditSongDialog.kt
│   │       └── EditPlaylistDialog.kt
│   ├── player/                        # 播放器
│   │   ├── PlayerScreen.kt
│   │   ├── PlayerViewModel.kt
│   │   └── components/
│   │       ├── NowPlayingBar.kt       # 底部迷你播放栏
│   │       ├── PlaybackControls.kt    # 播放控制按钮
│   │       ├── SeekBar.kt             # 进度条
│   │       ├── AlbumArt.kt            # 专辑封面
│   │       └── AddToPlaylistDialog.kt
│   ├── albumdetail/                   # 专辑详情
│   │   ├── AlbumDetailScreen.kt
│   │   └── AlbumDetailViewModel.kt
│   ├── artistdetail/                  # 艺术家详情
│   │   ├── ArtistDetailScreen.kt
│   │   └── ArtistDetailViewModel.kt
│   ├── playlist/                      # 播放列表管理
│   │   ├── PlaylistScreen.kt
│   │   └── PlaylistViewModel.kt
│   ├── playlistdetail/                # 播放列表详情
│   │   ├── PlaylistDetailScreen.kt
│   │   └── PlaylistDetailViewModel.kt
│   ├── lyrics/                        # 歌词显示
│   │   ├── LyricsScreen.kt
│   │   └── LyricsViewModel.kt
│   ├── equalizer/                     # 均衡器
│   │   ├── EqualizerScreen.kt
│   │   ├── EqualizerViewModel.kt
│   │   └── components/
│   ├── settings/                      # 设置
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── sleeptimer/
│       ├── SleepTimerDialog.kt
│       └── SleepTimerViewModel.kt
│
└── util/                              # 工具类
    ├── Constants.kt                    # 常量
    ├── TimeFormatter.kt                # 时间格式化
    ├── LyricParser.kt                  # LRC 解析器
    ├── LrcParser.kt                    # LRC 解析引擎（新版）
    ├── AudioTagManager.kt              # 音频标签写入
    ├── LocaleHelper.kt                 # 语言环境切换
    └── AppLogger.kt                    # 日志工具
```

---

## 4. 架构设计

### 4.1 三层 Clean Architecture

```
┌─────────────────────────────────────────────────┐
│              PRESENTATION 层                      │
│  ViewModel (StateFlow<UiState>) ←→ Compose UI    │
│  依赖：domain 层的 UseCase                         │
└────────────────────┬────────────────────────────┘
                     │ 调用
                     ▼
┌─────────────────────────────────────────────────┐
│                DOMAIN 层                          │
│  UseCase → Repository 接口 → Domain Model        │
│  纯 Kotlin，零 Android 依赖                        │
└────────────────────┬────────────────────────────┘
                     │ 实现
                     ▼
┌─────────────────────────────────────────────────┐
│                DATA 层                            │
│  RepositoryImpl → Room / MediaStore / Retrofit   │
│  依赖：Android SDK、Room、DataStore、Retrofit      │
└─────────────────────────────────────────────────┘
```

### 4.2 播放架构

```
PlaybackManager (Hilt @Singleton)
  ├── ExoPlayer 实例
  ├── StateFlow<PlaybackState> —— 播放状态
  ├── StateFlow<Long> —— 当前进度（250ms ticker）
  └── 回调绑定 ──→ MusicPlaybackService (MediaSessionService)
                      ├── MediaSession
                      ├── MediaNotificationManager
                      └── AudioFocusManager
```

**设计要点**：
- `PlaybackManager` 是单例，生命周期长于 Service
- ViewModel 直接观察 PlaybackManager 的 StateFlow
- 播放列表默认启用「全部循环」模式，连续播放不会自动停止
- ExoPlayer 原生处理切歌，避免手动干预导致的竞态

### 4.3 媒体扫描流程

```
用户触发扫描（或首次启动自动扫描）
  │
  ▼
ScanMusicFilesUseCase
  │
  ▼
MediaStoreDataSource.scanAudioFiles()
  │── ContentResolver.query(EXTERNAL_CONTENT_URI)
  │── 过滤：IS_MUSIC != 0, DURATION > 15000ms
  │── 解析 TRACK 列：discNum = raw/1000, trackNum = raw%1000
  │
  ▼
SongDao.deleteSongsByIds(已存在的旧ID) + upsertAll(新数据)
  │── Room Flow 自动推送 ──→ LibraryViewModel ──→ Compose UI 重组
```

### 4.4 歌词搜索流程

```
LyricsScreen → 搜索按钮 → 输入关键词 + 选择音源
  │
  ▼
LyricsViewModel.search()
  │
  ▼
LyricsRemoteDataSource.search() ──→ Retrofit API
  │── GET api.php?types=search&source=netease&name=歌名
  │── JSON → List<SearchResult>
  │
  ▼
用户点击搜索结果
  │
  ▼
LyricsRemoteDataSource.getLyric() ──→ Retrofit API
  │── GET api.php?types=lyric&source=netease&id=lyric_id
  │── JSON → LyricResponse (LRC + 翻译 LRC)
  │
  ▼
ModalBottomSheet 预览 → "嵌入到本地" → AudioTagManager.embedLyrics()
  │── 写入 .lrc 文件到音频文件同目录
```

---

## 5. 数据库设计

### 5.1 songs 表

| 列名 | 类型 | 说明 |
|------|------|------|
| id | INTEGER (PK, auto) | 主键 |
| media_store_id | INTEGER | MediaStore 的 _ID，唯一索引 |
| title | TEXT | 标题 |
| artist | TEXT | 艺术家 |
| album | TEXT | 专辑名 |
| album_id | INTEGER | 专辑 ID |
| duration | INTEGER | 时长（毫秒） |
| file_path | TEXT | 文件路径（可为空） |
| content_uri | TEXT | Content URI |
| album_art_uri | TEXT | 专辑封面 URI |
| date_added | INTEGER | 添加日期 |
| year | INTEGER | 年份 |
| track_number | INTEGER | 轨道号 |
| disc_number | INTEGER | 碟号 |
| genre | TEXT | 风格 |
| mime_type | TEXT | 文件类型 |
| size | INTEGER | 文件大小 |

### 5.2 playlists 表

| 列名 | 类型 | 说明 |
|------|------|------|
| id | INTEGER (PK, auto) | 主键 |
| name | TEXT | 名称 |
| description | TEXT | 简介（可为空） |
| cover_art_uri | TEXT | 封面图片路径 |
| created_at | INTEGER | 创建时间 |
| updated_at | INTEGER | 更新时间 |

### 5.3 playlist_song_cross_ref 表

| 列名 | 类型 | 说明 |
|------|------|------|
| playlist_id | INTEGER (PK, FK) | 播放列表 ID |
| song_id | INTEGER (PK, FK) | 歌曲 ID |
| position | INTEGER | 排序位置 |

---

## 6. 核心功能概览

| 功能分类 | 具体功能 |
|----------|---------|
| **音乐扫描** | MediaStore 扫描、去重、删除检测、年份/碟号/轨道号自动解析 |
| **音乐库** | 四标签浏览（歌曲/专辑/艺术家/播放列表）、搜索过滤、排序切换 |
| **播放控制** | 播放/暂停、上/下一首、进度拖动、随机/循环模式 |
| **后台播放** | 前台 Service、MediaSession、通知栏控制、音频焦点管理 |
| **播放列表** | 创建/编辑/删除、长按拖拽排序、封面设置、添加/移除歌曲 |
| **歌词** | 本地 .lrc 解析、UTF-8/GBK 自动识别、在线搜索下载、内嵌保存 |
| **均衡器** | 多频段竖直滑块、预设切换、开关控制 |
| **主题** | 10 色主题（白/黑/红/橙/黄/绿/蓝/紫/粉/灰）、亮色/暗色/系统模式 |
| **多语言** | 中文/英文切换、DataStore 持久化 |
| **歌曲管理** | 编辑元数据（标题/艺术家/专辑/年份/轨道号/碟号/风格）、删除本地文件 |
| **专辑管理** | 编辑专辑信息、批量删除专辑歌曲 |
| **睡眠定时** | 指定时间后暂停播放 |
| **权限处理** | 运行时请求 READ_MEDIA_AUDIO、无线调试安装 |

---

## 7. 导航结构

```
NavHost (startDestination: library)
├── library          —— 音乐库主页（歌曲/专辑/艺术家/播放列表）
├── player           —— 全屏播放器
├── album/{albumId}  —— 专辑详情
├── artist/{name}    —— 艺术家详情
├── playlists        —— 播放列表管理
├── playlist/{id}    —— 播放列表详情（支持拖拽排序）
├── lyrics/{songId}  —— 歌词显示 + 在线搜索
├── equalizer        —— 均衡器
└── settings         —— 设置页

底部导航栏（Library | Equalizer | Settings）
NowPlayingBar  —— 点击跳转 player
```

---

## 8. 主题色彩系统

| 主题色 | 色值 | 亮色 primaryContainer | 暗色 primary |
|--------|------|----------------------|-------------|
| 白色 White | #F8F8F8 | 浅白变体 | 0.9 透明度白 |
| 黑色 Black | #1A1A1A | 浅灰变体 | 0.9 透明度黑 |
| 红色 Red | #E53935 | 浅红变体 | 0.9 透明度红 |
| 橙色 Orange | #FB8C00 | 浅橙变体 | 0.9 透明度橙 |
| 黄色 Yellow | #FDD835 | 浅黄变体 | 0.9 透明度黄 |
| 绿色 Green | #43A047 | 浅绿变体 | 0.9 透明度绿 |
| 蓝色 Blue | #1E88E5 | 浅蓝变体 | 0.9 透明度蓝 |
| 紫色 Purple | #8E24AA | 浅紫变体 | 0.9 透明度紫 |
| 粉色 Pink | #D81B60 | 浅粉变体 | 0.9 透明度粉 |
| 灰色 Gray | #757575 | 浅灰变体 | 0.9 透明度灰 |

---

## 9. API 接口定义

### 在线歌词搜索 API

**搜索歌曲**：
```
GET https://music-api.gdstudio.xyz/api.php?types=search&source={source}&name={name}
```
- `source`：音源（netease/tencent/kuwo/spotify/apple）
- `name`：歌曲名称
- 返回：`List<SearchResult>` JSON 数组

**获取歌词**：
```
GET https://music-api.gdstudio.xyz/api.php?types=lyric&source={source}&id={id}
```
- 返回：`{lyric: "LRC 原语种歌词", tlyric: "翻译歌词（可能为空）"}`

> API 来源：GD 音乐台 (music.gdstudio.xyz)

---

## 10. 构建与部署

### 构建命令

```bash
export JAVA_HOME="C:\Users\<user>\jdks\jdk21\jdk-21.0.2"
cd /path/to/LocalMusicPlayer
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK（需签名）
```

### 无线安装

```bash
adb pair <IP>:<配对端口> <配对码>
adb connect <IP>:<连接端口>
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 输出位置

```
app/build/outputs/apk/debug/app-debug.apk     # Debug 版本 (~23MB)
app/build/outputs/apk/release/app-release-unsigned.apk  # Release 版本 (~16MB)
```

---

## 11. 版本历史

| 版本 | versionCode | 主要更新 |
|------|-------------|---------|
| 1.0 | 1 | 初始版本：项目搭建、领域层、主题系统 |
| 1.1 | 2 | 中英文切换、权限请求修复 |
| 1.1.2 | 3 | 底部导航标签翻译、语言持久化、NowPlayingBar 布局修复 |
| 1.1.3 | 4 | 主题模式修复、均衡器竖直滑块、随机播放关闭修复 |
| 1.1.4 | 5 | 长按删除、均衡器 dB 条自适应、本地文件删除 |
| 1.2.0 | 6 | 播放列表编辑、多彩主题（10 色）、艺术家头像 |
| 1.2.1 | - | 拖拽排序、UI 风格统一 |
| 1.2.2 | - | 底部导航栏颜色、主题 UI 布局 |
| 1.2.3 | - | 歌曲底部弹窗、三点菜单、播放列表图标 |
| 1.2.4 | - | Album/Artist 歌曲菜单、专辑编辑删除、NowPlayingBar 背景 |
| **1.3.0** | **7** | **在线歌词搜索、歌词内嵌、音源选择、LRC 解析引擎** |
