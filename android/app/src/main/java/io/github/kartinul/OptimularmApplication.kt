package io.github.kartinul

import android.app.Application
import android.util.Log
import io.github.kartinul.data.initPairing
import io.github.kartinul.data.signIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OptimularmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initPairing(this)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { signIn() }.onFailure { Log.e(TAG, "sign-in failed", it) }
        }
    }
}
