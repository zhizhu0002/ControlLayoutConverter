# ControlLayoutConverter

手机 **Minecraft Java 版启动器**的控件布局转换工具。用于在不同启动器使用的控件布局 JSON 之间互转，方便直接套用别人做好的按键布局。

支持三种启动器的布局格式：**FoldCraftLauncher**、**ZalithLauncher**、**ZalithLauncher2**。

- **FoldCraftLauncher** — `viewGroups` 结构
- **ZalithLauncher** — `mControlDataList` 结构（含 Pojav 布局）
- **ZalithLauncher2** — `layers` 结构

---

## 功能

- **FoldCraftLauncher ↔ ZalithLauncher2** 互转（libcc 原生引擎，最快）
- **ZalithLauncher ↔ ZalithLauncher2** 互转（WebView JS 引擎）
- **FoldCraftLauncher ↔ ZalithLauncher** 互转（经 ZalithLauncher2 中转）
- 输入格式**自动识别**
- 在线转换（可选，FoldCraftLauncher↔ZalithLauncher2），失败自动回退本地引擎
- Compose + Miuix UI，适配暗色模式

---

## 转换引擎

按优先级自动路由，某一级失败会回退到下一级：

| 方向 | 首选 | 回退 |
|---|---|---|
| FoldCraftLauncher → ZalithLauncher2 | libcc 原生 | WebView JS |
| ZalithLauncher2 → FoldCraftLauncher | libcc 原生 | WebView JS |
| ZalithLauncher ↔ ZalithLauncher2 | WebView JS | — |
| FoldCraftLauncher → ZalithLauncher | libcc + JS 链 | 全 JS 链 |
| ZalithLauncher → FoldCraftLauncher | JS + libcc 链 | 全 JS 链 |

- **libcc 原生引擎**：C++ 编译的原生库，经 JNI 接入，性能最优。
- **WebView JS 引擎**：WebView 内加载 JS 转换器，作为兜底覆盖所有方向。
- **在线转换**：可选，调用 `api.cc.miawa.cn` 接口，失败时回退本地引擎。

---

## 构建

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需本地配置 release.keystore）
./gradlew assembleRelease
```

APK 输出在 `app/build/outputs/apk/`。

需要 **JDK 17**、**Gradle 9.3.1**（由 wrapper 提供）、**AGP 9.1.1 / Kotlin 2.4.0**，仅支持 **arm64-v8a**。

> `local.properties`（本机 SDK 路径）与 `release.keystore`（签名证书）已被 `.gitignore` 忽略，克隆后请自行配置。

---

## 许可证

[MIT](LICENSE)

---

## 致谢

- [FoldCraftLauncher](https://github.com/FCL-Team/FoldCraftLauncher)（FCL 布局）
- [ZalithLauncher](https://github.com/ZalithLauncher/ZalithLauncher)（ZL1 布局）
- [ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2)（ZL2 布局）
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)（Pojav 布局）
- [NingZeStudio/control-converter](https://github.com/NingZeStudio/control-converter)（libcc 原生引擎）
- [miuix](https://github.com/YuKongA/miuix)（Compose UI 组件库）
- [api.cc.miawa.cn](https://api.cc.miawa.cn)（在线转换接口）
