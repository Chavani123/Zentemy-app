package mx.zentemi.yuhmu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.zentemi.yuhmu.util.LocalAppLanguage
import mx.zentemi.yuhmu.util.tr
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.zentemi.yuhmu.model.User
import mx.zentemi.yuhmu.ui.auth.LoginScreen
import mx.zentemi.yuhmu.ui.auth.RecoverScreen
import mx.zentemi.yuhmu.ui.auth.RecoveryCodeScreen
import mx.zentemi.yuhmu.ui.auth.RegisterScreen
import mx.zentemi.yuhmu.ui.auth.SplashScreen
import mx.zentemi.yuhmu.ui.auth.WelcomeScreen
import mx.zentemi.yuhmu.ui.culture.CultureScreen
import mx.zentemi.yuhmu.ui.games.AudioLibraryScreen
import mx.zentemi.yuhmu.ui.games.GameScreen
import mx.zentemi.yuhmu.ui.games.GameSetupScreen
import mx.zentemi.yuhmu.ui.games.GamesScreen
import mx.zentemi.yuhmu.ui.games.NumberLessonsScreen
import mx.zentemi.yuhmu.ui.learn.DailyScreen
import mx.zentemi.yuhmu.ui.learn.DictionaryScreen
import mx.zentemi.yuhmu.ui.learn.HomeScreen
import mx.zentemi.yuhmu.ui.learn.LessonScreen
import mx.zentemi.yuhmu.ui.profile.ProfileScreen
import mx.zentemi.yuhmu.ui.stories.StoryScreen
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.ui.theme.YuhmuTheme
import mx.zentemi.yuhmu.ui.trophies.TrophiesScreen
import mx.zentemi.yuhmu.viewmodel.AuthState
import mx.zentemi.yuhmu.viewmodel.AuthViewModel
import mx.zentemi.yuhmu.viewmodel.YuhmuViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YuhmuTheme {
                YuhmuRoot()
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val GAMES = "games"
    const val TROPHIES = "trophies"
    const val CULTURE = "culture"
    const val PROFILE = "profile"
    const val DAILY = "daily"
    const val DICTIONARY = "dictionary"
    const val STORY = "story/{id}"
    const val LESSON = "lesson/{id}"
    const val GAME_SETUP = "gameSetup/{kind}"
    const val NUMBER_LESSONS = "numberLessons"
    const val AUDIO_LIBRARY = "audioLibrary"
    const val PLAY = "play/{kind}/{category}"

    const val WELCOME = "welcome"
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val RECOVER = "recover"
}

/** Raíz: decide entre carga, acceso (login/registro), código de recuperación o la app. */
@Composable
private fun YuhmuRoot() {
    val auth: AuthViewModel = viewModel()
    val vm: YuhmuViewModel = viewModel()
    val state by auth.state.collectAsStateWithLifecycle()
    val recoveryCode by auth.recoveryCode.collectAsStateWithLifecycle()
    val currentLang by vm.language.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalAppLanguage provides currentLang) {
        LaunchedEffect(state) {
            when (val s = state) {
                is AuthState.LoggedIn -> vm.bindUser(s.user)
                AuthState.LoggedOut -> vm.unbind()
                AuthState.Loading -> Unit
            }
        }

        when (val s = state) {
            AuthState.Loading -> SplashScreen()
            AuthState.LoggedOut -> AuthFlow(auth)
            is AuthState.LoggedIn -> {
                val code = recoveryCode
                if (code != null) RecoveryCodeScreen(code) { auth.dismissRecoveryCode() }
                else MainFlow(s.user, auth, vm)
            }
        }
    }
}

@Composable
private fun AuthFlow(auth: AuthViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.WELCOME) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onStart = { nav.navigate(Routes.REGISTER) },
                onHaveAccount = { nav.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.REGISTER) { RegisterScreen(auth) { nav.popBackStack() } }
        composable(Routes.LOGIN) {
            LoginScreen(auth, onBack = { nav.popBackStack() }, onRecover = { nav.navigate(Routes.RECOVER) })
        }
        composable(Routes.RECOVER) { RecoverScreen(auth) { nav.popBackStack() } }
    }
}

@Composable
private fun MainFlow(user: User, auth: AuthViewModel, vm: YuhmuViewModel) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onOpenLesson = { nav.navigate("lesson/$it") },
                bottomBar = { MainBottomBar(nav) }
            )
        }
        composable(
            Routes.STORY,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            StoryScreen(entry.arguments?.getString("id").orEmpty(), vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.GAMES) {
            GamesScreen(
                vm = vm,
                onPick = { nav.navigate("gameSetup/$it") },
                onOpenDaily = { nav.navigate(Routes.DAILY) },
                onOpenStory = { nav.navigate("story/$it") },
                onOpenNumbers = { nav.navigate(Routes.NUMBER_LESSONS) },
                onOpenAudioLibrary = { nav.navigate(Routes.AUDIO_LIBRARY) },
                bottomBar = { MainBottomBar(nav) }
            )
        }
        composable(Routes.AUDIO_LIBRARY) {
            AudioLibraryScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.NUMBER_LESSONS) {
            NumberLessonsScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onOpenLesson = { nav.navigate("lesson/$it") }
            )
        }
        composable(Routes.TROPHIES) {
            TrophiesScreen(vm = vm, bottomBar = { MainBottomBar(nav) })
        }
        composable(Routes.CULTURE) {
            CultureScreen(
                bottomBar = { MainBottomBar(nav) },
                onOpenDictionary = { nav.navigate(Routes.DICTIONARY) }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(user = user, auth = auth, vm = vm, bottomBar = { MainBottomBar(nav) })
        }
        composable(Routes.DAILY) { DailyScreen(vm = vm, onBack = { nav.popBackStack() }) }
        composable(Routes.DICTIONARY) {
            DictionaryScreen(vm = vm, bottomBar = { MainBottomBar(nav) })
        }
        composable(
            Routes.LESSON,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            LessonScreen(
                lessonId = entry.arguments?.getString("id").orEmpty(),
                vm = vm,
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Routes.GAME_SETUP,
            arguments = listOf(navArgument("kind") { type = NavType.StringType })
        ) { entry ->
            val kind = entry.arguments?.getString("kind").orEmpty()
            GameSetupScreen(
                kind = kind,
                vm = vm,
                onBack = { nav.popBackStack() },
                onPick = { category -> nav.navigate("play/$kind/$category") }
            )
        }
        composable(
            Routes.PLAY,
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { entry ->
            GameScreen(
                kind = entry.arguments?.getString("kind").orEmpty(),
                category = entry.arguments?.getString("category").orEmpty(),
                vm = vm,
                onBack = { nav.popBackStack() }
            )
        }
    }
}

private data class Tab(val route: String, val labelKey: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "nav_home", Icons.Filled.Home),
    Tab(Routes.GAMES, "nav_games", Icons.Filled.SportsEsports),
    Tab(Routes.TROPHIES, "nav_trophies", Icons.Filled.EmojiEvents),
    Tab(Routes.CULTURE, "nav_culture", Icons.Filled.Eco),
    Tab(Routes.PROFILE, "nav_profile", Icons.Filled.Person)
)

@Composable
private fun MainBottomBar(nav: NavController) {
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    NavigationBar(containerColor = Color.White) {
        TABS.forEach { tab ->
            val title = tab.labelKey.tr()
            val selected = current == tab.route || (tab.route == Routes.CULTURE && current == Routes.DICTIONARY)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (current != tab.route) {
                        nav.navigate(tab.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = title) },
                label = { Text(title, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Brand.purple,
                    selectedTextColor = Brand.purple,
                    unselectedIconColor = Brand.gray,
                    unselectedTextColor = Brand.gray,
                    indicatorColor = Brand.lavender
                )
            )
        }
    }
}
