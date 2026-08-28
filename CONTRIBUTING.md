# 贡献指南 Contributing

感谢你对 **ControlLayoutConverter** 的关注与贡献！本指南说明如何参与到项目开发中。

## 贡献方式

本仓库采用分级协作模式：

| 角色 | 权限 | 如何贡献 |
|---|---|---|
| **核心协作者**（Collaborator） | 直接 push | 由仓库所有者邀请加入，可直接推送到 `main` |
| **外部贡献者** | 只读 | 通过 **Fork + Pull Request** 提交改动 |

---

## 一、核心协作者（Collaborator）

- 由仓库所有者（`zhizhu0002`）添加为 **Collaborator** 后即可直接 `git push`。
- 建议在 `main` 分支上保持**小步提交**，提交信息清晰说明改动内容。
- 涉及重大改动前，尽量先在 feature 分支上进行并同步给其他协作者。

---

## 二、外部贡献者（Fork + Pull Request）

### 1. Fork 仓库
点击仓库右上角的 **Fork** 按钮，把项目复制到你的账号下。

### 2. 克隆并创建分支
```bash
git clone https://github.com/<你的用户名>/ControlLayoutConverter.git
cd ControlLayoutConverter
git checkout -b feature/my-change
```

### 3. 进行改动
修改代码，并保持提交信息简洁、描述清晰：
```bash
git add .
git commit -m "describe your change here"
```

### 4. 推送并提交 Pull Request
```bash
git push origin feature/my-change
```
然后在 GitHub 上点击 **Contribute → Open pull request**，提交到上游的 `main` 分支。

### 5. 等待审核与合并
维护者会 review 你的 PR。若 CI 检查或不一致需要调整，请根据反馈修改并在同一分支继续 push。

---

## 三、环境与构建

### 环境要求
- **JDK 17**
- **Android SDK**（compileSdk 37, targetSdk 36, minSdk 26）
- **Gradle 9.3.1**（由 `gradlew` wrapper 提供）
- **AGP 9.1.1 / Kotlin 2.4.0**
- 仅支持 **arm64-v8a** ABI

> `local.properties`（本机 SDK 路径）与 `release.keystore`（签名证书）已被 `.gitignore` 忽略，克隆后请自行配置本机环境。

### 构建
```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK（需本地签名证书）
```

APK 输出在 `app/build/outputs/apk/`。

---

## 四、代码规范

- **语言**：Kotlin（Compose + Miuix UI）、Java（JNI 封装）、JS（WebView 转换引擎）
- 保持现有代码风格与命名约定。
- 涉及转换逻辑的改动，请确保控件数量守恒（避免破坏 1:1 转换）。
- 提交前请尽量本地跑一次 `assembleDebug` 确认编译通过。

---

## 五、提交信息规范

建议使用简洁、面向改动的描述，例如：
- `Fix: correct ZL2 to FCL color conversion`
- `Feat: add ZL1 joystick direction support`
- `Docs: update README build instructions`

---

## 六、许可

本项目基于 [MIT License](LICENSE) 开源。提交贡献即表示你同意将该贡献以 MIT 许可授权给本项目。
