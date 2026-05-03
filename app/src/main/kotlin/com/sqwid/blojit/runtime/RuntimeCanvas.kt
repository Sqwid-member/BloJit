package com.sqwid.blojit.runtime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.sqwid.blojit.scripting.DrawCommand
import com.sqwid.blojit.scripting.LuaEngine
import kotlinx.coroutines.delay

/**
 * Visual surface that runs a [LuaEngine]. Drives the engine's update/draw loop and replays
 * the recorded [DrawCommand] frame using Compose's drawing API.
 */
@Composable
fun RuntimeCanvas(
    engine: LuaEngine,
    modifier: Modifier = Modifier,
    background: Color = Color.Black
) {
    var frame by remember { mutableStateOf(emptyList<DrawCommand>()) }

    LaunchedEffect(engine) {
        var last = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val dt = (now - last) / 1000.0
            last = now
            engine.stepUpdate(dt)
            engine.renderFrame()
            frame = engine.snapshot()
            delay(33L) // ~30fps
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .pointerInput(engine) {
                detectTapGestures(
                    onPress = { offset ->
                        engine.fireTouch("down", offset.x, offset.y)
                        try {
                            tryAwaitRelease()
                            engine.fireTouch("up", offset.x, offset.y)
                        } catch (_: Throwable) {
                            engine.fireTouch("up", offset.x, offset.y)
                        }
                    }
                )
            }
    ) {
        engine.canvasSize = size
        replay(frame, this)
    }
}

private fun replay(frame: List<DrawCommand>, scope: DrawScope) = with(scope) {
    val saved = ArrayDeque<Triple<Float, Float, Float>>()
    var tx = 0f; var ty = 0f; var sx = 1f
    for (cmd in frame) {
        when (cmd) {
            is DrawCommand.Clear -> drawRect(cmd.color, size = size)
            is DrawCommand.Rect -> {
                val color = cmd.color.copy(alpha = cmd.alpha)
                if (cmd.radius > 0f) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(cmd.x, cmd.y),
                        size = Size(cmd.w, cmd.h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cmd.radius, cmd.radius),
                        style = if (cmd.filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 2f)
                    )
                } else {
                    drawRect(
                        color = color,
                        topLeft = Offset(cmd.x, cmd.y),
                        size = Size(cmd.w, cmd.h),
                        style = if (cmd.filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 2f)
                    )
                }
            }
            is DrawCommand.Circle -> drawCircle(
                color = cmd.color.copy(alpha = cmd.alpha),
                radius = cmd.r,
                center = Offset(cmd.cx, cmd.cy),
                style = if (cmd.filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 2f)
            )
            is DrawCommand.Oval -> drawOval(
                color = cmd.color.copy(alpha = cmd.alpha),
                topLeft = Offset(cmd.x, cmd.y),
                size = Size(cmd.w, cmd.h),
                style = if (cmd.filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 2f)
            )
            is DrawCommand.Line -> drawLine(
                color = cmd.color.copy(alpha = cmd.alpha),
                start = Offset(cmd.x1, cmd.y1),
                end = Offset(cmd.x2, cmd.y2),
                strokeWidth = cmd.width
            )
            is DrawCommand.Triangle -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cmd.x1, cmd.y1); lineTo(cmd.x2, cmd.y2); lineTo(cmd.x3, cmd.y3); close()
                }
                drawPath(
                    path = path,
                    color = cmd.color.copy(alpha = cmd.alpha),
                    style = if (cmd.filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 2f)
                )
            }
            is DrawCommand.Arc -> drawArc(
                color = cmd.color.copy(alpha = cmd.alpha),
                startAngle = cmd.start,
                sweepAngle = cmd.sweep,
                useCenter = cmd.filled,
                topLeft = Offset(cmd.x, cmd.y),
                size = Size(cmd.w, cmd.h),
                style = if (cmd.filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 2f)
            )
            is DrawCommand.Text -> drawIntoCanvas { c ->
                val paint = android.graphics.Paint().apply {
                    color = cmd.color.copy(alpha = cmd.alpha).toArgb()
                    textSize = cmd.size
                    isAntiAlias = true
                }
                c.nativeCanvas.drawText(cmd.text, cmd.x, cmd.y, paint)
            }
            is DrawCommand.Polygon -> {
                if (cmd.points.size >= 4) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cmd.points[0], cmd.points[1])
                        var i = 2
                        while (i + 1 < cmd.points.size) {
                            lineTo(cmd.points[i], cmd.points[i + 1])
                            i += 2
                        }
                        close()
                    }
                    drawPath(
                        path = path,
                        color = cmd.color.copy(alpha = cmd.alpha),
                        style = if (cmd.filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 2f)
                    )
                }
            }
            is DrawCommand.Transform.Translate -> { tx += cmd.x; ty += cmd.y; translate(tx, ty) {} }
            is DrawCommand.Transform.Rotate -> rotate(cmd.deg) {}
            is DrawCommand.Transform.Scale -> { sx *= cmd.sx; scale(cmd.sx, cmd.sy) {} }
            is DrawCommand.Transform.Push -> saved.addLast(Triple(tx, ty, sx))
            is DrawCommand.Transform.Pop -> { saved.removeLastOrNull()?.let { (a,b,c) -> tx = a; ty = b; sx = c } }
        }
    }
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255).toInt() and 0xFF
    val r = (red * 255).toInt() and 0xFF
    val g = (green * 255).toInt() and 0xFF
    val b = (blue * 255).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
