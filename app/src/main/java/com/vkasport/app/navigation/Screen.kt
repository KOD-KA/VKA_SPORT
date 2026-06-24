package com.vkasport.app.navigation

sealed class Screen(val route: String) {

    object Training : Screen("training")
    object Records : Screen("records")
    object Calendar : Screen("calendar")
    object Info : Screen("info")
}