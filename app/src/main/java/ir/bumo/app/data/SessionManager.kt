package ir.bumo.app.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.map
import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val Context.sessionStore by preferencesDataStore("bumo_session")
@Singleton class SessionManager @Inject constructor(@ApplicationContext private val context:Context){
    @Volatile var accessToken:String?=null; @Volatile var refreshToken:String?=null; @Volatile var baseUrl:String= ""; @Volatile var username:String=""
    suspend fun load(){val p=context.sessionStore.data.first();accessToken=p[ACCESS];refreshToken=p[REFRESH];baseUrl=p[SERVER].orEmpty();username=accessToken?.let(::tokenUsername).orEmpty()}
    suspend fun setServer(url:String){baseUrl=url.trimEnd('/');context.sessionStore.edit{it[SERVER]=baseUrl}}
    suspend fun setTokens(a:String,r:String){accessToken=a;refreshToken=r;username=tokenUsername(a);context.sessionStore.edit{it[ACCESS]=a;it[REFRESH]=r}}
    suspend fun clear(){accessToken=null;refreshToken=null;username="";context.sessionStore.edit{it.remove(ACCESS);it.remove(REFRESH)}}
    private fun tokenUsername(token:String):String=try{val parts=token.split(".");val decoded=Base64.decode(parts[1],Base64.URL_SAFE or Base64.NO_WRAP);Json.parseToJsonElement(String(decoded)).jsonObject["username"]?.jsonPrimitive?.content.orEmpty()}catch(_:Exception){""}
    fun snapshotServer():String=baseUrl
    companion object{private val ACCESS=stringPreferencesKey("access_token");private val REFRESH=stringPreferencesKey("refresh_token");private val SERVER=stringPreferencesKey("server_url")}
}
