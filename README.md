# BootLauncher

Android 桌面应用 —— 替代系统桌面，提供 4x5 网格启动界面，支持开机自启、延时启动、来电后自动恢复应用。

## 功能特性

- **4x5 桌面网格** —— 黑色背景，显示应用图标+名称，点击启动，最后一格进入设置
- **桌面应用管理** —— 设置中可为每个已配置应用开关"显示在桌面"，最多 19 个
- **开机自启** —— 监听 `BOOT_COMPLETED` 广播，按配置顺序延时启动应用
- **来电恢复** —— 来电结束 30s 后自动按配置重新启动所有应用
- **广播启动应用** —— 接收 `com.bootLauncher.LAUNCH_APP` 广播，指定包名启动应用
- **锁屏穿透** —— 可选开启"Show on Lock Screen"跳过锁屏直接显示桌面
- **顺序 + 延时启动** —— 每个应用可单独配置延时，按设定顺序逐一拉起
- **权限诊断** —— 一键检测所有必要权限和配置状态
- **华为/荣耀兼容** —— 支持 `pm set-home-activity` 命令设置默认桌面

## 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17 |
| Android SDK | API 34 (Android 14) |
| Android Build Tools | 34.0.0 |
| Kotlin | 2.0.21 |
| AGP | 8.5.0 |

支持系统：Android 7.0 (API 24) 及以上

## 编译

### 1. 安装依赖

```bash
# JDK
sudo apt install openjdk-17-jdk

# Gradle
wget https://services.gradle.org/distributions/gradle-8.10-bin.zip
unzip gradle-8.10-bin.zip -d ~/
export PATH=$HOME/gradle-8.10/bin:$PATH

# Android SDK 命令行工具
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

# 环境变量
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_PLATFORM-tools:$HOME/gradle-8.10/bin:$PATH

# 安装 SDK 组件
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
yes | sdkmanager --licenses
```

### 2. 生成 Gradle Wrapper

```bash
cd ~/cxr/code/git/bootlauncher
gradle wrapper --gradle-version 8.10
```

### 3. 生成签名证书

Release 版本需要签名才能安装。在 `app/` 目录下生成密钥库：

```bash
cd app
keytool -genkeypair -v \
  -keystore bootlauncher.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias bootlauncher \
  -storepass bootlauncher123 \
  -keypass bootlauncher123 \
  -dname "CN=BootLauncher, OU=Dev, O=BootLauncher, L=Unknown, ST=Unknown, C=CN"
```

### 4. 编译项目

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/app-release.apk`

## 使用方法

### 桌面

1. 安装 APK，设为默认桌面
2. 桌面显示 4x5 黑色网格，已配置的应用显示图标+名称
3. 点击图标启动应用，最后一格点击进入设置

### 设置

1. 点击 **+** 添加需要自启的应用
2. **Auto-start** 开关控制是否开机自动启动
3. **Desktop** 开关控制是否显示在桌面（最多 15 个）
4. **Show on Lock Screen** 开关控制是否跳过锁屏
5. 为每个应用设置启动延时
6. **Diagnose Permissions** 一键检测权限配置

### 设为默认桌面

1. 安装后打开设置，点击 **"Set as Default Launcher"**
2. 或手动：设置 → 应用 → 默认应用 → 桌面 → 选择 BootLauncher

### 广播启动应用

```bash
adb shell am broadcast \
  -a com.bootlauncher.LAUNCH_APP \
  -p com.bootlauncher \
  --es package_name com.tencent.mm
```

## 技术栈

- **语言**：Kotlin 2.0.21
- **UI**：Jetpack Compose + Material3
- **数据库**：Room (AppEntity + LaunchLog)
- **架构**：MVVM (AndroidViewModel + StateFlow)
- **构建**：Gradle + Kotlin DSL + Version Catalog

## 项目结构

```
app/src/main/kotlin/com/bootlauncher/
├── BootLauncherApp.kt               # Application 入口，SharedPreferences
├── data/local/
│   ├── AppEntity.kt                 # 应用实体（包名、延时、排序、启用、桌面显示）
│   ├── AppDao.kt                    # Room DAO
│   ├── AppDatabase.kt               # Room 数据库（v3）
│   ├── AppRepository.kt             # 数据仓库
│   ├── LaunchLog.kt                 # 启动日志实体
│   └── LaunchLogDao.kt              # 启动日志 DAO
├── receiver/
│   └── BootReceiver.kt              # 开机广播接收器
├── service/
│   └── AppLaunchService.kt          # 前台服务，顺序拉起应用
├── ui/
│   ├── theme/                       # Material3 主题
│   ├── main/
│   │   ├── MainActivity.kt          # 设置界面（权限诊断、应用管理）
│   │   └── MainViewModel.kt         # 设置界面状态管理
│   ├── launcher/
│   │   ├── LauncherActivity.kt      # 桌面（广播接收、来电监听）
│   │   ├── LauncherDesktopScreen.kt  # 4x5 网格桌面
│   │   └── LauncherViewModel.kt     # 桌面数据管理
│   └── components/
│       ├── AppListItem.kt           # 应用列表项（含 Desktop 开关）
│       ├── AppPickerSheet.kt        # 应用选择器底部弹窗
│       └── DelayPickerDialog.kt     # 延时设置对话框
└── util/
    ├── FileLogger.kt                # 文件日志（logcat 不可用时的调试方案）
    └── PackageManagerHelper.kt      # 已安装应用查询工具
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `RECEIVE_BOOT_COMPLETED` | 监听设备开机广播 |
| `READ_PHONE_STATE` | 监听来电状态，用于来电后恢复应用 |
| `FOREGROUND_SERVICE` | 前台服务（确保启动过程不被系统杀死） |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ 特殊用途前台服务声明 |
| `QUERY_ALL_PACKAGES` | 查询设备上所有已安装应用（Android 11+） |
| `POST_NOTIFICATIONS` | 前台服务通知栏通知（Android 13+） |

## License

MIT
