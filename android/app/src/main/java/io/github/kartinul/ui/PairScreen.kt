package io.github.kartinul.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import io.github.kartinul.R
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.github.kartinul.data.joinPairing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun scanAndPair(context: Context, scope: CoroutineScope, onPairChanged: () -> Unit) =
    GmsBarcodeScanning.getClient(context).startScan().addOnSuccessListener { code ->
        scope.launch {
            val joined = runCatching { joinPairing(code.rawValue.orEmpty()) }.getOrDefault(false)
            if (!joined) Toast.makeText(context, "Not an optimularm code", Toast.LENGTH_SHORT).show()
            onPairChanged()
        }
    }

private const val MAC_APP_URL = "https://github.com/kartinul/optimularm/releases/latest"

@Composable
fun PairScreen(onPairChanged: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ic_alarm), null, Modifier.size(72.dp), MaterialTheme.colorScheme.primary)
        Text("Pair your Mac", style = MaterialTheme.typography.headlineLarge)
        Text(
            "optimularm wakes you at the right point in your sleep cycle. It needs the Mac app to know when you fell asleep.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text("1. Install optimularm on your Mac", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, MAC_APP_URL.toUri())) }) {
            Text("Download from GitHub")
        }
        Spacer(Modifier.height(8.dp))
        Text("2. Scan the QR code it shows", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scanAndPair(context, scope, onPairChanged) }) { Text("Scan QR code") }
    }
}
