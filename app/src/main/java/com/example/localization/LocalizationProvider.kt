package com.example.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val LocalAppLanguage = compositionLocalOf { AppLanguage.DE }
val LocalAppStrings = compositionLocalOf { AppStrings.get(AppLanguage.DE) }

/**
 * Stellt allen untergeordneten Composables die aktuellen übersetzten Strings
 * sowie die korrekte Leserichtung (RTL für Arabisch, LTR für alle anderen) bereit.
 */
@Composable
fun ProvideAppLocalization(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val strings = AppStrings.get(language)
    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}
