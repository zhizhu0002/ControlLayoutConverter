package com.zhizhu.controlconverter

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import android.os.Bundle
import android.os.SystemClock
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import kotlin.math.max
import kotlin.math.min
import java.util.Base64
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

private val Accent = Color(0xFF2E7CF6)
private val Success = Color(0xFF35B779)
private val Error = Color(0xFFF72727)

// 自适应布局配色（单一颜色来源，同时驱动 Miuix ColorScheme 与自定义组件）。
// 深色：近纯黑页面 + #212121 卡片 + 深灰分段容器 + 亮灰选中胶囊；浅色：浅灰背板 + 白卡片。
private data class AppColors(
    val bg: Color,
    val card: Color,
    val input: Color,
    val inputBorder: Color,
    val pill: Color,
    val pillBorder: Color,
    val text: Color,
    val textSecondary: Color,
    val slot: Color,
)

private val DarkAppColors = AppColors(
    bg = Color(0xFF000000),
    card = Color(0xFF212121),
    input = Color(0xFF141416),
    inputBorder = Color(0xFF2E2E30),
    pill = Color(0xFF2C2C2E),
    pillBorder = Color(0xFF3A3A3C),
    text = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFA6A6A6),
    slot = Color(0xFF404040),
)

private val LightAppColors = AppColors(
    bg = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    input = Color(0xFFEDEDEE),
    inputBorder = Color(0xFFD1D1D6),
    pill = Color(0xFFFFFFFF),
    pillBorder = Color(0xFFC7C7CC),
    text = Color(0xFF000000),
    textSecondary = Color(0xFF8E8E93),
    slot = Color(0xFFF0F0F0),
)

private val LocalLayoutColors = staticCompositionLocalOf { DarkAppColors }

@Composable
private fun layoutColors(): AppColors = LocalLayoutColors.current

class MainActivity : ComponentActivity() {
    private var engine: WebView? = null
    private var callback: ((String) -> Unit)? = null
    /** WebView 引擎不可用时（如沙箱环境数据目录被占用），转换降级为仅原生引擎。 */
    var webViewEngineAvailable = true
        private set

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val startMs = SystemClock.elapsedRealtime()
        // ===== 启动性能检查：采集冷启动指标，检测上次崩溃，判定液态玻璃降级 =====
        AppStartProbe.run(applicationContext)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 标记本次启动以崩溃结束 → 下次启动若在低端/高负载状态将自动降级玻璃渲染
            AppStartProbe.armCrashGuard(applicationContext)
            CrashLogStore.record(applicationContext, throwable, thread.name)
            previousHandler?.uncaughtException(thread, throwable)
        }
        enableEdgeToEdge()
        CrashLogStore.log(this, "app-start")
        // WebView 转换引擎：创建失败（沙箱环境数据目录冲突等）时降级为仅原生引擎，
        // 保证 UI 与原生转换（FCL↔ZL2）始终可用，绝不因 WebView 初始化崩溃整个启动。
        engine = try {
            WebView(this).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                addJavascriptInterface(Bridge(), "NativeConverter")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        CrashLogStore.log(this@MainActivity, "webview-ready")
                        val metrics = resources.displayMetrics
                        val rawWidth = max(window.decorView.width, metrics.widthPixels)
                        val rawHeight = max(window.decorView.height, metrics.heightPixels)
                        view.evaluateJavascript("globalThis.setRuntimeDisplay(${max(rawWidth, rawHeight)},${min(rawWidth, rawHeight)},${metrics.density})", null)
                    }
                }
                loadUrl("file:///android_asset/index.html")
            }
        } catch (webViewError: Throwable) {
            webViewEngineAvailable = false
            CrashLogStore.log(this, "webview-init-failed", "降级为仅原生引擎：${webViewError.message ?: webViewError::class.java.simpleName}")
            null
        }
        setContent { ConverterApp() }
        // 启动看门狗：10 秒内未崩溃（首帧渲染稳定）则解除崩溃哨兵；
        // 之后的转换错误走正常错误处理，不触发玻璃降级。
        window.decorView.postDelayed({
            if (!isFinishing) {
                AppStartProbe.disarmCrashGuard(applicationContext)
                CrashLogStore.log(this, "startup-ok", "total=${SystemClock.elapsedRealtime() - startMs}ms")
            }
        }, 10_000L)
    }

    inner class Bridge {
        @JavascriptInterface fun complete(encoded: String) = runOnUiThread {
            CrashLogStore.log(this@MainActivity, "bridge-complete", "encodedLength=${encoded.length}")
            val decoded = runCatching { String(Base64.getDecoder().decode(encoded), Charsets.UTF_8) }
                .getOrElse { "__ERROR__:转换结果编码无效：${it.message ?: "未知错误"}" }
            CrashLogStore.setContext(this@MainActivity, "convert-complete", resultLength = decoded.length)
            callback?.invoke(decoded)
        }
        @JavascriptInterface fun failed(payload: String) = runOnUiThread {
            CrashLogStore.log(this@MainActivity, "bridge-failed", "payloadLength=${payload.length}")
            val parsed = runCatching {
                val json = JSONObject(payload)
                val msg = json.optString("message", "转换失败")
                val stack = json.optString("stack", "")
                val stage = json.optString("stage", "unknown")
                val widgetPath = json.optString("widgetPath", "")
                CrashLogStore.recordConversionFailure(
                    this@MainActivity,
                    input = json.optString("inputFormat", "?"),
                    target = json.optString("targetFormat", "?"),
                    stage = stage,
                    widgetPath = widgetPath,
                    message = msg,
                    stack = stack,
                    layoutName = json.optString("layoutName", "控制布局"),
                    inputLength = json.optInt("inputLength", 0)
                )
                msg.ifBlank { "转换失败" }
            }.getOrElse { "转换失败" }
            CrashLogStore.setContext(this@MainActivity, "convert-failed")
            callback?.invoke("__ERROR__:$parsed")
        }
    }

    fun convert(text: String, input: String, output: String, name: String, useOnline: Boolean, done: (String) -> Unit) {
        CrashLogStore.setContext(this, "convert-start", input, output, text.length)
        CrashLogStore.log(this, "convert-request", "nameLength=${name.length} online=$useOnline")
        if (input !in setOf("FCL", "ZL1", "ZL2")) {
            done("__ERROR__:不支持的输入格式：$input")
            return
        }
        if (output !in setOf("FCL", "ZL1", "ZL2")) {
            done("__ERROR__:不支持的输出格式：$output")
            return
        }
        Thread {
            runCatching { dispatchConversion(text, input, output, name, useOnline) }
                .onSuccess { rawResult ->
                    val backend = when {
                        input == "FCL" && output == "ZL2" -> "native-libcc"
                        input == "ZL2" && output == "FCL" -> "native-libcc"
                        else -> "web-engine"
                    }
                    CrashLogStore.log(this, "conversion-success", "backend=$backend direction=$input-$output resultLength=${rawResult.length}")
                    // 大 JSON 的合法性校验与 pretty 预处理移到后台线程，避免主线程解析造成卡顿。
                    val ok = runCatching { JSONObject(rawResult) }.isSuccess && rawResult.isNotBlank()
                    runOnUiThread { done(if (ok) rawResult else "__ERROR__:__INVALID_JSON__") }
                }
                .onFailure { error ->
                    CrashLogStore.log(this, "conversion-failed", "direction=$input-$output type=${error::class.java.simpleName}")
                    runOnUiThread { done("__ERROR__:${error.message ?: "未知错误"}") }
                }
        }.start()
    }

    /**
     * 转换分派：
     * - FCL -> ZL2：官方 libcc.so 原生，失败回退 WebView JS 引擎。
     * - ZL2 -> FCL：官方 libcc.so 原生（扩展导出 convertZl2ToFclNative），失败回退 JS。
     * - ZL1 <-> ZL2：WebView JS 引擎（migrateLayout / zl2ToZl1）。
     * - FCL <-> ZL1：经 ZL2 原生中转（原生 + JS 混合链）。
     */
    private fun dispatchConversion(text: String, input: String, output: String, name: String, useOnline: Boolean): String {
        // 在线转换优先（仅 FCL↔ZL2，且用户开启在线），失败回退本地引擎
        if (useOnline && input == "FCL" && output == "ZL2") {
            try {
                CrashLogStore.log(this, "api-convert-attempt", "direction=fcl2zl")
                return convertViaApiBlocking(text, input, output)
            } catch (apiError: Exception) {
                CrashLogStore.log(this, "api-convert-fallback-native", "reason=${apiError.message ?: "unknown"}")
            }
        }
        if (useOnline && input == "ZL2" && output == "FCL") {
            try {
                CrashLogStore.log(this, "api-convert-attempt", "direction=zl2fcl")
                return convertViaApiBlocking(text, input, output)
            } catch (apiError: Exception) {
                CrashLogStore.log(this, "api-convert-fallback-native", "reason=${apiError.message ?: "unknown"}")
            }
        }
        if (input == "FCL" && output == "ZL2") {
            // 前置校验：确认输入确实是 FCL 布局，避免 libcc 原生宽松解析把别的内容"侥幸"转换成功
            require(Regex("\"viewGroups\"").containsMatchIn(text)) { "不是有效的 FCL 控件布局（缺少 viewGroups）" }
            // 引擎优先级：libcc 原生 → WebView
            try {
                return OfficialConverter.convertFclToZl2(this, text)
            } catch (nativeError: Exception) {
                CrashLogStore.log(this, "native-fallback-web", "reason=${nativeError.message ?: "unknown"}")
            }
            return convertViaWebBlocking(text, input, output, name)
        }
        if (input == "ZL2" && output == "FCL") {
            // 前置校验：确认输入确实是 ZL2 布局
            require(Regex("\"layers\"").containsMatchIn(text)) { "不是有效的 ZL2 控件布局（缺少 layers）" }
            // 引擎优先级：libcc 原生 → WebView
            try {
                return OfficialConverter.convertZl2ToFcl(this, text)
            } catch (nativeError: Exception) {
                CrashLogStore.log(this, "native-zl2fcl-fallback-web", "reason=${nativeError.message ?: "unknown"}")
            }
            return convertViaWebBlocking(text, input, output, name)
        }
        if (input == "ZL1" && output == "ZL2") {
            // 前置校验：确认输入确实是 ZL1 布局，避免 migrateLayout 访问 undefined 字段抛晦涩错误
            require(Regex("\"mControlDataList\"").containsMatchIn(text)) { "不是有效的 ZL1 控件布局（缺少 mControlDataList）" }
            return convertViaWebBlocking(text, input, output, name)
        }
        if (input == "ZL2" && output == "ZL1") {
            return convertViaWebBlocking(text, input, output, name)
        }
        if (input == "FCL" && output == "ZL1") {
            // 前置校验（链式：FCL→ZL2→ZL1）
            require(Regex("\"viewGroups\"").containsMatchIn(text)) { "不是有效的 FCL 控件布局（缺少 viewGroups）" }
            val zl2 = try {
                OfficialConverter.convertFclToZl2(this, text)
            } catch (nativeError: Exception) {
                CrashLogStore.log(this, "native-fallback-web", "chain reason=${nativeError.message ?: "unknown"}")
                convertViaWebBlocking(text, "FCL", "ZL2", name)
            }
            return convertViaWebBlocking(zl2, "ZL2", "ZL1", name)
        }
        if (input == "ZL1" && output == "FCL") {
            // 前置校验（链式：ZL1→ZL2→FCL）
            require(Regex("\"mControlDataList\"").containsMatchIn(text)) { "不是有效的 ZL1 控件布局（缺少 mControlDataList）" }
            val zl2 = convertViaWebBlocking(text, "ZL1", "ZL2", name)
            return try {
                OfficialConverter.convertZl2ToFcl(this, zl2)
            } catch (nativeError: Exception) {
                CrashLogStore.log(this, "native-zl2fcl-fallback-web", "chain reason=${nativeError.message ?: "unknown"}")
                convertViaWebBlocking(zl2, "ZL2", "FCL", name)
            }
        }
        // 兜底：同格式或未覆盖方向
        return convertViaWebBlocking(text, input, output, name)
    }

    /** 在后台线程同步调用 WebView 引擎并等待结果。 */
    private fun convertViaWebBlocking(text: String, input: String, output: String, name: String): String {
        val latch = java.util.concurrent.CountDownLatch(1)
        var webResult: String? = null
        runOnUiThread {
            convertInWebView(text, input, output, name) { r ->
                webResult = r
                latch.countDown()
            }
        }
        require(latch.await(30, java.util.concurrent.TimeUnit.SECONDS)) { "WebView 转换超时" }
        val r = requireNotNull(webResult) { "WebView 转换没有返回结果" }
        if (r.startsWith("__ERROR__:")) throw IllegalStateException(r.removePrefix("__ERROR__:"))
        return r
    }

    /** 在线转换：调用 api.cc.miawa.cn/convert（官网仅支持 FCL↔ZL2）。失败抛异常，由调用方回退本地。 */
    private fun convertViaApiBlocking(text: String, input: String, output: String): String {
        val mode = when {
            input == "FCL" && output == "ZL2" -> "fcl2zl"
            input == "ZL2" && output == "FCL" -> "zl2fcl"
            else -> throw IllegalStateException("官网在线转换暂不支持 $input -> $output")
        }
        val url = URL("https://api.cc.miawa.cn/convert")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 8000
        conn.readTimeout = 20000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        // 直接传原始 JSON 文本作为 data；官网会解析其内容
        val body = buildString {
            append("{\"mode\":\"").append(mode).append("\",\"data\":")
            append(text.trim())
            append(",\"stripMeta\":false}")
        }
        conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
        conn.disconnect()
        CrashLogStore.log(this, "api-convert-http", "code=$code mode=$mode respLen=${resp.length}")
        val json = runCatching { JSONObject(resp) }.getOrElse { throw IllegalStateException("在线响应异常：$resp") }
        if (!json.optBoolean("ok", false)) {
            throw IllegalStateException(json.optString("error", "在线转换失败"))
        }
        val data = json.optJSONObject("data")
            ?: json.optJSONArray("data")
            ?: throw IllegalStateException("在线转换返回空结果")
        return data.toString()
    }

    private fun convertInWebView(text: String, input: String, output: String, name: String, done: (String) -> Unit) {
        val webEngine = engine
        if (webEngine == null) {
            done("__ERROR__:WebView 转换引擎不可用（启动时初始化失败，已降级为仅原生引擎）")
            return
        }
        callback = done
        val payload = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
        val safeName = name.replace("\\", "\\\\").replace("'", "\\'")
        val inputLen = text.length
        val js = """(function(){try{var raw=decodeURIComponent(escape(atob('$payload')));var o=ControlConverter.parseExactJson(raw);var r=ControlConverter.convert(o,'$input','$output','$safeName');if(r===null||r===undefined){throw new Error('转换器返回空结果')}var text=typeof r==='string'?r:JSON.stringify(r,null,2);if(!text||!text.trim()){throw new Error('转换器返回空文本')}JSON.parse(text);NativeConverter.complete(btoa(unescape(encodeURIComponent(text))))}catch(e){var p={message:e&&e.message?e.message:'',stack:e&&e.stack?e.stack:'',stage:e&&e.conversionStage?e.conversionStage:'convert-unknown',widgetPath:e&&e.widgetPath?e.widgetPath:null,inputFormat:'$input',targetFormat:'$output',layoutName:'$safeName',inputLength:$inputLen};NativeConverter.failed(JSON.stringify(p))}})()"""
        runCatching { webEngine.evaluateJavascript(js, null) }
            .onFailure { callback = null; done("__ERROR__:${it.message ?: "无法启动转换"}") }
    }

    override fun onDestroy() { engine?.destroy(); super.onDestroy() }
}

@Composable
private fun ConverterApp() {
    val activity = LocalContext.current as MainActivity

    var source by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("控制布局") }
    var input by rememberSaveable { mutableStateOf("ZL2") }
    var output by rememberSaveable { mutableStateOf("ZL2") }
    var actualInput by rememberSaveable { mutableStateOf("FCL") }
    var status by rememberSaveable { mutableStateOf("选择或粘贴一个布局 JSON") }
    var busy by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var online by rememberSaveable { mutableStateOf(true) }
    var inputTab by rememberSaveable { mutableStateOf("粘贴") }
    var crashLog by remember { mutableStateOf(CrashLogStore.read(activity)) }
    var runtimeLog by remember { mutableStateOf(CrashLogStore.readRuntime(activity)) }
    var conversionFailLog by remember { mutableStateOf(CrashLogStore.readConversionFailure(activity)) }
    var showLog by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var resultExpanded by remember { mutableStateOf(false) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { activity.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty() }
            .onSuccess { text ->
                source = text
                val displayName = activity.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
                }
                name = (displayName ?: "控制布局").substringBeforeLast('.', "控制布局")
                status = "已读取 $name.json"
                inputTab = "粘贴"
            }
            .onFailure { status = "读取失败：${it.message ?: "无法读取文件"}" }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        saving = true
        runCatching {
            require(result.isNotBlank()) { "没有可保存的转换结果" }
            val bytes = result.toByteArray(Charsets.UTF_8)
            val stream = requireNotNull(activity.contentResolver.openOutputStream(uri)) { "无法打开目标文件" }
            stream.use { it.write(bytes) }
        }
            .onSuccess { status = "已保存 ${exportBaseName(name, actualInput, output)}.json" }
            .onFailure { status = "保存失败：${it.message ?: "无法写入文件"}" }
        saving = false
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = systemDark
    val themeController = remember(isDark) {
        val app = if (isDark) DarkAppColors else LightAppColors
        ThemeController(
            colorSchemeMode = if (isDark) ColorSchemeMode.Dark else ColorSchemeMode.Light,
            lightColors = lightColorScheme(
                primary = Accent,
                background = LightAppColors.bg,
                onBackground = LightAppColors.text,
                surface = LightAppColors.bg,
                onSurface = LightAppColors.text,
                surfaceContainer = LightAppColors.card,
                onSurfaceContainer = LightAppColors.text,
                surfaceVariant = LightAppColors.input,
                outline = LightAppColors.inputBorder,
                dividerLine = LightAppColors.pillBorder
            ),
            darkColors = darkColorScheme(
                primary = Accent,
                background = DarkAppColors.bg,
                onBackground = DarkAppColors.text,
                surface = DarkAppColors.bg,
                onSurface = DarkAppColors.text,
                surfaceContainer = DarkAppColors.card,
                onSurfaceContainer = DarkAppColors.text,
                surfaceVariant = DarkAppColors.input,
                outline = DarkAppColors.inputBorder,
                dividerLine = DarkAppColors.pillBorder
            )
        )
    }

    val layoutColors = if (isDark) DarkAppColors else LightAppColors

    MiuixTheme(controller = themeController) {
        CompositionLocalProvider(LocalLayoutColors provides layoutColors) {
            SystemBarsIconColor(isDarkTheme = isDark)
            Scaffold(containerColor = layoutColors.bg) { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ===== Header =====
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "控件转换",
                                style = MiuixTheme.textStyles.title1,
                                fontWeight = FontWeight.Bold,
                                color = layoutColors().text
                            )
                            Text(
                                "FCL · ZL1/Pojav · ZL2",
                                style = MiuixTheme.textStyles.body2,
                                fontWeight = FontWeight.SemiBold,
                                color = Accent
                            )
                        }
                    }

                    // ===== Status =====
                    StatusPill(status, busy)

                    // ===== 格式选择 =====
                    SmallTitle(text = "格式选择", textColor = layoutColors().textSecondary, modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 2.dp))
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        cornerRadius = 16.dp,
                        colors = CardDefaults.defaultColors(color = layoutColors().card)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // 输入格式
                            Text("输入格式", style = MiuixTheme.textStyles.body2, color = layoutColors().textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                            FormatSegmented(
                                items = listOf("自动", "FCL", "ZL1/Pojav", "ZL2"),
                                values = listOf("自动", "FCL", "ZL1", "ZL2"),
                                selected = input,
                                onSelect = { input = it },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(10.dp))

                            // 输出格式
                            Text("输出格式", style = MiuixTheme.textStyles.body2, color = layoutColors().textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                            FormatSegmented(
                                items = listOf("FCL", "ZL1/Pojav", "ZL2"),
                                values = listOf("FCL", "ZL1", "ZL2"),
                                selected = output,
                                onSelect = { output = it },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(10.dp))

                            // 在线转换开关（调用 api.cc.miawa.cn，仅 FCL↔ZL2 时生效）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "在线转换",
                                        style = MiuixTheme.textStyles.body2,
                                        color = layoutColors().text
                                    )
                                    Text(
                                        "开启后 FCL ↔ ZL2 将调用 cc.miawa.cn 在线转换",
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = layoutColors().textSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Switch(
                                    checked = online,
                                    onCheckedChange = { online = it }
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // 自动识别说明（仅输入格式为「自动」时显示，带展开/收起过渡动画）
                            AnimatedVisibility(
                                visible = input == "自动",
                                enter = fadeIn(tween(200)) + expandVertically(tween(240, easing = FastOutSlowInEasing)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(200, easing = FastOutSlowInEasing))
                            ) {
                                Text(
                                    "输入格式设为「自动」时，将根据内容自动识别 FCL / ZL1 / Pojav / ZL2",
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = layoutColors().textSecondary,
                                    modifier = Modifier
                                        .padding(start = 2.dp)
                                        .padding(bottom = 2.dp)
                                )
                            }
                        }
                    }

                    // ===== 布局 Section =====
                    SmallTitle(text = "布局", textColor = layoutColors().textSecondary, modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 2.dp))
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        cornerRadius = 16.dp,
                        colors = CardDefaults.defaultColors(color = layoutColors().card)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Tab: 粘贴 / 选择文件（切换复用窗口）
                            MiuixSegmented(
                                items = listOf("粘贴", "选择文件"),
                                selectedIndex = if (inputTab == "粘贴") 0 else 1,
                                onSelect = { inputTab = if (it == 0) "粘贴" else "选择文件" },
                                containerHeight = 44.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(10.dp))

                            // 布局名称输入框（占满整行）
                            TextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "控制布局",
                                useLabelAsPlaceholder = true,
                                colors = TextFieldDefaults.textFieldColors(
                                    backgroundColor = layoutColors().input,
                                    labelColor = layoutColors().textSecondary,
                                    borderColor = Color.Transparent
                                )
                            )

                            Spacer(Modifier.height(10.dp))

                            // 内容区（按 TAB 切换，带过渡动画）
                            AnimatedContent(
                                targetState = inputTab,
                                transitionSpec = {
                                    val dir = if (targetState == "粘贴") -1 else 1
                                    (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing), initialOffsetX = { it * dir }) + fadeIn(tween(200)))
                                        .togetherWith(slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing), targetOffsetX = { it * -dir }) + fadeOut(tween(150)))
                                },
                                label = "layoutContent"
                            ) { tab ->
                                if (tab == "粘贴") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("JSON 内容", style = MiuixTheme.textStyles.body2, color = layoutColors().textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                                        JsonInputField(
                                            value = source,
                                            onValueChange = { source = it },
                                            placeholder = "粘贴 FCL、ZL1 或 ZL2 JSON",
                                            modifier = Modifier.fillMaxWidth().height(140.dp)
                                        )
                                    }
                                } else {
                                    // 选择布局文件按钮（占满整行）
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = layoutColors().slot,
                                        modifier = Modifier.fillMaxWidth().clickable { pick.launch(arrayOf("application/json", "text/plain")) }
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth().height(48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("选择布局文件", style = MiuixTheme.textStyles.body1, color = layoutColors().text)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ===== Convert Button =====
                    Button(
                        enabled = source.isNotBlank() && !busy,
                        onClick = {
                            busy = true
                            status = "正在转换…"
                            val actual = if (input == "自动") detectFormat(source) else input
                            actualInput = actual
                            result = ""
                            showResult = false
                            resultExpanded = false
                            activity.convert(source, actual, output, name, online) { r ->
                                busy = false
                                if (r == "__ERROR__:__INVALID_JSON__") {
                                    result = ""
                                    status = "转换失败：转换器返回了无效 JSON"
                                } else if (r.startsWith("__ERROR__:")) {
                                    result = ""
                                    val errRaw = r.removePrefix("__ERROR__:")
                                    status = "转换失败：${friendlyError(errRaw, actual, output)}"
                                } else {
                                    result = r
                                    showResult = true
                                    status = "转换完成：${exportBaseName(name, actualInput, output)}.json"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(54.dp),
                        cornerRadius = 14.dp,
                        colors = ButtonDefaults.buttonColors(
                            color = Accent,
                            disabledColor = Accent.copy(alpha = 0.4f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) { Text(if (busy) "转换中…" else "开始转换", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold) }

                    // ===== Output Section =====
                    AnimatedVisibility(
                        visible = showResult && result.isNotBlank(),
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(150))
                    ) {
                        Column {
                            SmallTitle(text = "输出结果", textColor = layoutColors().textSecondary, modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 2.dp))
                            Card(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                cornerRadius = 16.dp,
                                colors = CardDefaults.defaultColors(color = layoutColors().card)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // 结果信息标题
                                    Text(
                                        "转换结果",
                                        style = MiuixTheme.textStyles.body1,
                                        fontWeight = FontWeight.SemiBold,
                                        color = layoutColors().text
                                    )
                                    Text(
                                        "${result.length} 字符",
                                        style = MiuixTheme.textStyles.body2,
                                        color = layoutColors().textSecondary
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    // 下载 / 复制按钮组
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { save.launch("${exportBaseName(name, actualInput, output)}.json") },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            cornerRadius = 12.dp
                                        ) { Text("导出", style = MiuixTheme.textStyles.body2) }
                                        Button(
                                            onClick = {
                                                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                                    .setPrimaryClip(ClipData.newPlainText("布局 JSON", result))
                                                Toast.makeText(activity, "已复制输出结果", Toast.LENGTH_SHORT).show()
                                                status = "结果已复制"
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            cornerRadius = 12.dp
                                        ) { Text("复制", style = MiuixTheme.textStyles.body2) }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    // 展开 / 收起按钮（置于复制按钮之下）
                                    Button(
                                        onClick = { resultExpanded = !resultExpanded },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        cornerRadius = 12.dp
                                    ) { Text(if (resultExpanded) "收起结果" else "展开结果", style = MiuixTheme.textStyles.body2) }

                                    // 展开 / 收起的 JSON 预览（带展开收起过渡动画）
                                    AnimatedVisibility(
                                        visible = resultExpanded,
                                        enter = fadeIn(tween(220)) + expandVertically(tween(260, easing = FastOutSlowInEasing)),
                                        exit = fadeOut(tween(150)) + shrinkVertically(tween(200, easing = FastOutSlowInEasing))
                                    ) {
                                        Column {
                                            Spacer(Modifier.height(12.dp))
                                            JsonPreview(
                                                value = result,
                                                pretty = output != "ZL1",
                                                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    // Log Dialog
    if (showLog) {
        SimpleDialog(
            title = "日志",
            onDismiss = { showLog = false }
        ) {
            Box(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text(buildString {
                    if (conversionFailLog.isNotBlank()) {
                        appendLine("=== 转换失败日志 ===")
                        append("最近失败原因：")
                        append(
                            // 从原始失败日志里取 message= 行，映射为友好文案
                            runCatching {
                                Regex("message=(.+)").find(conversionFailLog)?.groupValues?.get(1)
                            }.getOrNull()?.let { friendlyError(it, "-", "-") } ?: conversionFailLog
                        )
                        appendLine()
                        append(conversionFailLog)
                    } else {
                        appendLine("=== 转换失败日志 ===")
                        append("暂无")
                    }
                    appendLine("\n=== 运行日志 ===")
                    append(runtimeLog.ifBlank { "暂无" })
                    appendLine("\n=== 崩溃日志 ===")
                    append(crashLog.ifBlank { "暂无" })
                }, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = layoutColors().text)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
                val allLogs = buildString {
                    appendLine("=== 转换失败日志 ===")
                    append(conversionFailLog)
                    appendLine("\n=== 运行日志 ===")
                    append(runtimeLog)
                    appendLine("\n=== 崩溃日志 ===")
                    append(crashLog)
                }
                if (runtimeLog.isNotBlank() || crashLog.isNotBlank() || conversionFailLog.isNotBlank()) {
                    TextButton(text = "复制", onClick = {
                        (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("日志", allLogs))
                        status = "日志已复制"
                    })
                    TextButton(text = "清除", onClick = { CrashLogStore.clear(activity); crashLog = ""; runtimeLog = ""; conversionFailLog = ""; showLog = false })
                }
                TextButton(text = "关闭", onClick = { showLog = false })
            }
        }
    }
}

/** 系统栏图标颜色 */
@Composable
private fun SystemBarsIconColor(isDarkTheme: Boolean) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    val barColor = layoutColors().bg.toArgb()
    SideEffect {
        val window = activity.window
        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
}

@Composable
private fun StatusPill(text: String, busy: Boolean) {
    val colors = layoutColors()
    val color = when {
        busy -> Accent
        text.startsWith("转换失败") || text.startsWith("读取失败") || text.startsWith("保存失败") -> Error
        text.startsWith("转换完成") || text.startsWith("结果已复制") || text.startsWith("已保存") -> Success
        else -> colors.textSecondary
    }
    Text(
        if (busy) "处理中 · $text" else text,
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
        style = MiuixTheme.textStyles.body2,
        color = color
    )
}

// 格式分段选择器（Step 1）
@Composable
private fun FormatSegmented(
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    values: List<String> = items
) {
    val colors = layoutColors()
    val selectedIndex = items.indices.firstOrNull { values[it] == selected } ?: 0

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bg)
            .padding(4.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val segWidth = maxWidth / items.size
            val targetOffset = segWidth * selectedIndex
            val pillOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "pillOffset"
            )

            // 选中背景（扁平高光：无描边）
            Box(
                Modifier
                    .offset(x = pillOffset)
                    .width(segWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.pill)
            )

            Row(Modifier.fillMaxWidth()) {
                items.indices.forEach { i ->
                    val item = items[i]
                    val value = values[i]
                    val isSelected = value == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(value) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            item,
                            style = MiuixTheme.textStyles.body2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.text else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

// 通用分段选择器
@Composable
private fun MiuixSegmented(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerHeight: Dp = 40.dp,
    showCheck: Boolean = false
) {
    val colors = layoutColors()

    Box(
        modifier = modifier
            .height(containerHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bg)
            .padding(4.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val segWidth = maxWidth / items.size
            val targetOffset = segWidth * selectedIndex
            val pillOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "pillOffset"
            )

            Box(
                Modifier
                    .offset(x = pillOffset)
                    .width(segWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.pill)
            )

            Row(Modifier.fillMaxWidth()) {
                items.forEachIndexed { i, label ->
                    val isSelected = i == selectedIndex
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showCheck && isSelected) {
                                Text(
                                    "✓ ",
                                    style = MiuixTheme.textStyles.body2,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                            }
                            Text(
                                label,
                                style = MiuixTheme.textStyles.body2,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.text else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// JSON 输入框
@Composable
private fun JsonInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val colors = layoutColors()
    val scrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.input,
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp).verticalScroll(scrollState)) {
            if (value.isBlank()) {
                Text(placeholder, color = colors.textSecondary, style = MiuixTheme.textStyles.body2)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = colors.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

// JSON 预览（只读）
@Composable
private fun JsonPreview(
    value: String,
    modifier: Modifier = Modifier,
    pretty: Boolean = true
) {
    val colors = layoutColors()
    val scrollState = rememberScrollState()
    // 对超大 JSON 的 pretty 重排只在 value 变化时计算一次；
    // 展开/收起动画每帧都会触发重组，若不缓存会对全量 JSON 反复扫描造成卡顿。
    val displayed = remember(value, pretty) { if (pretty) prettyJson(value) else value }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.input,
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp).verticalScroll(scrollState)) {
            SelectionContainer {
                Text(
                    displayed,
                    color = colors.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * 纯文本 JSON 美化器：只重排缩进/换行，绝不重新解析数值。
 * 这样既保证输出框有换行缩进，又不破坏 ZL2 的 signed 64-bit 大整数字面量（如颜色值 -9223372036854775808）。
 * 若无法识别为 JSON（对象/数组），原样返回（保留报错信息等）。
 */
private fun prettyJson(raw: String): String {
    if (raw.isBlank()) return raw
    val trimmed = raw.trim()
    val isJson = trimmed.startsWith("{") || trimmed.startsWith("[")
    if (!isJson) return raw

    val out = StringBuilder(trimmed.length * 2)
    var indent = 0
    var inString = false
    var emptyStack = ArrayDeque<Boolean>() // 标记每个容器是否为"空容器"
    var i = 0
    while (i < trimmed.length) {
        val c = trimmed[i]
        when {
            inString -> {
                out.append(c)
                if (c == '\\' && i + 1 < trimmed.length) {
                    out.append(trimmed[i + 1])
                    i++
                } else if (c == '"') {
                    inString = false
                }
            }
            c == '"' -> { inString = true; out.append(c) }
            c == '{' || c == '[' -> {
                out.append(c)
                indent++
                val emptyNow = isEmptyContainer(trimmed, i)
                emptyStack.addLast(emptyNow)
                if (!emptyNow) appendLineIndent(out, indent)
            }
            c == '}' || c == ']' -> {
                val emptyNow = emptyStack.removeLastOrNull() ?: false
                if (!emptyNow) {
                    indent = (indent - 1).coerceAtLeast(0)
                    out.append('\n').append("  ".repeat(indent)).append(c)
                } else {
                    indent = (indent - 1).coerceAtLeast(0)
                    out.append(c)
                }
            }
            c == ',' -> {
                out.append(c)
                appendLineIndent(out, indent)
            }
            c == ':' -> { out.append(c).append(' ') }
            else -> out.append(c)
        }
        i++
    }
    return out.toString()
}

/** 若当前 `{`/`[` 后紧跟 `}`/`]`（空容器），不换行，保持 `{}`/`[]` 紧凑。 */
private fun isEmptyContainer(s: String, i: Int): Boolean {
    val open = s[i]
    val close = if (open == '{') '}' else ']'
    var j = i + 1
    while (j < s.length && s[j] == ' ') j++
    return j < s.length && s[j] == close
}

/** 追加换行 + 指定缩进（若上一字符已是换行则只补缩进）。 */
private fun appendLineIndent(out: StringBuilder, indent: Int) {
    if (out.lastOrNull() != '\n') out.append('\n')
    out.append("  ".repeat(indent))
}

// 简单弹窗
@Composable
private fun SimpleDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = layoutColors()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .background(colors.card, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, color = colors.text)
                TextButton(text = "✕", onClick = onDismiss)
            }
            content()
        }
    }
}

private fun exportBaseName(name: String, source: String, target: String): String {
    val cleanName = name.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").trim('_').ifBlank { "控制布局" }
    val cleanSource = if (source in listOf("FCL", "ZL1", "ZL2")) source else "未知"
    val cleanTarget = if (target in listOf("FCL", "ZL1", "ZL2")) target else "未知"
    return "$cleanName-$cleanSource-$cleanTarget"
}

private fun detectFormat(text: String): String = runCatching {
    val s = text.trim()
    when { s.contains("\"viewGroups\"") -> "FCL"; s.contains("\"mControlDataList\"") -> "ZL1"; s.contains("\"layers\"") -> "ZL2"; else -> "未知" }
}.getOrDefault("未知")

/** 把转换器抛出的原始错误映射成简短、可操作的中文提示。 */
private fun friendlyError(raw: String, input: String, output: String): String {
    val msg = raw.trim()
    return when {
        msg.contains("不是有效的 FCL") -> "内容不是有效的 FCL 布局，请检查粘贴内容"
        msg.contains("不是有效的 ZL1") -> "内容不是有效的 ZL1 布局，请检查粘贴内容"
        msg.contains("不是有效的 ZL2") -> "内容不是有效的 ZL2 布局，请检查粘贴内容"
        msg.contains("缺少 viewGroups") || msg.contains("缺少 layers") || msg.contains("缺少 mControlDataList") ->
            "缺少关键字段，请输入对应格式的布局内容"
        msg.contains("结构校验失败") -> "格式结构校验失败，内容可能已损坏或版本不符"
        msg.contains("样式校验失败") -> "引用了不存在的样式，无法转换"
        msg.contains("摇杆校验失败") -> "摇杆控件字段不完整，无法转换"
        msg.contains("重复图层") -> "存在重复图层，无法转换"
        msg.contains("悬空图层引用") -> "存在无效的图层引用，无法转换"
        msg.contains("控件校验失败") || msg.contains("按钮校验失败") || msg.contains("方向控件校验失败") ->
            "存在字段不完整的控件，无法转换"
        msg.contains("控件完整性校验失败") -> "转换后控件数量下降，已保守停止以减少丢失"
        msg.contains("不支持的格式") -> "暂不支持 ${input} → $output 的转换"
        msg.contains("WebView 转换引擎不可用") -> "转换引擎未就绪，请重启应用"
        msg.contains("超时") -> "转换超时，请重试或换更小的布局"
        msg.contains("没有返回结果") -> "转换未能取得结果，请重试"
        msg.contains("在线响应异常") || msg.contains("在线转换失败") || msg.contains("在线转换返回空结果") ||
        msg.contains("官网在线转换暂不支持") -> "在线转换失败，已回退本地引擎"
        msg.contains("转换器返回空结果") || msg.contains("转换器返回空文本") -> "转换器未产生有效结果，请检查内容"
        else -> msg.ifBlank { "转换失败" }
    }
}
