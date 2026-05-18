# BootLauncher

Android 开机自启应用 —— 设备启动后自动按顺序、带延时地拉起用户配置的其他应用。

## 功能特性

- **开机自启** —— 监听 `BOOT_COMPLETED` 广播，设备启动后自动运行
- **双保险策略** —— 同时支持广播监听和 Launcher 桌面两种自启方式，兼容国产 ROM
- **顺序 + 延时启动** —— 可为每个应用单独配置启动延时，按用户设定的顺序逐一拉起
- **可视化管理** —— Material3 界面，支持添加、删除、排序、启用/禁用、配置延时
- **已安装应用选择** —— 从设备已安装应用列表中挑选，支持搜索过滤

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

# Gradle（仅需用于生成 Wrapper，实际编译由 Wrapper 管理的版本执行）
wget https://services.gradle.org/distributions/gradle-8.10-bin.zip
unzip gradle-8.10-bin.zip -d ~/
export PATH=$HOME/gradle-8.10/bin:$PATH
gradle --version  # 验证安装
# 注意：系统安装的 Gradle 版本不影响编译，Wrapper 会使用指定版本（当前 8.10）

# Android SDK 命令行工具
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

# 环境变量（加入 ~/.bashrc 持久化）
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$HOME/gradle-8.10/bin:$PATH

# 安装 SDK 组件
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
yes | sdkmanager --licenses
```

### 2. 生成 Gradle Wrapper

```bash
cd ~/cxr/code/git/bootlauncher
# 如果系统 Gradle 版本不是 8.10，需显式指定版本（AGP 8.5.0 要求 Gradle ≥ 8.7）
gradle wrapper --gradle-version 8.10
```

### 3. 生成签名证书

Release 版本需要签名才能安装。在 `app/` 目录下生成密钥库：

```bash
cd ~/cxr/code/git/bootlauncher/app
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

> 已有默认证书，无需重新生成即可直接编译。如需自定义密码或信息，修改上述参数，并同步更新 `app/build.gradle.kts` 中的 `signingConfigs`。

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

1. 安装 APK 到 Android 设备
2. 打开应用，点击右下角 **+** 按钮添加需要自启的应用
3. 为每个应用设置启动延时（单位：秒）
4. 长按拖拽调整启动顺序
5. 开关控制是否启用开机自启

### 设为默认桌面（备用方案）

如果设备开机后广播被系统拦截，可将应用设为默认桌面：

> 设置 → 应用 → 默认应用 → 桌面 → 选择 BootLauncher

## 技术栈

- **语言**：Kotlin 2.0.21
- **UI**：Jetpack Compose + Material3
- **数据库**：Room 2.6.1
- **架构**：MVVM (AndroidViewModel + StateFlow)
- **构建**：Gradle + Kotlin DSL + Version Catalog

## 项目结构

```
app/src/main/kotlin/com/bootlauncher/
├── BootLauncherApp.kt               # Application 入口
├── data/local/
│   ├── AppEntity.kt                 # Room 实体（包名、延时、排序、启用状态）
│   ├── AppDao.kt                    # 数据访问对象
│   ├── AppDatabase.kt               # Room 数据库
│   └── AppRepository.kt             # 数据仓库
├── receiver/
│   └── BootReceiver.kt              # 开机广播接收器
├── service/
│   └── AppLaunchService.kt          # 前台服务，顺序拉起应用
├── ui/
│   ├── theme/                       # Material3 主题
│   ├── main/
│   │   ├── MainActivity.kt          # 应用管理界面
│   │   └── MainViewModel.kt         # 界面状态管理
│   ├── launcher/
│   │   └── LauncherActivity.kt      # 桌面备用入口
│   └── components/
│       ├── AppListItem.kt           # 应用列表项
│       ├── AppPickerSheet.kt        # 应用选择器（底部弹窗）
│       └── DelayPickerDialog.kt     # 延时设置对话框
└── util/
    └── PackageManagerHelper.kt      # 已安装应用查询工具
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `RECEIVE_BOOT_COMPLETED` | 监听设备开机广播 |
| `FOREGROUND_SERVICE` | 前台服务（确保启动过程不被系统杀死） |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ 特殊用途前台服务声明 |
| `QUERY_ALL_PACKAGES` | 查询设备上所有已安装应用（Android 11+） |

## License

MIT
