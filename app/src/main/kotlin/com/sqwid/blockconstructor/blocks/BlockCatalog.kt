package com.sqwid.blockconstructor.blocks

import com.sqwid.blockconstructor.data.BlockInstance

/**
 * The full library of available blocks. Adding a new block = adding one entry here.
 *
 * Naming convention for ids is "<category>.<verb>" so the catalogue stays browsable from code.
 * The `emit` lambda returns Lua source — the host LuaEngine binds an `engine` table that exposes
 * primitives like draw_rect / set_color / get_var so the generated Lua stays small and readable.
 */
object BlockCatalog {

    // ----- shorthand helpers ---------------------------------------------------------------

    private fun p(name: String, label: String, default: String, kind: ParamKind = ParamKind.Expression) =
        ParamSpec(name, label, default, kind)

    private fun s(name: String, label: String, single: Boolean = false) = SlotSpec(name, label, single)

    private fun arg(b: BlockInstance, name: String, fallback: String): String =
        b.params[name]?.takeIf { it.isNotBlank() } ?: fallback

    /** Quote a Lua string literal — caller passes raw text. */
    private fun q(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun colorArg(b: BlockInstance, name: String, fallback: String): String {
        val raw = arg(b, name, fallback).trim()
        // accept "#RRGGBB" or "0xRRGGBB" or a Lua expression
        return when {
            raw.startsWith("#") -> "0xFF" + raw.substring(1).uppercase()
            raw.startsWith("0x") -> raw
            raw.toLongOrNull() != null -> raw
            else -> raw
        }
    }

    // ----- the catalogue -------------------------------------------------------------------

    private val all: List<BlockSpec> = buildList {

        // ===== Program lifecycle =========================================================
        add(BlockSpec(
            id = BlockInstance.PROGRAM_SPEC, category = BlockCategory.Program,
            label = "Програма", description = "Корінь скрипта",
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx ->
                val body = ctx.emitChildren(b.slots["body"] ?: emptyList())
                "-- script: ${b.displayName}\n${body}"
            }
        ))
        add(BlockSpec(
            id = "program.on_start", category = BlockCategory.Program,
            label = "Коли програма стартує",
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx -> "engine.on_start(function()\n${ctx.emitChildren(b.slots["body"] ?: emptyList())}\nend)" }
        ))
        add(BlockSpec(
            id = "program.on_update", category = BlockCategory.Program,
            label = "Кожен кадр (update)",
            params = listOf(p("dt", "ім'я dt", "dt", ParamKind.Identifier)),
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx ->
                "engine.on_update(function(${arg(b, "dt", "dt")})\n${ctx.emitChildren(b.slots["body"] ?: emptyList())}\nend)"
            }
        ))
        add(BlockSpec(
            id = "program.on_draw", category = BlockCategory.Program,
            label = "Кожен кадр (draw)",
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx -> "engine.on_draw(function()\n${ctx.emitChildren(b.slots["body"] ?: emptyList())}\nend)" }
        ))
        add(BlockSpec(
            id = "program.on_pause", category = BlockCategory.Program,
            label = "На паузі", slots = listOf(s("body", "Тіло")),
            emit = { b, ctx -> "engine.on_pause(function()\n${ctx.emitChildren(b.slots["body"] ?: emptyList())}\nend)" }
        ))
        add(BlockSpec(
            id = "program.on_resume", category = BlockCategory.Program,
            label = "На відновленні", slots = listOf(s("body", "Тіло")),
            emit = { b, ctx -> "engine.on_resume(function()\n${ctx.emitChildren(b.slots["body"] ?: emptyList())}\nend)" }
        ))

        // ===== Shapes =====================================================================
        add(BlockSpec(
            id = "shape.rectangle", category = BlockCategory.Shapes,
            label = "Прямокутник",
            params = listOf(
                p("x", "X", "0", ParamKind.Number),
                p("y", "Y", "0", ParamKind.Number),
                p("w", "Ширина", "100", ParamKind.Number),
                p("h", "Висота", "100", ParamKind.Number),
                p("color", "Колір", "#3B82F6", ParamKind.Color),
                p("radius", "Радіус кутів", "0", ParamKind.Number),
                p("filled", "Заповнений", "true", ParamKind.Boolean)
            ),
            emit = { b, _ ->
                "engine.draw_rect(${arg(b, "x", "0")}, ${arg(b, "y", "0")}, ${arg(b, "w", "100")}, " +
                    "${arg(b, "h", "100")}, ${colorArg(b, "color", "#3B82F6")}, " +
                    "${arg(b, "radius", "0")}, ${arg(b, "filled", "true")})"
            }
        ))
        add(BlockSpec(
            id = "shape.circle", category = BlockCategory.Shapes,
            label = "Коло",
            params = listOf(
                p("cx", "Центр X", "100", ParamKind.Number),
                p("cy", "Центр Y", "100", ParamKind.Number),
                p("r", "Радіус", "50", ParamKind.Number),
                p("color", "Колір", "#10B981", ParamKind.Color),
                p("filled", "Заповнений", "true", ParamKind.Boolean)
            ),
            emit = { b, _ ->
                "engine.draw_circle(${arg(b, "cx", "100")}, ${arg(b, "cy", "100")}, " +
                    "${arg(b, "r", "50")}, ${colorArg(b, "color", "#10B981")}, ${arg(b, "filled", "true")})"
            }
        ))
        add(BlockSpec(
            id = "shape.oval", category = BlockCategory.Shapes,
            label = "Овал",
            params = listOf(
                p("x", "X", "0", ParamKind.Number), p("y", "Y", "0", ParamKind.Number),
                p("w", "Ширина", "120", ParamKind.Number), p("h", "Висота", "80", ParamKind.Number),
                p("color", "Колір", "#A855F7", ParamKind.Color),
                p("filled", "Заповнений", "true", ParamKind.Boolean)
            ),
            emit = { b, _ ->
                "engine.draw_oval(${arg(b, "x", "0")}, ${arg(b, "y", "0")}, ${arg(b, "w", "120")}, " +
                    "${arg(b, "h", "80")}, ${colorArg(b, "color", "#A855F7")}, ${arg(b, "filled", "true")})"
            }
        ))
        add(BlockSpec(
            id = "shape.line", category = BlockCategory.Shapes,
            label = "Лінія",
            params = listOf(
                p("x1", "X1", "0", ParamKind.Number), p("y1", "Y1", "0", ParamKind.Number),
                p("x2", "X2", "100", ParamKind.Number), p("y2", "Y2", "100", ParamKind.Number),
                p("color", "Колір", "#FFFFFF", ParamKind.Color),
                p("width", "Товщина", "2", ParamKind.Number)
            ),
            emit = { b, _ ->
                "engine.draw_line(${arg(b, "x1", "0")}, ${arg(b, "y1", "0")}, ${arg(b, "x2", "100")}, " +
                    "${arg(b, "y2", "100")}, ${colorArg(b, "color", "#FFFFFF")}, ${arg(b, "width", "2")})"
            }
        ))
        add(BlockSpec(
            id = "shape.triangle", category = BlockCategory.Shapes,
            label = "Трикутник",
            params = listOf(
                p("x1", "X1", "0", ParamKind.Number), p("y1", "Y1", "0", ParamKind.Number),
                p("x2", "X2", "100", ParamKind.Number), p("y2", "Y2", "0", ParamKind.Number),
                p("x3", "X3", "50", ParamKind.Number), p("y3", "Y3", "100", ParamKind.Number),
                p("color", "Колір", "#F59E0B", ParamKind.Color),
                p("filled", "Заповнений", "true", ParamKind.Boolean)
            ),
            emit = { b, _ ->
                "engine.draw_triangle(${arg(b, "x1", "0")}, ${arg(b, "y1", "0")}, " +
                    "${arg(b, "x2", "100")}, ${arg(b, "y2", "0")}, ${arg(b, "x3", "50")}, ${arg(b, "y3", "100")}, " +
                    "${colorArg(b, "color", "#F59E0B")}, ${arg(b, "filled", "true")})"
            }
        ))
        add(BlockSpec(
            id = "shape.arc", category = BlockCategory.Shapes,
            label = "Дуга",
            params = listOf(
                p("x", "X", "0", ParamKind.Number), p("y", "Y", "0", ParamKind.Number),
                p("w", "Ширина", "100", ParamKind.Number), p("h", "Висота", "100", ParamKind.Number),
                p("start", "Початок (°)", "0", ParamKind.Number),
                p("sweep", "Розмах (°)", "90", ParamKind.Number),
                p("color", "Колір", "#EC4899", ParamKind.Color),
                p("filled", "Заповнений", "false", ParamKind.Boolean)
            ),
            emit = { b, _ ->
                "engine.draw_arc(${arg(b, "x", "0")}, ${arg(b, "y", "0")}, ${arg(b, "w", "100")}, " +
                    "${arg(b, "h", "100")}, ${arg(b, "start", "0")}, ${arg(b, "sweep", "90")}, " +
                    "${colorArg(b, "color", "#EC4899")}, ${arg(b, "filled", "false")})"
            }
        ))
        add(BlockSpec(
            id = "shape.text", category = BlockCategory.Shapes,
            label = "Текст",
            params = listOf(
                p("text", "Текст", "Привіт", ParamKind.Text),
                p("x", "X", "50", ParamKind.Number), p("y", "Y", "50", ParamKind.Number),
                p("size", "Розмір", "24", ParamKind.Number),
                p("color", "Колір", "#FFFFFF", ParamKind.Color)
            ),
            emit = { b, _ ->
                val raw = b.params["text"] ?: "Привіт"
                val text = if (raw.startsWith("\"") && raw.endsWith("\"")) raw else q(raw)
                "engine.draw_text($text, ${arg(b, "x", "50")}, ${arg(b, "y", "50")}, " +
                    "${arg(b, "size", "24")}, ${colorArg(b, "color", "#FFFFFF")})"
            }
        ))
        add(BlockSpec(
            id = "shape.polygon", category = BlockCategory.Shapes,
            label = "Багатокутник",
            params = listOf(
                p("points", "Точки {x1,y1,x2,y2,...}", "{0,0, 100,0, 50,100}"),
                p("color", "Колір", "#22C55E", ParamKind.Color),
                p("filled", "Заповнений", "true", ParamKind.Boolean)
            ),
            emit = { b, _ ->
                "engine.draw_polygon(${arg(b, "points", "{0,0, 100,0, 50,100}")}, " +
                    "${colorArg(b, "color", "#22C55E")}, ${arg(b, "filled", "true")})"
            }
        ))

        // ===== Drawing state ==============================================================
        add(BlockSpec(
            id = "draw.set_color", category = BlockCategory.Drawing,
            label = "Встановити колір",
            params = listOf(p("color", "Колір", "#FFFFFF", ParamKind.Color)),
            emit = { b, _ -> "engine.set_color(${colorArg(b, "color", "#FFFFFF")})" }
        ))
        add(BlockSpec(
            id = "draw.set_stroke", category = BlockCategory.Drawing,
            label = "Встановити товщину пера",
            params = listOf(p("width", "Товщина", "2", ParamKind.Number)),
            emit = { b, _ -> "engine.set_stroke(${arg(b, "width", "2")})" }
        ))
        add(BlockSpec(
            id = "draw.set_alpha", category = BlockCategory.Drawing,
            label = "Прозорість",
            params = listOf(p("alpha", "Альфа 0..1", "1.0", ParamKind.Number)),
            emit = { b, _ -> "engine.set_alpha(${arg(b, "alpha", "1.0")})" }
        ))
        add(BlockSpec(
            id = "draw.translate", category = BlockCategory.Drawing,
            label = "Перенести (translate)",
            params = listOf(p("x", "X", "0", ParamKind.Number), p("y", "Y", "0", ParamKind.Number)),
            emit = { b, _ -> "engine.translate(${arg(b, "x", "0")}, ${arg(b, "y", "0")})" }
        ))
        add(BlockSpec(
            id = "draw.rotate", category = BlockCategory.Drawing,
            label = "Повернути (degrees)",
            params = listOf(p("deg", "Градуси", "0", ParamKind.Number)),
            emit = { b, _ -> "engine.rotate(${arg(b, "deg", "0")})" }
        ))
        add(BlockSpec(
            id = "draw.scale", category = BlockCategory.Drawing,
            label = "Масштаб",
            params = listOf(p("sx", "X", "1", ParamKind.Number), p("sy", "Y", "1", ParamKind.Number)),
            emit = { b, _ -> "engine.scale(${arg(b, "sx", "1")}, ${arg(b, "sy", "1")})" }
        ))
        add(BlockSpec(
            id = "draw.push_matrix", category = BlockCategory.Drawing,
            label = "Зберегти трансформацію",
            emit = { _, _ -> "engine.push_matrix()" }
        ))
        add(BlockSpec(
            id = "draw.pop_matrix", category = BlockCategory.Drawing,
            label = "Відновити трансформацію",
            emit = { _, _ -> "engine.pop_matrix()" }
        ))
        add(BlockSpec(
            id = "draw.clear", category = BlockCategory.Drawing,
            label = "Очистити екран",
            params = listOf(p("color", "Колір", "#000000", ParamKind.Color)),
            emit = { b, _ -> "engine.clear(${colorArg(b, "color", "#000000")})" }
        ))

        // ===== Touch / input ==============================================================
        listOf(
            "on_touch_down" to "Коли торкнулися",
            "on_touch_up" to "Коли відпустили",
            "on_touch_move" to "Коли переміщення",
            "on_touch_cancel" to "Коли торк скасовано",
            "on_long_press" to "Довге натискання",
            "on_double_tap" to "Подвійне натискання"
        ).forEach { (key, label) ->
            add(BlockSpec(
                id = "input.$key", category = BlockCategory.Input,
                label = label,
                params = listOf(
                    p("x", "ім'я x", "x", ParamKind.Identifier),
                    p("y", "ім'я y", "y", ParamKind.Identifier)
                ),
                slots = listOf(s("body", "Тіло")),
                emit = { b, ctx ->
                    val body = ctx.emitChildren(b.slots["body"] ?: emptyList())
                    "engine.$key(function(${arg(b, "x", "x")}, ${arg(b, "y", "y")})\n${body}\nend)"
                }
            ))
        }
        add(BlockSpec(
            id = "input.is_pressed", category = BlockCategory.Input,
            label = "Зараз натиснуто?",
            emit = { _, _ -> "engine.is_pressed()" }
        ))
        add(BlockSpec(
            id = "input.touch_x", category = BlockCategory.Input, label = "X дотику",
            emit = { _, _ -> "engine.touch_x()" }
        ))
        add(BlockSpec(
            id = "input.touch_y", category = BlockCategory.Input, label = "Y дотику",
            emit = { _, _ -> "engine.touch_y()" }
        ))

        // ===== UI controls ================================================================
        add(BlockSpec(
            id = "ui.button", category = BlockCategory.UI, label = "Кнопка",
            params = listOf(
                p("id", "ID", "btn1", ParamKind.Identifier),
                p("text", "Текст", "Кнопка", ParamKind.Text),
                p("x", "X", "20", ParamKind.Number), p("y", "Y", "20", ParamKind.Number),
                p("w", "Ширина", "200", ParamKind.Number), p("h", "Висота", "60", ParamKind.Number)
            ),
            slots = listOf(s("on_click", "Коли натиснули")),
            emit = { b, ctx ->
                val body = ctx.emitChildren(b.slots["on_click"] ?: emptyList())
                val text = b.params["text"]?.let { if (it.startsWith("\"")) it else q(it) } ?: q("Кнопка")
                "engine.button(${q(arg(b, "id", "btn1"))}, $text, ${arg(b, "x", "20")}, ${arg(b, "y", "20")}, " +
                    "${arg(b, "w", "200")}, ${arg(b, "h", "60")}, function()\n${body}\nend)"
            }
        ))
        add(BlockSpec(
            id = "ui.label", category = BlockCategory.UI, label = "Підпис (label)",
            params = listOf(
                p("text", "Текст", "Привіт", ParamKind.Text),
                p("x", "X", "20", ParamKind.Number), p("y", "Y", "20", ParamKind.Number),
                p("size", "Розмір", "20", ParamKind.Number),
                p("color", "Колір", "#FFFFFF", ParamKind.Color)
            ),
            emit = { b, _ ->
                val text = b.params["text"]?.let { if (it.startsWith("\"")) it else q(it) } ?: q("Привіт")
                "engine.label($text, ${arg(b, "x", "20")}, ${arg(b, "y", "20")}, " +
                    "${arg(b, "size", "20")}, ${colorArg(b, "color", "#FFFFFF")})"
            }
        ))
        add(BlockSpec(
            id = "ui.image", category = BlockCategory.UI, label = "Зображення",
            params = listOf(
                p("name", "Назва ассета", "image1", ParamKind.Text),
                p("x", "X", "0", ParamKind.Number), p("y", "Y", "0", ParamKind.Number),
                p("w", "Ширина", "200", ParamKind.Number), p("h", "Висота", "200", ParamKind.Number)
            ),
            emit = { b, _ ->
                val name = b.params["name"]?.let { if (it.startsWith("\"")) it else q(it) } ?: q("image1")
                "engine.image($name, ${arg(b, "x", "0")}, ${arg(b, "y", "0")}, ${arg(b, "w", "200")}, ${arg(b, "h", "200")})"
            }
        ))
        add(BlockSpec(
            id = "ui.text_input", category = BlockCategory.UI, label = "Поле вводу",
            params = listOf(
                p("id", "ID", "in1", ParamKind.Identifier),
                p("x", "X", "20", ParamKind.Number), p("y", "Y", "20", ParamKind.Number),
                p("w", "Ширина", "200", ParamKind.Number), p("h", "Висота", "50", ParamKind.Number),
                p("hint", "Підказка", "Введіть текст…", ParamKind.Text)
            ),
            emit = { b, _ ->
                val hint = b.params["hint"]?.let { if (it.startsWith("\"")) it else q(it) } ?: q("…")
                "engine.text_input(${q(arg(b, "id", "in1"))}, ${arg(b, "x", "20")}, ${arg(b, "y", "20")}, " +
                    "${arg(b, "w", "200")}, ${arg(b, "h", "50")}, $hint)"
            }
        ))
        add(BlockSpec(
            id = "ui.slider", category = BlockCategory.UI, label = "Слайдер",
            params = listOf(
                p("id", "ID", "sl1", ParamKind.Identifier),
                p("x", "X", "20", ParamKind.Number), p("y", "Y", "20", ParamKind.Number),
                p("w", "Ширина", "300", ParamKind.Number),
                p("min", "min", "0", ParamKind.Number), p("max", "max", "100", ParamKind.Number),
                p("value", "Значення", "50", ParamKind.Number)
            ),
            emit = { b, _ ->
                "engine.slider(${q(arg(b, "id", "sl1"))}, ${arg(b, "x", "20")}, ${arg(b, "y", "20")}, " +
                    "${arg(b, "w", "300")}, ${arg(b, "min", "0")}, ${arg(b, "max", "100")}, ${arg(b, "value", "50")})"
            }
        ))
        add(BlockSpec(
            id = "ui.switch", category = BlockCategory.UI, label = "Перемикач",
            params = listOf(
                p("id", "ID", "sw1", ParamKind.Identifier),
                p("x", "X", "20", ParamKind.Number), p("y", "Y", "20", ParamKind.Number),
                p("on", "Увімк.", "false", ParamKind.Boolean)
            ),
            emit = { b, _ ->
                "engine.switch(${q(arg(b, "id", "sw1"))}, ${arg(b, "x", "20")}, ${arg(b, "y", "20")}, ${arg(b, "on", "false")})"
            }
        ))

        // ===== Variables ==================================================================
        add(BlockSpec(
            id = "var.declare", category = BlockCategory.Variables, label = "Оголосити змінну",
            params = listOf(
                p("name", "Ім'я", "x", ParamKind.Identifier),
                p("value", "Значення", "0")
            ),
            emit = { b, _ -> "local ${arg(b, "name", "x")} = ${arg(b, "value", "0")}" }
        ))
        add(BlockSpec(
            id = "var.assign", category = BlockCategory.Variables, label = "Присвоїти",
            params = listOf(
                p("name", "Змінна", "x", ParamKind.Identifier),
                p("value", "Значення", "0")
            ),
            emit = { b, _ -> "${arg(b, "name", "x")} = ${arg(b, "value", "0")}" }
        ))
        add(BlockSpec(
            id = "var.increment", category = BlockCategory.Variables, label = "Збільшити на",
            params = listOf(
                p("name", "Змінна", "x", ParamKind.Identifier),
                p("by", "На", "1", ParamKind.Number)
            ),
            emit = { b, _ -> "${arg(b, "name", "x")} = ${arg(b, "name", "x")} + ${arg(b, "by", "1")}" }
        ))
        add(BlockSpec(
            id = "var.decrement", category = BlockCategory.Variables, label = "Зменшити на",
            params = listOf(
                p("name", "Змінна", "x", ParamKind.Identifier),
                p("by", "На", "1", ParamKind.Number)
            ),
            emit = { b, _ -> "${arg(b, "name", "x")} = ${arg(b, "name", "x")} - ${arg(b, "by", "1")}" }
        ))
        add(BlockSpec(
            id = "var.global_set", category = BlockCategory.Variables, label = "Зберегти глобально",
            params = listOf(
                p("name", "Ім'я", "score", ParamKind.Identifier),
                p("value", "Значення", "0")
            ),
            emit = { b, _ -> "engine.set_var(${q(arg(b, "name", "score"))}, ${arg(b, "value", "0")})" }
        ))
        add(BlockSpec(
            id = "var.global_get", category = BlockCategory.Variables, label = "Отримати глобальну",
            params = listOf(p("name", "Ім'я", "score", ParamKind.Identifier)),
            emit = { b, _ -> "engine.get_var(${q(arg(b, "name", "score"))})" }
        ))

        // ===== Control flow ===============================================================
        add(BlockSpec(
            id = "ctl.if", category = BlockCategory.ControlFlow, label = "Якщо",
            params = listOf(p("cond", "Умова", "true")),
            slots = listOf(s("then", "Тоді")),
            emit = { b, ctx ->
                "if ${arg(b, "cond", "true")} then\n${ctx.emitChildren(b.slots["then"] ?: emptyList())}\nend"
            }
        ))
        add(BlockSpec(
            id = "ctl.if_else", category = BlockCategory.ControlFlow, label = "Якщо / інакше",
            params = listOf(p("cond", "Умова", "true")),
            slots = listOf(s("then", "Тоді"), s("else", "Інакше")),
            emit = { b, ctx ->
                "if ${arg(b, "cond", "true")} then\n" +
                    ctx.emitChildren(b.slots["then"] ?: emptyList()) + "\nelse\n" +
                    ctx.emitChildren(b.slots["else"] ?: emptyList()) + "\nend"
            }
        ))
        add(BlockSpec(
            id = "ctl.while", category = BlockCategory.ControlFlow, label = "Поки",
            params = listOf(p("cond", "Умова", "true")),
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx ->
                "while ${arg(b, "cond", "true")} do\n${ctx.emitChildren(b.slots["body"] ?: emptyList())}\nend"
            }
        ))
        add(BlockSpec(
            id = "ctl.repeat", category = BlockCategory.ControlFlow, label = "Повторити N",
            params = listOf(p("times", "Разів", "10", ParamKind.Number)),
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx ->
                "for _ = 1, ${arg(b, "times", "10")} do\n${ctx.emitChildren(b.slots["body"] ?: emptyList())}\nend"
            }
        ))
        add(BlockSpec(
            id = "ctl.for", category = BlockCategory.ControlFlow, label = "Для від..до",
            params = listOf(
                p("var", "Змінна", "i", ParamKind.Identifier),
                p("from", "Від", "1", ParamKind.Number),
                p("to", "До", "10", ParamKind.Number),
                p("step", "Крок", "1", ParamKind.Number)
            ),
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx ->
                "for ${arg(b, "var", "i")} = ${arg(b, "from", "1")}, ${arg(b, "to", "10")}, ${arg(b, "step", "1")} do\n" +
                    ctx.emitChildren(b.slots["body"] ?: emptyList()) + "\nend"
            }
        ))
        add(BlockSpec(
            id = "ctl.for_in", category = BlockCategory.ControlFlow, label = "Для кожного у списку",
            params = listOf(
                p("var", "Змінна", "v", ParamKind.Identifier),
                p("list", "Список", "{}")
            ),
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx ->
                "for _, ${arg(b, "var", "v")} in ipairs(${arg(b, "list", "{}")}) do\n" +
                    ctx.emitChildren(b.slots["body"] ?: emptyList()) + "\nend"
            }
        ))
        add(BlockSpec(
            id = "ctl.break", category = BlockCategory.ControlFlow, label = "Перервати (break)",
            emit = { _, _ -> "break" }
        ))
        add(BlockSpec(
            id = "ctl.return", category = BlockCategory.ControlFlow, label = "Повернути",
            params = listOf(p("value", "Значення", "nil")),
            emit = { b, _ -> "return ${arg(b, "value", "nil")}" }
        ))

        // ===== Functions ==================================================================
        add(BlockSpec(
            id = "fn.define", category = BlockCategory.Functions, label = "Визначити функцію",
            params = listOf(
                p("name", "Ім'я", "do_thing", ParamKind.Identifier),
                p("args", "Аргументи (через кому)", "", ParamKind.Identifier)
            ),
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx ->
                val args = (b.params["args"] ?: "").trim()
                "function ${arg(b, "name", "do_thing")}(${args})\n" +
                    ctx.emitChildren(b.slots["body"] ?: emptyList()) + "\nend"
            }
        ))
        add(BlockSpec(
            id = "fn.call", category = BlockCategory.Functions, label = "Викликати функцію",
            params = listOf(
                p("name", "Ім'я", "do_thing", ParamKind.Identifier),
                p("args", "Аргументи", "")
            ),
            emit = { b, _ -> "${arg(b, "name", "do_thing")}(${arg(b, "args", "")})" }
        ))

        // ===== Math =======================================================================
        listOf(
            "add" to ("a + b" to "Додати"),
            "sub" to ("a - b" to "Відняти"),
            "mul" to ("a * b" to "Помножити"),
            "div" to ("a / b" to "Поділити"),
            "mod" to ("a % b" to "Залишок (mod)"),
            "pow" to ("a ^ b" to "Степінь")
        ).forEach { (id, lp) ->
            val (expr, label) = lp
            add(BlockSpec(
                id = "math.$id", category = BlockCategory.Math, label = label,
                params = listOf(p("a", "a", "0"), p("b", "b", "0")),
                emit = { b, _ -> expr.replace("a", "(${arg(b, "a", "0")})").replace("b", "(${arg(b, "b", "0")})") }
            ))
        }
        listOf(
            "abs" to "math.abs", "sqrt" to "math.sqrt", "floor" to "math.floor",
            "ceil" to "math.ceil", "sin" to "math.sin", "cos" to "math.cos",
            "tan" to "math.tan", "asin" to "math.asin", "acos" to "math.acos",
            "atan" to "math.atan", "log" to "math.log", "exp" to "math.exp"
        ).forEach { (k, lua) ->
            add(BlockSpec(
                id = "math.$k", category = BlockCategory.Math, label = k,
                params = listOf(p("x", "x", "0")),
                emit = { b, _ -> "$lua(${arg(b, "x", "0")})" }
            ))
        }
        add(BlockSpec(
            id = "math.round", category = BlockCategory.Math, label = "Округлити",
            params = listOf(p("x", "x", "0")),
            emit = { b, _ -> "math.floor((${arg(b, "x", "0")}) + 0.5)" }
        ))
        add(BlockSpec(
            id = "math.random", category = BlockCategory.Math, label = "Випадкове від..до",
            params = listOf(p("a", "min", "0"), p("b", "max", "100")),
            emit = { b, _ -> "math.random(${arg(b, "a", "0")}, ${arg(b, "b", "100")})" }
        ))
        add(BlockSpec(
            id = "math.clamp", category = BlockCategory.Math, label = "Обмежити",
            params = listOf(p("x", "x", "0"), p("a", "min", "0"), p("b", "max", "100")),
            emit = { b, _ ->
                "math.max(${arg(b, "a", "0")}, math.min(${arg(b, "b", "100")}, ${arg(b, "x", "0")}))"
            }
        ))
        add(BlockSpec(
            id = "math.map_range", category = BlockCategory.Math, label = "Лінійне відображення",
            params = listOf(
                p("x", "x", "0"),
                p("a1", "вхід min", "0"), p("b1", "вхід max", "1"),
                p("a2", "вихід min", "0"), p("b2", "вихід max", "100")
            ),
            emit = { b, _ ->
                "engine.map_range(${arg(b, "x", "0")}, ${arg(b, "a1", "0")}, ${arg(b, "b1", "1")}, " +
                    "${arg(b, "a2", "0")}, ${arg(b, "b2", "100")})"
            }
        ))
        add(BlockSpec(
            id = "math.min", category = BlockCategory.Math, label = "Мінімум",
            params = listOf(p("a", "a", "0"), p("b", "b", "0")),
            emit = { b, _ -> "math.min(${arg(b, "a", "0")}, ${arg(b, "b", "0")})" }
        ))
        add(BlockSpec(
            id = "math.max", category = BlockCategory.Math, label = "Максимум",
            params = listOf(p("a", "a", "0"), p("b", "b", "0")),
            emit = { b, _ -> "math.max(${arg(b, "a", "0")}, ${arg(b, "b", "0")})" }
        ))
        add(BlockSpec(
            id = "math.pi", category = BlockCategory.Math, label = "π",
            emit = { _, _ -> "math.pi" }
        ))

        // ===== Strings ====================================================================
        add(BlockSpec(
            id = "str.concat", category = BlockCategory.Strings, label = "Об'єднати",
            params = listOf(p("a", "a", "\"a\"", ParamKind.Text), p("b", "b", "\"b\"", ParamKind.Text)),
            emit = { b, _ -> "(${arg(b, "a", "\"a\"")}) .. (${arg(b, "b", "\"b\"")})" }
        ))
        add(BlockSpec(
            id = "str.length", category = BlockCategory.Strings, label = "Довжина",
            params = listOf(p("s", "Рядок", "\"\"")),
            emit = { b, _ -> "string.len(${arg(b, "s", "\"\"")})" }
        ))
        add(BlockSpec(
            id = "str.upper", category = BlockCategory.Strings, label = "ВЕЛИКІ",
            params = listOf(p("s", "Рядок", "\"\"")),
            emit = { b, _ -> "string.upper(${arg(b, "s", "\"\"")})" }
        ))
        add(BlockSpec(
            id = "str.lower", category = BlockCategory.Strings, label = "малі",
            params = listOf(p("s", "Рядок", "\"\"")),
            emit = { b, _ -> "string.lower(${arg(b, "s", "\"\"")})" }
        ))
        add(BlockSpec(
            id = "str.substring", category = BlockCategory.Strings, label = "Підрядок",
            params = listOf(p("s", "Рядок", "\"\""), p("a", "від", "1", ParamKind.Number), p("b", "до", "-1", ParamKind.Number)),
            emit = { b, _ -> "string.sub(${arg(b, "s", "\"\"")}, ${arg(b, "a", "1")}, ${arg(b, "b", "-1")})" }
        ))
        add(BlockSpec(
            id = "str.find", category = BlockCategory.Strings, label = "Знайти",
            params = listOf(p("s", "Рядок", "\"\""), p("pat", "Що шукати", "\"\"", ParamKind.Text)),
            emit = { b, _ -> "(string.find(${arg(b, "s", "\"\"")}, ${arg(b, "pat", "\"\"")}, 1, true) or 0)" }
        ))
        add(BlockSpec(
            id = "str.replace", category = BlockCategory.Strings, label = "Замінити",
            params = listOf(
                p("s", "Рядок", "\"\""),
                p("from", "Що", "\"\"", ParamKind.Text),
                p("to", "На що", "\"\"", ParamKind.Text)
            ),
            emit = { b, _ ->
                "(string.gsub(${arg(b, "s", "\"\"")}, ${arg(b, "from", "\"\"")}, ${arg(b, "to", "\"\"")}))"
            }
        ))
        add(BlockSpec(
            id = "str.split", category = BlockCategory.Strings, label = "Розбити по",
            params = listOf(p("s", "Рядок", "\"a,b\""), p("sep", "Розділювач", "\",\"", ParamKind.Text)),
            emit = { b, _ -> "engine.split(${arg(b, "s", "\"\"")}, ${arg(b, "sep", "\",\"")})" }
        ))
        add(BlockSpec(
            id = "str.format", category = BlockCategory.Strings, label = "Формат",
            params = listOf(
                p("fmt", "Шаблон", "\"%d\"", ParamKind.Text),
                p("args", "Аргументи", "0")
            ),
            emit = { b, _ -> "string.format(${arg(b, "fmt", "\"%s\"")}, ${arg(b, "args", "")})" }
        ))
        add(BlockSpec(
            id = "str.tonumber", category = BlockCategory.Strings, label = "У число",
            params = listOf(p("s", "Рядок", "\"0\"")),
            emit = { b, _ -> "(tonumber(${arg(b, "s", "\"0\"")}) or 0)" }
        ))
        add(BlockSpec(
            id = "str.tostring", category = BlockCategory.Strings, label = "У рядок",
            params = listOf(p("x", "x", "0")),
            emit = { b, _ -> "tostring(${arg(b, "x", "0")})" }
        ))

        // ===== Lists / tables =============================================================
        add(BlockSpec(
            id = "list.new", category = BlockCategory.Lists, label = "Новий список",
            params = listOf(p("items", "Елементи", "{}")),
            emit = { b, _ -> arg(b, "items", "{}") }
        ))
        add(BlockSpec(
            id = "list.append", category = BlockCategory.Lists, label = "Додати в кінець",
            params = listOf(p("list", "Список", "t", ParamKind.Identifier), p("value", "Значення", "0")),
            emit = { b, _ -> "table.insert(${arg(b, "list", "t")}, ${arg(b, "value", "0")})" }
        ))
        add(BlockSpec(
            id = "list.insert", category = BlockCategory.Lists, label = "Вставити на позицію",
            params = listOf(
                p("list", "Список", "t", ParamKind.Identifier),
                p("idx", "Індекс", "1", ParamKind.Number),
                p("value", "Значення", "0")
            ),
            emit = { b, _ -> "table.insert(${arg(b, "list", "t")}, ${arg(b, "idx", "1")}, ${arg(b, "value", "0")})" }
        ))
        add(BlockSpec(
            id = "list.remove", category = BlockCategory.Lists, label = "Видалити за індексом",
            params = listOf(p("list", "Список", "t", ParamKind.Identifier), p("idx", "Індекс", "1", ParamKind.Number)),
            emit = { b, _ -> "table.remove(${arg(b, "list", "t")}, ${arg(b, "idx", "1")})" }
        ))
        add(BlockSpec(
            id = "list.length", category = BlockCategory.Lists, label = "Довжина списку",
            params = listOf(p("list", "Список", "t", ParamKind.Identifier)),
            emit = { b, _ -> "#${arg(b, "list", "t")}" }
        ))
        add(BlockSpec(
            id = "list.get", category = BlockCategory.Lists, label = "Отримати [i]",
            params = listOf(p("list", "Список", "t", ParamKind.Identifier), p("idx", "Індекс", "1", ParamKind.Number)),
            emit = { b, _ -> "${arg(b, "list", "t")}[${arg(b, "idx", "1")}]" }
        ))
        add(BlockSpec(
            id = "list.set", category = BlockCategory.Lists, label = "Встановити [i]",
            params = listOf(
                p("list", "Список", "t", ParamKind.Identifier),
                p("idx", "Індекс", "1", ParamKind.Number),
                p("value", "Значення", "0")
            ),
            emit = { b, _ -> "${arg(b, "list", "t")}[${arg(b, "idx", "1")}] = ${arg(b, "value", "0")}" }
        ))
        add(BlockSpec(
            id = "list.contains", category = BlockCategory.Lists, label = "Містить?",
            params = listOf(p("list", "Список", "t", ParamKind.Identifier), p("value", "Значення", "0")),
            emit = { b, _ -> "engine.contains(${arg(b, "list", "t")}, ${arg(b, "value", "0")})" }
        ))
        add(BlockSpec(
            id = "list.index_of", category = BlockCategory.Lists, label = "Індекс елемента",
            params = listOf(p("list", "Список", "t", ParamKind.Identifier), p("value", "Значення", "0")),
            emit = { b, _ -> "engine.index_of(${arg(b, "list", "t")}, ${arg(b, "value", "0")})" }
        ))
        add(BlockSpec(
            id = "list.sort", category = BlockCategory.Lists, label = "Сортувати",
            params = listOf(p("list", "Список", "t", ParamKind.Identifier)),
            emit = { b, _ -> "table.sort(${arg(b, "list", "t")})" }
        ))
        add(BlockSpec(
            id = "list.reverse", category = BlockCategory.Lists, label = "Розвернути",
            params = listOf(p("list", "Список", "t", ParamKind.Identifier)),
            emit = { b, _ -> "engine.reverse(${arg(b, "list", "t")})" }
        ))

        // ===== Time =======================================================================
        add(BlockSpec(
            id = "time.wait", category = BlockCategory.Time, label = "Зачекати (мс)",
            params = listOf(p("ms", "мс", "1000", ParamKind.Number)),
            emit = { b, _ -> "engine.wait(${arg(b, "ms", "1000")})" }
        ))
        add(BlockSpec(
            id = "time.now", category = BlockCategory.Time, label = "Зараз (мс)",
            emit = { _, _ -> "engine.now()" }
        ))
        add(BlockSpec(
            id = "time.every", category = BlockCategory.Time, label = "Кожні N мс",
            params = listOf(p("ms", "мс", "1000", ParamKind.Number)),
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx ->
                "engine.every(${arg(b, "ms", "1000")}, function()\n${ctx.emitChildren(b.slots["body"] ?: emptyList())}\nend)"
            }
        ))
        add(BlockSpec(
            id = "time.after", category = BlockCategory.Time, label = "Через N мс",
            params = listOf(p("ms", "мс", "1000", ParamKind.Number)),
            slots = listOf(s("body", "Тіло")),
            emit = { b, ctx ->
                "engine.after(${arg(b, "ms", "1000")}, function()\n${ctx.emitChildren(b.slots["body"] ?: emptyList())}\nend)"
            }
        ))

        // ===== Audio =====================================================================
        add(BlockSpec(
            id = "audio.play_sound", category = BlockCategory.Audio, label = "Програти звук",
            params = listOf(p("name", "Назва ассета", "sound1", ParamKind.Text)),
            emit = { b, _ ->
                val name = b.params["name"]?.let { if (it.startsWith("\"")) it else q(it) } ?: q("sound1")
                "engine.play_sound($name)"
            }
        ))
        add(BlockSpec(
            id = "audio.stop_sound", category = BlockCategory.Audio, label = "Зупинити звук",
            params = listOf(p("name", "Назва ассета", "sound1", ParamKind.Text)),
            emit = { b, _ ->
                val name = b.params["name"]?.let { if (it.startsWith("\"")) it else q(it) } ?: q("sound1")
                "engine.stop_sound($name)"
            }
        ))
        add(BlockSpec(
            id = "audio.set_volume", category = BlockCategory.Audio, label = "Гучність 0..1",
            params = listOf(p("v", "Гучність", "1.0", ParamKind.Number)),
            emit = { b, _ -> "engine.set_volume(${arg(b, "v", "1.0")})" }
        ))
        add(BlockSpec(
            id = "audio.vibrate", category = BlockCategory.Audio, label = "Вібрація (мс)",
            params = listOf(p("ms", "мс", "200", ParamKind.Number)),
            emit = { b, _ -> "engine.vibrate(${arg(b, "ms", "200")})" }
        ))

        // ===== Sensors ====================================================================
        add(BlockSpec(
            id = "sensor.accel_x", category = BlockCategory.Sensors, label = "Акселерометр X",
            emit = { _, _ -> "engine.accel_x()" }
        ))
        add(BlockSpec(
            id = "sensor.accel_y", category = BlockCategory.Sensors, label = "Акселерометр Y",
            emit = { _, _ -> "engine.accel_y()" }
        ))
        add(BlockSpec(
            id = "sensor.accel_z", category = BlockCategory.Sensors, label = "Акселерометр Z",
            emit = { _, _ -> "engine.accel_z()" }
        ))
        add(BlockSpec(
            id = "sensor.gyro_x", category = BlockCategory.Sensors, label = "Гіроскоп X",
            emit = { _, _ -> "engine.gyro_x()" }
        ))
        add(BlockSpec(
            id = "sensor.gyro_y", category = BlockCategory.Sensors, label = "Гіроскоп Y",
            emit = { _, _ -> "engine.gyro_y()" }
        ))
        add(BlockSpec(
            id = "sensor.gyro_z", category = BlockCategory.Sensors, label = "Гіроскоп Z",
            emit = { _, _ -> "engine.gyro_z()" }
        ))
        add(BlockSpec(
            id = "sensor.light", category = BlockCategory.Sensors, label = "Сенсор світла",
            emit = { _, _ -> "engine.light_sensor()" }
        ))

        // ===== System ====================================================================
        add(BlockSpec(
            id = "sys.log", category = BlockCategory.System, label = "Лог у консоль",
            params = listOf(p("text", "Текст", "\"Привіт\"")),
            emit = { b, _ -> "engine.log(${arg(b, "text", "\"\"")})" }
        ))
        add(BlockSpec(
            id = "sys.toast", category = BlockCategory.System, label = "Сповіщення (toast)",
            params = listOf(p("text", "Текст", "\"Привіт\"")),
            emit = { b, _ -> "engine.toast(${arg(b, "text", "\"\"")})" }
        ))
        add(BlockSpec(
            id = "sys.exit", category = BlockCategory.System, label = "Вийти з програми",
            emit = { _, _ -> "engine.exit()" }
        ))
        add(BlockSpec(
            id = "sys.screen_w", category = BlockCategory.System, label = "Ширина екрану",
            emit = { _, _ -> "engine.screen_width()" }
        ))
        add(BlockSpec(
            id = "sys.screen_h", category = BlockCategory.System, label = "Висота екрану",
            emit = { _, _ -> "engine.screen_height()" }
        ))

        // ===== Network ====================================================================
        add(BlockSpec(
            id = "net.http_get", category = BlockCategory.Network, label = "HTTP GET",
            params = listOf(p("url", "URL", "\"https://example.com\"", ParamKind.Text)),
            emit = { b, _ -> "engine.http_get(${arg(b, "url", "\"\"")})" }
        ))
        add(BlockSpec(
            id = "net.http_post", category = BlockCategory.Network, label = "HTTP POST",
            params = listOf(
                p("url", "URL", "\"https://example.com\"", ParamKind.Text),
                p("body", "Тіло", "\"\"", ParamKind.Text)
            ),
            emit = { b, _ -> "engine.http_post(${arg(b, "url", "\"\"")}, ${arg(b, "body", "\"\"")})" }
        ))

        // ===== Storage ====================================================================
        add(BlockSpec(
            id = "store.save", category = BlockCategory.Storage, label = "Зберегти значення",
            params = listOf(
                p("key", "Ключ", "\"key\"", ParamKind.Text),
                p("value", "Значення", "0")
            ),
            emit = { b, _ -> "engine.store_save(${arg(b, "key", "\"\"")}, ${arg(b, "value", "0")})" }
        ))
        add(BlockSpec(
            id = "store.load", category = BlockCategory.Storage, label = "Завантажити значення",
            params = listOf(p("key", "Ключ", "\"key\"", ParamKind.Text)),
            emit = { b, _ -> "engine.store_load(${arg(b, "key", "\"\"")})" }
        ))
        add(BlockSpec(
            id = "store.delete", category = BlockCategory.Storage, label = "Видалити значення",
            params = listOf(p("key", "Ключ", "\"key\"", ParamKind.Text)),
            emit = { b, _ -> "engine.store_delete(${arg(b, "key", "\"\"")})" }
        ))

        // ===== Logic / comparison ========================================================
        listOf(
            "eq" to ("a == b" to "a == b"),
            "neq" to ("a ~= b" to "a ≠ b"),
            "lt" to ("a < b" to "a < b"),
            "gt" to ("a > b" to "a > b"),
            "le" to ("a <= b" to "a ≤ b"),
            "ge" to ("a >= b" to "a ≥ b")
        ).forEach { (id, lp) ->
            val (luaExpr, label) = lp
            add(BlockSpec(
                id = "logic.$id", category = BlockCategory.Logic, label = label,
                params = listOf(p("a", "a", "0"), p("b", "b", "0")),
                emit = { b, _ ->
                    luaExpr
                        .replace("a", "(${arg(b, "a", "0")})")
                        .replace("b", "(${arg(b, "b", "0")})")
                }
            ))
        }
        add(BlockSpec(
            id = "logic.and", category = BlockCategory.Logic, label = "І (and)",
            params = listOf(p("a", "a", "true", ParamKind.Boolean), p("b", "b", "true", ParamKind.Boolean)),
            emit = { b, _ -> "((${arg(b, "a", "true")}) and (${arg(b, "b", "true")}))" }
        ))
        add(BlockSpec(
            id = "logic.or", category = BlockCategory.Logic, label = "АБО (or)",
            params = listOf(p("a", "a", "false", ParamKind.Boolean), p("b", "b", "false", ParamKind.Boolean)),
            emit = { b, _ -> "((${arg(b, "a", "false")}) or (${arg(b, "b", "false")}))" }
        ))
        add(BlockSpec(
            id = "logic.not", category = BlockCategory.Logic, label = "НЕ (not)",
            params = listOf(p("a", "a", "false", ParamKind.Boolean)),
            emit = { b, _ -> "(not (${arg(b, "a", "false")}))" }
        ))
        add(BlockSpec(
            id = "logic.true", category = BlockCategory.Logic, label = "true",
            emit = { _, _ -> "true" }
        ))
        add(BlockSpec(
            id = "logic.false", category = BlockCategory.Logic, label = "false",
            emit = { _, _ -> "false" }
        ))
    }

    private val byId: Map<String, BlockSpec> = all.associateBy { it.id }

    fun all(): List<BlockSpec> = all
    fun byId(id: String): BlockSpec? = byId[id]
    fun byCategory(): Map<BlockCategory, List<BlockSpec>> =
        all.groupBy { it.category }
            .toSortedMap(compareBy { it.ordinal })
}
