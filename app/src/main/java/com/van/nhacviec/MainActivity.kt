package com.van.nhacviec

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.van.nhacviec.data.Quadrant
import com.van.nhacviec.data.Store
import com.van.nhacviec.notify.Notifier
import com.van.nhacviec.timer.TimerController
import com.van.nhacviec.ui.CountdownScreen
import com.van.nhacviec.ui.EditTaskScreen
import com.van.nhacviec.ui.HomeScreen
import com.van.nhacviec.ui.NhacViecTheme
import com.van.nhacviec.ui.QuadrantScreen
import com.van.nhacviec.ui.SettingsScreen
import com.van.nhacviec.ui.TimerScreen
import com.van.nhacviec.update.Updater
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class InQuadrant(val q: Quadrant) : Screen
    data class Edit(val taskId: String?, val q: Quadrant) : Screen
    data object Timer : Screen
    data object Countdowns : Screen
    data object SettingsTab : Screen
}

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_START_TASK = "start_task"
        const val EXTRA_OPEN_QUADRANT = "open"
    }

    private var screen by mutableStateOf<Screen>(Screen.Home)
    private val stack = ArrayDeque<Screen>()

    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        handleIntent(intent)
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(this)) {
            askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        TimerController.restore(this)
        lifecycleScope.launch { Updater.check() }
        setContent { NhacViecTheme { Root() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        i ?: return
        i.getStringExtra(EXTRA_START_TASK)?.let { id ->
            i.removeExtra(EXTRA_START_TASK)
            if (Store.value.task(id) != null) {
                TimerController.start(this, id)
                go(Screen.Timer, reset = true)
            }
            return
        }
        i.getStringExtra(EXTRA_OPEN_QUADRANT)?.let { key ->
            i.removeExtra(EXTRA_OPEN_QUADRANT)
            if (key == "countdown") go(Screen.Countdowns, reset = true)
            else runCatching { Quadrant.valueOf(key) }.getOrNull()?.let { go(Screen.InQuadrant(it), reset = true) }
        }
    }

    fun go(s: Screen, reset: Boolean = false) {
        if (reset) stack.clear() else stack.addLast(screen)
        screen = s
    }

    fun back() {
        screen = stack.removeLastOrNull() ?: Screen.Home
    }

    fun tab(s: Screen) {
        stack.clear()
        screen = s
    }

    @Composable
    private fun Root() {
        val data by Store.data.collectAsState()
        LaunchedEffect(Unit) { Updater.check() }
        BackHandler(enabled = screen != Screen.Home) { back() }
        val nav: (Screen) -> Unit = { go(it) }
        val tabTo: (Screen) -> Unit = { tab(it) }
        val goBack: () -> Unit = { back() }
        when (val s = screen) {
            Screen.Home -> HomeScreen(data, nav = nav, tab = tabTo)
            is Screen.InQuadrant -> QuadrantScreen(data, s.q, nav = nav, back = goBack)
            is Screen.Edit -> EditTaskScreen(data.task(s.taskId), s.q, back = goBack)
            Screen.Timer -> TimerScreen(data, back = goBack)
            Screen.Countdowns -> CountdownScreen(data, nav = nav, tab = tabTo)
            Screen.SettingsTab -> SettingsScreen(data, tab = tabTo)
        }
    }
}
