package com.hanzi.learner.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

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
            AppTopBar(
                currentRoute = currentRoute,
                canNavigateBack = navController.previousBackStackEntry != null,
                onBack = { navController.popBackStack() },
                onAdminLongPress = { navController.navigate(AppRoutes.ADMIN) },
            )
        },
    ) { paddingValues ->
        AppNavGraph(
            navController = navController,
            paddingValues = paddingValues,
            appDeps = appDeps,
        )
    }
}
