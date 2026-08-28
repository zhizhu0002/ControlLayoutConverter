# ControlLayoutConverter

手机 **Minecraft Java版启动器**的控件布局转换工具。用于在不同启动器使用的控件布局 JSON 之间互转。

支持三种格式：**FCL**、**ZL1**、**ZL2**。

- **FCL** — `viewGroups` 结构（FoldCraftLauncher等）
- **ZL1** — `mControlDataList` 结构（ZalithLauncher）
- **ZL2** — `layers` 结构（ZalithLauncher2）

---

## 功能

- **FCL ↔ ZL2** 互转（libcc 原生引擎，最快）
- **ZL1 ↔ ZL2** 互转（WebView JS 引擎）
- **FCL ↔ ZL1** 互转（经 ZL2 中转）
- 输入格式**自动识别**
- 在线转换（可选，FCL↔ZL2），失败自动回退本地引擎
- Compose + Miuix UI

---

## 转换引擎

按优先级自动路由，某一级失败会回退到下一级：

| 方向 | 首选 | 回退 |
|---|---|---|
| FCL → ZL2 | libcc 原生 | WebView JS |
| ZL2 → FCL | libcc 原生 | WebView JS |
| ZL1 ↔ ZL2 | WebView JS | — |
| FCL → ZL1 | libcc + JS 链 | 全 JS 链 |
| ZL1 → FCL | JS + libcc 链 | 全 JS 链 |

- **libcc 原生引擎**：C++ 编译的原生库，经 JNI 接入，性能最优。
- **WebView JS 引擎**：WebView 内加载 JS 转换器，作为兜底覆盖所有方向。
- **在线转换**：可选，调用 `api.cc.miawa.cn` 接口，失败时回退本地引擎。

---

## 构建

```bash
./gradlew assembleDebug   # Debug APK
./gradlew assembleRelease # Release APK（需本地签名证书）
```

APK 输出在 `app/build/outputs/apk/`。

需要 **JDK 17**、**Gradle 9.3.1**（由 wrapper 提供）、**AGP 9.1.1 / Kotlin 2.4.0**，仅支持 **arm64-v8a**。

> `local.properties`（本机 SDK 路径）与 `release.keystore`（签名证书）已被 `.gitignore` 忽略，克隆后请自行配置。

---

## 项目结构

```
app/
├── src/main/
│   ├── assets/index.html          # WebView JS 转换引擎
│   ├── java/com/tungsten/fcl/util/
│   │   └── LayoutConverter.java   # libcc JNI 封装
│   ├── java/com/zhizhu/controlconverter/
│   │   ├── MainActivity.kt        # 主界面 + 转换分派
│   │   ├── OfficialConverter.kt   # 原生转换封装
│   │   ├── AppStartProbe.kt
│   │   └── CrashLogStore.kt       # 崩溃日志
│   └── jniLibs/arm64-v8a/libcc.so # 原生转换库
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/wrapper/
```

---

## 许可证

[MIT](LICENSE)
