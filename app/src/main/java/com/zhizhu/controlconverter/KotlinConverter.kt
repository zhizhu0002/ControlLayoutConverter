package com.zhizhu.controlconverter

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger

/**
 * 纯 Kotlin 实现 ZL2 ↔ FCL 互转（无 JNI / 无 WebView 依赖）。
 *
 * 从 assets/index.html 中的 fclToZl2 / zl2ToFcl 及其配套辅助函数移植而来，
 * 用 Android 内置 org.json，保证与 JS 引擎语义一致。
 *
 * 内建完整性自检：转换完成后比较源/目标控件数量，若目标下降则抛错，
 * 宁可失败也不产出丢控件的不可靠结果（1:1 保护）。
 */
object KotlinConverter {

    // ---- 键码表：GLFW -> FCL ----
    private val GLFW_TO_FCL: Map<Int, Int> = mapOf(
        32 to 57, 39 to 40, 44 to 51, 45 to 12, 46 to 52, 47 to 53,
        59 to 39, 61 to 13,
        91 to 26, 92 to 43, 93 to 27, 96 to 41,
        256 to 1, 257 to 28, 258 to 15, 259 to 14,
        260 to 110, 261 to 111, 262 to 106, 263 to 105, 264 to 108, 265 to 103,
        266 to 104, 267 to 109, 268 to 102, 269 to 107,
        280 to 58, 281 to 70, 282 to 69, 283 to 99, 284 to 119,
        290 to 59, 291 to 60, 292 to 61, 293 to 62, 294 to 63, 295 to 64,
        296 to 65, 297 to 66, 298 to 67, 299 to 68, 300 to 87, 301 to 88,
        340 to 42, 341 to 29, 342 to 56, 343 to 125,
        344 to 54, 345 to 97, 346 to 100, 347 to 126,
        98 to 98, 99 to 55, 100 to 74, 101 to 78
    )

    // ---- 键码表：FCL -> GLFW ----
    private val FCL_TO_GLFW: Map<Int, Int> = GLFW_TO_FCL.entries
        .groupBy({ it.value }, { it.key })
        .mapValues { (_, keys) -> keys.min() }

    // ---- 旧特殊键码 -> ZL2 事件 ----
    private val SPECIAL: Map<String, Pair<String, String>> = mapOf(
        "-1" to ("launcher_event" to "launcher.event.switch_ime"),
        "-3" to ("launcher_event" to "GLFW_MOUSE_BUTTON_LEFT"),
        "-4" to ("launcher_event" to "GLFW_MOUSE_BUTTON_RIGHT"),
        "-6" to ("launcher_event" to "GLFW_MOUSE_BUTTON_MIDDLE"),
        "-7" to ("launcher_event" to "launcher.event.scroll_up"),
        "-8" to ("launcher_event" to "launcher.event.scroll_down"),
        "-9" to ("launcher_event" to "launcher.event.switch_menu")
    )

    // FCL 特殊键码 -> ZL2 launcher_event key
    private val FCL_SPECIAL_TO_ZL2: Map<Int, String> = mapOf(
        -1 to "launcher.event.switch_ime",
        -3 to "GLFW_MOUSE_BUTTON_LEFT",
        -4 to "GLFW_MOUSE_BUTTON_RIGHT",
        -6 to "GLFW_MOUSE_BUTTON_MIDDLE",
        -7 to "launcher.event.scroll_up.single",
        -8 to "launcher.event.scroll_down.single",
        -9 to "launcher.event.switch_menu"
    )

    private const val WHITE = 0xFFFFFFFFu
    private const val PRESSED_BG = 0x3CFFFFFFu

    // 常量：参考画布（Dp）
    private const val DP_SW = 800.0
    private const val DP_SH = 360.0
    private const val MARGIN_DP = 2.0

    // ================== 入口 ==================

    fun fclToZl2(text: String): String {
        val src = JSONObject(text)
        require(src.has("viewGroups")) { "不是有效的 FCL 控件布局（缺少 viewGroups）" }
        val out = fclToZl2Internal(src)
        // 完整性自检：FCL -> ZL2 控件数量守恒
        val srcTotal = countFcl(src)
        val dstTotal = countZl2(out)
        require(dstTotal >= srcTotal) {
            "控件完整性校验失败：FCL→ZL2 控件数量下降（source=$srcTotal target=$dstTotal）"
        }
        return out.toString()
    }

    fun zl2ToFcl(text: String): String {
        val src = JSONObject(text)
        require(src.has("layers")) { "不是有效的 ZL2 控件布局（缺少 layers）" }
        val out = zl2ToFclInternal(src)
        val srcTotal = countZl2(src)
        val dstTotal = countFcl(out)
        require(dstTotal >= srcTotal) {
            "控件完整性校验失败：ZL2→FCL 控件数量下降（source=$srcTotal target=$dstTotal）"
        }
        return out.toString()
    }

    // ================== 控件计数 ==================

    private fun countFcl(root: JSONObject): Int {
        var total = 0
        val groups = root.optJSONArray("viewGroups") ?: return 0
        for (g in 0 until groups.length()) {
            val vd = groups.getJSONObject(g).optJSONObject("viewData") ?: continue
            total += vd.optJSONArray("buttonList")?.length() ?: 0
            total += vd.optJSONArray("directionList")?.length() ?: 0
        }
        return total
    }

    private fun countZl2(root: JSONObject): Int {
        var total = 0
        val layers = root.optJSONArray("layers") ?: return 0
        for (l in 0 until layers.length()) {
            val layer = layers.getJSONObject(l)
            total += layer.optJSONArray("normalButtons")?.length() ?: 0
            total += layer.optJSONArray("textBoxes")?.length() ?: 0
            total += layer.optJSONArray("joystickButtons")?.length() ?: 0
        }
        return total
    }

    // ================== FCL -> ZL2 ==================

    private fun fclToZl2Internal(src: JSONObject): JSONObject {
        val layers = JSONArray()
        val styles = JSONArray()
        val styleMap = HashMap<String, String>()
        val sourceStyles = HashMap<String, JSONObject>()
        val buttonStyles = src.optJSONArray("buttonStyles")
        if (buttonStyles != null) {
            for (i in 0 until buttonStyles.length()) {
                val s = buttonStyles.getJSONObject(i)
                sourceStyles[s.optString("name", "Default")] = s
            }
        }

        fun styleFor(name: String?): String {
            val n = name ?: "Default"
            styleMap[n]?.let { return it }
            val id = uid(12)
            val s = sourceStyles[n] ?: sourceStyles["Default"] ?: JSONObject()
            val corner = s.optDouble("cornerRadius", 0.0)
            val cornerPressed = if (s.has("cornerRadiusPressed")) s.optDouble("cornerRadiusPressed", corner) else corner
            val c = JSONObject().apply {
                put("alpha", 1)
                put("pressedAlpha", 1)
                put("backgroundColor", fclColorToZl2(s.optLong("fillColor", 0L)))
                put("pressedBackgroundColor", fclColorToZl2(s.optLong("fillColorPressed", 0L)))
                put("contentColor", fclColorToZl2(s.optLong("textColor", WHITE.toLong())))
                put("pressedContentColor", fclColorToZl2(s.optLong("textColorPressed", WHITE.toLong())))
                put("borderWidth", s.optDouble("strokeWidth", 0.0) / 10.0)
                put("pressedBorderWidth", s.optDouble("strokeWidthPressed", 0.0) / 10.0)
                put("borderColor", fclColorToZl2(s.optLong("strokeColor", 0L)))
                put("pressedBorderColor", fclColorToZl2(s.optLong("strokeColorPressed", 0L)))
                put("borderRadius", JSONObject().apply {
                    put("topStart", corner / 10.0); put("topEnd", corner / 10.0)
                    put("bottomEnd", corner / 10.0); put("bottomStart", corner / 10.0)
                })
                put("pressedBorderRadius", JSONObject().apply {
                    put("topStart", cornerPressed / 10.0); put("topEnd", cornerPressed / 10.0)
                    put("bottomEnd", cornerPressed / 10.0); put("bottomStart", cornerPressed / 10.0)
                })
            }
            styleMap[n] = id
            styles.put(JSONObject().apply {
                put("name", n); put("uuid", id); put("animateSwap", false); put("commonStyle", true)
                put("lightStyle", c); put("darkStyle", JSONObject(c.toString()))
            })
            return id
        }

        // viewGroup id -> 新 uuid
        val groupIds = HashMap<String, String>()
        val groups = src.getJSONArray("viewGroups")
        for (g in 0 until groups.length()) {
            groupIds[groups.getJSONObject(g).optString("id")] = uid(12)
        }

        // FCL directionStyles -> ZL joystickStyles
        val joystickStyleByName = HashMap<String, String>()
        val joystickStyles = JSONArray()
        val dirStyles = src.optJSONArray("directionStyles")
        if (dirStyles != null) {
            for (i in 0 until dirStyles.length()) {
                val ds = dirStyles.getJSONObject(i)
                val r = ds.optJSONObject("rockerStyle") ?: JSONObject()
                val name = "ZL 摇杆 " + (ds.optString("name", "Default"))
                var cn = name
                var k = 2
                while (containsName(joystickStyles, cn)) { cn = name + "_" + k; k++ }
                val sid = uid(12)
                val c = JSONObject().apply {
                    put("alpha", 1)
                    put("backgroundColor", fclColorToZl2(0x80000000L))
                    put("joystickColor", fclColorToZl2(0x80FFFFFFL))
                    put("joystickCanLockColor", fclColorToZl2(0x80FFFF00L))
                    put("joystickLockedColor", fclColorToZl2(0x8000FF00L))
                    put("lockMarkColor", fclColorToZl2(0xFFFFFFFFL))
                    put("borderWidthRatio", clamp(r.optDouble("bgStrokeWidth", 0.0) / 10.0, 0.0, 50.0))
                    put("borderColor", fclColorToZl2(0xFFFFFFFFL))
                    put("backgroundShape", clamp(r.optDouble("bgCornerRadius", 500.0) / 10.0, 0.0, 50.0))
                    put("joystickShape", clamp(r.optDouble("rockerCornerRadius", 500.0) / 10.0, 0.0, 50.0))
                    put("joystickSize", clamp(r.optDouble("rockerSize", 500.0) / 1000.0, 0.0, 1.0))
                }
                joystickStyles.put(JSONObject().apply {
                    put("name", cn); put("uuid", sid); put("commonStyle", true)
                    put("lightStyle", c); put("darkStyle", JSONObject(c.toString()))
                })
                joystickStyleByName[ds.optString("name")] = sid
            }
        }
        // 默认摇杆样式兜底
        if (joystickStyles.length() == 0) {
            val dsid = uid(12)
            val dc = JSONObject().apply {
                put("alpha", 1); put("backgroundColor", fclColorToZl2(0x80000000L))
                put("joystickColor", fclColorToZl2(0x80FFFFFFL)); put("joystickCanLockColor", fclColorToZl2(0x80FFFF00L))
                put("joystickLockedColor", fclColorToZl2(0x8000FF00L)); put("lockMarkColor", fclColorToZl2(0xFFFFFFFFL))
                put("borderWidthRatio", 0); put("borderColor", fclColorToZl2(0xFFFFFFFFL))
                put("backgroundShape", 50); put("joystickShape", 50); put("joystickSize", 0.5)
            }
            joystickStyles.put(JSONObject().apply {
                put("name", "JOY"); put("uuid", dsid); put("commonStyle", true)
                put("lightStyle", dc); put("darkStyle", JSONObject(dc.toString()))
            })
            joystickStyleByName["__default__"] = dsid
        }

        // 源 joystickStyles 中未被引用的样式也保留
        val srcJoystickStyles = src.optJSONArray("joystickStyles")
        val outJoyIds = HashSet<String>()
        for (i in 0 until joystickStyles.length()) {
            outJoyIds.add(joystickStyles.getJSONObject(i).optString("uuid"))
        }
        srcJoystickStyles?.let { js ->
            for (i in 0 until js.length()) {
                val s = js.getJSONObject(i)
                if (s.has("uuid") && !outJoyIds.contains(s.getString("uuid"))) joystickStyles.put(s)
            }
        }

        // 生成各层
        for (g in 0 until groups.length()) {
            val group = groups.getJSONObject(g)
            val layer = JSONObject().apply {
                put("name", group.optString("name", "Default"))
                put("uuid", groupIds[group.optString("id")] ?: uid(12))
                put("hide", group.optString("visibility", "").equals("INVISIBLE", true))
                put("visibilityType", fclVisibility(group.optString("visibility")))
                put("normalButtons", JSONArray())
                put("textBoxes", JSONArray())
                put("joystickButtons", JSONArray())
            }
            group.optJSONObject("_control_converter_layer")?.let { layer.put("_control_converter_layer", it) }
            val vd = group.optJSONObject("viewData") ?: JSONObject()
            val buttonList = vd.optJSONArray("buttonList") ?: JSONArray()
            for (b in 0 until buttonList.length()) {
                val bj = buttonList.getJSONObject(b)
                val bi = bj.optJSONObject("baseInfo") ?: JSONObject()
                val w = bi.optDouble("absoluteWidth", 80.0)
                val h = bi.optDouble("absoluteHeight", 30.0)
                val ev = fclEventsToZl2(bj.optJSONObject("event"), groupIds)
                val btn = JSONObject().apply {
                    put("text", JSONObject().apply {
                        put("default", textValue(bj.opt("text"))); put("matchQueue", JSONArray())
                    })
                    put("uuid", bj.optString("id", uid(18)))
                    put("position", JSONObject().apply {
                        put("x", fclPercent(bi.optDouble("xPosition", 0.0)))
                        put("y", fclPercent(bi.optDouble("yPosition", 0.0)))
                    })
                    put("buttonSize", JSONObject().apply {
                        put("type", if (bi.optString("sizeType", "PERCENTAGE").equals("ABSOLUTE", true)) "dp" else "percentage")
                        put("widthDp", w); put("heightDp", h)
                        put("widthPercentage", Math.round(bi.optJSONObject("percentageWidth")?.optDouble("size", 100.0) ?: 100.0) * 10)
                        put("heightPercentage", Math.round(bi.optJSONObject("percentageHeight")?.optDouble("size", 100.0) ?: 100.0) * 10)
                        put("widthReference", refLower(bi.optJSONObject("percentageWidth")?.optString("reference", "SCREEN_HEIGHT") ?: "SCREEN_HEIGHT"))
                        put("heightReference", refLower(bi.optJSONObject("percentageHeight")?.optString("reference", "SCREEN_HEIGHT") ?: "SCREEN_HEIGHT"))
                    })
                    put("buttonStyle", styleFor(bj.optString("style")))
                    put("textAlignment", "Center")
                    put("visibilityType", fclVisibility(bi.optString("visibilityType", group.optString("visibility"))))
                    put("isSwipple", bj.optJSONObject("event")?.optBoolean("pointerFollow", false) ?: false)
                    put("isPenetrable", false)
                    put("isToggleable", false)
                }
                bj.optJSONObject("_control_converter")?.let { btn.put("_control_converter", it) }
                if (ev.length() > 0) btn.put("clickEvents", ev)
                layer.getJSONArray("normalButtons").put(btn)
            }
            val directionList = vd.optJSONArray("directionList") ?: JSONArray()
            for (d in 0 until directionList.length()) {
                layer.getJSONArray("joystickButtons").put(
                    fclDirectionToZlJoystick(directionList.getJSONObject(d), group, joystickStyleByName)
                )
            }
            layers.put(layer)
        }

        val name = textValue(src.opt("name"))
        val out = JSONObject().apply {
            put("_control_converter", src.opt("_control_converter") ?: JSONObject.NULL)
            put("info", JSONObject().apply {
                put("name", simpleText(name.ifBlank { "FCL 控件布局" }))
                put("author", simpleText(textValue(src.opt("author"))))
                put("description", JSONObject().apply {
                    put("default", textValue(src.opt("description"))); put("matchQueue", JSONArray())
                })
                put("versionCode", src.optInt("versionCode", 0))
                put("versionName", src.optString("version", "1.0"))
            })
            put("layers", layers); put("styles", styles); put("joystickStyles", joystickStyles)
            put("editorVersion", 10)
        }
        stripForeignMeta(out)
        return out
    }

    // FCL direction -> ZL joystickButton
    private fun fclDirectionToZlJoystick(direction: JSONObject, group: JSONObject, styleById: Map<String, String>): JSONObject {
        val bi = direction.optJSONObject("baseInfo") ?: JSONObject()
        val isAbs = bi.optString("sizeType", "PERCENTAGE").equals("ABSOLUTE", true)
        val pct = bi.optJSONObject("percentageWidth")?.optDouble("size", 300.0) ?: 300.0
        val sizeDp = bi.optDouble("absoluteWidth", 50.0).takeIf { it > 0 } ?: bi.optDouble("absoluteHeight", 50.0)
        val styleId = styleById[direction.optString("style")] ?: styleById["__default__"] ?: ""
        val ev = direction.optJSONObject("event") ?: JSONObject()
        val de = JSONObject().apply {
            put("north", fclDirKeycodeList(ev.opt("upKeycode")))
            put("south", fclDirKeycodeList(ev.opt("downKeycode")))
            put("west", fclDirKeycodeList(ev.opt("leftKeycode")))
            put("east", fclDirKeycodeList(ev.opt("rightKeycode")))
        }
        val usedStyleId = direction.optJSONObject("_control_converter")?.optString("joystickStyleId")?.takeIf { it.isNotEmpty() }
            ?: (styleId.ifEmpty { styleById["__default__"] ?: "" })
        return JSONObject().apply {
            put("uuid", direction.optString("id", uid(18)))
            put("position", JSONObject().apply {
                put("x", fclPercent(bi.optDouble("xPosition", 0.0)))
                put("y", fclPercent(bi.optDouble("yPosition", 0.0)))
            })
            put("sizeType", if (isAbs) "dp" else "Percentage")
            put("sizeDp", sizeDp)
            put("sizePercentage", if (isAbs) Math.round(sizeDp / DP_SH * 10000) else clamp(pct * 10, 100.0, 10000.0).toInt())
            put("joystickStyleId", usedStyleId)
            put("deadZoneRatio", 0.15); put("lockThreshold", 0.3); put("canLock", false)
            put("triggerMode", if (isAbs) "absolute" else "touch")
            put("directionEvents", de); put("lockEvents", JSONArray())
            put("visibilityType", fclVisibility(bi.optString("visibilityType", group.optString("visibility"))))
        }
    }

    // ================== ZL2 -> FCL ==================

    private fun zl2ToFclInternal(src: JSONObject): JSONObject {
        val layerArr = src.getJSONArray("layers")
        val groupIds = HashMap<String, String>()
        for (l in 0 until layerArr.length()) {
            val layer = layerArr.getJSONObject(l)
            if (!layer.optString("name").equals("GUI", true)) groupIds[layer.optString("uuid")] = uid(12)
        }

        val styleMap = HashMap<String, String>()
        val buttonStyles = JSONArray()
        val styles = src.optJSONArray("styles")
        if (styles != null) {
            for (i in 0 until styles.length()) {
                val s = styles.getJSONObject(i)
                val name = s.optString("name", "style_$i")
                styleMap[s.optString("uuid")] = name
                buttonStyles.put(zl2StyleToFcl(s, name))
            }
        }
        if (buttonStyles.length() == 0) buttonStyles.put(zl2StyleToFcl(null, "Default"))

        // 摇杆样式 -> FCL 方向样式
        val directionStyles = JSONArray()
        val rockerNameByJoystickStyleId = HashMap<String, String>()
        val joystickStyles = src.optJSONArray("joystickStyles")
        if (joystickStyles != null) {
            for (i in 0 until joystickStyles.length()) {
                val js = joystickStyles.getJSONObject(i)
                val light = js.optJSONObject("lightStyle") ?: JSONObject()
                val base = "ZL 摇杆 ${js.optString("name", "Default")}"
                var name = base
                var k = 2
                while (containsNameStr(directionStyles, name)) { name = base + "_" + k; k++ }
                val rocker = JSONObject().apply {
                    put("rockerSize", clamp(light.optDouble("joystickSize", 0.5) * 1000.0, 100.0, 1000.0).toInt())
                    put("bgCornerRadius", clamp(light.optDouble("backgroundShape", 50.0) * 10.0, 0.0, 500.0).toInt())
                    put("bgStrokeWidth", clamp(light.optDouble("borderWidthRatio", 0.0) * 10.0, 0.0, 500.0).toInt())
                    put("bgStrokeColor", composeColor(largeLong(light.optString("borderColor")), -12303292))
                    put("bgFillColor", composeColor(largeLong(light.optString("backgroundColor")), 0))
                    put("rockerCornerRadius", clamp(light.optDouble("joystickShape", 50.0) * 10.0, 0.0, 500.0).toInt())
                    put("rockerStrokeWidth", 10)
                    put("rockerStrokeColor", composeColor(largeLong(light.optString("joystickColor")), -12303292))
                    put("rockerFillColor", composeColor(largeLong(light.optString("joystickColor")), -7829368))
                }
                val alpha = light.optDouble("alpha", 1.0)
                if (alpha < 0.999) {
                    rocker.put("bgFillColor", applyColorAlpha(rocker.optInt("bgFillColor"), alpha))
                    rocker.put("rockerFillColor", applyColorAlpha(rocker.optInt("rockerFillColor"), alpha))
                }
                directionStyles.put(JSONObject().apply {
                    put("name", name); put("styleType", "ROCKER"); put("rockerStyle", rocker)
                })
                if (js.has("uuid")) rockerNameByJoystickStyleId[js.getString("uuid")] = name
            }
        }
        if (directionStyles.length() == 0) {
            directionStyles.put(JSONObject().apply {
                put("name", "Default Rocker"); put("styleType", "ROCKER")
                put("rockerStyle", JSONObject().apply {
                    put("rockerSize", 500); put("bgCornerRadius", 500); put("bgStrokeWidth", 0)
                    put("bgStrokeColor", -12303292); put("bgFillColor", -2147483648)
                    put("rockerCornerRadius", 500); put("rockerStrokeWidth", 10)
                    put("rockerStrokeColor", -12303292); put("rockerFillColor", -7829368)
                })
            })
        }
        val defaultRockerName = directionStyles.getJSONObject(0).optString("name")

        val srcJoyStyleById = HashMap<String, JSONObject>()
        joystickStyles?.let { js ->
            for (i in 0 until js.length()) {
                val s = js.getJSONObject(i)
                if (s.has("uuid")) srcJoyStyleById[s.getString("uuid")] = s
            }
        }

        // 非 GUI 层 -> viewGroup（逆序）
        val groups = JSONArray()
        val normalLayers = JSONArray()
        for (l in 0 until layerArr.length()) {
            val layer = layerArr.getJSONObject(l)
            if (!layer.optString("name").equals("GUI", true)) normalLayers.put(layer)
        }
        for (li in normalLayers.length() - 1 downTo 0) {
            val layer = normalLayers.getJSONObject(li)
            val buttons = JSONArray()
            val normalBtns = layer.optJSONArray("normalButtons") ?: JSONArray()
            for (b in 0 until normalBtns.length()) {
                buttons.put(zl2ButtonToFcl(normalBtns.getJSONObject(b), groupIds, styleMap, buttonStyles))
            }
            val directions = JSONArray()
            val joysticks = layer.optJSONArray("joystickButtons") ?: JSONArray()
            for (j in 0 until joysticks.length()) {
                directions.put(zl2JoystickToFclDirection(joysticks.getJSONObject(j), layer, groupIds, defaultRockerName, srcJoyStyleById))
            }
            val group = JSONObject().apply {
                put("id", groupIds[layer.optString("uuid")] ?: uid(12))
                put("name", layer.optString("name", "Default"))
                put("visibility", if (layer.optBoolean("hide", false)) "INVISIBLE" else "VISIBLE")
                put("viewData", JSONObject().apply {
                    put("buttonList", buttons); put("directionList", directions)
                })
            }
            layer.optJSONObject("_control_converter_layer")?.let { group.put("_control_converter_layer", it) }
            groups.put(group)
        }

        // GUI 层 -> GUI viewGroup
        val guiLayer = layerArr.let { arr ->
            for (l in 0 until arr.length()) {
                val layer = arr.getJSONObject(l)
                if (layer.optString("name").equals("GUI", true)) return@let layer
            }
            null
        }
        if (guiLayer != null) {
            val gb = guiLayer.optJSONArray("normalButtons")?.optJSONObject(0)
            if (gb != null && groups.length() > 0) {
                val gp = gb.optJSONObject("position") ?: JSONObject()
                val gs = gb.optJSONObject("buttonSize") ?: JSONObject()
                val guiGroup = JSONObject().apply {
                    put("id", uid(12)); put("name", "GUI"); put("visibility", "VISIBLE")
                    put("viewData", JSONObject().apply { put("buttonList", JSONArray()); put("directionList", JSONArray()) })
                }
                val guiBtn = JSONObject().apply {
                    put("id", gb.optString("uuid", uid(18)))
                    put("text", textValue(gb.opt("text")).ifEmpty { "GUI" })
                    put("style", styleMap[gb.optString("buttonStyle")] ?: buttonStyles.getJSONObject(0).optString("name"))
                    put("baseInfo", JSONObject().apply {
                        put("visibilityType", "ALWAYS")
                        put("xPosition", Math.round(gp.optDouble("x", 0.0) / 10.0))
                        put("yPosition", Math.round(gp.optDouble("y", 0.0) / 10.0))
                        put("sizeType", if (gs.optString("type", "").equals("percentage", true)) "PERCENTAGE" else "ABSOLUTE")
                        put("absoluteWidth", Math.round(gs.optDouble("widthDp", 80.0)))
                        put("absoluteHeight", Math.round(gs.optDouble("heightDp", 30.0)))
                        put("percentageWidth", JSONObject().apply {
                            put("reference", gs.optString("widthReference", "screen_height").uppercase())
                            put("size", Math.round(gs.optDouble("widthPercentage", 1000.0)) / 10.0)
                        })
                        put("percentageHeight", JSONObject().apply {
                            put("reference", gs.optString("heightReference", "screen_height").uppercase())
                            put("size", Math.round(gs.optDouble("heightPercentage", 1000.0)) / 10.0)
                        })
                    })
                    put("event", JSONObject().apply {
                        put("pointerFollow", false); put("Movable", false)
                        put("pressEvent", JSONObject().apply {
                            put("autoKeep", false); put("autoClick", false); put("openMenu", true)
                            put("switchTouchMode", false); put("switchMouseMode", false); put("input", false)
                            put("quickInput", false); put("outputText", ""); put("outputKeycodes", JSONArray()); put("bindViewGroup", JSONArray())
                        })
                        put("longPressEvent", makeFclEvent()); put("clickEvent", makeFclEvent()); put("doubleClickEvent", makeFclEvent())
                    })
                }
                guiGroup.getJSONObject("viewData").getJSONArray("buttonList").put(guiBtn)
                groups.put(guiGroup)
            }
        }

        val info = src.optJSONObject("info") ?: JSONObject()
        val out = JSONObject().apply {
            put("_control_converter", src.opt("_control_converter") ?: JSONObject.NULL)
            put("id", uid(8))
            put("name", textValue(info.opt("name")).ifBlank { "ZL2 转换布局" })
            put("version", textValue(info.opt("versionName")).ifBlank { "1.0" })
            put("versionCode", info.optInt("versionCode", 0))
            put("author", textValue(info.opt("author")))
            put("description", textValue(info.opt("description")))
            put("controllerVersion", 21)
            put("buttonStyles", buttonStyles); put("directionStyles", directionStyles); put("viewGroups", groups)
        }
        stripForeignMeta(out)
        return out
    }

    private fun zl2ButtonToFcl(b: JSONObject, groupIds: Map<String, String>, styleMap: Map<String, String>, buttonStyles: JSONArray): JSONObject {
        val p = b.optJSONObject("position") ?: JSONObject()
        val sz = b.optJSONObject("buttonSize") ?: JSONObject()
        val press = zl2EventsToFcl(b.optJSONArray("clickEvents"), groupIds)
        return JSONObject().apply {
            put("id", b.optString("uuid", uid(18)))
            put("text", textValue(b.opt("text")))
            put("style", styleMap[b.optString("buttonStyle")] ?: buttonStyles.getJSONObject(0).optString("name"))
            put("baseInfo", JSONObject().apply {
                put("visibilityType", zl2Visibility(b.optString("visibilityType")))
                put("xPosition", Math.round(p.optDouble("x", 0.0) / 10.0))
                put("yPosition", Math.round(p.optDouble("y", 0.0) / 10.0))
                put("sizeType", if (sz.optString("type", "").equals("percentage", true)) "PERCENTAGE" else "ABSOLUTE")
                put("absoluteWidth", Math.round(sz.optDouble("widthDp", 80.0)))
                put("absoluteHeight", Math.round(sz.optDouble("heightDp", 30.0)))
                put("percentageWidth", JSONObject().apply {
                    put("reference", sz.optString("widthReference", "screen_height").uppercase())
                    put("size", Math.round(sz.optDouble("widthPercentage", 1000.0)) / 10.0)
                })
                put("percentageHeight", JSONObject().apply {
                    put("reference", sz.optString("heightReference", "screen_height").uppercase())
                    put("size", Math.round(sz.optDouble("heightPercentage", 1000.0)) / 10.0)
                })
            })
            put("event", JSONObject().apply {
                put("pointerFollow", b.optBoolean("isSwipple", false))
                put("Movable", b.optBoolean("isSwipple", false))
                put("pressEvent", press)
                put("longPressEvent", makeFclEvent()); put("clickEvent", makeFclEvent()); put("doubleClickEvent", makeFclEvent())
            })
        }
    }

    private fun zl2JoystickToFclDirection(joystick: JSONObject, layer: JSONObject, groupIds: Map<String, String>, defaultRockerName: String, srcJoyStyleById: Map<String, JSONObject>): JSONObject {
        val p = joystick.optJSONObject("position") ?: JSONObject()
        val de = joystick.optJSONObject("directionEvents") ?: JSONObject()
        val rockerName = joystick.optString("joystickStyleId", "").takeIf { it.isNotEmpty() }
            ?: defaultRockerName
        val sizeType = joystick.optString("sizeType", "Percentage").lowercase()
        val isAbs = sizeType == "dp" || sizeType == "dip" || sizeType == "absolute"
        val baseInfo = JSONObject().apply {
            put("visibilityType", zl2Visibility(joystick.optString("visibilityType", layer.optString("visibilityType"))))
            put("xPosition", clamp(p.optDouble("x", 0.0) / 10.0, 0.0, 1000.0).toInt())
            put("yPosition", clamp(p.optDouble("y", 0.0) / 10.0, 0.0, 1000.0).toInt())
            if (isAbs) {
                val abs = maxOf(5, Math.round(joystick.optDouble("sizeDp", 50.0)))
                put("sizeType", "ABSOLUTE"); put("absoluteWidth", abs); put("absoluteHeight", abs)
                put("percentageWidth", JSONObject().apply { put("reference", "SCREEN_HEIGHT"); put("size", 300) })
                put("percentageHeight", JSONObject().apply { put("reference", "SCREEN_HEIGHT"); put("size", 300) })
            } else {
                val pct = clamp(joystick.optDouble("sizePercentage", 2500.0) / 10.0, 100.0, 1000.0)
                put("sizeType", "PERCENTAGE")
                put("absoluteWidth", maxOf(5, Math.round(joystick.optDouble("sizeDp", 50.0))))
                put("absoluteHeight", maxOf(5, Math.round(joystick.optDouble("sizeDp", 50.0))))
                put("percentageWidth", JSONObject().apply { put("reference", "SCREEN_HEIGHT"); put("size", pct) })
                put("percentageHeight", JSONObject().apply { put("reference", "SCREEN_HEIGHT"); put("size", pct) })
            }
        }
        return JSONObject().apply {
            put("id", joystick.optString("uuid", uid(18)))
            put("baseInfo", baseInfo)
            put("event", JSONObject().apply {
                put("upKeycode", zl2DirKeycodes(de.optJSONArray("north")))
                put("downKeycode", zl2DirKeycodes(de.optJSONArray("south")))
                put("leftKeycode", zl2DirKeycodes(de.optJSONArray("west")))
                put("rightKeycode", zl2DirKeycodes(de.optJSONArray("east")))
            })
            put("style", rockerName)
        }
    }

    // ================== 辅助：颜色 ==================

    /** FCL 32 位 ARGB -> ZL2 signed 64-bit packed string（ARGB 放高 32 位）。 */
    private fun fclColorToZl2(v: Long): String {
        val n = v and 0xFFFFFFFFL
        val big = BigInteger.valueOf(n).shiftLeft(32)
        return big.toString()
    }

    /** 解析 org.json 的 Long/string 大整数 -> Long。 */
    private fun largeLong(v: Any?): Long = when (v) {
        is Long -> v
        is Int -> v.toLong()
        is String -> v.toLongOrNull() ?: 0L
        else -> 0L
    }

    /** ZL2 signed 64-bit packed -> ARGB int。 */
    private fun composeColor(v: Long, fallback: Int): Int {
        // JS composeColorToArgb：仅当数字为 64-bit 打包 Long（>10 位、或超出 32 位有符号/无符号范围）
        // 才提取高 32 位；否则当作普通 32 位 ARGB。
        val packed = v.toString().length > 10 || v < -2147483648L || v > 4294967295L
        if (!packed) {
            return signedColor(v and 0xFFFFFFFFL, fallback.toLong()).toInt()
        }
        val big = BigInteger.valueOf(v)
        val b = if (big < BigInteger.ZERO) big.add(BigInteger.ONE.shiftLeft(64)) else big
        val hex = b.toString(16).padStart(16, '0')
        val upper = hex.substring(0, 8)
        val intVal = upper.toLong(16).toInt().toLong()
        return signedColor(intVal, fallback.toLong()).toInt()
    }

    private fun signedColor(n: Long, fallback: Long): Long = (n and 0xFFFFFFFFL)

    private fun normColor(c: Long): Long = (c and 0xFFFFFFFFL)

    /** 把 ARGB 颜色乘以整体 alpha。 */
    private fun applyColorAlpha(color: Int, alpha: Double): Int {
        val argb = color.toLong() and 0xFFFFFFFFL
        val a = ((argb ushr 24) and 0xFF).toInt()
        val aa = Math.round(a * clamp(alpha, 0.0, 1.0)).toInt().coerceIn(0, 255)
        return ((aa shl 24) or (argb.toInt() and 0x00FFFFFF))
    }

    // ================== 辅助：事件 ==================

    private fun makeFclEvent(): JSONObject = JSONObject().apply {
        put("autoKeep", false); put("autoClick", false); put("openMenu", false)
        put("switchTouchMode", false); put("switchMouseMode", false)
        put("input", false); put("quickInput", false); put("outputText", "")
        put("outputKeycodes", JSONArray()); put("bindViewGroup", JSONArray())
    }

    private fun fclEventsToZl2(event: JSONObject?, groupIds: Map<String, String>): JSONArray {
        val out = JSONArray()
        val p = event?.optJSONObject("pressEvent") ?: JSONObject()
        if (p.optBoolean("input", false)) out.put(JSONObject().apply { put("type", "launcher_event"); put("key", "launcher.event.switch_ime") })
        if (p.optBoolean("openMenu", false)) out.put(JSONObject().apply { put("type", "launcher_event"); put("key", "launcher.event.switch_menu") })
        val ok = p.optJSONArray("outputKeycodes")
        if (ok != null) {
            for (i in 0 until ok.length()) {
                val k = ok.optInt(i)
                if (k == 0 || k == -2 || k == -5) continue
                if (k > 0) {
                    val name = glfwFromFclKey(k)
                    if (name != null) out.put(JSONObject().apply { put("type", "key"); put("key", name) })
                } else {
                    FCL_SPECIAL_TO_ZL2[k]?.let { out.put(JSONObject().apply { put("type", "launcher_event"); put("key", it) }) }
                }
            }
        }
        val bvg = p.optJSONArray("bindViewGroup")
        if (bvg != null) {
            for (i in 0 until bvg.length()) {
                val id = bvg.optString(i)
                groupIds[id]?.let { out.put(JSONObject().apply { put("type", "switch_layer"); put("key", it) }) }
            }
        }
        return out
    }

    private fun zl2EventsToFcl(events: JSONArray?, groupIds: Map<String, String>): JSONObject {
        val out = makeFclEvent()
        if (events != null) {
            for (i in 0 until events.length()) {
                val e = events.getJSONObject(i)
                when (e.optString("type")) {
                    "key" -> {
                        val k = fclKeyFromGlfw(e.optString("key"))
                        if (k != null) out.getJSONArray("outputKeycodes").put(k)
                    }
                    "launcher_event" -> {
                        val sk = specialKeyFromEvent(e.optString("key"))
                        if (sk != null) out.getJSONArray("outputKeycodes").put(sk)
                    }
                    "switch_layer" -> {
                        groupIds[e.optString("key")]?.let { out.getJSONArray("bindViewGroup").put(it) }
                    }
                }
            }
        }
        return out
    }

    private fun zl2DirKeycodes(events: JSONArray?): JSONArray {
        val out = JSONArray()
        if (events != null) {
            for (i in 0 until events.length()) {
                val e = events.getJSONObject(i)
                if (e.optString("type") == "key") {
                    val k = fclKeyFromGlfw(e.optString("key"))
                    if (k != null) out.put(k)
                } else {
                    val sk = specialKeyFromEvent(e.optString("key"))
                    if (sk != null) out.put(sk)
                }
            }
        }
        return out
    }

    private fun fclDirKeycodeList(v: Any?): JSONArray {
        val out = JSONArray()
        val list: List<Int> = when (v) {
            is JSONArray -> (0 until v.length()).map { v.optInt(it) }
            null -> emptyList()
            is Int -> listOf(v)
            is Long -> listOf(v.toInt())
            is Number -> listOf(v.toInt())
            else -> emptyList()
        }
        list.forEach { k ->
            if (k != 0) glfwFromFclKey(k)?.let { out.put(JSONObject().apply { put("type", "key"); put("key", it) }) }
        }
        return out
    }

    // ================== 辅助：键码 ==================

    private fun specialKeyFromEvent(key: String): Int? = when (key) {
        "launcher.event.switch_ime" -> -1
        "GLFW_MOUSE_BUTTON_LEFT" -> -3
        "GLFW_MOUSE_BUTTON_RIGHT" -> -4
        "GLFW_MOUSE_BUTTON_MIDDLE" -> -6
        "launcher.event.scroll_up.single" -> -7
        "launcher.event.scroll_down.single" -> -8
        "launcher.event.switch_menu" -> -9
        else -> null
    }

    private fun fclKeyFromGlfw(key: String): Int? = key.toIntOrNull()?.let { GLFW_TO_FCL[it] }
    private fun glfwFromFclKey(key: Int): String? = FCL_TO_GLFW[key]?.toString()

    // ================== 辅助：样式 ==================

    private fun zl2StyleToFcl(style: JSONObject?, name: String): JSONObject {
        val c = style?.optJSONObject("lightStyle") ?: JSONObject()
        val p = c.optJSONObject("pressedBorderRadius") ?: c.optJSONObject("borderRadius") ?: JSONObject()
        return JSONObject().apply {
            put("name", name)
            put("textColor", composeColor(largeLong(c.opt("contentColor")), -1))
            put("textSize", 12)
            put("strokeColor", composeColor(largeLong(c.opt("borderColor")), 0))
            put("strokeWidth", Math.round(c.optDouble("borderWidth", 0.0) * 10.0))
            put("cornerRadius", Math.round(c.optJSONObject("borderRadius")?.optDouble("topStart", 0.0) ?: 0.0) * 10)
            put("fillColor", composeColor(largeLong(c.opt("backgroundColor")), 0))
            put("textColorPressed", composeColor(largeLong(c.opt("pressedContentColor")), -1))
            put("textSizePressed", 12)
            put("strokeColorPressed", composeColor(largeLong(c.opt("pressedBorderColor")), 0))
            put("strokeWidthPressed", Math.round(c.optDouble("pressedBorderWidth", 0.0) * 10.0))
            put("cornerRadiusPressed", Math.round(p.optDouble("topStart", 0.0)) * 10)
            put("fillColorPressed", composeColor(largeLong(c.opt("pressedBackgroundColor")), 0))
        }
    }

    // ================== 辅助：常用工具 ==================

    private fun fclVisibility(v: String): String = when (v.uppercase()) {
        "IN_GAME" -> "in_game"
        "MENU" -> "in_menu"
        else -> "always"
    }

    private fun zl2Visibility(v: String): String = when (v.lowercase()) {
        "in_game" -> "IN_GAME"
        "in_menu" -> "MENU"
        else -> "ALWAYS"
    }

    private fun refLower(v: String): String = v.lowercase()

    private fun fclPercent(v: Double): Int = Math.round(clamp(v, 0.0, 1000.0) * 10.0).toInt().coerceIn(0, 10000)

    private fun clamp(v: Double, lo: Double, hi: Double): Double = Math.max(lo, Math.min(hi, v))

    private fun textValue(v: Any?): String {
        return when (v) {
            is JSONObject -> v.optString("default", v.optString("zh_CN", v.optString("en_US", "")))
            null -> ""
            is String -> v
            else -> v.toString()
        }
    }

    private fun simpleText(value: String): JSONObject = JSONObject().apply {
        put("default", value); put("matchQueue", JSONArray())
    }

    private fun containsName(arr: JSONArray, name: String): Boolean {
        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i).optString("name") == name) return true
        }
        return false
    }

    private fun containsNameStr(arr: JSONArray, name: String): Boolean {
        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i).optString("name") == name) return true
        }
        return false
    }

    /** 递归移除仅供往返恢复的内部辅助字段。 */
    private fun stripForeignMeta(value: Any): Any {
        when (value) {
            is JSONArray -> for (i in 0 until value.length()) { stripForeignMeta(value.get(i)) }
            is JSONObject -> {
                value.remove("_control_byIQge报错别找我")
                value.remove("_control_converter")
                value.remove("_control_converter_layer")
                value.remove("_control_converter_grid")
                val keys = value.keys()
                while (keys.hasNext()) { stripForeignMeta(value.get(keys.next())) }
            }
        }
        return value
    }

    private val UUID_CHARS = "0123456789abcdef"
    private fun uid(n: Int): String {
        val sb = StringBuilder(n)
        repeat(n) { sb.append(UUID_CHARS[Math.floor(Math.random() * 16).toInt()]) }
        return sb.toString()
    }
}
