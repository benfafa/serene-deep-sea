# Android 技术方案

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- 单 Activity
- 本地静态数据模型作为第一阶段数据源
- Android Lock Task Mode 作为锁定入口

## 当前工程结构

```text
android-app/
  app/src/main/java/com/fanqieyu/deepfocus/
    MainActivity.kt
    Models.kt
    AquariumCatalog.kt
    LockTaskController.kt
  app/src/main/res/drawable-nodpi/
    aquarium and fish assets
```

`MainActivity` 当前负责页面导航、Compose UI、鱼类动画和第一版交互。`Models.kt` 保存鱼类和场景模型。`AquariumCatalog.kt` 是场景与住客的内容唯一来源，集中维护场景列表、住客列表、默认投放组合、按名称反查资源的辅助方法和定价表。`LockTaskController` 将系统锁定任务调用隔离出来，便于后续接入设备所有者策略或专用设备模式。

Activity 和 Manifest 统一使用横屏方向。进入水族箱首页时隐藏系统栏，水族房使用整屏水下背景。进入场景、鱼苗或设置页时保持横屏并恢复系统栏，菜单切换不会触发方向旋转，也不会因为旋转重建后回到沉浸式首页。Todo 和天气作为水族房内部的玻璃侧栏显示。鱼苗投放状态当前保存在 Compose 内存状态中，投放后立即出现在当前场景中，后续接入 Room 后再保留跨进程和重启状态。

水族房 HUD 默认隐藏。触摸任意位置会显示导航、Todo、天气、禅定操作和鱼类档案，5 秒无触摸后自动收起。鱼类使用 Compose Animatable 生成带方向的随机路径、转向、身体摆动和缩放变化，水母会有更明显的漂浮和呼吸节奏，背景叠加了不同速度的气泡、水草摆动和水流光影。

HUD 的空白水域使用独立点击层负责唤出 HUD，不再覆盖场景、鱼苗和设置按钮的点击事件。住客档案提供手动关闭按钮，关闭时清除当前选中住客。Todo 侧栏支持新增、点击完成和删除，禅定卡片支持 25、50、90 分钟切换。

鱼苗商店当前使用 36 个本地住客条目（由 `AquariumCatalog.residents` 提供），提供鱼类、贝类和其他水族生物筛选。贝类和寄居蟹使用底部活动物渲染，贝壳会轻微摆动和呼吸缩放，投放后与鱼类一起出现在水族房内。

## Android 系统能力映射

### 锁定手机

当前入口调用 `Activity.startLockTask()`，Activity 的 Manifest 配置为 `android:lockTaskMode="if_whitelisted"`。普通个人设备可能弹出系统屏幕固定确认，专用设备或设备所有者模式需要额外的 DevicePolicyManager 配置。

### 电话和短信

正式版本需要明确电话和短信的展示策略。推荐把电话和短信作为系统级入口保留，把应用摘要放在锁定态自己的信息层中。涉及默认短信、通知监听和系统权限时，需要在真机上单独验证。

### 通知白名单

当前设置页只展示白名单 UI。正式版本可以使用 NotificationListenerService 接收通知，再按照用户选择的包名和通知类别生成摘要。通知内容需要本地过滤和最小化存储。

### 天气和待办

当前使用静态演示数据。正式版本建议先接入 Android 日历和本地 Room 数据，再决定是否加入天气服务和定位权限。

## 数据边界

第一阶段不保存真实通知内容、电话记录或短信内容。鱼类、场景、金币和专注记录先使用本地模型，后续再加入 Room、DataStore 和备份策略。

## 当前已验证

- Compose 工程可以生成 Debug APK。
- Lock Task 调用入口已经隔离。
- 鱼类点击档案和随机游动已经接入。
- 36 个住客条目、分类筛选和贝类投放已经接入。
- 场景和鱼苗页面已经迁移到 Android。
- `AquariumCatalog` 已接入全部 8 个场景与 36 个住客，资源引用在编译期校验。

## 真机阶段必须验证

- 不同 Android 版本的 Lock Task 行为
- 设备所有者和普通个人设备的差异
- 电话、短信和通知白名单策略
- 锁定态退出路径和异常恢复
- 长时间运行时鱼类动画和电量表现
