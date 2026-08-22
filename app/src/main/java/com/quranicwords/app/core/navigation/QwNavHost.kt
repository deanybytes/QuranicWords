package com.quranicwords.app.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.feature.achievements.AchievementsScreen
import com.quranicwords.app.feature.intro.IntroScreen
import com.quranicwords.app.feature.lesson.LessonScreen
import com.quranicwords.app.feature.wordbrowse.WordBrowseScreen
import com.quranicwords.app.feature.lessonsummary.LessonSummaryScreen
import com.quranicwords.app.feature.onboarding.dailygoal.DailyGoalSelectScreen
import com.quranicwords.app.feature.onboarding.font.FontSelectScreen
import com.quranicwords.app.feature.onboarding.learningstyle.LearningStyleSelectScreen
import com.quranicwords.app.feature.roadmap.RoadmapScreen
import com.quranicwords.app.feature.onboarding.language.LanguageSelectScreen
import com.quranicwords.app.feature.splash.SplashScreen

private typealias EnterSpec = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias ExitSpec = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

/**
 * Per-route-category nav transitions - previously every destination here was an instant cut
 * (Navigation Compose defaults). Distinguishes the linear onboarding flow (horizontal slide), the
 * "immersive entry" into a lesson (scale+fade), a plain crossfade into the lesson summary (so it
 * doesn't compete with the summary's own celebration reveal), and a modal-feeling vertical slide
 * for lateral destinations (Settings). All collapse to instant cuts under reduced
 * motion, since [NavTransitions] is rebuilt from [rememberReducedMotion] on every recomposition.
 */
private class NavTransitions(reducedMotion: Boolean) {
    private val none: EnterSpec = { EnterTransition.None }
    private val noneExit: ExitSpec = { ExitTransition.None }

    val onboardingEnter: EnterSpec = if (reducedMotion) none else {
        { slideInHorizontally(animationSpec = tween(320)) { it / 4 } + fadeIn(animationSpec = tween(320)) }
    }
    val onboardingExit: ExitSpec = if (reducedMotion) noneExit else {
        { slideOutHorizontally(animationSpec = tween(320)) { -it / 4 } + fadeOut(animationSpec = tween(200)) }
    }
    val onboardingPopEnter: EnterSpec = if (reducedMotion) none else {
        { slideInHorizontally(animationSpec = tween(320)) { -it / 4 } + fadeIn(animationSpec = tween(320)) }
    }
    val onboardingPopExit: ExitSpec = if (reducedMotion) noneExit else {
        { slideOutHorizontally(animationSpec = tween(320)) { it / 4 } + fadeOut(animationSpec = tween(200)) }
    }

    val immersiveEnter: EnterSpec = if (reducedMotion) none else {
        { scaleIn(initialScale = 0.92f, animationSpec = tween(280)) + fadeIn(animationSpec = tween(280)) }
    }
    val immersiveExit: ExitSpec = if (reducedMotion) noneExit else {
        { scaleOut(targetScale = 1.05f, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)) }
    }
    val immersivePopEnter: EnterSpec = if (reducedMotion) none else {
        { scaleIn(initialScale = 1.05f, animationSpec = tween(280)) + fadeIn(animationSpec = tween(280)) }
    }
    val immersivePopExit: ExitSpec = if (reducedMotion) noneExit else {
        { scaleOut(targetScale = 0.92f, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)) }
    }

    val crossfadeEnter: EnterSpec = if (reducedMotion) none else {
        { fadeIn(animationSpec = tween(260)) }
    }
    val crossfadeExit: ExitSpec = if (reducedMotion) noneExit else {
        { fadeOut(animationSpec = tween(200)) }
    }

    val modalEnter: EnterSpec = if (reducedMotion) none else {
        { slideInVertically(animationSpec = tween(320)) { it / 3 } + fadeIn(animationSpec = tween(320)) }
    }
    val modalExit: ExitSpec = if (reducedMotion) noneExit else {
        { fadeOut(animationSpec = tween(200)) }
    }
    val modalPopExit: ExitSpec = if (reducedMotion) noneExit else {
        { slideOutVertically(animationSpec = tween(280)) { it / 3 } + fadeOut(animationSpec = tween(200)) }
    }
}

@Composable
fun QwNavHost(navController: NavHostController = rememberNavController()) {
    val t = NavTransitions(rememberReducedMotion())

    NavHost(navController = navController, startDestination = Route.Splash) {
        composable<Route.Splash>(
            exitTransition = t.crossfadeExit
        ) {
            SplashScreen(
                onNavigateTo = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.LanguageSelect>(
            enterTransition = t.onboardingEnter,
            exitTransition = t.onboardingExit,
            popEnterTransition = t.onboardingPopEnter,
            popExitTransition = t.onboardingPopExit
        ) {
            LanguageSelectScreen(
                onContinue = {
                    navController.navigate(Route.FontSelect) {
                        popUpTo(Route.LanguageSelect) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.FontSelect>(
            enterTransition = t.onboardingEnter,
            exitTransition = t.onboardingExit,
            popEnterTransition = t.onboardingPopEnter,
            popExitTransition = t.onboardingPopExit
        ) {
            FontSelectScreen(
                onContinue = {
                    navController.navigate(Route.LearningStyleSelect) {
                        popUpTo(Route.FontSelect) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.LearningStyleSelect>(
            enterTransition = t.onboardingEnter,
            exitTransition = t.onboardingExit,
            popEnterTransition = t.onboardingPopEnter,
            popExitTransition = t.onboardingPopExit
        ) {
            LearningStyleSelectScreen(
                onContinue = {
                    navController.navigate(Route.DailyGoalSelect) {
                        popUpTo(Route.LearningStyleSelect) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.DailyGoalSelect>(
            enterTransition = t.onboardingEnter,
            exitTransition = t.onboardingExit,
            popEnterTransition = t.onboardingPopEnter,
            popExitTransition = t.onboardingPopExit
        ) {
            DailyGoalSelectScreen(
                onContinue = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.DailyGoalSelect) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.Home>(
            enterTransition = t.onboardingEnter,
            popEnterTransition = t.immersivePopEnter
        ) {
            QwBottomNavShell(
                onOpenLesson = { lessonId -> navController.navigate(Route.Lesson(lessonId)) },
                onOpenReview = { navController.navigate(Route.Review) },
                onOpenChapterIntro = { chapterId -> navController.navigate(Route.ChapterIntro(chapterId)) },
                onOpenSectionIntro = { sectionId -> navController.navigate(Route.SectionIntro(sectionId)) },
                onOpenWordBrowse = { sectionId -> navController.navigate(Route.WordBrowse(sectionId)) },
                onOpenAchievements = { navController.navigate(Route.Achievements) },
                onOpenRoadmap = { navController.navigate(Route.Roadmap) }
            )
        }
        composable<Route.ChapterIntro>(
            enterTransition = t.immersiveEnter,
            exitTransition = t.immersiveExit,
            popEnterTransition = t.immersivePopEnter,
            popExitTransition = t.immersivePopExit
        ) {
            IntroScreen(onContinue = { navController.popBackStack() })
        }
        composable<Route.SectionIntro>(
            enterTransition = t.immersiveEnter,
            exitTransition = t.immersiveExit,
            popEnterTransition = t.immersivePopEnter,
            popExitTransition = t.immersivePopExit
        ) {
            IntroScreen(onContinue = { navController.popBackStack() })
        }
        composable<Route.WordBrowse>(
            enterTransition = t.immersiveEnter,
            exitTransition = t.immersiveExit,
            popEnterTransition = t.immersivePopEnter,
            popExitTransition = t.immersivePopExit
        ) {
            WordBrowseScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Lesson>(
            enterTransition = t.immersiveEnter,
            exitTransition = t.immersiveExit,
            popEnterTransition = t.immersivePopEnter,
            popExitTransition = t.immersivePopExit
        ) {
            LessonScreen(
                onExit = { navController.popBackStack() },
                onFinished = { summary ->
                    navController.navigate(summary) {
                        popUpTo(Route.Home)
                    }
                }
            )
        }
        composable<Route.Review>(
            enterTransition = t.immersiveEnter,
            exitTransition = t.immersiveExit,
            popEnterTransition = t.immersivePopEnter,
            popExitTransition = t.immersivePopExit
        ) {
            LessonScreen(
                onExit = { navController.popBackStack() },
                onFinished = { summary ->
                    navController.navigate(summary) {
                        popUpTo(Route.Home)
                    }
                }
            )
        }
        composable<Route.LessonSummary>(
            enterTransition = t.crossfadeEnter,
            exitTransition = t.crossfadeExit,
            popExitTransition = t.crossfadeExit
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<Route.LessonSummary>()
            LessonSummaryScreen(
                route = route,
                onContinue = {
                    val nextLessonId = route.nextLessonId
                    if (nextLessonId != null) {
                        // Flow straight into the next lesson instead of always dropping back to
                        // Home - same "pop everything above Home, then push" shape as opening a
                        // lesson normally from Home, so the back stack ends up identical either
                        // way (Home -> Lesson), not accumulating a LessonSummary -> Lesson chain.
                        navController.navigate(Route.Lesson(nextLessonId)) {
                            popUpTo(Route.Home)
                        }
                    } else {
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Home) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable<Route.Achievements>(
            enterTransition = t.modalEnter,
            exitTransition = t.modalExit,
            popExitTransition = t.modalPopExit
        ) {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Roadmap>(
            enterTransition = t.modalEnter,
            exitTransition = t.modalExit,
            popExitTransition = t.modalPopExit
        ) {
            RoadmapScreen(
                onBack = { navController.popBackStack() },
                onOpenLesson = { lessonId -> navController.navigate(Route.Lesson(lessonId)) }
            )
        }
    }
}
