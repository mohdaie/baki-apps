package com.mohdaie.baki.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mohdaie.baki.model.Category

val Category.color: Color
    get() = when (this) {
        Category.FOOD -> Color(0xFFF2A04E)
        Category.BILLS -> Color(0xFFA3A3F2)
        Category.TRANSPORT -> Color(0xFF7DBDEE)
        Category.HEALTH -> Color(0xFFEE8F93)
        Category.SHOPPING -> Color(0xFFF09CC6)
        Category.ENTERTAINMENT -> Color(0xFFF2D15C)
    }

val Category.icon: ImageVector
    get() = when (this) {
        Category.FOOD -> Icons.Filled.Fastfood
        Category.BILLS -> Icons.Filled.Lightbulb
        Category.TRANSPORT -> Icons.Filled.DirectionsCar
        Category.HEALTH -> Icons.Filled.LocalHospital
        Category.SHOPPING -> Icons.Filled.ShoppingCart
        Category.ENTERTAINMENT -> Icons.Filled.Movie
    }
