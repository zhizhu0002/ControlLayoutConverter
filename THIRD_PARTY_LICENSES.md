# Third-Party Licenses

**English** | [简体中文](THIRD_PARTY_LICENSES.zh-CN.md)

This project (ControlLayoutConverter) is licensed under the [MIT License](LICENSE) and uses the third-party open-source libraries and components listed below. We thank the authors of those projects.

> This file records license information for the third-party components that this project depends on directly, for compliance reference only. The authoritative text of each license is the LICENSE file in that project's own repository.

---

## Android / Compose runtime libraries

All of the following are published by JetBrains / AndroidX / Google under the **Apache License 2.0**:

| Component | License |
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

## Third-party UI component libraries

| Component | Version | License | Notes |
|---|---|---|---|
| [Miuix](https://github.com/YuKongA/miuix) (`top.yukonga.miuix.kmp:miuix-ui-android`, `miuix-preference-android`) | 0.9.3 | **Apache 2.0** | Compose Multiplatform UI library (provides this project's Miuix-style interface) |
| [Backdrop](https://github.com/Kyant0/backdrop) (`io.github.kyant0:backdrop`) | 2.0.1 | **Apache 2.0** | Compose frosted-glass / Liquid Glass effect |

---

## Native conversion library

| Component | Source | License | Notes |
|---|---|---|---|
| **libcc.so** | [NingZeStudio/control-converter](https://github.com/NingZeStudio/control-converter) | **MIT** | Native engine (Rust, via JNI) for FastCraft/FCL ↔ ZalithLauncher2 layout conversion, by NingZeStudio |

> This native library backs FCL ↔ ZL2 conversion in `com.tungsten.fcl.util.LayoutConverter` (the JNI wrapper) and `OfficialConverter`. Its copyright and license notice are retained as required by the MIT license (see the credits in the README).

---

## Online conversion service

| Service | Notes |
|---|---|
| [api.cc.miawa.cn](https://api.cc.miawa.cn) | Online conversion API (optional) |

---

## Notes

- This project **does not depend on any GPL/AGPL or other strongly copyleft-licensed components**, so licensing this project (including derivative code) under MIT is compatible.
- If you have questions about a particular component's license, refer to the LICENSE file in that component's own repository.
