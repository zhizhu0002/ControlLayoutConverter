package com.zhizhu.controlconverter

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/**
 * All user-facing copy for one language, in one place.
 *
 * The app's actual data/tokens (format names like "FCL"/"ZL1"/"ZL2", the theme-mode
 * persistence key, etc.) never live here — only what a person reads on screen. That keeps
 * the toggle in [InfoTab] purely cosmetic: switching language never changes what gets
 * converted or how a layout is saved.
 */
data class Strings(
    val code: String,

    // ---- Navigation ----
    val navHome: String,
    val navAbout: String,

    // ---- Home / conversion screen ----
    val appTitle: String,
    val appSubtitle: String,
    val statusReady: String,
    val statusProcessingPrefix: String,

    val formatSelection: String,
    val inputFormatLabel: String,
    val outputFormatLabel: String,
    val formatAuto: String,
    val onlineToggleTitle: String,
    val onlineToggleSummary: String,
    val autoHint: String,

    val layoutSection: String,
    val tabPaste: String,
    val tabChooseFile: String,
    val layoutNameLabel: String,
    val pasteHint: String,
    val chooseFileButton: String,

    val convertButton: String,
    val convertingButton: String,

    val outputResultSection: String,
    val resultTitleLabel: String,
    val exportButton: String,
    val copyButton: String,
    val expandResult: String,
    val collapseResult: String,
    val generatingPreview: String,

    val readingFile: String,
    val noReadableFile: String,
    val converting: String,
    val convertInvalidJson: String,
    val exporting: String,
    val noExportableResult: String,
    val cannotOpenTarget: String,
    val resultCopiedStatus: String,
    val resultCopiedToast: String,
    val clipLabelResult: String,

    // ---- About screen ----
    val aboutTitle: String,
    val appSection: String,
    val versionLabel: String,
    val githubPageTitle: String,
    val themeSection: String,
    val colorLabel: String,
    val themeAuto: String,
    val themeLight: String,
    val themeDark: String,
    val languageSection: String,
    val languageLabel: String,
    val languageZh: String,
    val languageEn: String,
    val aboutSection: String,
    val developerLabel: String,
    val licenseLabel: String,
    val thirdPartyTitle: String,
    val thirdPartySummary: String,

    // ---- Licenses screen ----
    val licensesBack: String,
    val licensesIntro: String,
    val runtimeLibsSection: String,
    val componentLibsSection: String,
    val nativeLibSection: String,
    val onlineServiceSection: String,
    val onlineServiceSummary: String,
    val funFact: String,

    // ---- Log dialog ----
    val logTitle: String,
    val logFailHeader: String,
    val logLastFailureReason: String,
    val logNone: String,
    val logRuntimeHeader: String,
    val logCrashHeader: String,
    val logClear: String,
    val logClose: String,
    val logCopied: String,
    val clipLabelLog: String,

    // ---- Formatted / dynamic strings ----
    val charsCount: (Int) -> String,
    val readDone: (String) -> String,
    val readFailed: (String) -> String,
    val convertFailed: (String) -> String,
    val convertDone: (String) -> String,
    val exportDone: (String) -> String,
    val exportFailed: (String) -> String,

    // ---- Engine error messages (mapped from the converter's raw error text) ----
    val err: FriendlyErrors,
)

data class FriendlyErrors(
    val invalidFcl: String,
    val invalidZl1: String,
    val invalidZl2: String,
    val missingField: String,
    val structureCheckFailed: String,
    val styleCheckFailed: String,
    val joystickCheckFailed: String,
    val duplicateLayers: String,
    val danglingLayerRef: String,
    val widgetCheckFailed: String,
    val widgetCountDropped: String,
    val unsupportedDirection: (input: String, output: String) -> String,
    val engineNotReady: String,
    val timeout: String,
    val noResult: String,
    val onlineFailedFallback: String,
    val emptyResult: String,
    val genericFailed: String,
)

val ZH = Strings(
    code = "zh",

    navHome = "主页",
    navAbout = "关于",

    appTitle = "控件转换",
    appSubtitle = "FCL · ZL1/Pojav · ZL2",
    statusReady = "选择或粘贴一个布局 JSON",
    statusProcessingPrefix = "处理中 · ",

    formatSelection = "格式选择",
    inputFormatLabel = "输入格式",
    outputFormatLabel = "输出格式",
    formatAuto = "自动",
    onlineToggleTitle = "在线转换（仅支持ZL2、FCL互转）",
    onlineToggleSummary = "FCL ↔ ZL2 时调用 cc.miawa.cn 转换",
    autoHint = "输入格式设为「自动」时，将根据内容自动识别 FCL / ZL1 / Pojav / ZL2",

    layoutSection = "布局",
    tabPaste = "粘贴",
    tabChooseFile = "选择文件",
    layoutNameLabel = "控制布局",
    pasteHint = "粘贴 FCL、ZL1 或 ZL2 json内容",
    chooseFileButton = "选择布局文件",

    convertButton = "开始转换",
    convertingButton = "转换中…",

    outputResultSection = "输出结果",
    resultTitleLabel = "转换结果",
    exportButton = "导出",
    copyButton = "复制",
    expandResult = "展开结果",
    collapseResult = "收起结果",
    generatingPreview = "正在生成预览…",

    readingFile = "正在读取文件…",
    noReadableFile = "无法读取文件",
    converting = "正在转换…",
    convertInvalidJson = "转换失败：转换器返回了无效 JSON",
    exporting = "正在导出…",
    noExportableResult = "没有可保存的转换结果",
    cannotOpenTarget = "无法打开目标文件",
    resultCopiedStatus = "结果已复制",
    resultCopiedToast = "已复制输出结果",
    clipLabelResult = "布局 JSON",

    aboutTitle = "关于",
    appSection = "应用",
    versionLabel = "版本",
    githubPageTitle = "GitHub 开源页面",
    themeSection = "主题",
    colorLabel = "颜色",
    themeAuto = "自动",
    themeLight = "浅色",
    themeDark = "深色",
    languageSection = "语言",
    languageLabel = "界面语言",
    languageZh = "中文",
    languageEn = "English",
    aboutSection = "关于",
    developerLabel = "开发者",
    licenseLabel = "开源许可证",
    thirdPartyTitle = "第三方开源项目",
    thirdPartySummary = "查看许可证与致谢",

    licensesBack = "返回",
    licensesIntro = "ControlLayoutConverter 用到了下面这些开源项目。点击任意一项可以打开它的主页，去看它自己的许可证原文。",
    runtimeLibsSection = "运行时库",
    componentLibsSection = "组件库",
    nativeLibSection = "原生转换库",
    onlineServiceSection = "在线服务",
    onlineServiceSummary = "在线转换接口 · 可选使用",
    funFact = "你知道吗：其实本软件的大部分UI都是借鉴 MobileGlues（https://github.com/MobileGL-Dev/MobileGlues-release）的",

    logTitle = "日志",
    logFailHeader = "=== 转换失败日志 ===",
    logLastFailureReason = "最近失败原因：",
    logNone = "暂无",
    logRuntimeHeader = "=== 运行日志 ===",
    logCrashHeader = "=== 崩溃日志 ===",
    logClear = "清除",
    logClose = "关闭",
    logCopied = "日志已复制",
    clipLabelLog = "日志",

    charsCount = { n -> "$n 字符" },
    readDone = { name -> "已读取 $name.json" },
    readFailed = { msg -> "读取失败：$msg" },
    convertFailed = { msg -> "转换失败：$msg" },
    convertDone = { file -> "转换完成：$file" },
    exportDone = { name -> "已保存 $name.json" },
    exportFailed = { msg -> "保存失败：$msg" },

    err = FriendlyErrors(
        invalidFcl = "内容不是有效的 FCL 布局，请检查粘贴内容",
        invalidZl1 = "内容不是有效的 ZL1 布局，请检查粘贴内容",
        invalidZl2 = "内容不是有效的 ZL2 布局，请检查粘贴内容",
        missingField = "缺少关键字段，请输入对应格式的布局内容",
        structureCheckFailed = "格式结构校验失败，内容可能已损坏或版本不符",
        styleCheckFailed = "引用了不存在的样式，无法转换",
        joystickCheckFailed = "摇杆控件字段不完整，无法转换",
        duplicateLayers = "存在重复图层，无法转换",
        danglingLayerRef = "存在无效的图层引用，无法转换",
        widgetCheckFailed = "存在字段不完整的控件，无法转换",
        widgetCountDropped = "转换后控件数量下降，已保守停止以减少丢失",
        unsupportedDirection = { input, output -> "暂不支持 $input → $output 的转换" },
        engineNotReady = "转换引擎未就绪，请重启应用",
        timeout = "转换超时，请重试或换更小的布局",
        noResult = "转换未能取得结果，请重试",
        onlineFailedFallback = "在线转换失败，已回退本地引擎",
        emptyResult = "转换器未产生有效结果，请检查内容",
        genericFailed = "转换失败",
    ),
)

val EN = Strings(
    code = "en",

    navHome = "Home",
    navAbout = "About",

    appTitle = "Control Conversion",
    appSubtitle = "FCL · ZL1/Pojav · ZL2",
    statusReady = "Select or paste a layout JSON",
    statusProcessingPrefix = "Processing · ",

    formatSelection = "Format",
    inputFormatLabel = "Input Format",
    outputFormatLabel = "Output Format",
    formatAuto = "Auto",
    onlineToggleTitle = "Online conversion (ZL2 ↔ FCL only)",
    onlineToggleSummary = "Uses cc.miawa.cn for FCL ↔ ZL2",
    autoHint = "When input format is set to \"Auto\", the format is detected automatically: FCL / ZL1 / Pojav / ZL2",

    layoutSection = "Layout",
    tabPaste = "Paste",
    tabChooseFile = "Choose File",
    layoutNameLabel = "Layout Name",
    pasteHint = "Paste FCL, ZL1, or ZL2 JSON content",
    chooseFileButton = "Choose Layout File",

    convertButton = "Start Conversion",
    convertingButton = "Converting…",

    outputResultSection = "Output",
    resultTitleLabel = "Conversion Result",
    exportButton = "Export",
    copyButton = "Copy",
    expandResult = "Expand Result",
    collapseResult = "Collapse Result",
    generatingPreview = "Generating preview…",

    readingFile = "Reading file…",
    noReadableFile = "Unable to read the file",
    converting = "Converting…",
    convertInvalidJson = "Conversion failed: the converter returned invalid JSON",
    exporting = "Exporting…",
    noExportableResult = "There's no result to save yet",
    cannotOpenTarget = "Unable to open the target file",
    resultCopiedStatus = "Result copied",
    resultCopiedToast = "Output copied",
    clipLabelResult = "Layout JSON",

    aboutTitle = "About",
    appSection = "App",
    versionLabel = "Version",
    githubPageTitle = "GitHub Repository",
    themeSection = "Theme",
    colorLabel = "Color",
    themeAuto = "Auto",
    themeLight = "Light",
    themeDark = "Dark",
    languageSection = "Language",
    languageLabel = "Interface Language",
    languageZh = "中文",
    languageEn = "English",
    aboutSection = "About",
    developerLabel = "Developer",
    licenseLabel = "Open Source License",
    thirdPartyTitle = "Third-Party Projects",
    thirdPartySummary = "View licenses & credits",

    licensesBack = "Back",
    licensesIntro = "ControlLayoutConverter is built on the open-source projects below. Tap any of them to open its homepage and read its own license.",
    runtimeLibsSection = "Runtime Libraries",
    componentLibsSection = "UI Components",
    nativeLibSection = "Native Conversion Library",
    onlineServiceSection = "Online Service",
    onlineServiceSummary = "Online conversion API · optional",
    funFact = "Fun fact: most of this app's UI was inspired by MobileGlues (https://github.com/MobileGL-Dev/MobileGlues-release)",

    logTitle = "Log",
    logFailHeader = "=== Conversion Failure Log ===",
    logLastFailureReason = "Most recent failure: ",
    logNone = "None",
    logRuntimeHeader = "=== Runtime Log ===",
    logCrashHeader = "=== Crash Log ===",
    logClear = "Clear",
    logClose = "Close",
    logCopied = "Log copied",
    clipLabelLog = "Log",

    charsCount = { n -> "$n characters" },
    readDone = { name -> "Loaded $name.json" },
    readFailed = { msg -> "Failed to read file: $msg" },
    convertFailed = { msg -> "Conversion failed: $msg" },
    convertDone = { file -> "Conversion complete: $file" },
    exportDone = { name -> "Saved $name.json" },
    exportFailed = { msg -> "Save failed: $msg" },

    err = FriendlyErrors(
        invalidFcl = "This isn't a valid FCL layout — please check the pasted content",
        invalidZl1 = "This isn't a valid ZL1 layout — please check the pasted content",
        invalidZl2 = "This isn't a valid ZL2 layout — please check the pasted content",
        missingField = "Missing required fields — please provide content in the matching format",
        structureCheckFailed = "Structure check failed — the content may be corrupted or an unsupported version",
        styleCheckFailed = "References a style that doesn't exist — can't convert",
        joystickCheckFailed = "Joystick control is missing required fields — can't convert",
        duplicateLayers = "Duplicate layers found — can't convert",
        danglingLayerRef = "Found a reference to a layer that doesn't exist — can't convert",
        widgetCheckFailed = "Some controls are missing required fields — can't convert",
        widgetCountDropped = "The control count dropped after conversion, so it was stopped to avoid losing data",
        unsupportedDirection = { input, output -> "Converting $input → $output isn't supported yet" },
        engineNotReady = "The conversion engine isn't ready yet — please restart the app",
        timeout = "Conversion timed out — try again or use a smaller layout",
        noResult = "Conversion didn't produce a result — please try again",
        onlineFailedFallback = "Online conversion failed, fell back to the local engine",
        emptyResult = "The converter didn't produce a valid result — please check the content",
        genericFailed = "Conversion failed",
    ),
)

val LocalStrings = staticCompositionLocalOf { ZH }

private const val LANG_ZH = "zh"
private const val LANG_EN = "en"

/** Language persistence: same pattern as [ThemePrefs] — read once at startup, write on toggle. */
object LanguagePrefs {
    private const val PREFS = "language"
    private const val KEY = "code"

    fun read(context: Context): String {
        val saved = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        }.getOrNull()
        if (saved == LANG_ZH || saved == LANG_EN) return saved
        // No explicit choice yet: default to the system language when it's Chinese, English otherwise.
        val systemLang = Locale.getDefault().language
        return if (systemLang == "zh") LANG_ZH else LANG_EN
    }

    fun write(context: Context, code: String) {
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, code).apply()
        }
    }
}

fun stringsFor(code: String): Strings = if (code == LANG_EN) EN else ZH
