// AGP 9 compila Kotlin da sé (Kotlin integrato): niente plugin org.jetbrains.kotlin.android.
// La versione di Kotlin arriva dai plugin compose/serialization qui sotto (docs/AGGIORNAMENTO_DIPENDENZE.md).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
}
