package com.marcogn.kartlog.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Lingua dell'app, con le API per-app di AndroidX (funzionano da API 26 grazie al backport, non solo
 * da API 33). [tag] null = segue la lingua di sistema. Stesso schema di ThePatientGamerHelper.
 */
enum class AppLanguage(val tag: String?) {
    SISTEMA(null),
    ITALIANO("it"),
    ENGLISH("en"),
}

fun currentAppLanguage(): AppLanguage {
    val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotEmpty() }
    return AppLanguage.entries.firstOrNull { it.tag == currentTag } ?: AppLanguage.SISTEMA
}

/**
 * autoStoreLocales (manifest) salva la scelta da solo. Nessun `recreate()` a mano:
 * `setApplicationLocales()` ricrea già l'activity, ma **solo se estende `AppCompatActivity`**
 * (vedi `MainActivity`): con un semplice `ComponentActivity` il cambio verrebbe ignorato.
 */
fun applyAppLanguage(language: AppLanguage) {
    val locales = language.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
    AppCompatDelegate.setApplicationLocales(locales)
}
