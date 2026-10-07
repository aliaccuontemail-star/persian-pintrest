package ir.bumo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ir.bumo.app.data.SessionManager
import ir.bumo.app.ui.AppNavGraph
import ir.bumo.app.ui.AuthScreen
import ir.bumo.app.ui.ServerSetupScreen
import ir.bumo.app.ui.theme.BumoTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var boot by remember { mutableStateOf(false) }
            var serverReady by remember { mutableStateOf(false) }
            var loggedIn by remember { mutableStateOf(false) }
            var dark by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                session.load()
                dark = ThemeStore(this@MainActivity).read()
                serverReady = session.baseUrl.isNotBlank()
                loggedIn = session.accessToken != null
                boot = true
            }

            BumoTheme(darkTheme = dark) {
                when {
                    !boot -> CircularProgressIndicator()
                    !serverReady -> ServerSetupScreen(session) { serverReady = true }
                    !loggedIn -> AuthScreen { loggedIn = true }
                    else -> {
                        val nav = rememberNavController()
                        val deepLink = intent?.data
                        LaunchedEffect(deepLink) {
                            when (deepLink?.scheme) {
                                "bumo" -> when (deepLink.host) {
                                    "pin" -> deepLink.pathSegments.firstOrNull()?.toLongOrNull()?.let { nav.navigate("pin/$it") }
                                    "create" -> nav.navigate("create")
                                    "search" -> nav.navigate("search")
                                }
                            }
                        }
                        AppNavGraph(
                            nav = nav,
                            session = session,
                            dark = dark,
                            onDarkChange = {
                                dark = it
                                scope.launch { ThemeStore(this@MainActivity).set(it) }
                            },
                            onLogout = {
                                scope.launch {
                                    session.logout()
                                    loggedIn = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
