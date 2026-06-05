# MiTime

> **小米 / HyperOS 时间悬浮窗 — 一键快捷开关**

将系统深埋 4 步的「时间悬浮窗」开关，缩短至 **1 次点击**。

---

## 功能概览

| 功能 | 说明 |
|---|---|
| 🔘 主界面一键切换 | 启动即显示当前状态，点击按钮立即切换 |
| 🔔 通知栏快捷磁贴 | Quick Settings Tile，无需打开 App |
| 🪟 桌面小组件 | 支持 1×1 图标型 / 2×1 带文字型 Widget |
| 🔒 仅需一项权限 | 仅请求 `WRITE_SETTINGS`，无网络、无广告、无数据收集 |

---

## 背景与原理

### 为什么需要这个 App？

小米 / HyperOS 内置了「时间悬浮窗」功能，可将当前时间常驻显示在屏幕顶部。但其开关深藏于：

```
设置 → 更多设置 → 开发者选项 → 时间悬浮窗
```

每次操作至少需要 4 步。MiTime 将此操作缩短为 **1 次点击**。

### 技术原理

时间悬浮窗由系统 SystemUI 实现，其开关由一个隐藏的系统设置键控制：

```
Settings.System 键名：miui_time_floating_window
取值：1 = 开启 | 0 = 关闭
```

MiTime 通过标准 Android API `Settings.System.putInt()` 写入该键值，**无需 root，无需 ADB**，仅需用户授权一次「修改系统设置」权限。

**核心代码逻辑：**

```java
// 读取当前状态
Settings.System.getInt(contentResolver, "miui_time_floating_window", 0) == 1

// 开启
Settings.System.putInt(contentResolver, "miui_time_floating_window", 1)

// 关闭
Settings.System.putInt(contentResolver, "miui_time_floating_window", 0)
```

每次写入后立即 read-back 校验，确保状态真实反映。

> **已验证设备：** Xiaomi 23127PN0CC / Android 16 / SDK 36 / HyperOS OS3.0

---

## 权限说明

| 权限 | 用途 | 是否必需 |
|---|---|---|
| `WRITE_SETTINGS` | 写入系统设置键以控制悬浮窗 | ✅ 必需 |
| 网络权限 | — | ❌ 不声明 |
| 存储 / 位置 / 联系人 | — | ❌ 不声明 |

首次启动时，App 会引导用户前往系统授权界面完成一次性授权。

---

## 技术规格

| 项目 | 规格 |
|---|---|
| 包名 | `com.chitang.mitime` |
| minSdkVersion | 22 (Android 5.1) |
| targetSdkVersion | 22（兼容 HyperOS 私有设置写入路径） |
| compileSdkVersion | 35 |
| 安装包大小 | < 2 MB |
| 冷启动时间 | < 1 秒 |
| 网络访问 | 完全离线 |
| 后台服务 | 无常驻后台（TileService 由系统按需唤醒） |
| 支持设备 | 小米 / Redmi / POCO 运行 MIUI / HyperOS 的设备 |
| 主要语言 | 简体中文 |

> `targetSdkVersion = 22` 是有意为之：Android API 23+ 对 `WRITE_SETTINGS` 的访问限制更严格，
> 保持低 target 可沿用 HyperOS 对 `miui_time_floating_window` 的兼容写入路径。

---

## 项目结构

```
MiTime/
├── app/
│   ├── src/main/
│   │   ├── java/com/chitang/mitime/
│   │   │   ├── MainActivity.java           # 主界面 + UI 逻辑
│   │   │   ├── FloatingWindowHelper.java   # 核心开关逻辑（read / write / verify）
│   │   │   ├── MiTimeTileService.java      # 通知栏快捷磁贴（TileService）
│   │   │   ├── MiTimeWidget.java           # 桌面小组件（2×1 带文字型）
│   │   │   ├── MiTimeIconWidget.java       # 桌面小组件（1×1 图标型）
│   │   │   ├── AtmosphereScrollView.java   # 自定义视差滚动视图
│   │   │   └── PermissionHelper.java       # 权限检查与引导
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── xml/widget_info.xml
│   │   │   ├── xml/widget_icon_info.xml
│   │   │   └── drawable/                   # 图标与状态图
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── docs/
│   └── MiTime软件功能介绍.md               # 产品功能规格文档
├── build.ps1                               # PowerShell 构建脚本
└── build.bat                               # Batch 构建脚本
```

---

## 构建与安装

### 前置条件

- Android Studio 或已配置的 JDK + Android SDK 环境
- 已连接 Android 设备（用于调试安装）

### 调试构建并安装

```powershell
# PowerShell
.\build.ps1
```

```bat
:: Batch
build.bat
```

调试安装时，构建脚本会自动通过 ADB 授予 `WRITE_SETTINGS` 权限，无需手动操作：

```bash
adb shell appops set com.chitang.mitime WRITE_SETTINGS allow
```

### 手动 Gradle 构建

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 备选技术方案

如果在特定系统版本或安全策略下，普通 App 无法直接写入 `miui_time_floating_window`，按优先级依次尝试：

| 优先级 | 方案 | 说明 |
|---|---|---|
| 1 | **直接写入**（当前方案） | `Settings.System.putInt()` + read-back 校验 |
| 2 | **Shizuku** | 通过 Shizuku 以 shell UID 执行 `settings put system` 命令 |
| 3 | **Root** | root 环境下直接 shell 写入 |
| 4 | **ADB 引导** | 引导用户通过电脑 ADB 执行一次性授权 |
| 5 | **系统广播探索** | 探索 HyperOS 是否提供公开系统广播或 Content Provider 接口 |

> 注意：普通 App 进程 UID ≠ shell UID，直接在 App 内 `Runtime.exec("settings put ...")` 通常无效，不作为首选方案。

---

## 隐私声明

- ❌ 不收集任何用户数据
- ❌ 无埋点、无统计、无崩溃上报
- ❌ 无广告
- ❌ 无网络请求
- ✅ 完全离线运行

---

## License

MIT License — 详见 [LICENSE](LICENSE) 文件（如有）。
