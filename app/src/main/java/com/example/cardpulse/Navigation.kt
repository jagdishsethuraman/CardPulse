package com.example.cardpulse

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.cardpulse.ui.analytics.AnalyticsScreen
import com.example.cardpulse.ui.analytics.AnalyticsViewModel
import com.example.cardpulse.ui.analytics.AnalyticsViewModelFactory
import com.example.cardpulse.ui.cards.CardsScreen
import com.example.cardpulse.ui.cards.CardsViewModel
import com.example.cardpulse.ui.cards.CardsViewModelFactory
import com.example.cardpulse.ui.dashboard.DashboardScreen
import com.example.cardpulse.ui.dashboard.DashboardViewModel
import com.example.cardpulse.ui.dashboard.DashboardViewModelFactory
import com.example.cardpulse.ui.import_flow.ImportPreviewScreen
import com.example.cardpulse.ui.import_flow.ImportScreen
import com.example.cardpulse.ui.import_flow.ImportViewModel
import com.example.cardpulse.ui.import_flow.ImportViewModelFactory
import com.example.cardpulse.ui.onboarding.OnboardingScreen
import com.example.cardpulse.ui.onboarding.OnboardingViewModel
import com.example.cardpulse.ui.onboarding.OnboardingViewModelFactory
import com.example.cardpulse.ui.settings.SettingsScreen
import com.example.cardpulse.ui.settings.SettingsViewModel
import com.example.cardpulse.ui.settings.SettingsViewModelFactory
import com.example.cardpulse.ui.transactions.TransactionsScreen
import com.example.cardpulse.ui.transactions.TransactionsViewModel
import com.example.cardpulse.ui.transactions.TransactionsViewModelFactory

enum class MainTab { DASHBOARD, TRANSACTIONS, ANALYTICS, CARDS, SETTINGS }

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as CardPulseApp
    val hasCompletedOnboardingState by app.userPreferences.hasCompletedOnboardingFlow.collectAsState(initial = null)

    val hasCompletedOnboarding = hasCompletedOnboardingState
    if (hasCompletedOnboarding == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val startDestination = if (hasCompletedOnboarding) Main else Onboarding
    val backStack = rememberNavBackStack(startDestination)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Onboarding> {
                val onboardingViewModel: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModelFactory(app.userPreferences, app.repository)
                )
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onFinished = {
                        backStack.add(Main)
                        backStack.remove(Onboarding)
                    }
                )
            }
            
            entry<Main> {
                MainScreen(
                    app = app,
                    onNavigateToImport = { backStack.add(Import) },
                    onWiped = {
                        backStack.add(Onboarding)
                        backStack.remove(Main)
                    }
                )
            }
            
            entry<Import> {
                // Scope ImportViewModel to the Activity so it can be shared with ImportPreview
                val activity = LocalContext.current as ComponentActivity
                val importViewModel: ImportViewModel = viewModel(
                    viewModelStoreOwner = activity,
                    factory = ImportViewModelFactory(app.repository, context)
                )
                ImportScreen(
                    viewModel = importViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToPreview = { backStack.add(ImportPreview) }
                )
            }
            
            entry<ImportPreview> {
                val activity = LocalContext.current as ComponentActivity
                val importViewModel: ImportViewModel = viewModel(
                    viewModelStoreOwner = activity,
                    factory = ImportViewModelFactory(app.repository, context)
                )
                ImportPreviewScreen(
                    viewModel = importViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onImportConfirmed = {
                        // After successful import, pop back to Main dashboard
                        backStack.remove(ImportPreview)
                        backStack.remove(Import)
                    }
                )
            }
        }
    )
}

@Composable
fun MainScreen(
    app: CardPulseApp,
    onNavigateToImport: () -> Unit,
    onWiped: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    val userName by app.userPreferences.userNameFlow.collectAsState(initial = "")

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.DASHBOARD,
                    onClick = { currentTab = MainTab.DASHBOARD },
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.TRANSACTIONS,
                    onClick = { currentTab = MainTab.TRANSACTIONS },
                    icon = { Text("📄", fontSize = 20.sp) },
                    label = { Text("Activity") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.ANALYTICS,
                    onClick = { currentTab = MainTab.ANALYTICS },
                    icon = { Text("📈", fontSize = 20.sp) },
                    label = { Text("Trends") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.CARDS,
                    onClick = { currentTab = MainTab.CARDS },
                    icon = { Text("💳", fontSize = 20.sp) },
                    label = { Text("Cards") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { currentTab = MainTab.SETTINGS },
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("Settings") }
                )
            }
        },
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                MainTab.DASHBOARD -> {
                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = DashboardViewModelFactory(app.repository)
                    )
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        userName = userName,
                        onViewAllTransactions = { currentTab = MainTab.TRANSACTIONS },
                        onNavigateToImport = onNavigateToImport
                    )
                }
                MainTab.TRANSACTIONS -> {
                    val transactionsViewModel: TransactionsViewModel = viewModel(
                        factory = TransactionsViewModelFactory(app.repository)
                    )
                    TransactionsScreen(
                        viewModel = transactionsViewModel
                    )
                }
                MainTab.ANALYTICS -> {
                    val analyticsViewModel: AnalyticsViewModel = viewModel(
                        factory = AnalyticsViewModelFactory(app.repository)
                    )
                    AnalyticsScreen(
                        viewModel = analyticsViewModel
                    )
                }
                MainTab.CARDS -> {
                    val cardsViewModel: CardsViewModel = viewModel(
                        factory = CardsViewModelFactory(app.repository)
                    )
                    CardsScreen(
                        viewModel = cardsViewModel
                    )
                }
                MainTab.SETTINGS -> {
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(app.userPreferences, app.repository)
                    )
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onWiped = onWiped
                    )
                }
            }
        }
    }
}
