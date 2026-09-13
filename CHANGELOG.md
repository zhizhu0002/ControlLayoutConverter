# 更新日志 (Changelog)

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，
版本号遵循语义化版本（SemVer）。

## [0.4] - 2026-09-13

### 性能

- 移除转换结果的 `JSONObject(...).toString()` 重序列化，引擎输出原样透传。修复由此带来的正确性问题：ZL2 颜色是 signed 64-bit 打包整数（如 `-9223372036854775808`），经 JSON 解析再序列化会被改写
- JSON 输入框改用惰性文本 API（`TextFieldState` + `lineLimits`）。此前 Miuix `TextField(value: String)` 每次输入都会重排整段文本，粘贴大布局明显卡顿
- 预览改为虚拟化列表：内容全量保留，只渲染可见行

### 体积

- `libcc.so` 在 APK 内改用 deflate 压缩存放（`useLegacyPackaging = true`）：release 包 5.68MB → 约 3.6MB。原生库二进制未改动（CRC-32 校验一致），安装后由系统解压到应用数据目录

### 横屏适配

- 导航栏由底部改为**左侧 `NavigationRail`**：图标在上、文字在下，垂直居中，去掉展开/收起按钮
- 主页改为左右双栏（左为格式与输入，右为输出结果），两栏独立滚动
- 关于页与第三方许可证页横屏限宽居中

### 视觉效果

- 导航栏毛玻璃改用官方 `miuix-blur`。因 `minSdk 26 < 33`（RuntimeShader 需 API 33），按官方文档以 `uses-sdk overrideLibrary` 放行合并，并用运行时能力检查门控全部 blur 代码路径，低版本回退为半透明底色
- 新增深浅色模式：**自动 / 浅色 / 深色**三档，位于「关于 → 主题」板块，标题「颜色」（Miuix `WindowDropdownMenu`），选择持久化
- 主题切换过渡改为叠加层淡出：动画帧只重绘、不再触发重组（原先逐帧动画调色板并重建 Miuix 配色，切换时会卡）

### 修复

- **切换页面或旋转后转换结果消失**：主页位于 `AnimatedContent` 内、旋转会重建 Activity，`remember` 状态随之丢失。新增进程级会话持有者保存输入、结果与各项选择（不放进 Bundle，避免大结果触发 `TransactionTooLargeException`）
- **显示 Miuix `WindowDialog` 时崩溃**（`IllegalStateException: No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner`）：Miuix 的 `DialogContentLayout` 会调用 `NavigationBackHandler`，旧版 `androidx.activity` 不提供该 owner。升级至 `androidx.activity` 1.12.0 解决
- **崩溃日志经常为空**：崩溃处理器原先用异步 `apply()` 写盘，进程先被终止导致日志丢失，改为同步 `commit()`

## [0.3] - 2026-08-28

### UI 全面重构

- **全量迁移至 [Miuix](https://github.com/YuKongA/miuix) 官方组件库**，移除全部手写组合控件：
  - 分段选择改用 `TabRowWithContour`：选中胶囊 200ms 滑动动画、浅色胶囊选中态、标签自动居中滚动，`ZL1/Pojav` 完整显示不再截断
  - 「在线转换」改用 `BasicComponent` + 官方 `Switch`，点击整行即可切换
  - 日志弹窗改用官方 `WindowDialog`
  - 信息页与许可页统一使用 `SmallTitle` / `BasicComponent` / `ArrowPreference`
- **新增「第三方开源项目」应用内页面**：
  - 固定顶栏（`SmallTopAppBar`）+ 返回键 / 系统返回手势均可退出
  - 按 [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) 分组展示（运行时库 / 组件库 / 原生转换库 / 在线服务）
  - 打开时页面从右滑入、底部导航栏向底部收回；返回时反向过渡
  - 页脚致谢 MobileGlues（可点击跳转仓库）
- 底部导航标签更名：**首页 → 主页**、**信息 → 关于**
- 主页 ↔ 关于、关于 → 许可页切换增加滑动过渡动画

### 布局与体验

- 首页整体紧凑化，一屏完整呈现（标题 / 格式选择 / 布局 / 开始转换）
- 内容安全区上沿扩展至**手机状态栏边界**，消除顶部空带；底部精确截止于导航栏细线
- 小字说明尽量单行显示（自动识别提示除外）
- JSON 粘贴框：占位文字居中显示，输入内容自适应换行并靠上排布，随行数自动增高
- 「在线转换」标题改为「在线转换（仅支持 ZL2、FCL 互转）」，单行小字号
- 卡片 hover / 按压高亮裁剪至圆角，不再出现方角溢出
- 关于页移除「图标设计」条目

### 其他

- 补充 `THIRD_PARTY_LICENSES.md` 第三方许可证文档
- 新增贡献指南并链接至 README

## [1.0] - 2026-08-28

### 首个公开版本

- FCL / ZL1(Pojav) / ZL2 控件布局 JSON 互转
- 原生转换引擎（libcc.so，基于 [NingZeStudio/control-converter](https://github.com/NingZeStudio/control-converter)）
- 可选在线转换接口（api.cc.miawa.cn，仅 FCL ↔ ZL2）
- 布局 JSON 文件选择、导出与重命名
- 转换失败 / 运行 / 崩溃日志记录与导出

[0.4]: https://github.com/zhizhu0002/ControlLayoutConverter/releases/tag/v0.4
[0.3]: https://github.com/zhizhu0002/ControlLayoutConverter/releases/tag/v0.3
[1.0]: https://github.com/zhizhu0002/ControlLayoutConverter/releases/tag/v1.0
