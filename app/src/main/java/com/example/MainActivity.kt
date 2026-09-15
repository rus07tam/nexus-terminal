package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConnectionsScreen
import com.example.ui.SessionsScreen
import com.example.ui.SettingsScreen
import com.example.ui.TerminalScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.AppTab
import com.example.viewmodel.TerminalViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: TerminalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                TerminalApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TerminalApp(viewModel: TerminalViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .statusBarsPadding(),
        containerColor = Slate950,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            TerminalBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.CONNECTIONS -> ConnectionsScreen(viewModel = viewModel)
                AppTab.SESSIONS -> SessionsScreen(viewModel = viewModel)
                AppTab.TERMINAL -> TerminalScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TerminalBottomNavigation(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .testTag("bottom_nav_bar"),
        containerColor = Slate900,
        contentColor = CyanNeon,
        tonalElevation = 8.dp
    ) {
        AppTab.values().forEach { tab ->
            val isSelected = (tab == currentTab)
            val icon = when (tab) {
                AppTab.CONNECTIONS -> Icons.Default.Share
                AppTab.SESSIONS -> Icons.Default.List
                AppTab.TERMINAL -> Icons.Default.PlayArrow
                AppTab.SETTINGS -> Icons.Default.Settings
            }
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Slate950,
                    selectedTextColor = CyanNeon,
                    indicatorColor = CyanNeon,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}
