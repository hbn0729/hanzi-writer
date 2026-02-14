package com.hanzi.learner.ui.navigation

import androidx.navigation.NavOptionsBuilder
import com.hanzi.learner.feature.home.viewmodel.HomeNavigation
import com.hanzi.learner.ui.AppRoutes
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HomeNavigationHandlerTest {

    private lateinit var fakeNavController: FakeNavigationController
    private lateinit var navigationHandler: HomeNavigationHandler

    @Before
    fun setup() {
        fakeNavController = FakeNavigationController()
        navigationHandler = HomeNavigationHandler(fakeNavController)
    }

    @Test
    fun `handle NavigateToPractice when current route is HOME`() {
        // Given
        fakeNavController.currentRoute = AppRoutes.HOME

        // When
        navigationHandler.handle(HomeNavigation.NavigateToPractice)

        // Then
        assertEquals(1, fakeNavController.navigateCalls.size)
        assertEquals(AppRoutes.PRACTICE, fakeNavController.navigateCalls[0])
    }

    @Test
    fun `handle NavigateToReview when current route is HOME`() {
        // Given
        fakeNavController.currentRoute = AppRoutes.HOME

        // When
        navigationHandler.handle(HomeNavigation.NavigateToReview)

        // Then
        assertEquals(1, fakeNavController.navigateCalls.size)
        assertEquals(AppRoutes.REVIEW, fakeNavController.navigateCalls[0])
    }

    @Test
    fun `do not navigate when current route is not HOME`() {
        // Given
        fakeNavController.currentRoute = AppRoutes.PRACTICE

        // When
        navigationHandler.handle(HomeNavigation.NavigateToPractice)

        // Then
        assertEquals(0, fakeNavController.navigateCalls.size)
    }

    @Test
    fun `do not navigate when current route is null`() {
        // Given
        fakeNavController.currentRoute = null

        // When
        navigationHandler.handle(HomeNavigation.NavigateToPractice)

        // Then
        assertEquals(0, fakeNavController.navigateCalls.size)
    }

    // Simple fake implementation for testing
    class FakeNavigationController : NavigationController {
        val navigateCalls = mutableListOf<String>()
        override var currentRoute: String? = null

        override fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
            navigateCalls.add(route)
        }
    }
}
