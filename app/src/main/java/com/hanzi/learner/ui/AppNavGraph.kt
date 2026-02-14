package com.hanzi.learner.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hanzi.learner.feature.admin.ui.AdminScreen
import com.hanzi.learner.feature.home.ui.HomeScreen
import com.hanzi.learner.feature.home.viewmodel.HomeNavigation
import com.hanzi.learner.feature.home.viewmodel.HomeViewModel
import com.hanzi.learner.feature.home.viewmodel.HomeViewModelFactoryBuilder
import com.hanzi.learner.feature.practice.ui.PracticeScreen

@Composable
internal fun AppNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    appDeps: AppDependencies,
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(AppRoutes.HOME) {
            val deps = appDeps.homeDeps
            val factory = remember(deps) {
                HomeViewModelFactoryBuilder.fromDependencies(
                    progressRepository = deps.progressRepository,
                    appSettingsRepository = deps.appSettingsRepository,
                    disabledCharRepository = deps.disabledCharRepository,
                    characterRepositoryProvider = deps.characterRepositoryProvider,
                    navigationCallback = { nav ->
                        val current = navController.currentBackStackEntry?.destination?.route
                        if (current == AppRoutes.HOME) {
                            when (nav) {
                                HomeNavigation.NavigateToPractice -> navController.navigate(AppRoutes.PRACTICE) {
                                    launchSingleTop = true
                                }

                                HomeNavigation.NavigateToReview -> navController.navigate(AppRoutes.REVIEW) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    },
                )
            }
            val viewModel: HomeViewModel = viewModel(
                factory = factory,
                key = "home",
            )

            HomeScreen(
                paddingValues = paddingValues,
                viewModel = viewModel,
            )
        }
        composable(AppRoutes.ADMIN) {
            AdminScreen(
                paddingValues = paddingValues,
                onBack = { navController.popBackStack() },
                deps = appDeps.adminDeps,
            )
        }
        composable(AppRoutes.PRACTICE) {
            PracticeScreen(
                paddingValues = paddingValues,
                onExit = { navController.popBackStack() },
                deps = appDeps.practiceDeps,
            )
        }
        composable(AppRoutes.REVIEW) {
            PracticeScreen(
                paddingValues = paddingValues,
                onExit = { navController.popBackStack() },
                deps = appDeps.practiceDeps,
                reviewOnly = true,
            )
        }
    }
}
