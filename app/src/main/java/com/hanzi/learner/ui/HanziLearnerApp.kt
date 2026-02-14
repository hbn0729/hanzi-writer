package com.hanzi.learner.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hanzi.learner.R
import com.hanzi.learner.feature.admin.ui.AdminScreen
import com.hanzi.learner.feature.home.ui.HomeScreen
import com.hanzi.learner.feature.home.viewmodel.HomeNavigation
import com.hanzi.learner.feature.home.viewmodel.HomeViewModel
import com.hanzi.learner.feature.home.viewmodel.HomeViewModelFactoryBuilder
import com.hanzi.learner.feature.practice.ui.PracticeScreen

private object Routes {
    const val HOME = "home"
    const val ADMIN = "admin"
    const val PRACTICE = "practice"
    const val REVIEW = "review"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HanziLearnerApp(
    appDeps: AppDependencies,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentRoute != Routes.PRACTICE && currentRoute != Routes.REVIEW) {
                TopAppBar(
                    title = { Text(text = "Hanzi Learner") },
                    navigationIcon = {
                        if (navController.previousBackStackEntry != null && currentRoute != Routes.HOME) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_back),
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute == Routes.HOME) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { navController.navigate(Routes.ADMIN) },
                                        )
                                    },
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_settings),
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.HOME) {
                val deps = appDeps.homeDeps
                val factory = remember(deps) {
                    HomeViewModelFactoryBuilder.fromDependencies(
                        progressRepository = deps.progressRepository,
                        appSettingsRepository = deps.appSettingsRepository,
                        disabledCharRepository = deps.disabledCharRepository,
                        characterRepositoryProvider = deps.characterRepositoryProvider,
                        navigationCallback = { nav ->
                            val current = navController.currentBackStackEntry?.destination?.route
                            if (current == Routes.HOME) {
                                when (nav) {
                                    HomeNavigation.NavigateToPractice -> navController.navigate(Routes.PRACTICE) {
                                        launchSingleTop = true
                                    }

                                    HomeNavigation.NavigateToReview -> navController.navigate(Routes.REVIEW) {
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
            composable(Routes.ADMIN) {
                AdminScreen(
                    paddingValues = paddingValues,
                    onBack = { navController.popBackStack() },
                    deps = appDeps.adminDeps,
                )
            }
            composable(Routes.PRACTICE) {
                PracticeScreen(
                    paddingValues = paddingValues,
                    onExit = { navController.popBackStack() },
                    deps = appDeps.practiceDeps,
                )
            }
            composable(Routes.REVIEW) {
                PracticeScreen(
                    paddingValues = paddingValues,
                    onExit = { navController.popBackStack() },
                    deps = appDeps.practiceDeps,
                    reviewOnly = true,
                )
            }
        }
    }
}
