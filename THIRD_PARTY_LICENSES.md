# Third-Party Licenses

本项目（ControlLayoutConverter）基于 [MIT License](LICENSE) 授权，同时使用了以下第三方开源库/组件。感谢这些项目的作者。

> 本文件列出本项目直接依赖的第三方组件的许可证信息，仅供合规参考。各许可证的完整文本以各项目仓库中的 LICENSE 为准。

---

## Android / Compose 运行时库

以下库均来自 JetBrains / AndroidX / Google 官方，采用 **Apache License 2.0**：

| 组件 | 许可证 |
|---|---|
| androidx.compose:compose-bom | Apache 2.0 |
| androidx.compose.ui:ui | Apache 2.0 |
| androidx.compose.ui:ui-tooling-preview | Apache 2.0 |
| androidx.compose.ui:ui-tooling | Apache 2.0 |
| androidx.compose.foundation:foundation | Apache 2.0 |
| androidx.compose.material3:material3 | Apache 2.0 |
| androidx.activity:activity-compose | Apache 2.0 |
| androidx.core:core-ktx | Apache 2.0 |

---

## 第三方 UI 组件库

| 组件 | 版本 | 许可证 | 说明 |
|---|---|---|---|
| [Miuix](https://github.com/YuKongA/miuix) (`top.yukonga.miuix.kmp:miuix-ui-android`, `miuix-preference-android`) | 0.9.3 | **Apache 2.0** | Compose MultiPlatform UI 库（本项目的 Miuix 风格界面） |
| [Backdrop](https://github.com/Kyant0/backdrop) (`io.github.kyant0:backdrop`) | 2.0.1 | **Apache 2.0** | Compose 毛玻璃/Liquid Glass 效果 |

---

## 原生转换库

| 组件 | 来源 | 许可证 | 说明 |
|---|---|---|---|
| **libcc.so** | [NingZeStudio/control-converter](https://github.com/NingZeStudio/control-converter) | **MIT** | FastCraft/FCL ↔ ZalithLauncher2 布局转换的原生引擎（Go JNI），作者 NingZeStudio |

> 该原生库用于 `com.tungsten.fcl.util.LayoutConverter`（JNI 封装）与 `OfficialConverter` 的 FCL↔ZL2 转换。按 MIT 许可要求保留其版权与许可声明（见 README 致谢）。

---

## 在线转换服务

| 服务 | 说明 |
|---|---|
| [api.cc.miawa.cn](https://api.cc.miawa.cn) | 在线转换接口（可选使用） |

---

## 说明

- 本项目**未引入 GPL/AGPL 等强传染性许可**的依赖，因此以 MIT 授权本项目（含衍生代码）是兼容的。
- 若你对某一组件的许可证有疑问，请以该组件对应仓库的 LICENSE 文件为准。
