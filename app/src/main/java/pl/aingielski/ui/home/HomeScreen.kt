package pl.aingielski.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.aingielski.R
import pl.aingielski.ui.components.AppIcon
import pl.aingielski.ui.components.Tile
import pl.aingielski.ui.icons.rememberListenTileIcon
import pl.aingielski.ui.icons.rememberStatsTileIcon

/** Main screen: burger menu with "Ustawienia" / "Słówka" and the function tiles (FR-005, FR-006). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenWords: () -> Unit,
    onOpenListen: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    fun closeThen(action: () -> Unit) {
        scope.launch { drawerState.close() }
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_settings)) },
                    icon = { AppIcon(R.drawable.ic_settings, contentDescription = null) },
                    selected = false,
                    onClick = { closeThen(onOpenSettings) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_words)) },
                    icon = { AppIcon(R.drawable.ic_words, contentDescription = null) },
                    selected = false,
                    onClick = { closeThen(onOpenWords) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            AppIcon(R.drawable.ic_menu, contentDescription = stringResource(R.string.menu_open))
                        }
                    },
                )
            },
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(padding),
            ) {
                item {
                    Tile(stringResource(R.string.tile_listen), rememberListenTileIcon(), onOpenListen)
                }
                item {
                    Tile(stringResource(R.string.tile_stats), rememberStatsTileIcon(), onOpenStats)
                }
            }
        }
    }
}
