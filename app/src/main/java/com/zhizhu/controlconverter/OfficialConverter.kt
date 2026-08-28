package com.zhizhu.controlconverter

import android.content.Context
import com.tungsten.fcl.util.LayoutConverter
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 官方 control-converter libcc.so 原生转换（Go JNI）。
 * 支持 FCL -> ZL2 与 ZL2 -> FCL 双向。
 */
object OfficialConverter {

    fun convertFclToZl2(context: Context, input: String): String =
        runConversion(context, input) { inFile, outFile -> LayoutConverter.convertFclToZl2(inFile, outFile) }
            .also { requireZl2(it) }

    fun convertZl2ToFcl(context: Context, input: String): String =
        runConversion(context, input) { inFile, outFile -> LayoutConverter.convertZl2ToFcl(inFile, outFile) }
            .also { requireFcl(it) }

    private inline fun runConversion(
        context: Context,
        input: String,
        nativeCall: (File, File) -> String?
    ): String {
        require(input.isNotBlank()) { "native 转换输入为空" }
        val dir = File(context.cacheDir, "official-converter").apply { mkdirs() }
        val prefix = "conversion-${UUID.randomUUID()}"
        val inFile = File(dir, "$prefix-input.json")
        val outFile = File(dir, "$prefix-output.json")
        try {
            inFile.writeText(input, Charsets.UTF_8)
            val error = nativeCall(inFile, outFile)
            require(error == null) { error ?: "官方转换器失败" }
            return requireNotNull(outFile.takeIf { it.isFile && it.length() > 0 }?.readText(Charsets.UTF_8)) {
                "官方转换器没有生成输出"
            }
        } finally {
            inFile.delete()
            outFile.delete()
        }
    }

    private fun requireZl2(raw: String) {
        val root = JSONObject(raw)
        require(root.optJSONArray("layers") != null) { "官方转换器输出不是 ZL2 布局" }
    }

    private fun requireFcl(raw: String) {
        val root = JSONObject(raw)
        require(root.optJSONArray("viewGroups") != null) { "官方转换器输出不是 FCL 布局" }
    }
}
