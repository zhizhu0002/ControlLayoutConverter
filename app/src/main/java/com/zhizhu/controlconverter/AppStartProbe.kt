package com.zhizhu.controlconverter

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Debug
import android.os.Process
import android.os.SystemClock

/**
 * 启动性能检查（每次冷启动运行一次）：
 * 1. 采集冷启动耗时、Java/Native 堆占用、可用/总内存；
 * 2. 检测上一次启动是否崩溃（prev-crash 标志）；
 * 3. 判定降级：上次崩溃 + 低端/高负载设备 → 自动关闭真实液态玻璃渲染，
 *    改用「渐变 + 高光 + 描边」仿玻璃，优先保证流畅与稳定；
 * 4. 所有指标写入运行日志，可在「日志」弹窗中查看。
 */
object AppStartProbe {

    data class Report(
        val coldStartMs: Long,
        val javaHeapKb: Long,
        val nativeHeapKb: Long,
        val memAvailableMb: Long,
        val memTotalMb: Long,
        val nightUiMode: Boolean,
        val prevCrash: Boolean,
        val lowDevice: Boolean,
        val degraded: Boolean
    )

    @Volatile
    var lastReport: Report? = null
        private set

    /** 液态玻璃当前是否处于降级模式（供 UI 读取）。 */
    fun isGlassDegraded(context: Context): Boolean =
        context.getSharedPreferences("perf", Context.MODE_PRIVATE).getBoolean("glass-degraded", false)

    /**
     * 执行启动性能检查。返回降级判定结果。
     * 注意：调用后需要再调用 [armCrashGuard]，在本次启动期间标记崩溃哨兵。
     */
    fun run(context: Context): Report {
        val coldStartMs = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()

        val runtime = Runtime.getRuntime()
        val javaHeapKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024
        val nativeHeapKb = Debug.getNativeHeapAllocatedSize() / 1024

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val memAvailableMb = memInfo.availMem / (1024L * 1024L)
        val memTotalMb = memInfo.totalMem / (1024L * 1024L)

        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val nightUiMode = uiMode == Configuration.UI_MODE_NIGHT_YES

        val prefs = context.getSharedPreferences("perf", Context.MODE_PRIVATE)
        val prevCrash = prefs.getBoolean("prev-crash", false)

        // 低端/高负载判定：总内存 < 4GB、可用内存 < 12%、或冷启动耗时已超 2.5s
        val lowDevice = memTotalMb < 4096 || memAvailableMb * 100 < memTotalMb * 12 || coldStartMs > 2500

        // 降级策略：上次启动崩溃 + 本次处于低端/高负载状态 → 关闭真实液态玻璃保稳定
        val degraded = prevCrash && lowDevice

        prefs.edit()
            .putBoolean("prev-crash", false)
            .putBoolean("glass-degraded", degraded)
            .putLong("last-cold-start-ms", coldStartMs)
            .apply()

        val report = Report(
            coldStartMs = coldStartMs,
            javaHeapKb = javaHeapKb,
            nativeHeapKb = nativeHeapKb,
            memAvailableMb = memAvailableMb,
            memTotalMb = memTotalMb,
            nightUiMode = nightUiMode,
            prevCrash = prevCrash,
            lowDevice = lowDevice,
            degraded = degraded
        )
        lastReport = report

        CrashLogStore.log(
            context, "app-start-probe",
            "coldStart=${coldStartMs}ms javaHeap=${javaHeapKb}KB nativeHeap=${nativeHeapKb}KB " +
                "avail=${memAvailableMb}MB total=${memTotalMb}MB api=${Build.VERSION.SDK_INT} " +
                "night=$nightUiMode prevCrash=$prevCrash lowDevice=$lowDevice glassDegraded=$degraded"
        )
        if (prevCrash) {
            CrashLogStore.log(context, "prev-crash-detected", if (degraded) "玻璃效果已自动降级" else "设备状态良好，未降级")
        }
        return report
    }

    /** 本次启动期间开启崩溃哨兵：若进程异常退出，下次启动能检测到。 */
    fun armCrashGuard(context: Context) {
        context.getSharedPreferences("perf", Context.MODE_PRIVATE)
            .edit().putBoolean("prev-crash", true).apply()
    }

    /** 首帧稳定渲染后解除崩溃哨兵：本次启动视为成功，不触发下次降级。 */
    fun disarmCrashGuard(context: Context) {
        context.getSharedPreferences("perf", Context.MODE_PRIVATE)
            .edit().putBoolean("prev-crash", false).apply()
    }
}
