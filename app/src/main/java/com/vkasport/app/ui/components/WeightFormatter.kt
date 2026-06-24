package com.vkasport.app.ui.components

fun formatWeight(
    weight: Float
): String {

    return if (
        weight % 1f == 0f
    ) {
        weight.toInt().toString()
    } else {
        weight.toString()
    }
}