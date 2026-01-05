package com.example.touristassistant.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

/**
 * Утилиты для анимаций переходов между экранами.
 * Предоставляет предустановленные анимации для навигации.
 */
object Transitions {

    /**
     * Анимация горизонтального появления с правой стороны.
     *
     * @return анимация для входа на экран с движением из-за правой границы
     */
    @ExperimentalAnimationApi
    fun slideInHorizontally() = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(300)
    )

    /**
     * Анимация горизонтального исчезновения в левую сторону.
     *
     * @return анимация для выхода с экрана с движением за левую границу
     */
    @ExperimentalAnimationApi
    fun slideOutHorizontally() = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(300)
    )

    /**
     * Анимация плавного появления с увеличением прозрачности.
     *
     * @return анимация для входа на экран с эффектом fade-in
     */
    @ExperimentalAnimationApi
    fun fadeIn() = fadeIn(animationSpec = tween(300))

    /**
     * Анимация плавного исчезновения с уменьшением прозрачности.
     *
     * @return анимация для выхода с экрана с эффектом fade-out
     */
    @ExperimentalAnimationApi
    fun fadeOut() = fadeOut(animationSpec = tween(300))
}