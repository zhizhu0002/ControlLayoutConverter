# 更新日志 (Changelog)

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，
版本号遵循语义化版本（SemVer）。

## [0.6] - 2026-09-19

### ZL1 转换修复

- **摇杆尺寸偏小**：ZL2→ZL1 的摇杆尺寸原先按 `sizePercentage / 100` 计算，2500/5000 只得到 50dp，正确应为 90dp/180dp；且 50dp 下限把小摇杆一律压平。现按屏幕高度占比换算，`sizeType` 为 dp 时直接取 `sizeDp`
- **摇杆方向键被丢弃**：原先固定输出 WASD，自定义方向键的摇杆转换后一律变成 WASD。现从方向事件还原真实键码
- **抽屉把手丢失位置与尺寸**：真实 ZL2 布局的抽屉把手缺少 `dynamicX` / `width` 等几何字段，在 ZL1 里会全部退化并叠在同一处。现按把手按钮还原几何；ZL2 独立叠加层没有把手按钮时，在右上角错开补齐可点把手
- 转换校验补强：抽屉把手必须有几何信息、摇杆数量不得下降

### 界面

- 横屏左右两栏（导入设置 / 导出结果）之间新增分割线
- 横屏侧边导航栏新增右侧分割线

### 构建

- debug 包改为独立包名 `com.zhizhu.controlconverter.debug`、名称「FCL ZL 控件转换器 Debug」，可与正式版同机并存

### 说明

- ZL1 的抽屉结构（`mDrawerDataList`）只有 `buttonProperties`，无法表达「抽屉内的摇杆」。ZL2→ZL1 时抽屉层里的摇杆会并入主屏摇杆列表，这是 ZL1 格式本身的能力限制

## [0.5] - 2026-09-19

### 转换引擎

- **libcc.so 同步至上游 Rust 重写版**（[NingZeStudio/control-converter](https://github.com/NingZeStudio/control-converter)，原为 Go 版），JNI 接口不变
- **FCL → ZL2 移除 JS 旁路**：原先遇到 BUTTON 样式方向控件会绕道 WebView JS 引擎，现统一由原生引擎转换，方向控件转为 ZL2 摇杆

### 体积

- 原生库 4.33MB → 0.89MB；Debug 包 22.98MB → 14.69MB

### 文档

- 引擎描述更正：`C++` / `Go JNI` → `Rust`

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

[0.6]: https://github.com/zhizhu0002/ControlLayoutConverter/releases/tag/v0.6
[0.5]: https://github.com/zhizhu0002/ControlLayoutConverter/releases/tag/v0.5
[0.4]: https://github.com/zhizhu0002/ControlLayoutConverter/releases/tag/v0.4
[0.3]: https://github.com/zhizhu0002/ControlLayoutConverter/releases/tag/v0.3
[1.0]: https://github.com/zhizhu0002/ControlLayoutConverter/releases/tag/v1.0
