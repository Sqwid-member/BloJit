package com.sqwid.blojit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom on-screen keyboard for editing a block parameter.
 *
 * Layout:
 *   [edit field for the current Lua expression]
 *   [tabs: Числа · Знаки · Змінні · Функції · Lua API]
 *   [grid of insertion buttons for the active tab]
 *   [Скасувати] [Готово]
 *
 * Each button inserts text into the edit field at the cursor (or replaces selection).
 */
@Composable
fun BlockKeyboard(
    initial: String,
    paramLabel: String,
    onCommit: (String) -> Unit,
    onCancel: () -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    var tab by remember { mutableStateOf(0) }

    fun insert(s: String) {
        val text = value.text
        val sel = value.selection
        val before = text.substring(0, sel.start)
        val after = text.substring(sel.end)
        val combined = before + s + after
        value = TextFieldValue(combined, TextRange(before.length + s.length))
    }
    fun backspace() {
        val text = value.text
        val sel = value.selection
        if (sel.start == sel.end) {
            if (sel.start == 0) return
            val combined = text.substring(0, sel.start - 1) + text.substring(sel.end)
            value = TextFieldValue(combined, TextRange(sel.start - 1))
        } else {
            val combined = text.substring(0, sel.start) + text.substring(sel.end)
            value = TextFieldValue(combined, TextRange(sel.start))
        }
    }
    fun clearAll() { value = TextFieldValue("") }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Редагування: $paramLabel",
            style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        // value display
        Card {
            Box(modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(12.dp)) {
                Text(
                    if (value.text.isEmpty()) "(пусто)" else value.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // tabs
        val tabs = listOf("Числа", "Знаки", "Змінні", "Функції", "Lua API")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { i, label ->
                val selected = tab == i
                Card(
                    modifier = Modifier.weight(1f).clickable { tab = i },
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = if (selected)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        color = if (selected)
                            MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        when (tab) {
            0 -> NumberPad(
                onKey = ::insert,
                onBackspace = ::backspace,
                onClear = ::clearAll
            )
            1 -> SymbolPad(::insert)
            2 -> KeyGrid(VARIABLES, ::insert)
            3 -> KeyGrid(FUNCTIONS, ::insert)
            4 -> KeyGrid(LUA_API, ::insert)
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Скасувати") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onCommit(value.text) }) { Text("Готово") }
        }
    }
}

@Composable
private fun NumberPad(onKey: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    val rows = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "(", ")"),
        listOf("=", ",", "+", "%"),
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k -> KeyButton(k, modifier = Modifier.weight(1f)) { onKey(k) } }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyButton("⌫", modifier = Modifier.weight(1f), onClick = onBackspace)
            KeyButton("Очистити", modifier = Modifier.weight(2f), onClick = onClear)
            KeyButton("␣", modifier = Modifier.weight(1f)) { onKey(" ") }
        }
    }
}

@Composable
private fun SymbolPad(onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("\"", "'", "`", "_"),
        listOf("[", "]", "{", "}"),
        listOf("<", ">", "<=", ">="),
        listOf("==", "~=", "and", "or"),
        listOf("not", "..", "#", "nil"),
        listOf("true", "false", ":", ";"),
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k -> KeyButton(k, modifier = Modifier.weight(1f)) { onKey(k) } }
            }
        }
    }
}

private val VARIABLES = listOf(
    "x", "y", "i", "j", "k", "n", "t", "dt", "score", "lives", "speed",
    "width", "height", "color", "msg", "name", "value", "list", "result"
)
private val FUNCTIONS = listOf(
    "tonumber(", "tostring(", "ipairs(", "pairs(", "type(", "print(", "select("
)
private val LUA_API = listOf(
    "math.pi", "math.abs(", "math.floor(", "math.ceil(", "math.sqrt(",
    "math.sin(", "math.cos(", "math.tan(", "math.random(", "math.min(", "math.max(",
    "string.len(", "string.upper(", "string.lower(", "string.sub(", "string.format(",
    "table.insert(", "table.remove(", "table.sort(", "table.concat(",
    "engine.draw_rect(", "engine.draw_circle(", "engine.draw_line(",
    "engine.set_color(", "engine.log(", "engine.now(", "engine.toast(",
    "engine.touch_x()", "engine.touch_y()", "engine.is_pressed()"
)

@Composable
private fun KeyGrid(items: List<String>, onKey: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k -> KeyButton(k, modifier = Modifier.weight(1f)) { onKey(k) } }
                if (row.size < 3) repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
    }
}
