package com.zhizhu.controlconverter

import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Persists bounded, privacy-conscious diagnostics for the next launch. */
object CrashLogStore {
    private const val PREFS = "diagnostics"
    private const val CRASH_LOG = "last_crash"
    private const val CONTEXT = "last_context"
    private const val RUNTIME_LOG = "runtime_log"
    private const val CONVERSION_FAILURE = "last_conversion_failure"
    private const val MAX_RUNTIME_CHARS = 12000

    private fun now(): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

    fun log(context: Context, event: String, details: String = "") {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val line = buildString {
            append(now()).append(" ").append(event)
            if (details.isNotBlank()) append(" ").append(details)
        }
        val old = prefs.getString(RUNTIME_LOG, "").orEmpty()
        prefs.edit().putString(RUNTIME_LOG, (old + line + "\n").takeLast(MAX_RUNTIME_CHARS)).apply()
    }

    fun readRuntime(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RUNTIME_LOG, "").orEmpty()

    fun setContext(context: Context, stage: String, source: String = "-", target: String = "-", inputLength: Int = 0, resultLength: Int = 0) {
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
        prefs.edit().putString(CONVERSION_FAILURE, report).commit()
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
            appendLine(prefs.getString(RUNTIME_LOG, "unknown") ?: "unknown")
            appendLine("exception:")
            appendLine(stack)
        }.takeLast(24000)
        prefs.edit().putString(CRASH_LOG, log).apply()
    }

    fun read(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CRASH_LOG, "").orEmpty()

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(CRASH_LOG).remove(RUNTIME_LOG).remove(CONTEXT).remove(CONVERSION_FAILURE).apply()
    }
}