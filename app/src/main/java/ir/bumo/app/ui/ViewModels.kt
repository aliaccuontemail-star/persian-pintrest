package ir.bumo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.Pager
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.bumo.app.data.BumoRepository
import ir.bumo.app.data.SessionManager
import ir.bumo.app.data.remote.Board
import ir.bumo.app.data.remote.Comment
import ir.bumo.app.data.remote.Notification
import ir.bumo.app.data.remote.Pin
import ir.bumo.app.data.remote.SearchBoard
import ir.bumo.app.data.remote.SearchUser
import ir.bumo.app.data.remote.UserProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cachedIn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class FeedSource(
    private val repo: BumoRepository,
    private val kind: String
) : PagingSource<String, Pin>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Pin> = try {
        val page = repo.feed(kind, params.key.orEmpty())
        repo.cache(page)
        LoadResult.Page(
            data = page.items,
            prevKey = null,
            nextKey = page.next_cursor.ifBlank { null }
        )
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    override fun getRefreshKey(state: PagingState<String, Pin>): String? = null
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: BumoRepository
) : ViewModel() {
    private val kind = MutableStateFlow("home")
    val items = kind
        .flatMapLatest { selected ->
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                FeedSource(repo, selected)
            }.flow
        }
        .cachedIn(viewModelScope)
    val cached = repo.cachedPins()

    fun setKind(value: String) {
        kind.value = value
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: BumoRepository
) : ViewModel() {
    val query = MutableStateFlow("")
    val type = MutableStateFlow("pins")

    val results: Flow<PagingData<Pin>> = combine(query, type) { q, selectedType -> q to selectedType }
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { (q, selectedType) ->
            if (q.isBlank() || selectedType != "pins") {
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            } else {
                Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                    object : PagingSource<String, Pin>() {
                        override suspend fun load(params: LoadParams<String>): LoadResult<String, Pin> = try {
                            val page = repo.search(q, params.key.orEmpty())
                            LoadResult.Page(
                                data = page.items,
                                prevKey = null,
                                nextKey = page.next_cursor.ifBlank { null }
                            )
                        } catch (e: Exception) {
                            LoadResult.Error(e)
                        }

                        override fun getRefreshKey(state: PagingState<String, Pin>): String? = null
                    }
                }.flow
            }
        }
        .cachedIn(viewModelScope)

    val users = MutableStateFlow<List<SearchUser>>(emptyList())
    val boards = MutableStateFlow<List<SearchBoard>>(emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setType(value: String) {
        type.value = value
        searchSecondary(query.value, value)
    }

    fun searchSecondary(q: String, selectedType: String) {
        viewModelScope.launch {
            if (q.isBlank()) {
                users.value = emptyList()
                boards.value = emptyList()
                return@launch
            }
            if (selectedType == "users") {
                runCatching { repo.searchUsers(q) }
                    .onSuccess { users.value = it.items }
            } else if (selectedType == "boards") {
                runCatching { repo.searchBoards(q) }
                    .onSuccess { boards.value = it.items }
            }
        }
    }
}

@HiltViewModel
class PinViewModel @Inject constructor(
    private val repo: BumoRepository
) : ViewModel() {
    val pin = MutableStateFlow<Pin?>(null)
    val comments = MutableStateFlow<List<Comment>>(emptyList())
    val related = MutableStateFlow<List<Pin>>(emptyList())

    fun load(id: Long) {
        viewModelScope.launch {
            runCatching { repo.pin(id) }.onSuccess { pin.value = it }
            runCatching { repo.comments(id) }.onSuccess { comments.value = it.items }
            runCatching { repo.related(id) }.onSuccess { related.value = it.items }
        }
    }

    fun like() {
        val id = pin.value?.id ?: return
        viewModelScope.launch {
            val result = if (pin.value?.liked == true) repo.unlike(id) else repo.like(id)
            val old = pin.value ?: return@launch
            val newLiked = result.liked ?: !old.liked
            pin.value = old.copy(
                liked = newLiked,
                likes = (old.likes + if (newLiked && !old.liked) 1 else if (!newLiked && old.liked) -1 else 0).coerceAtLeast(0)
            )
        }
    }

    fun save(board: Long? = null) {
        val id = pin.value?.id ?: return
        viewModelScope.launch {
            repo.save(id, board)
            pin.value = pin.value?.copy(saved = true)
        }
    }

    fun unsave() {
        val id = pin.value?.id ?: return
        viewModelScope.launch {
            repo.unsave(id)
            pin.value = pin.value?.copy(saved = false)
        }
    }

    fun comment(text: String) {
        val id = pin.value?.id ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.comment(id, text)
            runCatching { repo.comments(id) }.onSuccess { comments.value = it.items }
        }
    }
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: BumoRepository
) : ViewModel() {
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow("")

    fun login(identifier: String, password: String, done: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            error.value = ""
            runCatching { repo.login(identifier, password) }
                .onSuccess { done() }
                .onFailure { error.value = it.message ?: "ورود ناموفق بود" }
            busy.value = false
        }
    }

    fun register(username: String, email: String, password: String, done: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            error.value = ""
            runCatching { repo.register(username, email, password) }
                .onSuccess { done() }
                .onFailure { error.value = it.message ?: "ثبت نام ناموفق بود" }
            busy.value = false
        }
    }

    fun otp(identifier: String, code: String, done: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            error.value = ""
            runCatching {
                if (code.isBlank()) {
                    val result = repo.requestOtp(identifier)
                    result.code?.takeIf { it.all(Char::isDigit) }
                        ?.let { error.value = "کد آزمایشی: $it" }
                } else {
                    repo.verifyOtp(identifier, code)
                }
            }.onSuccess {
                if (code.isNotBlank()) done()
            }.onFailure {
                error.value = it.message ?: "OTP نامعتبر"
            }
            busy.value = false
        }
    }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: BumoRepository
) : ViewModel() {
    val user = MutableStateFlow<UserProfile?>(null)
    val boards = MutableStateFlow<List<Board>>(emptyList())
    val pins = MutableStateFlow<List<Pin>>(emptyList())
    val saved = MutableStateFlow<List<Pin>>(emptyList())

    fun load(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.profile(name) }.onSuccess { user.value = it }
            runCatching { repo.boards(name) }.onSuccess { boards.value = it.items }
            runCatching { repo.userPins(name) }.onSuccess { pins.value = it.items }
            runCatching { repo.savedPins(name) }.onSuccess { saved.value = it.items }
        }
    }

    fun follow() {
        val current = user.value ?: return
        viewModelScope.launch {
            runCatching {
                if (current.following_me) repo.unfollow(current.id) else repo.follow(current.id)
            }.onSuccess {
                user.value = current.copy(
                    following_me = !current.following_me,
                    followers = (current.followers + if (current.following_me) -1 else 1).coerceAtLeast(0)
                )
            }
        }
    }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: BumoRepository
) : ViewModel() {
    val items = MutableStateFlow<List<Notification>>(emptyList())

    fun load() {
        viewModelScope.launch {
            runCatching { repo.notifications() }.onSuccess { items.value = it.items }
        }
    }

    fun markRead() {
        viewModelScope.launch { runCatching { repo.readNotifications() } }
    }
}

@HiltViewModel
class BoardsViewModel @Inject constructor(
    private val repo: BumoRepository,
    private val session: SessionManager
) : ViewModel() {
    val items = MutableStateFlow<List<Board>>(emptyList())
    val message = MutableStateFlow("")

    fun load() {
        val name = session.username
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.boards(name) }.onSuccess { items.value = it.items }
        }
    }

    fun create(title: String, description: String) {
        viewModelScope.launch {
            runCatching { repo.createBoard(title, description) }
                .onSuccess { message.value = "بورد ساخته شد"; load() }
                .onFailure { message.value = it.message.orEmpty() }
        }
    }

    fun update(id: Long, title: String, description: String) {
        viewModelScope.launch {
            runCatching { repo.patchBoard(id, title, description) }
                .onSuccess { message.value = "بورد ویرایش شد"; load() }
                .onFailure { message.value = it.message.orEmpty() }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            runCatching { repo.deleteBoard(id) }
                .onSuccess { message.value = "بورد حذف شد"; load() }
                .onFailure { message.value = it.message.orEmpty() }
        }
    }
}

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val repo: BumoRepository
) : ViewModel() {
    val item = MutableStateFlow<ir.bumo.app.data.remote.BoardDetail?>(null)

    fun load(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            runCatching { repo.board(id) }.onSuccess { item.value = it }
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: BumoRepository
) : ViewModel() {
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow("")

    fun save(name: String, bio: String) {
        viewModelScope.launch {
            busy.value = true
            runCatching { repo.patchMe(name, bio) }
                .onSuccess { message.value = "ذخیره شد" }
                .onFailure { message.value = it.message.orEmpty() }
            busy.value = false
        }
    }

    fun delete(done: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.deleteMe() }
                .onSuccess { done() }
                .onFailure { message.value = it.message.orEmpty() }
        }
    }
}
