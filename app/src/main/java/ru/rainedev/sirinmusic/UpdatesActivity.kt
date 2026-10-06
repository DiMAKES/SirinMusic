package ru.rainedev.sirinmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.rainedev.sirinmusic.ui.theme.SirinMusicTheme
import ru.rainedev.sirinmusic.update.UpdatesSection

class UpdatesActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        val app = application as SirinApp
        if (savedInstanceState == null && intent.getBooleanExtra("check_updates", false)) {
            lifecycleScope.launch { app.updates.check(notify = true) }
        }
        setContent {
            val appearance by app.settings.appearance.collectAsStateWithLifecycle()
            SirinMusicTheme(appearance) {
                Scaffold(topBar = { TopAppBar(title = { Text("Обновления") }, navigationIcon = {
                    IconButton(onClick = ::finish) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
                }) }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                        Column(Modifier.widthIn(max = 760.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                            UpdatesSection(app)
                        }
                    }
                }
            }
        }
    }
}
