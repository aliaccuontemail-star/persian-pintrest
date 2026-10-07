package ir.bumo.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

private val Context.bumoPrefs by preferencesDataStore("bumo_preferences")
class ThemeStore(private val context:Context){private val darkKey=booleanPreferencesKey("dark");suspend fun read():Boolean=context.bumoPrefs.data.first()[darkKey]?:false;suspend fun set(v:Boolean){context.bumoPrefs.edit{it[darkKey]=v}}}
