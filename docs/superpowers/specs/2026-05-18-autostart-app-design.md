# AutoStart — Android 开机自启应用设计文档

## 概述

一个 Android 应用，实现设备开机后自动按顺序、带延时地拉起用户配置的其他应用。支持两种自启方式：监听 `BOOT_COMPLETED` 广播（主方案）和作为默认桌面 Launcher（备用方案）。

## 目标设备

标准 Android 设备（Android 7.0+，targetSdk 34）。

## 架构

```
AutoStart (Android App)
├── MainActivity          — 应用管理 UI
├── LauncherActivity      — 默认桌面备用入口
├── BootReceiver          — 监听 ACTION_BOOT_COMPLETED
├── AppLaunchService       — 前台 Service，按顺序+延时拉起应用
├── AppRepository         — 数据管理（Room 数据库）
└── data/
    └── AppEntity         — 待启动应用的包名、延时、排序信息
```

### 启动流程

1. 设备开机 → `BootReceiver` 收到广播 → 启动 `AppLaunchService`
2. `AppLaunchService` 从数据库读取应用列表，按排序 + 延时逐一启动
3. 若应用设为默认桌面 → `LauncherActivity` 被系统拉起 → 同样触发 `AppLaunchService`

## 数据模型（Room）

```
AppEntity
├── id: Long (PK, auto-generated)
├── packageName: String    — 应用包名
├── label: String          — 应用显示名称
├── delayMs: Long          — 启动延时（毫秒），默认 0
├── sortOrder: Int         — 启动顺序
├── enabled: Boolean       — 是否启用
└── icon: ByteArray?       — 应用图标缓存（可选）
```

## UI 设计

### MainActivity — 应用管理界面

- 顶部：标题 "开机自启管理"
- 中部：RecyclerView 列表，每项显示：应用图标+名称、延时配置、启用/禁用开关、拖拽排序手柄、删除按钮
- 底部："添加应用" 按钮 → 弹出已安装应用选择列表
- 底部工具栏：一键开关（启用/禁用整个开机自启功能）

### LauncherActivity — 空壳桌面

- 仅显示简单提示信息 "自启动服务运行中..."

### 技术栈

Kotlin + Jetpack Compose

## 权限

- `RECEIVE_BOOT_COMPLETED` — 监听开机广播
- `FOREGROUND_SERVICE` — 前台服务
- `FOREGROUND_SERVICE_SPECIAL_USE` — Android 14+ 特殊用途前台服务
- `QUERY_ALL_PACKAGES` — 查询所有已安装应用（Android 11+）

## 兼容性

- targetSdkVersion: 34 (Android 14)
- minSdkVersion: 24 (Android 7.0)
- Android 10+ 前台服务需显示通知
- Android 12+ `QUERY_ALL_PACKAGES` 需额外权限声明或限制可见应用范围
- 部分国产 ROM 可能杀掉后台广播，LauncherActivity 作为备用方案

## 应用启动方式

使用 `packageManager.getLaunchIntentForPackage(packageName)` + `FLAG_ACTIVITY_NEW_TASK` 启动目标应用。
