# MiTime — 小米时间悬浮窗快捷开关

## 一、软件定位

**MiTime** 是一款专为小米 / HyperOS 设备设计的极简工具类 App。  
软件包名：`com.chitang.mitime`。  
它的唯一功能是：**快捷开启 / 关闭小米系统内置的"时间悬浮窗"**。

> 系统原生路径：设置 → 更多设置 → 开发者选项 → 时间悬浮窗（操作路径深，每次需 4 步以上）
>
> MiTime 将此操作缩短至 **1 次点击**。

---

## 二、功能背景与技术原理

### 2.1 小米时间悬浮窗的系统机制

小米 / HyperOS 系统内置了一个可以将当前时间显示在屏幕上方的悬浮窗功能。该功能由系统 SystemUI 实现，**并非普通 Android 悬浮窗**，不依赖 `SYSTEM_ALERT_WINDOW` 权限。

其开关由一个隐藏的系统设置键控制：

```
Settings.System 键名：miui_time_floating_window
取值：1 = 开启 | 0 = 关闭
```

已在以下设备上通过 ADB 验证有效：

- Xiaomi 23127PN0CC / Android 16 / SDK 36 / HyperOS OS3.0

### 2.2 App 控制原理

MiTime 通过 Android 标准 API `Settings.System.putInt()` 写入上述键值，即可触发系统悬浮窗的显示/隐藏，**无需 root，无需 ADB**，仅需用户授权"修改系统设置"权限（`WRITE_SETTINGS`）。

**读取当前状态：**
```java
Settings.System.getInt(contentResolver, "miui_time_floating_window", 0) == 1
```

**开启：**
```java
Settings.System.putInt(contentResolver, "miui_time_floating_window", 1)
```

**关闭：**
```java
Settings.System.putInt(contentResolver, "miui_time_floating_window", 0)
```

**权限检查与引导：**
```java
if (!Settings.System.canWrite(context)) {
    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
    intent.setData(Uri.parse("package:" + context.getPackageName()));
    startActivity(intent);
}
```

### 2.3 关键技术约束

| 项目 | 说明 |
|---|---|
| 所需权限 | `WRITE_SETTINGS`（修改系统设置） |
| 最低 SDK | minSdkVersion = 23 |
| 目标 SDK | targetSdkVersion = 25（兼容 Android 16 / HyperOS 3 安装） |
| 编译 SDK | compileSdkVersion = 35 或更高 |
| 网络权限 | 不需要，App 完全离线 |
| 不需要的权限 | `SYSTEM_ALERT_WINDOW`、位置、存储、联系人等一切无关权限 |

> **注意**：targetSdkVersion 设置为 25 是为了绕过 Android 14+ 对低目标版本 App 的限制，同时维持对隐藏系统设置键的写入能力。后续如系统策略变化，备选方案详见第六章。

---

## 三、核心功能需求

### 3.1 主界面功能

- **状态感知**：App 启动时自动读取 `miui_time_floating_window` 的当前值，显示真实的开启/关闭状态
- **一键切换**：点击主开关按钮，即可切换时间悬浮窗的开启/关闭状态
- **写入验证**：每次写入后必须立即 read-back 校验实际值，用 UI 反馈真实结果（成功/失败）
- **权限引导**：首次启动或检测到无 `WRITE_SETTINGS` 权限时，自动弹出引导页，跳转系统授权界面

### 3.2 通知栏快捷开关（Quick Settings Tile）

- 在系统通知栏/快捷设置区域提供 MiTime 专属磁贴
- 磁贴实时显示当前状态（开/关），点击即可切换，**无需打开 App 主界面**
- 磁贴图标随状态变化（激活/未激活两种样式）
- 实现方式：继承 `TileService`，在 `onTileAdded` 和 `onStartListening` 中同步状态

### 3.3 桌面小组件（App Widget）

- 提供可添加到手机桌面的小组件（Widget）
- 小组件上显示当前时间悬浮窗状态，并提供点击切换按钮
- 支持多种尺寸（至少 1×1 图标型，推荐支持 2×1 带文字型）
- 实现方式：继承 `AppWidgetProvider`，通过 `RemoteViews` + `PendingIntent` 响应点击

---

## 四、界面设计要求

### 4.1 设计风格

- 整体风格：**现代极简 + 小米 Material You / HyperOS 设计语言**
- 配色：以深色/暗色调为主，搭配单一强调色（推荐深蓝/橙色/绿色三选一）
- 字体：使用系统字体（Roboto 或设备默认中文字体），字号层级清晰
- 无多余装饰，界面干净，操作一目了然

### 4.2 主界面布局

```
┌─────────────────────────┐
│   MiTime                │  ← App 名称/Logo（顶部居中或左上角）
│                         │
│   ┌─────────────────┐   │
│   │  时间悬浮窗      │   │
│   │  ● 当前：已开启  │   │  ← 实时状态显示
│   └─────────────────┘   │
│                         │
│       [ 关闭 / 开启 ]   │  ← 主切换按钮（大号，居中，状态联动文字）
│                         │
│   提示：已获取系统权限   │  ← 权限状态提示（小字，底部）
└─────────────────────────┘
```

### 4.3 交互细节

- 切换动画：按钮点击时有轻微缩放/涟漪反馈
- 状态变化时图标/颜色平滑过渡（不超过 300ms）
- 写入失败时显示 Snackbar / Toast 提示，引导用户检查权限
- 首次启动有简短引导（一屏，说明唯一功能及所需权限）

---

## 五、非功能性要求

| 项目 | 要求 |
|---|---|
| 包名 | `com.chitang.mitime`（全新包名，与原版 `com.miui.mitime` 区分） |
| 网络访问 | **完全无网络请求**，Manifest 中不声明网络权限 |
| 数据收集 | **不收集任何用户数据**，无埋点，无统计 |
| 广告 | **无广告** |
| 后台服务 | 无常驻后台服务（TileService 由系统按需唤醒） |
| 安装包大小 | 目标 < 2 MB |
| 启动速度 | 冷启动 < 1 秒 |
| 支持设备 | 小米 / Redmi / POCO 运行 MIUI / HyperOS 的设备 |
| 最低系统版本 | Android 6.0 (API 23) |
| 语言 | 简体中文（主语言） |

---

## 六、备选技术方案（当直接写入不可用时）

如果在某些系统版本或安全策略下，普通 App 无法直接通过 `Settings.System.putInt()` 写入 `miui_time_floating_window`，按优先级依次尝试以下备选方案：

| 优先级 | 方案 | 说明 |
|---|---|---|
| 1 | 直接写入（首选） | `Settings.System.putInt(...)` + read-back 校验 |
| 2 | Shizuku 方案 | 通过 Shizuku 以 shell UID 执行 `settings put system miui_time_floating_window 1/0` |
| 3 | Root 方案 | root 环境下直接 shell 写入 |
| 4 | ADB 引导 | 引导用户通过电脑 ADB 执行一次性授权或命令 |
| 5 | 查找系统广播 | 探索 HyperOS 是否提供公开系统广播或 Content Provider 接口 |

> 普通 App 进程 UID 不等于 shell UID，故直接在 App 内执行 `Runtime.exec("settings put ...")` 通常无效，**不作为首选方案**。

---

## 七、项目结构规划（全新开发）

```
MiTime/
├── app/
│   ├── src/main/
│   │   ├── java/com/chitang/mitime/
│   │   │   ├── MainActivity.java          # 主界面
│   │   │   ├── FloatingWindowHelper.java  # 核心开关逻辑（read/write/verify）
│   │   │   ├── MiTimeTileService.java     # 通知栏快捷磁贴
│   │   │   ├── MiTimeWidget.java          # 桌面小组件
│   │   │   └── PermissionHelper.java      # 权限检查与引导
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── xml/tile_service.xml
│   │   │   ├── xml/widget_info.xml
│   │   │   └── drawable/（图标和状态图）
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── docs/
│   ├── TECHNICAL_NOTES.md                # 技术研究笔记（现有）
│   └── MiTime软件功能介绍.md             # 本文档
└── build.ps1                             # 构建脚本
```

---

## 八、开发里程碑

| 阶段 | 内容 | 目标 |
|---|---|---|
| M1 | 核心功能 MVP | 主界面 + 开关按钮 + WRITE_SETTINGS 权限引导 + read-back 校验 |
| M2 | 通知栏磁贴 | 实现 TileService，支持通知栏一键切换 |
| M3 | 桌面小组件 | 实现 AppWidgetProvider，支持 1×1 和 2×1 桌面组件 |
| M4 | UI 打磨 | 动画、深色模式、引导页、图标设计 |
| M5 | 兼容性测试 | 多机型 / 多系统版本测试，必要时集成 Shizuku 备选方案 |
