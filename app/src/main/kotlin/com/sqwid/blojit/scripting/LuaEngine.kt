package com.sqwid.blojit.scripting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JsePlatform

/**
 * Wraps a LuaJ VM and exposes the runtime API that block-generated scripts call.
 *
 * The [drawCommands] list is rebuilt on every `engine.on_draw` invocation; the host UI
 * (RuntimeCanvas) consumes it on the main thread to render shapes. The engine intentionally
 * does NOT hold a reference to the UI — it just records draw commands and lets the UI poll.
 */
class LuaEngine {

    /** Recorded draw operations from the most recent draw frame. */
    private val drawBuffer = mutableListOf<DrawCommand>()
    @Volatile private var publishedFrame: List<DrawCommand> = emptyList()

    /** Current pen / state. */
    private var currentColor: Color = Color.White
    private var strokeWidth: Float = 2f
    private var alpha: Float = 1f

    /** Most recent log lines (for the run console). */
    private val logBuffer = ArrayDeque<String>()
    private val maxLogLines = 200

    /** Registered hook callbacks. */
    private var onStart: LuaFunction? = null
    private var onUpdate: LuaFunction? = null
    private var onDraw: LuaFunction? = null
    private var onTouchDown: LuaFunction? = null
    private var onTouchUp: LuaFunction? = null
    private var onTouchMove: LuaFunction? = null

    private val globalVars = mutableMapOf<String, Any?>()

    private val globals: Globals = JsePlatform.standardGlobals().also { g ->
        g.set("engine", buildEngineTable())
    }

    /**
     * Compile and execute the supplied Lua source. Returns a result that contains either
     * the error message (if compilation/execution failed) or success.
     */
    fun run(source: String): Result {
        return try {
            drawBuffer.clear()
            publishedFrame = emptyList()
            logBuffer.clear()
            val chunk = globals.load(source, "script")
            chunk.call()
            // run on_start once if registered
            onStart?.call()
            // produce an initial frame
            renderFrame()
            Result.Success
        } catch (e: LuaError) {
            Result.Error(e.message ?: e.toString())
        } catch (e: Exception) {
            Result.Error(e.message ?: e.toString())
        }
    }

    fun stepUpdate(dt: Double) {
        try {
            onUpdate?.call(LuaValue.valueOf(dt))
        } catch (e: LuaError) { log("update error: ${e.message}") }
    }

    fun renderFrame() {
        drawBuffer.clear()
        try {
            onDraw?.call()
        } catch (e: LuaError) { log("draw error: ${e.message}") }
        publishedFrame = drawBuffer.toList()
    }

    fun fireTouch(kind: String, x: Float, y: Float) {
        try {
            val fn = when (kind) {
                "down" -> onTouchDown
                "up" -> onTouchUp
                "move" -> onTouchMove
                else -> null
            }
            fn?.call(LuaValue.valueOf(x.toDouble()), LuaValue.valueOf(y.toDouble()))
        } catch (e: LuaError) { log("touch error: ${e.message}") }
    }

    fun snapshot(): List<DrawCommand> = publishedFrame
    fun logs(): List<String> = logBuffer.toList()

    private fun log(msg: String) {
        if (logBuffer.size >= maxLogLines) logBuffer.removeFirst()
        logBuffer.addLast(msg)
    }

    // ----- engine.* table construction --------------------------------------------------

    private fun buildEngineTable(): LuaTable {
        val t = LuaTable()

        fun reg(name: String, fn: VarArgFunction) { t.set(name, fn) }

        // ---- shape draw commands ----------------------------------------------------
        reg("draw_rect", varargFn { args ->
            val cmd = DrawCommand.Rect(
                x = args.checkdouble(1).toFloat(),
                y = args.checkdouble(2).toFloat(),
                w = args.checkdouble(3).toFloat(),
                h = args.checkdouble(4).toFloat(),
                color = parseLuaColor(args.optdouble(5, currentColor.toARGBLong().toDouble()).toLong()),
                radius = args.optdouble(6, 0.0).toFloat(),
                filled = args.optboolean(7, true),
                alpha = alpha
            )
            drawBuffer.add(cmd)
            LuaValue.NIL
        })
        reg("draw_circle", varargFn { args ->
            drawBuffer.add(DrawCommand.Circle(
                cx = args.checkdouble(1).toFloat(),
                cy = args.checkdouble(2).toFloat(),
                r = args.checkdouble(3).toFloat(),
                color = parseLuaColor(args.optdouble(4, currentColor.toARGBLong().toDouble()).toLong()),
                filled = args.optboolean(5, true),
                alpha = alpha
            ))
            LuaValue.NIL
        })
        reg("draw_oval", varargFn { args ->
            drawBuffer.add(DrawCommand.Oval(
                x = args.checkdouble(1).toFloat(),
                y = args.checkdouble(2).toFloat(),
                w = args.checkdouble(3).toFloat(),
                h = args.checkdouble(4).toFloat(),
                color = parseLuaColor(args.optdouble(5, currentColor.toARGBLong().toDouble()).toLong()),
                filled = args.optboolean(6, true),
                alpha = alpha
            ))
            LuaValue.NIL
        })
        reg("draw_line", varargFn { args ->
            drawBuffer.add(DrawCommand.Line(
                x1 = args.checkdouble(1).toFloat(),
                y1 = args.checkdouble(2).toFloat(),
                x2 = args.checkdouble(3).toFloat(),
                y2 = args.checkdouble(4).toFloat(),
                color = parseLuaColor(args.optdouble(5, currentColor.toARGBLong().toDouble()).toLong()),
                width = args.optdouble(6, strokeWidth.toDouble()).toFloat(),
                alpha = alpha
            ))
            LuaValue.NIL
        })
        reg("draw_triangle", varargFn { args ->
            drawBuffer.add(DrawCommand.Triangle(
                x1 = args.checkdouble(1).toFloat(),
                y1 = args.checkdouble(2).toFloat(),
                x2 = args.checkdouble(3).toFloat(),
                y2 = args.checkdouble(4).toFloat(),
                x3 = args.checkdouble(5).toFloat(),
                y3 = args.checkdouble(6).toFloat(),
                color = parseLuaColor(args.optdouble(7, currentColor.toARGBLong().toDouble()).toLong()),
                filled = args.optboolean(8, true),
                alpha = alpha
            ))
            LuaValue.NIL
        })
        reg("draw_arc", varargFn { args ->
            drawBuffer.add(DrawCommand.Arc(
                x = args.checkdouble(1).toFloat(),
                y = args.checkdouble(2).toFloat(),
                w = args.checkdouble(3).toFloat(),
                h = args.checkdouble(4).toFloat(),
                start = args.checkdouble(5).toFloat(),
                sweep = args.checkdouble(6).toFloat(),
                color = parseLuaColor(args.optdouble(7, currentColor.toARGBLong().toDouble()).toLong()),
                filled = args.optboolean(8, false),
                alpha = alpha
            ))
            LuaValue.NIL
        })
        reg("draw_text", varargFn { args ->
            drawBuffer.add(DrawCommand.Text(
                text = args.checkjstring(1),
                x = args.checkdouble(2).toFloat(),
                y = args.checkdouble(3).toFloat(),
                size = args.checkdouble(4).toFloat(),
                color = parseLuaColor(args.optdouble(5, currentColor.toARGBLong().toDouble()).toLong()),
                alpha = alpha
            ))
            LuaValue.NIL
        })
        reg("draw_polygon", varargFn { args ->
            val table = args.checktable(1)
            val pts = mutableListOf<Float>()
            var i = 1
            while (true) {
                val v = table.get(i); if (v.isnil()) break
                pts.add(v.todouble().toFloat()); i++
            }
            drawBuffer.add(DrawCommand.Polygon(
                points = pts,
                color = parseLuaColor(args.optdouble(2, currentColor.toARGBLong().toDouble()).toLong()),
                filled = args.optboolean(3, true),
                alpha = alpha
            ))
            LuaValue.NIL
        })
        reg("clear", varargFn { args ->
            drawBuffer.add(DrawCommand.Clear(
                color = parseLuaColor(args.optdouble(1, 0.0).toLong())
            ))
            LuaValue.NIL
        })

        // ---- drawing state ---------------------------------------------------------
        reg("set_color", varargFn { args ->
            currentColor = parseLuaColor(args.checkdouble(1).toLong()); LuaValue.NIL
        })
        reg("set_stroke", varargFn { args ->
            strokeWidth = args.checkdouble(1).toFloat(); LuaValue.NIL
        })
        reg("set_alpha", varargFn { args ->
            alpha = args.checkdouble(1).toFloat().coerceIn(0f, 1f); LuaValue.NIL
        })
        reg("translate", varargFn { args ->
            drawBuffer.add(DrawCommand.Transform.Translate(
                args.checkdouble(1).toFloat(), args.checkdouble(2).toFloat()
            )); LuaValue.NIL
        })
        reg("rotate", varargFn { args ->
            drawBuffer.add(DrawCommand.Transform.Rotate(args.checkdouble(1).toFloat())); LuaValue.NIL
        })
        reg("scale", varargFn { args ->
            drawBuffer.add(DrawCommand.Transform.Scale(
                args.optdouble(1, 1.0).toFloat(), args.optdouble(2, 1.0).toFloat()
            )); LuaValue.NIL
        })
        reg("push_matrix", varargFn { _ -> drawBuffer.add(DrawCommand.Transform.Push); LuaValue.NIL })
        reg("pop_matrix", varargFn { _ -> drawBuffer.add(DrawCommand.Transform.Pop); LuaValue.NIL })

        // ---- lifecycle hooks -------------------------------------------------------
        reg("on_start", varargFn { args -> onStart = args.checkfunction(1); LuaValue.NIL })
        reg("on_update", varargFn { args -> onUpdate = args.checkfunction(1); LuaValue.NIL })
        reg("on_draw", varargFn { args -> onDraw = args.checkfunction(1); LuaValue.NIL })
        reg("on_pause", varargFn { _ -> LuaValue.NIL })
        reg("on_resume", varargFn { _ -> LuaValue.NIL })

        // ---- input hooks -----------------------------------------------------------
        reg("on_touch_down", varargFn { args -> onTouchDown = args.checkfunction(1); LuaValue.NIL })
        reg("on_touch_up", varargFn { args -> onTouchUp = args.checkfunction(1); LuaValue.NIL })
        reg("on_touch_move", varargFn { args -> onTouchMove = args.checkfunction(1); LuaValue.NIL })
        reg("on_touch_cancel", varargFn { _ -> LuaValue.NIL })
        reg("on_long_press", varargFn { _ -> LuaValue.NIL })
        reg("on_double_tap", varargFn { _ -> LuaValue.NIL })

        // ---- utility ---------------------------------------------------------------
        reg("log", varargFn { args ->
            val msg = (1..args.narg()).joinToString(" ") { args.tojstring(it) }
            log(msg); LuaValue.NIL
        })
        reg("toast", varargFn { args -> log("[toast] ${args.checkjstring(1)}"); LuaValue.NIL })
        reg("now", varargFn { _ -> LuaValue.valueOf(System.currentTimeMillis().toDouble()) })
        reg("wait", varargFn { args ->
            try { Thread.sleep(args.checkdouble(1).toLong().coerceAtMost(2000L)) }
            catch (_: InterruptedException) {}
            LuaValue.NIL
        })
        reg("screen_width", varargFn { _ -> LuaValue.valueOf(canvasSize.width.toDouble()) })
        reg("screen_height", varargFn { _ -> LuaValue.valueOf(canvasSize.height.toDouble()) })
        reg("set_var", varargFn { args ->
            globalVars[args.checkjstring(1)] = args.arg(2).touserdata() ?: args.arg(2).tojstring(); LuaValue.NIL
        })
        reg("get_var", varargFn { args ->
            LuaValue.valueOf(globalVars[args.checkjstring(1)]?.toString() ?: "")
        })
        reg("map_range", varargFn { args ->
            val x = args.checkdouble(1)
            val a1 = args.checkdouble(2); val b1 = args.checkdouble(3)
            val a2 = args.checkdouble(4); val b2 = args.checkdouble(5)
            val pct = if (b1 == a1) 0.0 else (x - a1) / (b1 - a1)
            LuaValue.valueOf(a2 + pct * (b2 - a2))
        })
        reg("contains", varargFn { args ->
            val table = args.checktable(1)
            val needle = args.arg(2)
            var i = 1
            while (true) {
                val v = table.get(i); if (v.isnil()) break
                if (v.eq_b(needle)) return@varargFn LuaValue.TRUE
                i++
            }
            LuaValue.FALSE
        })
        reg("index_of", varargFn { args ->
            val table = args.checktable(1)
            val needle = args.arg(2)
            var i = 1
            while (true) {
                val v = table.get(i); if (v.isnil()) break
                if (v.eq_b(needle)) return@varargFn LuaValue.valueOf(i)
                i++
            }
            LuaValue.valueOf(0)
        })
        reg("reverse", varargFn { args ->
            val table = args.checktable(1)
            val n = table.length()
            for (i in 1..(n / 2)) {
                val a = table.get(i); val b = table.get(n - i + 1)
                table.set(i, b); table.set(n - i + 1, a)
            }
            LuaValue.NIL
        })
        reg("split", varargFn { args ->
            val s = args.checkjstring(1); val sep = args.checkjstring(2)
            val out = LuaTable()
            s.split(sep).forEachIndexed { i, part -> out.set(i + 1, LuaValue.valueOf(part)) }
            out
        })

        // ---- placeholders for sensors/audio/network/storage so the script runs --
        listOf(
            "play_sound", "stop_sound", "set_volume", "vibrate", "exit",
            "accel_x", "accel_y", "accel_z", "gyro_x", "gyro_y", "gyro_z", "light_sensor",
            "http_get", "http_post", "store_save", "store_load", "store_delete",
            "image", "label", "button", "text_input", "slider", "switch",
            "is_pressed", "touch_x", "touch_y", "every", "after"
        ).forEach { name ->
            reg(name, varargFn { _ -> LuaValue.NIL })
        }

        return t
    }

    /** Bridge canvas size into Lua so screen_width()/height() work after layout. */
    @Volatile var canvasSize: Size = Size(0f, 0f)

    sealed class Result {
        object Success : Result()
        data class Error(val message: String) : Result()
    }

    private fun varargFn(block: (org.luaj.vm2.Varargs) -> LuaValue) = object : VarArgFunction() {
        override fun invoke(args: org.luaj.vm2.Varargs): org.luaj.vm2.Varargs = block(args)
    }
}

/**
 * Color encoding follows Android's 0xAARRGGBB. We interpret 0x00xxxxxx as 0xFFxxxxxx so
 * users can pass `#RRGGBB` without an alpha prefix.
 */
fun parseLuaColor(raw: Long): Color {
    val v = raw and 0xFFFFFFFFL
    val withAlpha = if ((v ushr 24) == 0L) v or 0xFF000000L else v
    return Color(withAlpha.toInt())
}

private fun Color.toARGBLong(): Long {
    val a = (alpha * 255).toInt() and 0xFF
    val r = (red * 255).toInt() and 0xFF
    val g = (green * 255).toInt() and 0xFF
    val b = (blue * 255).toInt() and 0xFF
    return ((a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
}

/** Recorded draw operations executed by the runtime canvas. */
sealed class DrawCommand {
    data class Rect(val x: Float, val y: Float, val w: Float, val h: Float, val color: Color, val radius: Float, val filled: Boolean, val alpha: Float) : DrawCommand()
    data class Circle(val cx: Float, val cy: Float, val r: Float, val color: Color, val filled: Boolean, val alpha: Float) : DrawCommand()
    data class Oval(val x: Float, val y: Float, val w: Float, val h: Float, val color: Color, val filled: Boolean, val alpha: Float) : DrawCommand()
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val color: Color, val width: Float, val alpha: Float) : DrawCommand()
    data class Triangle(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float, val color: Color, val filled: Boolean, val alpha: Float) : DrawCommand()
    data class Arc(val x: Float, val y: Float, val w: Float, val h: Float, val start: Float, val sweep: Float, val color: Color, val filled: Boolean, val alpha: Float) : DrawCommand()
    data class Text(val text: String, val x: Float, val y: Float, val size: Float, val color: Color, val alpha: Float) : DrawCommand()
    data class Polygon(val points: List<Float>, val color: Color, val filled: Boolean, val alpha: Float) : DrawCommand()
    data class Clear(val color: Color) : DrawCommand()
    sealed class Transform : DrawCommand() {
        data class Translate(val x: Float, val y: Float) : Transform()
        data class Rotate(val deg: Float) : Transform()
        data class Scale(val sx: Float, val sy: Float) : Transform()
        object Push : Transform()
        object Pop : Transform()
    }
}
