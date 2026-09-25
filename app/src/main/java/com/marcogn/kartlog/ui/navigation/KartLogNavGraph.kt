package com.marcogn.kartlog.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.consigliami.ConsigliamiScreen
import com.marcogn.kartlog.ui.home.HomeScreen
import com.marcogn.kartlog.ui.medallions.PeachMedallionsScreen
import com.marcogn.kartlog.ui.pswitches.PSwitchesScreen
import com.marcogn.kartlog.ui.settings.SettingsScreen
import com.marcogn.kartlog.ui.skin.SkinScreen
import kotlinx.coroutines.launch

/** ModalNavigationDrawer con hamburger sempre disponibile, voci per SPEC §2.1. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KartLogNavGraph(navController: NavHostController = rememberNavController()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val navigateFromDrawer: (Destination) -> Unit = { destination ->
        scope.launch { drawerState.close() }
        navController.navigate(destination) {
            popUpTo(Destination.Home) { saveState = true }
            launchSingleTop = true
            restoreState = true
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
        NavHost(navController = navController, startDestination = Destination.Home) {
            composable<Destination.Home> {
                HomeScreen(
                    onMenuClick = openDrawer,
                    onSkinClick = { navController.navigate(Destination.Skin) },
                    onMedallionsClick = { navController.navigate(Destination.PeachMedallions) },
                    onPSwitchesClick = { navController.navigate(Destination.PSwitches) },
                    onConsigliamiClick = { navController.navigate(Destination.Consigliami) },
                )
            }
            composable<Destination.Skin> {
                SkinScreen(onMenuClick = openDrawer)
            }
            composable<Destination.PeachMedallions> {
                PeachMedallionsScreen(onMenuClick = openDrawer)
            }
            composable<Destination.PSwitches> {
                PSwitchesScreen(onMenuClick = openDrawer)
            }
            composable<Destination.Consigliami> {
                ConsigliamiScreen(onMenuClick = openDrawer)
            }
            composable<Destination.Settings> {
                SettingsScreen(onMenuClick = openDrawer)
            }
        }
    }
}
