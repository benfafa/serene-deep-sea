# 深海禅定 · 番茄鱼钟

这是一个面向 Android 的水族箱专注工具原型。用户开始一轮禅定后，应用进入锁定任务入口，水族箱负责承载时间、待办、天气和鱼类收藏反馈。

## 当前目录

- Web UI 原型位于项目根目录，入口是 [index.html](./index.html)。
- Android Compose 工程位于 [android-app](./android-app)。
- 产品说明位于 [docs/product-spec.md](./docs/product-spec.md)。
- Android 技术方案位于 [docs/android-architecture.md](./docs/android-architecture.md)。
- 分阶段开发计划位于 [docs/implementation-plan.md](./docs/implementation-plan.md)。

## 当前 Android 版本

Android 端基于 Jetpack Compose 与 Material 3 构建，包含以下核心特性：

- **iOS 液态玻璃主题体系**：支持暗黑、白天与跟随系统三种配色模式（[LiquidGlassTheme.kt](./android-app/app/src/main/java/com/fanqieyu/deepfocus/LiquidGlassTheme.kt)）。
- **个性化与实时天气**：首次启动引导设定用户昵称；接入系统定位 / 网络定位与 Open-Meteo 实时天气（[LocationWeatherManager.kt](./android-app/app/src/main/java/com/fanqieyu/deepfocus/LocationWeatherManager.kt)）。
- **8 个水域场景与 36 位水族住客**：全部场景与住客默认免费开放，支持在图鉴中自由投放到水箱或移出（[AquariumCatalog.kt](./android-app/app/src/main/java/com/fanqieyu/deepfocus/AquariumCatalog.kt)）。
- **极坐标航向物理引擎与指尖互动**：连续时间步游动与俯仰角校准，支持指尖长按/拖拽触发鱼群爆发聚拢或四散滑行。
- **禅定锁定与统计看板**：支持 `25m / 50m / 90m` 及 `1~180m` 自定义时长，前台服务持续计时（[FocusForegroundService.kt](./android-app/app/src/main/java/com/fanqieyu/deepfocus/FocusForegroundService.kt)）；触发 Lock Task 锁屏，中途退出锁定或切离应用自动判定失败并记录到统计看板。
- **本机应用白名单与弹幕通知**：通过本机已安装应用选择器勾选白名单 App（[InstalledAppsProvider.kt](./android-app/app/src/main/java/com/fanqieyu/deepfocus/InstalledAppsProvider.kt)），借助 `NotificationListenerService` 将白名单通知实时转化为水族箱顶部弹幕（[FocusNotificationListenerService.kt](./android-app/app/src/main/java/com/fanqieyu/deepfocus/FocusNotificationListenerService.kt)）。
- **本地 SQLite v3 持久化**：表结构与迁移脚本维护在 [sql.sql](./sql.sql) 与 [LocalAquariumDatabase.kt](./android-app/app/src/main/java/com/fanqieyu/deepfocus/LocalAquariumDatabase.kt)。

## 品牌图标

品牌标志是「番茄鱼」：一只长着鱼鳍和鱼尾的番茄，闭眼安坐，外圈是留断口的禅圆（enso），对应番茄钟的计时。

素材与产物都在 [brand](./brand) 目录：

| 文件 | 用途 |
| --- | --- |
| [brand/tomato-fish-logo.jpg](./brand/tomato-fish-logo.jpg) | 完整场景版（4096×4096，深海背景 + 光晕），适合宣传与商店页 |
| [brand/tomato-fish-mark.jpg](./brand/tomato-fish-mark.jpg) | 纯主体平色底版（4096×4096），抠图与再加工的母版 |
| [brand/tomato-fish-fish.png](./brand/tomato-fish-fish.png) | 抠好的透明主体，合成各种尺寸的输入 |
| [brand/app-icon-512.png](./brand/app-icon-512.png) | 全出血方形图（512×512），与应用内品牌标记一致 |
| [brand/ic-launcher-foreground-512.png](./brand/ic-launcher-foreground-512.png) | Adaptive icon 前景层（透明底，内容在中心 62% 安全区内） |
| [brand/tomato-fish-logo.request.json](./brand/tomato-fish-logo.request.json) | 场景版生成参数（模型、比例、prompt） |
| [brand/tomato-fish-mark.request.json](./brand/tomato-fish-mark.request.json) | 主体版生成参数 |
| [brand/backup](./brand/backup) | 替换前的旧图标资源，需要回退时从这里恢复 |

Android 端资源落位：

- `res/drawable-nodpi/ic_launcher_foreground_fish.png` —— adaptive icon 前景层，由 `res/drawable/ic_launcher_foreground.xml` 以 0% inset 引用；背景层沿用原有 `ic_launcher_background.xml` 的 `#07131D → #0D2D3E` 135° 渐变。
- `res/drawable-nodpi/app_logo.png` —— 应用内 `BrandLogoMark` 使用的品牌图，已换成番茄鱼。
- `res/mipmap-xxxhdpi/ic_launcher.png` 与 `ic_launcher_round.png` —— Android 8 以下的传统图标，分别为方形与圆形裁切。

合成要点：

- 前景层内容必须落在中心 62% 范围内，禅圆直径即整体宽度，番茄鱼按 alpha 重心做光学居中。
- 辉光一律在整幅画布上做高斯模糊再合成，不要在主体自身尺寸的图层上模糊，否则图层边界会被硬切出矩形接缝。
- 透明底素材的 RGB 不能带背景色，否则缩放时会在描边渗出色边；抠图后需把 alpha 为 0 的像素 RGB 置为描边色。

## 构建

```bash
cd android-app
./gradlew assembleDebug
```

工程使用 Gradle Wrapper 8.7、Android Gradle Plugin 8.6.1、Kotlin 1.9.22、compileSdk 36 和 Java 17。首次构建需要本机 Android SDK，并在 `local.properties` 中配置 `sdk.dir`。
