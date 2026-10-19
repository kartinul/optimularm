package io.github.kartinul

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import io.github.kartinul.data.latestSleepFlow
import io.github.kartinul.data.pairStatusFlow
import io.github.kartinul.data.wakeSettingsFlow
import io.github.kartinul.ui.ClockScreen
import io.github.kartinul.ui.SettingsScreen
import io.github.kartinul.ui.theme.OptimularmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startSyncWorker(this)
        setContent {
            OptimularmTheme { App() }
        }
    }
}

private val tabs = listOf("Clock" to R.drawable.ic_alarm, "Settings" to R.drawable.ic_settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val settings by remember { wakeSettingsFlow() }.collectAsState(initial = null)
    val pairStatus by remember { pairStatusFlow() }.collectAsState(initial = null)
    val latestSleep by remember { latestSleepFlow() }.collectAsState(initial = null)
    var ringsAt by remember { mutableStateOf(ringAt(context)) }

    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val needsPermission = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // any settings change or new sleep event (incl. late sleep) from either device re-runs sync
    LaunchedEffect(settings, latestSleep) {
        if (settings == null) return@LaunchedEffect
        runCatching { sync(context) }.onFailure { Log.e(TAG, "sync failed", it) }
        ringsAt = ringAt(context)
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(tabs[tab].first) }) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(painterResource(icon), contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val current = settings ?: return@Scaffold
        val content = Modifier.padding(innerPadding)
        if (tab == 0) ClockScreen(current, ringsAt, content)
        else SettingsScreen(pairStatus, content)
    }
}
