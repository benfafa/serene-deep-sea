# Android 安装排障

## 推荐安装包

优先使用 [app-release.apk](../android-app/app/build/outputs/apk/release/app-release.apk)。当前版本号是 `0.1.1`，包名是 `com.fanqieyu.deepfocus`。

Debug 包适合开发验证。手机端安装优先使用 release 包。

## 手机端安装

1. 把 APK 完整下载到手机本地存储。
2. 使用系统文件管理器打开 APK。
3. 如果系统提示来源权限，只给当前文件管理器开启“允许安装未知应用”。
4. 如果手机已经安装过旧版本，先卸载旧版本，再安装当前 release 包。

聊天软件内置文件预览可能会持续显示“正在安装”。遇到这种情况，把文件保存到下载目录后，从系统文件管理器重新打开。

## ADB 安装

连接设备并确认 `adb devices` 能看到设备后，可以使用：

```bash
adb uninstall com.fanqieyu.deepfocus
adb install -r app-release.apk
```

如果只是版本号较低导致安装失败，可以使用：

```bash
adb install -r -d app-release.apk
```

## 获取安装错误

```bash
adb logcat -c
adb install -r app-release.apk
adb logcat -d -b all | grep -E "PackageInstaller|PackageManager|INSTALL_FAILED"
```

常见结果包括旧签名冲突、来源权限限制、设备存储不足和文件传输不完整。当前 APK 已在本地通过 ZIP 完整性、zipalign 和 APK v2 签名校验。
