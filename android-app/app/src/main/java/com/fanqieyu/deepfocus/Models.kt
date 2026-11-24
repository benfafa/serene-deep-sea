package com.fanqieyu.deepfocus

import androidx.annotation.DrawableRes

enum class Destination(val label: String, val glyph: String) {
    Aquarium("水族箱", "⌁"),
    Scenes("场景", "▦"),
    Nursery("鱼苗", "✦"),
    Settings("设置", "⚙")
}

data class Fish(
    val name: String,
    val description: String,
    val temperament: String,
    val rarity: String,
    val water: String,
    val bond: Int,
    @DrawableRes val imageRes: Int,
    val category: String = "鱼类",
)

data class AquariumScene(
    val name: String,
    val description: String,
    val unlocked: Boolean,
    @DrawableRes val imageRes: Int,
)

data class TodoItem(
    val id: Int,
    val title: String,
    val completed: Boolean = false,
)
