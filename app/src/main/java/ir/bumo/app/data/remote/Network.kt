package ir.bumo.app.data.remote

import ir.bumo.app.data.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class HostRewriteInterceptor @Inject constructor(private val session:SessionManager):Interceptor{override fun intercept(chain:Interceptor.Chain):Response{val req=chain.request();val base=session.snapshotServer();if(base.isBlank())return chain.proceed(req);return try{val u=URI(base);val host=u.host?:return chain.proceed(req);val new=req.url.newBuilder().scheme(u.scheme).host(host).apply{if(u.port>0)port(u.port)}.build();chain.proceed(req.newBuilder().url(new).build())}catch(_:Exception){chain.proceed(req)}}}

class AuthInterceptor @Inject constructor(private val session:SessionManager):Interceptor{override fun intercept(chain:Interceptor.Chain):Response{val t=session.accessToken?:return chain.proceed(chain.request());return chain.proceed(chain.request().newBuilder().header("Authorization","Bearer $t").build())}}

class RefreshAuthenticator @Inject constructor(private val session:SessionManager):Authenticator{
    private val json=Json{ignoreUnknownKeys=true;coerceInputValues=true}
    override fun authenticate(route:Route?,response:Response):Request?{
        if(responseCount(response)>=2)return null
        val refresh=session.refreshToken ?: return null
        val base=session.snapshotServer().trimEnd('/')
        if(base.isBlank())return null
        return try{
            val body=json.encodeToString(mapOf("refresh_token" to refresh)).toRequestBody("application/json".toMediaType())
            val request=Request.Builder().url("$base/v1/auth/refresh").post(body).build()
            OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).build().newCall(request).execute().use{r->
                if(!r.isSuccessful)return null
                val pair=json.decodeFromString<TokenPair>(r.body?.string() ?: return null)
                runBlocking{session.setTokens(pair.accessToken,pair.refreshToken)}
                response.request.newBuilder().header("Authorization","Bearer ${pair.accessToken}").build()
            }
        }catch(_:Exception){null}
    }
    private fun responseCount(response:Response):Int{var count=1;var prior=response.priorResponse;while(prior!=null){count++;prior=prior.priorResponse};return count}
}

@Singleton class ApiFactory @Inject constructor(private val session:SessionManager,private val host:HostRewriteInterceptor,private val auth:AuthInterceptor,private val refresher:RefreshAuthenticator){val api:ApiService by lazy{val json=Json{ignoreUnknownKeys=true;coerceInputValues=true};val http=OkHttpClient.Builder().addInterceptor(host).addInterceptor(auth).authenticator(refresher).connectTimeout(10,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).writeTimeout(60,TimeUnit.SECONDS).build();Retrofit.Builder().baseUrl("http://127.0.0.1/").client(http).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(ApiService::class.java)}}
