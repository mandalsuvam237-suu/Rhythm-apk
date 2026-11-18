package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryVisuals {

    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "bolt" to Icons.Rounded.Bolt,
        "bedtime" to Icons.Rounded.Bedtime,
        "restaurant" to Icons.Rounded.Restaurant,
        "menu_book" to Icons.Rounded.MenuBook,
        "fitness_center" to Icons.Rounded.FitnessCenter,
        "auto_stories" to Icons.Rounded.AutoStories,
        "lunch_dining" to Icons.Rounded.LunchDining,
        "coffee" to Icons.Rounded.Coffee,
        "laptop_mac" to Icons.Rounded.LaptopMac,
        "favorite" to Icons.Rounded.Favorite,
        "directions_car" to Icons.Rounded.DirectionsCar,
        "self_improvement" to Icons.Rounded.SelfImprovement,
        "music_note" to Icons.Rounded.MusicNote,
        "palette" to Icons.Rounded.Palette,
        "work" to Icons.Rounded.Work,
        "category" to Icons.Rounded.Category
    )

    val presetColors: List<String> = listOf(
        "#FF6B4A", "#6366F1", "#10B981", "#F59E0B",
        "#3B82F6", "#8B5CF6", "#EC4899", "#14B8A6",
        "#F97316", "#EF4444", "#06B6D4", "#64748B"
    )

    fun iconFor(key: String): ImageVector =
        availableIcons.firstOrNull { it.first == key }?.second ?: Icons.Rounded.Category
}
