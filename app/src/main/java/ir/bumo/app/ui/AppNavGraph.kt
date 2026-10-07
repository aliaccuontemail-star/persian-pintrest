package ir.bumo.app.ui

import ir.bumo.app.BuildConfig

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import ir.bumo.app.data.BumoRepository
import ir.bumo.app.data.SessionManager
import ir.bumo.app.data.remote.*
import ir.bumo.app.ui.components.*
import ir.bumo.app.ui.theme.BumoRed
import kotlinx.coroutines.launch

sealed class Destination(val route:String,val label:String){data object Home:Destination("home","خانه");data object Search:Destination("search","جستجو");data object Create:Destination("create","ساخت");data object Notifications:Destination("notifications","اعلان");data object Profile:Destination("profile","پروفایل")}

@Composable
fun AppNavGraph(
    nav: NavHostController,
    session: SessionManager,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val tabs = listOf(
        Destination.Home,
        Destination.Search,
        Destination.Create,
        Destination.Notifications,
        Destination.Profile
    )

    val currentEntry by nav.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    fun goTo(destination: Destination) {
        nav.navigate(destination.route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        Row(Modifier.fillMaxSize()) {
            if (!compact) {
                NavigationRail(
                    header = {
                        Icon(Icons.Filled.GridView, contentDescription = "Bumo")
                    }
                ) {
                    tabs.forEach { destination ->
                        NavigationRailItem(
                            selected = currentRoute == destination.route,
                            onClick = { goTo(destination) },
                            icon = {
                                val icon = when (destination) {
                                    Destination.Home -> Icons.Filled.Home
                                    Destination.Search -> Icons.Filled.Search
                                    Destination.Create -> Icons.Filled.AddBox
                                    Destination.Notifications -> Icons.Filled.Notifications
                                    Destination.Profile -> Icons.Filled.Person
                                }
                                Icon(icon, contentDescription = null)
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (compact) {
                        NavigationBar {
                            tabs.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = { goTo(destination) },
                                    icon = {
                                        val icon = when (destination) {
                                            Destination.Home -> Icons.Filled.Home
                                            Destination.Search -> Icons.Filled.Search
                                            Destination.Create -> Icons.Filled.AddBox
                                            Destination.Notifications -> Icons.Filled.Notifications
                                            Destination.Profile -> Icons.Filled.Person
                                        }
                                        Icon(icon, contentDescription = null)
                                    },
                                    label = { Text(destination.label) }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = Destination.Home.route,
                    modifier = Modifier.padding(padding).fillMaxSize()
                ) {
            composable(Destination.Home.route) { HomeScreen(nav, session) }
            composable(Destination.Search.route) { SearchScreen(nav, session) }
            composable(Destination.Create.route) { CreatePinScreen(session) }
            composable(Destination.Notifications.route) { NotificationsScreen(nav) }
            composable(Destination.Profile.route) { ProfileScreen(nav, session) }
            composable("profile/{username}") { entry ->
                ProfileScreen(nav, session, entry.arguments?.getString("username").orEmpty())
            }
            composable("settings") { SettingsScreen(session, dark, onDarkChange, onLogout) }
            composable("boards") { BoardsScreen(nav) }
            composable("board/{id}") { entry ->
                BoardScreen(
                    nav,
                    entry.arguments?.getString("id")?.toLongOrNull() ?: 0L,
                    session
                )
            }
            composable("pin/{id}") { entry ->
                PinDetailScreen(
                    nav,
                    entry.arguments?.getString("id")?.toLongOrNull() ?: 0L,
                    session
                )
                }
            }
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun HomeScreen(nav:NavHostController,session:SessionManager,vm:HomeViewModel=hiltViewModel()){
    var tab by remember{mutableStateOf("home")};val items=vm.items.collectAsLazyPagingItems();val cached by vm.cached.collectAsState(initial=emptyList())
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){BumoBrand();Spacer(Modifier.weight(1f));IconButton(onClick={nav.navigate("settings"){launchSingleTop=true}}){Icon(Icons.Filled.Settings,null)}}
        TabRow(selectedTabIndex=listOf("home","following","trending").indexOf(tab)){listOf("home" to "برای تو","following" to "دنبال‌شده‌ها","trending" to "ترند").forEach{(k,t)->Tab(selected=tab==k,onClick={tab=k;vm.setKind(k)},text={Text(t)})}}
        BoxWithConstraints(Modifier.fillMaxSize()){
            val columns=when{maxWidth<600.dp->StaggeredGridCells.Fixed(2);maxWidth<900.dp->StaggeredGridCells.Fixed(3);maxWidth<1200.dp->StaggeredGridCells.Fixed(4);else->StaggeredGridCells.Fixed(5)}
            if(items.itemCount==0&&cached.isNotEmpty()){LazyVerticalStaggeredGrid(columns=columns,contentPadding=PaddingValues(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalItemSpacing=10.dp){items(cached){e->PinCard(Pin(e.id,e.title,e.description,"",e.width,e.height,e.createdAt,e.imageUrl,Author("",e.authorName)),session.baseUrl){nav.navigate("pin/${e.id}")}}}}
            else{LazyVerticalStaggeredGrid(columns=columns,contentPadding=PaddingValues(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalItemSpacing=10.dp){items(items.itemCount,key=items.itemKey{it.id},contentType=items.itemContentType{"pin"}){i->items[i]?.let{p->PinCard(p,session.baseUrl){nav.navigate("pin/${p.id}")}}}}}
        }
    }
}

@Composable fun SearchScreen(nav:NavHostController,session:SessionManager,vm:SearchViewModel=hiltViewModel()){
    val query by vm.query.collectAsState();val type by vm.type.collectAsState();val items=vm.results.collectAsLazyPagingItems();val users by vm.users.collectAsState();val boards by vm.boards.collectAsState()
    LaunchedEffect(query,type){if(type!="pins")vm.searchSecondary(query,type)}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        OutlinedTextField(value=query,onValueChange=vm::setQuery,modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("جستجو")},leadingIcon={Icon(Icons.Filled.Search,null)})
        TabRow(selectedTabIndex=listOf("pins","boards","users").indexOf(type)){listOf("pins" to "پین‌ها","boards" to "بوردها","users" to "کاربران").forEach{(k,t)->Tab(selected=type==k,onClick={vm.setType(k)},text={Text(t)})}}
        Spacer(Modifier.height(10.dp))
        when(type){
            "pins"->BoxWithConstraints(Modifier.fillMaxSize()){val columns=when{maxWidth<600.dp->StaggeredGridCells.Fixed(2);maxWidth<900.dp->StaggeredGridCells.Fixed(3);else->StaggeredGridCells.Fixed(4)};LazyVerticalStaggeredGrid(columns=columns,horizontalArrangement=Arrangement.spacedBy(10.dp),verticalItemSpacing=10.dp,contentPadding=PaddingValues(bottom=16.dp)){items(items.itemCount,key=items.itemKey{it.id}){i->items[i]?.let{p->PinCard(p,session.baseUrl){nav.navigate("pin/${p.id}")}}}}}
            "users"->LazyColumn{items(users){u->ListItem(headlineContent={Text(u.display_name)},supportingContent={Text("@${u.username}")},leadingContent={Icon(Icons.Filled.AccountCircle,null)},modifier=Modifier.clickable{nav.navigate("profile/${u.username}")})}}
            else->LazyColumn{items(boards){b->ListItem(headlineContent={Text(b.title)},supportingContent={Text(b.description)},modifier=Modifier.clickable{nav.navigate("board/${b.id}")})}}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PinDetailScreen(nav:NavHostController,id:Long,session:SessionManager,vm:PinViewModel=hiltViewModel(),boardsVm:BoardsViewModel=hiltViewModel()){
    LaunchedEffect(id){vm.load(id)}
    val p by vm.pin.collectAsState(); val comments by vm.comments.collectAsState(); val related by vm.related.collectAsState(); val boards by boardsVm.items.collectAsState()
    var comment by remember{mutableStateOf("")}; var saveSheet by remember{mutableStateOf(false)}; LaunchedEffect(saveSheet){if(saveSheet)boardsVm.load()}
    LazyColumn(Modifier.fillMaxSize()){
        item{IconButton(onClick={nav.popBackStack()}){Icon(Icons.Filled.ArrowBack,null)}}
        p?.let{pin->
            item{AnimatedContent(pin.image_url,label="pin image"){AsyncImage(model=imageUrl(session.baseUrl,pin.image_url),contentDescription=pin.title,modifier=Modifier.fillMaxWidth().aspectRatio(pin.width.toFloat()/pin.height.coerceAtLeast(1).toFloat()),contentScale=ContentScale.Fit)}}
            item{Column(Modifier.padding(18.dp)){Text(pin.title,style=MaterialTheme.typography.headlineSmall);Text(pin.description,style=MaterialTheme.typography.bodyMedium);Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={saveSheet=true}){Icon(if(pin.saved)Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,null);Spacer(Modifier.width(5.dp));Text(if(pin.saved)"ذخیره شده" else "ذخیره")};OutlinedButton(onClick=vm::like){Icon(if(pin.liked)Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,null);Spacer(Modifier.width(5.dp));Text("${pin.likes}")}}}}
        }
        item{Text("دیدگاه‌ها",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(18.dp))}
        items(comments){c->ListItem(headlineContent={Text(c.user.display_name)},supportingContent={Text(c.body)},leadingContent={Icon(Icons.Filled.AccountCircle,null)})}
        item{Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(comment,{comment=it},Modifier.weight(1f),singleLine=true,label={Text("نظر شما")});IconButton(onClick={vm.comment(comment);comment=""}){Icon(Icons.Filled.Send,null)}}}
        item{Text("مرتبط",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(18.dp))}
        items(related.take(8)){rp->PinCard(rp,session.baseUrl){nav.navigate("pin/${rp.id}")}}
    }
    if(saveSheet){ModalBottomSheet(onDismissRequest={saveSheet=false}){Column(Modifier.fillMaxWidth().padding(20.dp)){Text("ذخیره در بورد",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));if(boards.isEmpty())Text("هنوز بوردی ساخته نشده است.",style=MaterialTheme.typography.bodyMedium);boards.forEach{b->ListItem(headlineContent={Text(b.title)},modifier=Modifier.fillMaxWidth().clickable{vm.save(b.id);saveSheet=false})};Spacer(Modifier.height(8.dp));OutlinedButton(onClick={saveSheet=false;nav.navigate("boards")}){Text("ساخت بورد جدید")}}}}
}

@Composable fun AuthScreen(vm:AuthViewModel=hiltViewModel(),onSuccess:()->Unit){var register by remember{mutableStateOf(false)};var id by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var code by remember{mutableStateOf("")};var username by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){BumoBrand();Spacer(Modifier.height(28.dp));if(register)OutlinedTextField(username,{username=it},Modifier.fillMaxWidth(),label={Text("نام کاربری")});if(register)Spacer(Modifier.height(8.dp));if(register)OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),label={Text("ایمیل")});Spacer(Modifier.height(8.dp));OutlinedTextField(id,{id=it},Modifier.fillMaxWidth(),label={Text(if(register)"شناسه ورود دیگر" else "نام کاربری / ایمیل")});Spacer(Modifier.height(8.dp));OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("رمز عبور")},visualTransformation=PasswordVisualTransformation());Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(enabled=vm.busy.collectAsState().value.not(),onClick={if(register)vm.register(username,email,password,onSuccess)else vm.login(id,password,onSuccess)}){Text(if(register)"ثبت نام" else "ورود")};OutlinedButton(onClick={ { vm.otp(id,code,onSuccess) } }){Text(if(code.isBlank())"درخواست OTP" else "تأیید OTP")}};if(code.isNotBlank()||vm.error.collectAsState().value.startsWith("کد آزمایشی")){Spacer(Modifier.height(8.dp));OutlinedTextField(code,{code=it},Modifier.fillMaxWidth(),label={Text("کد OTP")})};TextButton(onClick={register=!register}){Text(if(register)"حساب دارم" else "ساخت حساب")};vm.error.collectAsState().value.takeIf{it.isNotBlank()}?.let{Text(it,color=MaterialTheme.colorScheme.error)}}}

@Composable fun ServerSetupScreen(session:SessionManager,onDone:()->Unit){val scope=rememberCoroutineScope();var url by remember{mutableStateOf(session.baseUrl.ifBlank { BuildConfig.DEFAULT_BASE_URL.trimEnd('/') })};var error by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){BumoMark();Spacer(Modifier.height(16.dp));Text("اتصال به سرور",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(8.dp));Text("آدرس IP:PORT سرور Bumo را وارد کنید.");Spacer(Modifier.height(16.dp));OutlinedTextField(url,{url=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("http://192.168.1.10:8080")});Spacer(Modifier.height(12.dp));Button(onClick={if(url.startsWith("http://")||url.startsWith("https://")){val u=url.trimEnd('/');scope.launch{session.setServer(u);onDone()}}else error="آدرس باید با http:// یا https:// شروع شود"}){Text("اتصال")};if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CreatePinScreen(session:SessionManager,boardsVm:BoardsViewModel=hiltViewModel()){val context=LocalContext.current;var uri by remember{mutableStateOf<android.net.Uri?>(null)};var title by remember{mutableStateOf("")};var desc by remember{mutableStateOf("")};var tags by remember{mutableStateOf("")};var info by remember{mutableStateOf("")};var expanded by remember{mutableStateOf(false)};var selected by remember{mutableStateOf<Board?>(null)};val boards by boardsVm.items.collectAsState();LaunchedEffect(Unit){boardsVm.load()};val launcher=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri=it};Column(Modifier.fillMaxSize().padding(16.dp)){Text("ساخت پین",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));Button(onClick={launcher.launch("image/*")}){Text(if(uri==null)"انتخاب تصویر" else "تصویر انتخاب شد")};OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("عنوان")});OutlinedTextField(desc,{desc=it},Modifier.fillMaxWidth(),label={Text("توضیح")});OutlinedTextField(tags,{tags=it},Modifier.fillMaxWidth(),label={Text("تگ‌ها (JSON یا comma)")});Spacer(Modifier.height(8.dp));Box{OutlinedButton(onClick={expanded=true}){Text(selected?.title?:"انتخاب بورد")};DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){boards.forEach{b->DropdownMenuItem(text={Text(b.title)},onClick={selected=b;expanded=false})}}};Spacer(Modifier.height(12.dp));Button(enabled=uri!=null&&title.isNotBlank(),onClick={val data=Data.Builder().putString("uri",uri.toString()).putString("title",title).putString("description",desc).putString("tags",tags).putString("server",session.baseUrl).putString("board_id",selected?.id?.toString().orEmpty()).build();val req=OneTimeWorkRequestBuilder<ir.bumo.app.work.UploadWorker>().setInputData(data).build();WorkManager.getInstance(context).enqueue(req);info="آپلود در پس‌زمینه صف شد"}){Text("آپلود")};if(info.isNotBlank())Text(info,color=BumoRed)}}

@Composable fun NotificationsScreen(nav:NavHostController,vm:NotificationsViewModel=hiltViewModel()){LaunchedEffect(Unit){vm.load();vm.markRead()};val items by vm.items.collectAsState();LazyColumn(Modifier.fillMaxSize()){item{Text("اعلان‌ها",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(18.dp))};items(items){n->ListItem(headlineContent={Text(n.actor?.display_name.orEmpty())},supportingContent={Text(n.body)},leadingContent={Icon(Icons.Filled.Notifications,null)},modifier=Modifier.fillMaxWidth())}}}

@Composable fun ProfileScreen(nav:NavHostController,session:SessionManager,username:String=session.username,vm:ProfileViewModel=hiltViewModel()){
    LaunchedEffect(username){vm.load(username)}
    val u by vm.user.collectAsState(); val boards by vm.boards.collectAsState(); val pins by vm.pins.collectAsState(); val saved by vm.saved.collectAsState(); var tab by remember{mutableStateOf(0)}
    val isMe = username == session.username
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            u?.let { profile ->
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, null, Modifier.size(72.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(profile.display_name, style = MaterialTheme.typography.headlineSmall)
                        Text("@${profile.username}")
                        Text("${profile.followers} دنبال‌کننده · ${profile.pins} پین")
                    }
                    Spacer(Modifier.weight(1f))
                    if (!isMe) {
                        OutlinedButton(onClick = vm::follow) {
                            Text(if (profile.following_me) "دنبال می‌کنید" else "دنبال کردن")
                        }
                    }
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isMe) {
                    OutlinedButton(onClick = { nav.navigate("settings") }) { Text("تنظیمات") }
                }
            }
        }
        item {
            TabRow(selectedTabIndex = if (isMe) tab else tab.coerceAtMost(1)) {
                Tab(tab == 0, { tab = 0 }, text = { Text("پین‌ها") })
                Tab(tab == 1, { tab = 1 }, text = { Text("بوردها") })
                if (isMe) {
                    Tab(tab == 2, { tab = 2 }, text = { Text("ذخیره‌شده") })
                }
            }
        }
        when (tab.coerceAtMost(if (isMe) 2 else 1)) {
            0 -> items(pins) { pin ->
                PinCard(pin, session.baseUrl) { nav.navigate("pin/${pin.id}") }
            }
            1 -> items(boards) { board ->
                ListItem(
                    headlineContent = { Text(board.title) },
                    supportingContent = { Text(board.description) },
                    modifier = Modifier.clickable { nav.navigate("board/${board.id}") }
                )
            }
            2 -> items(saved) { pin ->
                PinCard(pin, session.baseUrl) { nav.navigate("pin/${pin.id}") }
            }
        }
    }
}


@Composable fun BoardsScreen(nav:NavHostController,vm:BoardsViewModel=hiltViewModel()){
    LaunchedEffect(Unit){vm.load()}
    var title by remember{mutableStateOf("")};var desc by remember{mutableStateOf("")};var editing by remember{mutableStateOf<Board?>(null)};var editTitle by remember{mutableStateOf("")};var editDesc by remember{mutableStateOf("")};val items by vm.items.collectAsState();val message by vm.message.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("بوردهای من",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(10.dp));OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("نام بورد")});OutlinedTextField(desc,{desc=it},Modifier.fillMaxWidth(),label={Text("توضیح")});Button(onClick={if(title.isNotBlank()){vm.create(title,desc);title="";desc=""}}){Text("ساخت بورد")};if(message.isNotBlank())Text(message,color=BumoRed);Spacer(Modifier.height(12.dp));
        LazyColumn{items(items){b->ListItem(headlineContent={Text(b.title)},supportingContent={Text(b.description)},modifier=Modifier.clickable{nav.navigate("board/${b.id}")},trailingContent={Row{IconButton(onClick={editing=b;editTitle=b.title;editDesc=b.description}){Icon(Icons.Filled.Edit,null)};IconButton(onClick={vm.delete(b.id)}){Icon(Icons.Filled.Delete,null)}}})}}
    }
    editing?.let{b->AlertDialog(onDismissRequest={editing=null},title={Text("ویرایش بورد")},text={Column{OutlinedTextField(editTitle,{editTitle=it},label={Text("نام")});OutlinedTextField(editDesc,{editDesc=it},label={Text("توضیح")})}},confirmButton={TextButton(onClick={if(editTitle.isNotBlank()){vm.update(b.id,editTitle,editDesc);editing=null}}){Text("ذخیره")}},dismissButton={TextButton(onClick={editing=null}){Text("انصراف")}})}
}

@Composable fun BoardScreen(nav:NavHostController,id:Long,session:SessionManager,vm:BoardDetailViewModel=hiltViewModel()){LaunchedEffect(id){vm.load(id)};val board by vm.item.collectAsState();Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={nav.popBackStack()}){Icon(Icons.Filled.ArrowBack,null)};board?.let{Text(it.title,style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.weight(1f));Text("${it.pins.size} پین")}};board?.description?.takeIf{it.isNotBlank()}?.let{Text(it,modifier=Modifier.padding(horizontal=18.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)};BoxWithConstraints(Modifier.fillMaxSize()){val columns=when{maxWidth<600.dp->StaggeredGridCells.Fixed(2);maxWidth<900.dp->StaggeredGridCells.Fixed(3);else->StaggeredGridCells.Fixed(4)};LazyVerticalStaggeredGrid(columns=columns,contentPadding=PaddingValues(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalItemSpacing=10.dp){items(board?.pins.orEmpty(),key={it.id}){pin->PinCard(pin,session.baseUrl){nav.navigate("pin/${pin.id}")}}}}}}

@Composable
fun SettingsScreen(
    session: SessionManager,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val msg by vm.message.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("نام نمایشی") }
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("بیو") }
        )
        Button(onClick = { vm.save(name, bio) }) {
            Text("ذخیره پروفایل")
        }
        ListItem(
            headlineContent = { Text("تم تاریک") },
            trailingContent = { Switch(checked = dark, onCheckedChange = onDarkChange) }
        )
        ListItem(
            headlineContent = { Text("سرور") },
            supportingContent = { Text(session.baseUrl) }
        )
        if (msg.isNotBlank()) {
            Text(msg, color = BumoRed)
        }
        ListItem(
            headlineContent = { Text("خروج") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { onLogout() }
        )
        ListItem(
            headlineContent = {
                Text("حذف حساب", color = MaterialTheme.colorScheme.error)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { confirmDelete = true }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف حساب؟") },
            text = { Text("تمام محتوای حساب شما حذف می‌شود و این عمل قابل بازگشت نیست.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        vm.delete(onLogout)
                    }
                ) {
                    Text("حذف حساب", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
