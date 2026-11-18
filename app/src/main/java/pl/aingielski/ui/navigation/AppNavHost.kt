package pl.aingielski.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.ui.home.HomeScreen
import pl.aingielski.ui.quiz.QuizScreen
import pl.aingielski.ui.quiz.SetupScreen
import pl.aingielski.ui.quiz.SummaryScreen
import pl.aingielski.ui.settings.SettingsScreen
import pl.aingielski.ui.stats.StatsScreen
import pl.aingielski.ui.words.WordSetDetailScreen
import pl.aingielski.ui.words.WordSetsScreen

/** Routes from contracts/ui-navigation.md. */
object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val WORDS = "words"
    const val WORD_SET = "wordSet/{id}"
    const val SETUP = "setup?setName={setName}&count={count}&criterion={criterion}"
    const val QUIZ = "quiz/{sessionId}"
    const val SUMMARY = "summary/{sessionId}"
    const val STATS = "stats"

    const val ARG_ID = "id"
    const val ARG_SESSION_ID = "sessionId"
    const val ARG_SET_NAME = "setName"
    const val ARG_COUNT = "count"
    const val ARG_CRITERION = "criterion"

    fun wordSet(id: Long) = "wordSet/$id"
    fun quiz(sessionId: Long) = "quiz/$sessionId"
    fun summary(sessionId: Long) = "summary/$sessionId"

    fun setup(setName: String? = null, count: Int? = null, criterion: SelectionCriterion? = null): String {
        val params = buildList {
            if (setName != null) add("$ARG_SET_NAME=${Uri.encode(setName)}")
            if (count != null) add("$ARG_COUNT=$count")
            if (criterion != null) add("$ARG_CRITERION=${criterion.name}")
        }
        return if (params.isEmpty()) "setup" else "setup?" + params.joinToString("&")
    }
}

@Composable
fun AppNavHost(
    onOpenVoiceInstall: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenWords = { navController.navigate(Routes.WORDS) },
                onOpenListen = { navController.navigate(Routes.setup()) },
                onOpenStats = { navController.navigate(Routes.STATS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onDone = { navController.popBackStack(Routes.HOME, inclusive = false) }, onOpenVoiceInstall = onOpenVoiceInstall)
        }
        composable(Routes.WORDS) {
            WordSetsScreen(
                onBack = { navController.popBackStack() },
                onOpenSet = { navController.navigate(Routes.wordSet(it)) },
            )
        }
        composable(
            Routes.WORD_SET,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType }),
        ) {
            WordSetDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.SETUP,
            arguments = listOf(
                navArgument(Routes.ARG_SET_NAME) { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument(Routes.ARG_COUNT) { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument(Routes.ARG_CRITERION) { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) {
            SetupScreen(
                onBack = { navController.popBackStack() },
                onOpenWords = { navController.navigate(Routes.WORDS) },
                onOpenVoiceInstall = onOpenVoiceInstall,
                onQuizStarted = { sessionId ->
                    navController.navigate(Routes.quiz(sessionId)) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }
        composable(
            Routes.QUIZ,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.LongType }),
        ) {
            QuizScreen(
                onFinished = { sessionId ->
                    navController.navigate(Routes.summary(sessionId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onExitToHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(
            Routes.SUMMARY,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.LongType }),
        ) {
            SummaryScreen(
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onNewQuiz = { setName, count, criterion ->
                    navController.navigate(Routes.setup(setName, count, criterion)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
    }
}
