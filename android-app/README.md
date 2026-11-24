# 深海禅定 Android

## 构建环境

- Java 17
- Gradle Wrapper 8.7
- Android Gradle Plugin 8.6.1
- Kotlin 1.9.22
- compileSdk 36

## 构建 Debug APK

```bash
./gradlew assembleDebug
```

输出位于 `app/build/outputs/apk/debug/app-debug.apk`。

手机端优先测试 `app/build/outputs/apk/release/app-release.apk`。安装排障步骤见 [../docs/android-install-troubleshooting.md](../docs/android-install-troubleshooting.md)。

## 当前功能

- 水族箱首页
- 默认横屏全屏水族房
- 水族房内部 Todo 和天气侧栏
- 触摸显示 HUD，5 秒后自动隐藏
- Todo 支持新增、完成和删除
- 禅定时长支持 25、50、90 分钟切换
- 全应用横屏，菜单切换不会自动旋转
- 真实海底隧道背景
- 鱼类随机游动
- 鱼类带方向转向、身体摆动和呼吸缩放
- 水草、气泡和水流光影动画
- 点击鱼类显示档案
- 鱼苗投放后显示在当前场景
- 22 个住客条目，支持鱼类、贝类和其他筛选
- 扇贝、蓝贻贝、牡蛎、鲍鱼、海螺和寄居蟹等贝类与底栖生物
- 场景图鉴
- 鱼苗商店
- 禅定设置
- Lock Task 调用入口

当前工作区没有连接 Android 设备或模拟器，因此安装和真机锁定验证需要在下一阶段执行。
