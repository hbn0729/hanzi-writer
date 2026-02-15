package com.hanzi.learner.app.navigation

import com.hanzi.learner.features.home.viewmodel.HomeNavigation
import com.hanzi.learner.app.AppRoutes

class HomeNavigationHandler(
    private val navController: NavigationController,
) {
    fun handle(navigation: HomeNavigation) {
        if (navController.currentRoute != AppRoutes.HOME) return

        when (navigation) {
            HomeNavigation.NavigateToPractice -> navigateToPractice()
            HomeNavigation.NavigateToReview -> navigateToReview()
        }
    }

    private fun navigateToPractice() {
        navController.navigate(AppRoutes.PRACTICE) {
            launchSingleTop = true
        }
    }

    private fun navigateToReview() {
        navController.navigate(AppRoutes.REVIEW) {
            launchSingleTop = true
        }
    }
}
