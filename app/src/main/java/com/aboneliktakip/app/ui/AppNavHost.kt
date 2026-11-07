package com.aboneliktakip.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aboneliktakip.app.ui.components.TopLevel
import com.aboneliktakip.app.ui.edit.EditScreen
import com.aboneliktakip.app.ui.edit.EditViewModel
import com.aboneliktakip.app.ui.home.HomeScreen
import com.aboneliktakip.app.ui.settings.SettingsScreen
import com.aboneliktakip.app.ui.stats.StatsScreen

private const val EDIT_ROUTE = "edit?${EditViewModel.ARG_ID}={${EditViewModel.ARG_ID}}"

private fun editRoute(id: Long) = "edit?${EditViewModel.ARG_ID}=$id"

@Composable
fun AppNavHost(pendingRoute: String?, onRouteConsumed: () -> Unit) {
    val nav = rememberNavController()

    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        TopLevel.entries.firstOrNull { it.route == route }?.let { nav.navigateTopLevel(it) } ?: nav.navigate(route)
        onRouteConsumed()
    }

    val navigateTopLevel: (TopLevel) -> Unit = { nav.navigateTopLevel(it) }

    NavHost(
        navController = nav,
        startDestination = TopLevel.HOME.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(TopLevel.HOME.route) {
            HomeScreen(
                vm = viewModel(factory = AppViewModels.Factory),
                onAdd = { nav.navigate(editRoute(0)) },
                onOpen = { nav.navigate(editRoute(it)) },
                onOpenStats = { nav.navigateTopLevel(TopLevel.STATS) },
                onNavigate = navigateTopLevel,
            )
        }
        composable(TopLevel.STATS.route) {
            StatsScreen(vm = viewModel(factory = AppViewModels.Factory), onNavigate = navigateTopLevel)
        }
        composable(TopLevel.SETTINGS.route) {
            SettingsScreen(vm = viewModel(factory = AppViewModels.Factory), onNavigate = navigateTopLevel)
        }
        composable(
            route = EDIT_ROUTE,
            arguments = listOf(navArgument(EditViewModel.ARG_ID) { type = NavType.LongType; defaultValue = 0L }),
            enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
        ) { entry ->
            EditScreen(
                vm = viewModel(factory = AppViewModels.Factory),
                // Yalnızca bu ekran hâlâ en üstteyse kapat (çift kapatmayı önler).
                onClose = {
                    if (nav.currentBackStackEntry?.id == entry.id) nav.popBackStack()
                },
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(dest: TopLevel) {
    navigate(dest.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
