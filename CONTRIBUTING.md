# Contributing

**English** | [简体中文](CONTRIBUTING.zh-CN.md)

Thanks for your interest in **ControlLayoutConverter**! This guide explains how to get involved.

## Ways to contribute

This repository uses a two-tier collaboration model:

| Role | Access | How to contribute |
|---|---|---|
| **Core collaborator** | Direct push | Invited by the repository owner; may push straight to `main` |
| **External contributor** | Read-only | Submit changes via **Fork + Pull Request** |

---

## 1. Core collaborators

- Once the repository owner (`zhizhu0002`) adds you as a **collaborator**, you can `git push` directly.
- Keep commits **small** on `main`, with messages that clearly describe the change.
- For significant changes, prefer working on a feature branch first and syncing with the other collaborators.

---

## 2. External contributors (Fork + Pull Request)

### 1. Fork the repository
Click **Fork** in the top-right corner to copy the project into your own account.

### 2. Clone and create a branch
```bash
git clone https://github.com/<your-username>/ControlLayoutConverter.git
cd ControlLayoutConverter
git checkout -b feature/my-change
```

### 3. Make your changes
Edit the code, and keep commit messages short and clear:
```bash
git add .
git commit -m "describe your change here"
```

### 4. Push and open a Pull Request
```bash
git push origin feature/my-change
```
Then on GitHub click **Contribute → Open pull request**, targeting the upstream `main` branch.

### 5. Review and merge
A maintainer will review your PR. If CI checks or review feedback call for adjustments, push further commits to the same branch.

---

## 3. Environment and build

### Requirements
- **JDK 17**
- **Android SDK** (compileSdk 37, targetSdk 36, minSdk 26)
- **Gradle 9.3.1** (provided by the `gradlew` wrapper)
- **AGP 9.1.1 / Kotlin 2.4.0**
- **arm64-v8a** ABI only

> `local.properties` (your local SDK path) and `release.keystore` (signing certificate) are ignored by `.gitignore` — configure them yourself after cloning.

### Build
```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK (requires a local signing certificate)
```

APKs are written to `app/build/outputs/apk/`.

---

## 4. Code conventions

- **Languages**: Kotlin (Compose + Miuix UI), Java (JNI wrappers), JS (WebView conversion engine)
- Follow the existing code style and naming conventions.
- For changes to conversion logic, make sure the control count is conserved (don't break 1:1 conversion).
- Before submitting, please run `assembleDebug` locally to confirm it compiles.

---

## 5. Commit message conventions

Short, change-oriented descriptions are preferred, for example:
- `Fix: correct ZL2 to FCL color conversion`
- `Feat: add ZL1 joystick direction support`
- `Docs: update README build instructions`

---

## 6. License

This project is released under the [MIT License](LICENSE). By submitting a contribution, you agree to license that contribution to this project under the MIT license.
