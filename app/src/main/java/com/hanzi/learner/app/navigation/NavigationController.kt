package com.hanzi.learner.app.navigation

import androidx.navigation.NavOptionsBuilder

interface NavigationController {
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit = {})
    val currentRoute: String?
}
