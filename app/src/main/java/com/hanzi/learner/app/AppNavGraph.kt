package com.hanzi.learner.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hanzi.learner.features.admin.ui.AdminScreen
import com.hanzi.learner.features.home.ui.HomeScreen
import com.hanzi.learner.features.home.viewmodel.HomeViewModel
import com.hanzi.learner.features.home.viewmodel.HomeViewModelFactoryBuilder
import com.hanzi.learner.features.practice.ui.PracticeScreen
import com.hanzi.learner.app.navigation.HomeNavigationHandler
import com.hanzi.learner.app.navigation.NavHostControllerAdapter

@Composable
internal fun AppNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    appDeps: AppDependencies,
) {
    val homeNavigationHandler = remember { HomeNavigationHandler(NavHostControllerAdapter(navController)) }

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
                    navigationCallback = homeNavigationHandler::handle,
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
