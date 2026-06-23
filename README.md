# LocalMusicPlayer

一款基于 Jetpack Compose + Material 3 的 Android 本地音乐播放器，采用 MVVM + Clean Architecture 架构，支持后台播放、在线歌词搜索、音频标签编辑、播放列表管理、均衡器与多彩主题。

- 当前版本：**v1.3.0**（versionCode 7）
- 最低支持：Android 8.0（API 26）
- 目标 SDK：Android 14（API 35）
- 包名：`com.musicplayer.localmusicplayer`

---

## 功能特性

### 音乐库
- **四标签浏览**：歌曲 / 专辑 / 艺术家 / 播放列表
- **MediaStore 扫描**：自动扫描本地音乐，过滤非音乐文件（时长 ≥ 15 秒），解析年份、碟号、轨道号
- **搜索与排序**：按标题/艺术家/专辑实时搜索，支持多种排序方式
- **Paging 3 分页**：歌曲列表采用 Room + Paging 3 流式加载，长列表流畅滚动

### 播放控制
- **后台播放**：基于 Media3 ExoPlayer + MediaSessionService 的前台服务，通知栏控制
- **队列管理**：手动实现的随机/循环模式（不使用 ExoPlayer 原生 repeat），默认循环全部
- **上一首智能重启**：进度超过 3 秒时点击上一首会重启当前曲目
- **音频焦点**：自动响应来电、其他应用播放
- **睡眠定时器**：设定时间后自动暂停

### 歌词
- **本地 LRC 解析**：支持 UTF-8 / GBK 自动识别的 LRC 解析引擎
- **在线歌词搜索**：聚合网易云 / QQ音乐 / 酷我 / Spotify / Apple Music 多音源
- **歌词内嵌**：将搜索到的歌词写入音频文件标签

### 元数据编辑
- **歌曲信息编辑**：标题、艺术家、专辑、年份、轨道号、碟号、风格、专辑封面
- **专辑信息编辑**：批量修改专辑名与艺术家
- **文件删除**：长按删除歌曲文件或整张专辑（适配 Android 10+ 分区存储权限）
- **标签读写**：基于 JAudioTagger 的 ID3 / Vorbis / MP4 元数据读写

### 播放列表
- 创建 / 编辑 / 删除播放列表
- 长按拖拽排序
- 自定义封面与图标
- 添加 / 移除歌曲

### 个性化
- **10 色主题**：白 / 黑 / 红 / 橙 / 黄 / 绿 / 蓝 / 紫 / 粉 / 灰（基于种子色的自定义配色，非 Material You 动态取色）
- **主题模式**：亮色 / 暗色 / 跟随系统
- **均衡器**：多频段竖直滑块、预设切换
- **多语言**：中文（默认）/ 英文，DataStore 持久化

---

## 技术栈

| 分类 | 技术 |
|------|------|
| 语言 | Kotlin 2.1.0 |
| UI | Jetpack Compose + Material 3（BOM 2025.04.00） |
| 架构 | MVVM + Clean Architecture |
| 依赖注入 | Hilt 2.53.1（KSP） |
| 播放引擎 | Media3 ExoPlayer 1.6.1 + MediaSession |
| 数据库 | Room 2.7.0 |
| 分页 | Paging 3.3.4 + Room Paging |
| 图片加载 | Coil 3 |
| 偏好存储 | DataStore Preferences |
| 网络请求 | Retrofit 2.11.0 + OkHttp 4.12.0 + Gson |
| 音频标签 | JAudioTagger 2.2.3（AdrienPoupa fork） |
| 导航 | Navigation Compose 2.9.0 |
| 桌面小部件 | Glance 1.1.1（已声明依赖，暂未实现） |
| 构建 | Gradle 8.11.1 / AGP 8.7.3 / JDK 21 / 字节码 17 |

---

## 项目结构

```
app/src/main/java/com/musicplayer/localmusicplayer/
├── MusicPlayerApplication.kt        # Application 入口（启动时同步加载语言）
├── MainActivity.kt                  # 单 Activity（Compose 宿主）
│
├── domain/                          # 领域层（纯 Kotlin，无 Android 依赖）
│   ├── model/                       # 数据模型（Song/Album/Artist/Playlist 等）
│   ├── repository/                  # 仓库接口
│   └── usecase/                     # 用例（业务逻辑）
│
├── data/                            # 数据层
│   ├── local/db/                    # Room 数据库、DAO、实体
│   ├── local/datasource/            # MediaStore 数据源
│   ├── edit/                        # 音频标签编辑（MediaStoreEditManager）
│   ├── deletion/                    # 文件删除（MediaStoreDeleteManager）
│   ├── remote/                      # Retrofit 在线歌词 API
│   └── repository/                  # 仓库实现
│
├── presentation/                    # 表现层
│   ├── library/                     # 音乐库（歌曲/专辑/艺术家/播放列表）
│   ├── albumdetail/                 # 专辑详情
│   ├── artistdetail/                # 艺术家详情
│   ├── playlistdetail/              # 播放列表详情
│   ├── player/                      # 播放器界面
│   ├── lyrics/                      # 歌词界面
│   ├── equalizer/                   # 均衡器
│   ├── settings/                    # 设置
│   ├── theme/                       # 主题构建
│   └── navigation/                  # 导航图（Screen.kt）
│
├── service/                         # ExoPlayer 播放服务 + PlaybackManager
├── di/                              # Hilt 依赖注入模块
└── util/                            # 工具类（时间格式化、歌词解析、语言助手等）
```

> 完整的实体表结构、导航图、API 契约与架构深挖见 [`TECHNICAL_DOCUMENTATION.md`](./TECHNICAL_DOCUMENTATION.md)。

---

## 构建与运行

### 环境要求
- **JDK 21**（运行 Gradle 8.11.1 / AGP 8.7.3 必需）
- Android SDK（compileSdk 35）
- 在 `local.properties`（已 gitignore）中配置 `sdk.dir` 指向你的 Android SDK 路径

### 构建命令

Windows 环境使用 `gradlew.bat`：

```bash
# Debug APK
gradlew.bat assembleDebug

# Release APK（未签名，输出 app-release-unsigned.apk）
gradlew.bat assembleRelease

# Lint 检查（abortOnError=false，不会中断构建）
gradlew.bat lint
```

APK 输出路径：

```
app/build/outputs/apk/debug/app-debug.apk            # Debug 版本
app/build/outputs/apk/release/app-release-unsigned.apk  # Release 版本（未签名）
```

> **注意**：Release 构建未启用 R8/ProGuard 混淆（`isMinifyEnabled = false`），且无签名配置，需自行配置签名后才能发布。

---

## 测试

本项目无 CI，测试需手动运行。

### 单元测试（JVM，约 47 个用例）

覆盖 mappers、repositories、utils（时间格式化、歌词解析）：

```bash
gradlew.bat :app:testDebugUnitTest
```

测试依赖：JUnit、Turbine、MockK、kotlinx-coroutines-test。

### 插桩测试（需设备/模拟器）

覆盖 Room DAO（`SongDaoTest`、`PlaylistDaoTest`）：

```bash
gradlew.bat :app:connectedAndroidTest
```

> **覆盖范围说明**：目前无 ViewModel、导航、Service、均衡器的测试。改动后至少应运行单元测试 + `assembleDebug`，并在真机上手动验证播放与导航。

---

## 数据库说明

- 数据库版本 3，文件 `localmusicplayer.db`，`exportSchema = false`（`app/schemas/` 已 gitignore，无 schema 基线）
- **使用 `fallbackToDestructiveMigration()`**：修改任何实体/表会**清空所有用户数据**（播放列表、播放列表-歌曲关联）。
- 如需保留用户数据，请在升版本前改用显式 `Migration` 对象。

---

## 关键设计说明

- **播放架构**：`MusicPlaybackService` 是 ExoPlayer 的唯一持有者；`PlaybackManager` 是 Hilt `@Singleton`，生命周期长于 Service，委托控制调用并镜像 Service 的 StateFlow 供 UI 消费。
- **循环/随机**：手动在 Service 的 `STATE_ENDED` 处理器中实现，**未启用** ExoPlayer 原生 repeat，避免冲突。默认循环全部。
- **专辑来源**：专辑不是独立表，而是从 `songs` 表通过 `SELECT DISTINCT album, album_id, artist, album_art_uri` 聚合而来。
- **语言加载**：`MusicPlayerApplication.onCreate` 使用 `runBlocking` 在 Activity 启动前同步读取 DataStore 中的语言设置，确保 locale 正确应用——此同步读取是有意为之，请勿改为 suspend。
- **主题**：自定义种子色配色，非 Material You 动态取色。`MusicPlayerTheme` 通过 `Application` 强转访问 `ThemeRepository`，而非 Hilt 注入。
- **在线歌词 API**：第三方端点 `https://music-api.gdstudio.xyz/api.php`，非本仓库所有，可能不稳定，失败时应优雅降级到本地 `.lrc` 文件。

---

## 版本历史

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

---

## 开发约定

- Kotlin 代码风格：`official`（见 `gradle.properties`）
- 注解处理使用 **KSP**（Room 与 Hilt 均通过 KSP，非 kapt）
- 依赖版本集中在 `gradle/libs.versions.toml`，新增依赖在此添加并通过 `libs.*` 引用（Retrofit/OkHttp/Gson/Paging 例外，目前在 `app/build.gradle.kts` 内联）
- Hilt 模块位于 `di/`，新绑定放于此，不要散落在其他文件
- **领域层必须保持 Android 无关**（`domain/` 内禁止 `android.*` 导入）
- 图片加载统一使用 Coil 3

---

## 许可证

本项目仅供学习交流使用。
