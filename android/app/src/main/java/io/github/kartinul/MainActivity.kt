package io.github.kartinul

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseException
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.flow.flowOf
import androidx.core.content.ContextCompat
import io.github.kartinul.data.latestSleepFlow
import io.github.kartinul.data.pairId
import io.github.kartinul.data.wakeSettingsFlow
import io.github.kartinul.ui.ClockScreen
import io.github.kartinul.ui.SettingsScreen
import io.github.kartinul.ui.checkPermissions
import androidx.lifecycle.compose.LifecycleResumeEffect
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

@Composable
fun App() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pairedId by remember { mutableStateOf(pairId) }
    val settings by remember(pairedId) { pairedId?.let(::wakeSettingsFlow) ?: flowOf(null) }.collectAsState(null)
    val latestSleep by remember(pairedId) { pairedId?.let(::latestSleepFlow) ?: flowOf(null) }.collectAsState(null)
    var ringsAt by remember { mutableStateOf(ringAt(context)) }
    var permissions by remember { mutableStateOf(checkPermissions(context)) }
    LifecycleResumeEffect(Unit) {
        permissions = checkPermissions(context)
        onPauseOrDispose {}
    }

    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val needsPermission = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // any settings change or new sleep event (incl. late sleep) from either device re-runs sync
    LaunchedEffect(pairedId, settings, latestSleep) {
        try {
            sync(context)
        } catch (e: FirebaseException) {
            Log.e(TAG, "sync failed", e)
        }
        ringsAt = ringAt(context)
    }

    Scaffold(
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
        val content = Modifier.padding(innerPadding)
        val current = settings
        when {
            tab == 1 -> SettingsScreen(pairedId, permissions, onPairChanged = { pairedId = pairId }, content)
            pairedId == null -> Box(content.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pair with your Mac in Settings")
            }
            current != null ->
                ClockScreen(current, ringsAt, permissions.count { !it.granted }, content)
        }
    }
}
