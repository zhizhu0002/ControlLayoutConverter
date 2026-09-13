package com.zhizhu.controlconverter

import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/** Persists bounded, privacy-conscious diagnostics for the next launch. */
object CrashLogStore {
    private const val PREFS = "diagnostics"
    private const val CRASH_LOG = "last_crash"
    private const val CONTEXT = "last_context"
    private const val RUNTIME_LOG = "runtime_log"
    private const val CONVERSION_FAILURE = "last_conversion_failure"
    private const val MAX_RUNTIME_CHARS = 12000

    /**
     * 运行日志的内存缓冲 + 单线程落盘。
     *
     * 原先 [log] 每次都要 getString(最多 12KB) → 拼接 → putString(最多 12KB)，
     * 而它会在转换点击路径、桥接回调等主线程上下文里被频繁调用，开销可观。
     * 现在 [log] 只追加到内存并安排一次异步落盘，调用线程不再做 prefs 读写。
     */
    private val lock = Any()
    private val pending = StringBuilder()
    private var flushScheduled = false
    private val io = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "crashlog-io").apply { isDaemon = true }
    }

    private fun now(): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

    fun log(context: Context, event: String, details: String = "") {
        val line = buildString {
            append(now()).append(" ").append(event)
            if (details.isNotBlank()) append(" ").append(details)
        } + "\n"
        val appContext = context.applicationContext
        synchronized(lock) {
            pending.append(line)
            if (flushScheduled) return
            flushScheduled = true
        }
        io.execute { flush(appContext) }
    }

    /** 把内存缓冲合并进 prefs（仅在 crashlog-io 线程执行）。 */
    private fun flush(context: Context) {
        val chunk = synchronized(lock) {
            val text = pending.toString()
            pending.setLength(0)
            flushScheduled = false
            text
        }
        if (chunk.isEmpty()) return
        runCatching {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val old = prefs.getString(RUNTIME_LOG, "").orEmpty()
            prefs.edit().putString(RUNTIME_LOG, (old + chunk).takeLast(MAX_RUNTIME_CHARS)).apply()
        }
    }

    fun readRuntime(context: Context): String {
        val persisted = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(RUNTIME_LOG, "").orEmpty()
        val unflushed = synchronized(lock) { pending.toString() }
        return (persisted + unflushed).takeLast(MAX_RUNTIME_CHARS)
    }

    fun setContext(context: Context, stage: String, source: String = "-", target: String = "-", inputLength: Int = 0, resultLength: Int = 0) {
        // CONTEXT 很小（约 80 字节），保留同步写入：崩溃处理程序需要立即读到最新上下文。
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(CONTEXT, "stage=$stage\nsource=$source\ntarget=$target\ninputLength=$inputLength\nresultLength=$resultLength")
            .apply()
        log(context, stage, "source=$source target=$target inputLength=$inputLength resultLength=$resultLength")
    }

    fun recordConversionFailure(
        context: Context,
        input: String,
        target: String,
        stage: String,
        widgetPath: String?,
        message: String,
        stack: String,
        layoutName: String,
        inputLength: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val report = buildString {
            appendLine("FCL ZL 控件转换失败")
            appendLine("time=$now")
            appendLine("input=$input")
            appendLine("target=$target")
            appendLine("stage=$stage")
            appendLine("widgetPath=${widgetPath ?: "-"}")
            appendLine("layoutName=$layoutName")
            appendLine("inputLength=$inputLength")
            appendLine("thread=${Thread.currentThread().name}")
            appendLine("message=$message")
            appendLine("stack:")
            appendLine(stack.take(16000))
        }.takeLast(22000)
        // 由 commit() 改为 apply()：本方法会从 UI 线程（桥接失败回调）调用，
        // commit 是同步落盘会阻塞界面；apply 立即更新内存值并异步落盘，读取语义不变。
        prefs.edit().putString(CONVERSION_FAILURE, report).apply()
        log(context, "conversion-failure", "input=$input target=$target stage=$stage path=${widgetPath ?: "-"}")
    }

    fun readConversionFailure(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CONVERSION_FAILURE, "").orEmpty()

    fun record(context: Context, throwable: Throwable, threadName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val stack = throwable.stackTraceToString().takeLast(12000)
        val log = buildString {
            appendLine("FCL ZL 控件转换器崩溃日志")
            appendLine("time=$now")
            appendLine("thread=$threadName")
            appendLine("android=${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("context:")
            appendLine(prefs.getString(CONTEXT, "unknown") ?: "unknown")
            appendLine("conversionFailure:")
            appendLine(prefs.getString(CONVERSION_FAILURE, "none") ?: "none")
            appendLine("runtimeLog:")
            appendLine(readRuntime(context).ifBlank { "unknown" })
            appendLine("exception:")
            appendLine(stack)
        }.takeLast(24000)
        // 崩溃处理器调用此方法时进程即将被杀，必须用 commit() 同步落盘；
        // 原先用 apply() 异步写入，进程先死导致崩溃日志经常丢失（排查时白跑一趟）。
        prefs.edit().putString(CRASH_LOG, log).commit()
    }

    fun read(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CRASH_LOG, "").orEmpty()

    fun clear(context: Context) {
        synchronized(lock) {
            pending.setLength(0)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(CRASH_LOG).remove(RUNTIME_LOG).remove(CONTEXT).remove(CONVERSION_FAILURE).apply()
    }
}
