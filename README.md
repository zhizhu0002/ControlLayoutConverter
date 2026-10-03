# ControlLayoutConverter

**English** | [简体中文](README.zh-CN.md)

A control-layout converter for mobile **Minecraft: Java Edition launchers**. It converts control-layout JSON between the formats used by different launchers, so you can apply a key layout that someone else has already made.

Three launcher layout formats are supported: **FoldCraftLauncher**, **ZalithLauncher**, and **ZalithLauncher2**.

- **FoldCraftLauncher** — `viewGroups` structure
- **ZalithLauncher** — `mControlDataList` structure (includes Pojav layouts)
- **ZalithLauncher2** — `layers` structure

---

## Features

- **FoldCraftLauncher ↔ ZalithLauncher2** (libcc native engine; fastest)
- **ZalithLauncher ↔ ZalithLauncher2** (WebView JS engine)
- **FoldCraftLauncher ↔ ZalithLauncher** (routed through ZalithLauncher2)
- Automatic input-format detection
- Optional online conversion (FoldCraftLauncher ↔ ZalithLauncher2), falling back to the local engine on failure
- Compose + Miuix UI with dark-mode support
- Chinese and English interface

---

## Conversion engines

Each conversion is routed by priority; if one engine fails, it falls back to the next.

| Direction | Preferred | Fallback |
|---|---|---|
| FoldCraftLauncher → ZalithLauncher2 | libcc native | WebView JS |
| ZalithLauncher2 → FoldCraftLauncher | libcc native | WebView JS |
| ZalithLauncher ↔ ZalithLauncher2 | WebView JS | — |
| FoldCraftLauncher → ZalithLauncher | libcc + JS chain | full JS chain |
| ZalithLauncher → FoldCraftLauncher | JS + libcc chain | full JS chain |

- **libcc native engine** — a native library written in Rust and exposed through JNI; the fastest path.
- **WebView JS engine** — loads the JS converter inside a WebView; acts as the fallback and covers every direction.
- **Online conversion** — optional; calls the `api.cc.miawa.cn` API and falls back to the local engine on failure.

---

## Build

```bash
# Build the debug APK
./gradlew assembleDebug

# Build the release APK (requires a local release.keystore)
./gradlew assembleRelease
```

APKs are written to `app/build/outputs/apk/`.

Requires **JDK 17**, **Gradle 9.3.1** (provided by the wrapper) and **AGP 9.1.1 / Kotlin 2.4.0**. Only the **arm64-v8a** ABI is supported.

> `local.properties` (your local SDK path) and `release.keystore` (the signing certificate) are ignored by `.gitignore`; configure your own after cloning.

---

## Contributing

Contributions are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).


---

## License

This project is released under the [MIT](LICENSE) license. Third-party dependency licenses are listed in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

---

## Credits

- [FoldCraftLauncher](https://github.com/FCL-Team/FoldCraftLauncher) (FCL layout format)
- [ZalithLauncher](https://github.com/ZalithLauncher/ZalithLauncher) (ZL1 layout format)
- [ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2) (ZL2 layout format)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher) (Pojav layout format)
- [NingZeStudio/control-converter](https://github.com/NingZeStudio/control-converter) (libcc native engine)
- [miuix](https://github.com/YuKongA/miuix) (Compose UI component library)
- [api.cc.miawa.cn](https://api.cc.miawa.cn) (online conversion API)
