package ir.bumo.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import ir.bumo.app.data.local.LocalDb
import ir.bumo.app.data.local.PinEntity
import ir.bumo.app.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class BumoRepository @Inject constructor(private val apiFactory:ApiFactory,private val session:SessionManager,private val db:LocalDb){val api get()=apiFactory.api
    suspend fun login(id:String,p:String){val t=api.login(LoginRequest(id,p));session.setTokens(t.accessToken,t.refreshToken)}
    suspend fun register(u:String,email:String,p:String){val t=api.register(RegisterRequest(u,email.ifBlank{null},null,p,u));session.setTokens(t.accessToken,t.refreshToken)}
    suspend fun requestOtp(id:String)=api.requestOtp(OtpRequest(id))
    suspend fun verifyOtp(id:String,code:String){val t=api.verifyOtp(OtpVerifyRequest(id,code));session.setTokens(t.accessToken,t.refreshToken)}
    suspend fun pin(id:Long)=api.pin(id)
    suspend fun feed(kind:String,cursor:String="")=when(kind){"following"->api.following(cursor);"trending"->api.trending(cursor);else->api.home(cursor)}
    suspend fun cache(page:FeedPage){db.pinDao().upsertAll(page.items.map{PinEntity(it.id,it.title,it.description,it.image_url,it.width,it.height,it.author?.display_name.orEmpty(),it.created_at)})}
    fun cachedPins():Flow<List<PinEntity>> = db.pinDao().observe()
    suspend fun search(q:String,cursor:String="")=api.search(q,"pins",cursor)
    suspend fun searchUsers(q:String,cursor:String="")=api.searchUsers(q,"users",cursor)
    suspend fun searchBoards(q:String,cursor:String="")=api.searchBoards(q,"boards",cursor)
    suspend fun related(id:Long)=api.related(id)
    suspend fun save(id:Long,board:Long?)=api.save(id,SaveRequest(board))
    suspend fun unsave(id:Long)=api.unsave(id)
    suspend fun like(id:Long)=api.like(id)
    suspend fun unlike(id:Long)=api.unlike(id)
    suspend fun comment(id:Long,body:String)=api.addComment(id,CommentRequest(body))
    suspend fun comments(id:Long)=api.comments(id)
    suspend fun notifications()=api.notifications()
    suspend fun readNotifications(){api.markNotificationsRead(mapOf("ids" to emptyList()))}
    suspend fun deleteMe(){api.deleteMe();session.clear()}
    suspend fun logout(){session.refreshToken?.let{runCatching{api.logout(mapOf("refresh_token" to it))}};session.clear()}
    suspend fun profile(u:String)=api.user(u)
    suspend fun boards(u:String)=api.userBoards(u)
    suspend fun userPins(u:String)=api.userPins(u)
    suspend fun savedPins(u:String)=api.savedPins(u)
    suspend fun patchMe(name:String,bio:String)=api.patchMe(mapOf("display_name" to name,"bio" to bio))
    suspend fun follow(id:Long)=api.follow(id)
    suspend fun unfollow(id:Long)=api.unfollow(id)
    suspend fun createBoard(title:String,desc:String)=api.createBoard(CreateBoard(title,desc))
    suspend fun board(id:Long)=api.board(id)
    suspend fun patchBoard(id:Long,title:String,desc:String)=api.patchBoard(id,CreateBoard(title,desc))
    suspend fun deleteBoard(id:Long)=api.deleteBoard(id)
    suspend fun upload(context:Context,uri:Uri,title:String,desc:String,tags:String,boardId:String){withContext(Dispatchers.IO){val resolver=context.contentResolver;val bytes=resolver.openInputStream(uri)?.use{it.readBytes()}?:error("image unreadable");val req=bytes.toRequestBody("image/jpeg".toMediaType());val part=MultipartBody.Part.createFormData("file","bumo.jpg",req);api.createPin(part,title.toRequestBody(),desc.toRequestBody(),"".toRequestBody(),tags.toRequestBody(),boardId.toRequestBody())}}
}
