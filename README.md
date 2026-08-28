# ControlLayoutConverter

一个针对 **Minecraft 基岩版（PojavLauncher / FCL / 游侠）使用的控件布局 JSON** 的格式转换工具。支持在 **FCL（FoldCraftLauncher）**、**ZL1（游侠旧版）**、**ZL2（游侠新版 / 控制布局）** 三种布局格式之间相互转换。

> 原项目名：`ControlLayoutConverter`（包 `com.zhizhu.controlconverter`）。早期阶段名称为 `com.iqge.controlconverter`。

---

## ✨ 功能特性

- ✅ **FCL ↔ ZL2 互转**（libcc 原生引擎，最快最稳）
- ✅ **ZL1 ↔ ZL2 互转**（WebView JS 引擎）
- ✅ **FCL ↔ ZL1 互转**（经 ZL2 中转的链式转换）
- ✅ **在线转换**（可开关，FCL↔ZL2 调用 cc.miawa.cn 在线接口，失败自动回退本地引擎）
- ✅ **输入格式自动识别**（FCL / ZL1 / Pojav / ZL2）
- ✅ **1:1 控件转换**（结构/控件数守恒，Kotlin 引擎内建守恒自检）
- ✅ **Compose + Miuix 现代 UI**（暗色模式适配）
- ✅ **复制结果 / 保存为 JSON 文件**
- ✅ **崩溃日志记录**（内置 `CrashLogStore`）

---

## 🗂 支持的格式

| 格式 | 说明 | 典型来源 |
|---|---|---|
| **FCL** | `viewGroups` 结构 | FoldCraftLauncher / Pojav 新版控件布局 |
| **ZL1** | `mControlDataList` 结构 | 游侠旧版（Zeppelin / Zalp） |
| **ZL2** | `layers` 结构 | 游侠新版（控制布局 v21） |

---

## 🔁 转换引擎与优先级

项目内置了多种转换引擎，按优先级自动路由，**某一级失败会自动回退到下一级**：

| 方向 | 首选引擎 | 回退引擎 |
|---|---|---|
| **FCL → ZL2** | libcc 原生（`libcc.so`） | WebView JS |
| **ZL2 → FCL** | libcc 原生 | WebView JS |
| **ZL1 ↔ ZL2** | WebView JS（`migrateLayout` / `zl2ToZl1`） | — |
| **FCL → ZL1** | FCL→ZL2(libcc) → ZL2→ZL1(JS) | 全 JS 链 |
| **ZL1 → FCL** | ZL1→ZL2(JS) → ZL2→FCL(libcc) | 全 JS 链 |

### 各引擎说明

- **libcc 原生引擎**（`LayoutConverter.java` + `jniLibs/arm64-v8a/libcc.so`）
  由 C++ 编译的原生库，通过 JNI 接入，性能最优，用于 FCL↔ZL2 直接转换。

- **WebView JS 引擎**（`assets/index.html`）
  WebView 内加载一份 JS 转换器（`ControlConverter` / `ZL1ToZL2`），覆盖所有方向的转换，是 libcc 失败时的兜底。

- **在线转换**（可选，`useOnline`）
  FCL↔ZL2 时可调用 `cc.miawa.cn` 在线接口，失败回退到本地引擎。

---

## 📁 项目结构

```
controlconverter/
├── app/
│   ├── build.gradle.kts          # 模块构建配置（Compose / Miuix / JNI）
│   ├── proguard-rules.pro        # R8 混淆规则（保留 JNI / Bridge / Activity）
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── index.html        # WebView JS 转换引擎
│       ├── java/
│       │   ├── com/tungsten/fcl/util/LayoutConverter.java   # libcc JNI 封装
│       │   └── com/zhizhu/controlconverter/
│       │       ├── MainActivity.kt     # 主界面 + 转换分派逻辑
│       │       ├── OfficialConverter.kt # libcc 原生转换封装
│       │       ├── AppStartProbe.kt
│       │       └── CrashLogStore.kt    # 崩溃日志存储
│       ├── jniLibs/
│       │   └── arm64-v8a/libcc.so      # 原生转换库
│       └── res/                        # 资源（含暗色主题）
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/                     # Gradle 9.3.1 wrapper
└── release.keystore                    # 本地签名证书（已被 .gitignore 忽略）
```

---

## 🛠 环境要求

- **Android Studio / Android SDK**（compileSdk 37, targetSdk 36, minSdk 26）
- **JDK 17**
- **Gradle 9.3.1**（由 wrapper 提供）
- **AGP 9.1.1 · Kotlin 2.4.0**
- 仅支持 **arm64-v8a** ABI（原生 libcc.so）

> ⚠️ 项目中 `.gitignore` 已忽略 `local.properties`（本机 SDK 路径）与 `release.keystore`（签名证书）。克隆后请自行配置 `local.properties`（指向你的 Android SDK）与本地签名证书。

---

## 🔨 构建

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需本地配置 release.keystore）
./gradlew assembleRelease
```

生成的 APK 位于 `app/build/outputs/apk/`。

---

## 📄 许可证

本项目基于 **MIT License** 开源。详见 [LICENSE](LICENSE)。

---

## 🙏 致谢

- [FoldCraftLauncher](https://github.com/FoldCraftLauncher)（FCL 布局）
- 游侠 / ZL 布局格式
- [Miuix](https://github.com/YuKongA)（Compose UI 组件库）
- cc.miawa.cn（在线转换接口）
