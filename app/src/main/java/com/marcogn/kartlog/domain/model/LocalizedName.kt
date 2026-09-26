package com.marcogn.kartlog.domain.model

import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Lingua effettiva dell'app, osservabile. La aggiorna `MainActivity` a ogni (ri)creazione con la
 * lingua della sua configurazione, che tiene conto della scelta in Impostazioni: sotto Android 13
 * `setApplicationLocales()` (backport AppCompat) cambia la configurazione dell'activity ma NON
 * `Locale.getDefault()`, e in ogni caso i ViewModel sopravvivono alla ricreazione con i nomi già
 * calcolati — per questo i loro Flow passano da [relocalizing].
 */
object AppLocale {
    // Solo il valore iniziale: MainActivity.onCreate lo aggiorna a ogni (ri)creazione, proprio per
    // seguire i cambi di lingua a app avviata.
    @Suppress("ConstantLocale")
    private val mutableCurrent = MutableStateFlow<Locale>(Locale.getDefault())
    val current: StateFlow<Locale> = mutableCurrent.asStateFlow()

    fun update(locale: Locale) {
        mutableCurrent.value = locale
    }
}

/** Ri-emette l'ultimo valore quando cambia la lingua dell'app, così chi localizza i nomi li ricalcola. */
fun <T> Flow<T>.relocalizing(): Flow<T> = combine(this, AppLocale.current) { value, _ -> value }

/**
 * Nome ufficiale di un elemento di gioco (personaggio, outfit, corso, bioma, evento) nella lingua
 * dell'app, se esiste; altrimenti quello inglese di [name]. [nameIt] arriva da `seedgen/i18n.py` o
 * `seedgen/it_wiki.py` (i cibi da una traduzione manuale dichiarata non ufficiale): `null` quando
 * non è noto.
 */
fun localizedName(name: String, nameIt: String?, locale: Locale = AppLocale.current.value): String =
    if (locale.language == "it" && !nameIt.isNullOrBlank()) nameIt else name
