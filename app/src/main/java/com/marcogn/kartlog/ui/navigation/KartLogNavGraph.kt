package com.marcogn.kartlog.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.consigliami.ConsigliamiDetailScreen
import com.marcogn.kartlog.ui.consigliami.ConsigliamiScreen
import com.marcogn.kartlog.ui.home.HomeScreen
import com.marcogn.kartlog.ui.medallions.PeachMedallionsScreen
import com.marcogn.kartlog.ui.pswitches.PSwitchesScreen
import com.marcogn.kartlog.ui.results.ResultsScreen
import com.marcogn.kartlog.ui.settings.SettingsScreen
import com.marcogn.kartlog.ui.skin.SkinDetailScreen
import com.marcogn.kartlog.ui.skin.SkinScreen
import kotlinx.coroutines.launch

// Una NavBackStackEntry raggiunge RESUMED solo quando la sua transizione di enter/exit è
// completamente finita ed è stabile in cima allo stack. Guardare ogni navigate()/popBackStack()
// dietro questo controllo, sull'entry *specifica* proprietaria della callback, è la soluzione
// raccomandata per un doppio tap rapido (es. indietro e poi subito un'altra destinazione) che
// altrimenti atterrerebbe su una schermata ancora a metà transizione invece di quella voluta —
// stesso fix di ThePatientGamerHelperNavGraph.kt (vedi CLAUDE.md, decisione fase 1, "da
// rivalutare se emergono problemi di doppio tap": qui sono emersi davvero, dopo il rilascio 0.1.1).
private fun NavBackStackEntry.lifecycleIsResumed() = lifecycle.currentState == Lifecycle.State.RESUMED

private const val NAV_ANIM_DURATION_MS = 300

private val navEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> fullWidth },
    ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION_MS))
}
private val navExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
    ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION_MS))
}
private val navPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> -fullWidth / 4 },
    ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION_MS))
}
private val navPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> fullWidth },
    ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION_MS))
}

/** ModalNavigationDrawer con hamburger sempre disponibile, voci per SPEC §2.1. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KartLogNavGraph(navController: NavHostController = rememberNavController()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val navigateFromDrawer: (Destination) -> Unit = { destination ->
        // Guardia contro un tap sul drawer che atterra mentre la schermata corrente è ancora a
        // metà transizione (vedi lifecycleIsResumed() sopra) — stessa corsa di un doppio tap
        // avanti/indietro.
        if (navController.currentBackStackEntry?.lifecycleIsResumed() != false) {
            scope.launch { drawerState.close() }
            if (destination == Destination.Home) {
                // Non un popUpTo(Home){saveState=true} + restoreState=true come le altre voci:
                // qui il target DELLA navigazione e il target del popUpTo coincidono, e Home a
                // quel punto non è quasi mai più in cima allo stack (l'ha già lasciato con lo
                // stesso saveState quando si è navigato altrove) — un caso limite in cui
                // Navigation Compose non ripristina in modo affidabile lo stato salvato di se
                // stessa. Risultato osservato: "Home" nel drawer non portava mai a Home. Si pop
                // sempre fino in fondo e si spinge una Home nuova, senza fare affidamento su
                // save/restoreState.
                navController.navigate(Destination.Home) {
                    popUpTo<Destination.Home> { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                navController.navigate(destination) {
                    popUpTo(Destination.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_home)) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.Home) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_skin)) },
                    icon = { Icon(Icons.Filled.Checkroom, contentDescription = null) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.Skin) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_medallions)) },
                    icon = { Icon(Icons.Filled.MonetizationOn, contentDescription = null) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.PeachMedallions) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_pswitches)) },
                    icon = { Icon(Icons.Filled.TouchApp, contentDescription = null) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.PSwitches) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_consigliami)) },
                    icon = { Icon(Icons.Filled.Lightbulb, contentDescription = null) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.Consigliami) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_results)) },
                    icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.Results) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_settings)) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.Settings) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = Destination.Home,
            enterTransition = navEnterTransition,
            exitTransition = navExitTransition,
            popEnterTransition = navPopEnterTransition,
            popExitTransition = navPopExitTransition,
        ) {
            composable<Destination.Home> { entry ->
                HomeScreen(
                    onMenuClick = openDrawer,
                    onSkinClick = { if (entry.lifecycleIsResumed()) navController.navigate(Destination.Skin) },
                    onMedallionsClick = { if (entry.lifecycleIsResumed()) navController.navigate(Destination.PeachMedallions) },
                    onPSwitchesClick = { if (entry.lifecycleIsResumed()) navController.navigate(Destination.PSwitches) },
                    onConsigliamiClick = { if (entry.lifecycleIsResumed()) navController.navigate(Destination.Consigliami) },
                    onResultsClick = { if (entry.lifecycleIsResumed()) navController.navigate(Destination.Results) },
                )
            }
            composable<Destination.Skin> { entry ->
                SkinScreen(
                    onMenuClick = openDrawer,
                    onCharacterClick = { characterId ->
                        if (entry.lifecycleIsResumed()) navController.navigate(Destination.SkinDetail(characterId))
                    },
                )
            }
            composable<Destination.SkinDetail> { entry ->
                SkinDetailScreen(onBack = { if (entry.lifecycleIsResumed()) navController.popBackStack() })
            }
            composable<Destination.PeachMedallions> {
                PeachMedallionsScreen(onMenuClick = openDrawer)
            }
            composable<Destination.PSwitches> {
                PSwitchesScreen(onMenuClick = openDrawer)
            }
            composable<Destination.Consigliami> { entry ->
                ConsigliamiScreen(
                    onMenuClick = openDrawer,
                    onEventClick = { eventId, includeNearby ->
                        if (entry.lifecycleIsResumed()) {
                            navController.navigate(Destination.ConsigliamiDetail(eventId, includeNearby))
                        }
                    },
                )
            }
            composable<Destination.ConsigliamiDetail> { entry ->
                ConsigliamiDetailScreen(onBack = { if (entry.lifecycleIsResumed()) navController.popBackStack() })
            }
            composable<Destination.Results> {
                ResultsScreen(onMenuClick = openDrawer)
            }
            composable<Destination.Settings> {
                SettingsScreen(onMenuClick = openDrawer)
            }
        }
    }
}
