package com.sqwid.blojit.blocks

import androidx.compose.ui.graphics.Color

enum class BlockCategory(val label: String, val color: Color) {
    Program("Програма", Color(0xFF2E3F76)),
    Shapes("Фігури", Color(0xFF1F6FEB)),
    Drawing("Малювання", Color(0xFF7C3AED)),
    Input("Дотик / ввід", Color(0xFFE34A6F)),
    UI("Інтерфейс", Color(0xFFEC4899)),
    Variables("Змінні", Color(0xFFF59E0B)),
    ControlFlow("Логіка", Color(0xFFD97706)),
    Functions("Функції", Color(0xFF8B5CF6)),
    Math("Математика", Color(0xFF14B8A6)),
    Strings("Рядки", Color(0xFF22C55E)),
    Lists("Списки", Color(0xFF10B981)),
    Time("Час", Color(0xFF0EA5E9)),
    Audio("Звук", Color(0xFFEAB308)),
    Sensors("Сенсори", Color(0xFFA855F7)),
    System("Система", Color(0xFF64748B)),
    Network("Мережа", Color(0xFF0284C7)),
    Storage("Сховище", Color(0xFF475569)),
    Logic("Порівняння", Color(0xFFEF4444)),
}
