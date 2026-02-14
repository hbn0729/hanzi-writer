package com.hanzi.learner.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

class NavHostControllerAdapter(
    private val navController: NavHostController,
) : NavigationController {

    override fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        navController.navigate(route, builder)
    }

    override val currentRoute: String?
        get() = navController.currentBackStackEntry?.destination?.route
}
