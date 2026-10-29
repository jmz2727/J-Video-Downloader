package com.jameel.jvideodownloader.presentation

import android.Manifest
import android.content.*
import android.content.ClipboardManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jameel.jvideodownloader.R
import com.jameel.jvideodownloader.data.Preferences
import com.jameel.jvideodownloader.domain.DownloadItem
import com.jameel.jvideodownloader.ui.theme.JTheme
import java.util.Locale

@Composable
fun AppScreen(vm: MainViewModel) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val rows by vm.history.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(vm.sharedVersion) { if(vm.sharedVersion > 0) page = 0 }
    val snack = remember { SnackbarHostState() }
    val context = LocalContext.current
    val message = vm.message
    val translated = message?.let { errorText(it) }
    LaunchedEffect(message) { if (translated != null) { snack.showSnackbar(translated); vm.dismissMessage() } }
    JTheme(prefs.theme) {
        Scaffold(snackbarHost = { SnackbarHost(snack) }, bottomBar = {
            NavigationBar {
                val labels = listOf(R.string.home, R.string.downloads, R.string.history, R.string.settings)
                val icons = listOf(Icons.Default.Home, Icons.AutoMirrored.Filled.List, Icons.Default.DateRange, Icons.Default.Settings)
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(selected = page == index, onClick = { page = index }, icon = { Icon(icons[index], null) }, label = { Text(stringResource(label), maxLines = 1) })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_brand), null, tint = Color.Unspecified, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("J Video Downloader", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.tagline), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Crossfade(targetState = page, label = "navigation") { destination ->
                    when(destination) {
                        0 -> Home(vm, rows.take(3), onDownloads = { page = 1 })
                        1 -> DownloadList(vm, rows, false)
                        2 -> DownloadList(vm, rows, true)
                        else -> SettingsScreen(vm, prefs)
                    }
                }
            }
        }
    }
}
@Composable private fun Home(vm: MainViewModel, recent: List<DownloadItem>, onDownloads: () -> Unit) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val preferences by vm.preferences.collectAsStateWithLifecycle()
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.download() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF25264C), Color(0xFF151B2B))), MaterialTheme.shapes.extraLarge).padding(22.dp)) {
                Text(stringResource(R.string.hero), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.hero_body), color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            OutlinedTextField(vm.input, vm::input, label = { Text(stringResource(R.string.url)) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                supportingText = { Text(stringResource(R.string.direct_hint)) })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let(vm::input) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.paste)) }
                Button(onClick = { focus.clearFocus(); keyboard?.hide(); vm.analyze() }, enabled = !vm.busy && vm.input.isNotBlank(), modifier = Modifier.weight(1f)) {
                    if (vm.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.analyze))
                }
            }
        }
        vm.media?.let { media -> item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(media.title, style = MaterialTheme.typography.titleMedium)
                Text(media.platform, color = MaterialTheme.colorScheme.onSurfaceVariant)
                var audioMode by remember(media.url) { mutableStateOf(media.choices.all { it.audioOnly }) }
                var expanded by remember(media.url) { mutableStateOf(false) }
                val selected=media.selected()
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(media.choices.any { !it.audioOnly }) FilterChip(selected=!audioMode,onClick={ audioMode=false; vm.select(null) },label={ Text(stringResource(R.string.video_mode)) })
                    if(media.choices.any { it.audioOnly }) FilterChip(selected=audioMode,onClick={ audioMode=true; vm.select(null) },label={ Text(stringResource(R.string.audio_mode)) })
                }
                Box {
                    OutlinedButton(onClick={ expanded=true }) { Text(selected?.label ?: stringResource(R.string.choose_quality)) }
                    DropdownMenu(expanded=expanded,onDismissRequest={ expanded=false }) {
                        media.choices.filter { it.audioOnly==audioMode }.forEach { choice ->
                            DropdownMenuItem(text={ Text(choice.label + " · " + size(choice.size)) },onClick={ vm.select(choice.id); expanded=false })
                        }
                    }
                }
                if(selected!=null) Text(selected.mime + " · " + size(selected.size),style=MaterialTheme.typography.bodySmall)
                media.duration?.let { Text(stringResource(R.string.duration_seconds,it.toLong().toString()),style=MaterialTheme.typography.bodySmall) }
                Text(stringResource(R.string.original_only), style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && vm.preferences.value.notifications && androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permissions.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.download()
                }, enabled = media.selected() != null, modifier = Modifier.fillMaxWidth()) { Icon(painterResource(R.drawable.ic_download), null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.download)) }
            } }
        } }
        item { Text(stringResource(R.string.support_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.recent), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onDownloads) { Text(stringResource(R.string.view_all)) }
        } }
        if (recent.isEmpty()) item { EmptyState() }
        items(recent, key = { it.id }) { DownloadCard(vm, it) }
    }
}
@Composable private fun DownloadList(vm: MainViewModel, all: List<DownloadItem>, history: Boolean) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var newest by rememberSaveable { mutableStateOf(true) }
    var clear by remember { mutableStateOf(false) }
    val filtered = all.filter { (!history || !it.active) && (filter == "ALL" || it.status == filter) && (it.title.contains(query, true) || it.fileName.contains(query, true)) }.let { if (newest) it else it.reversed() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(if(history) R.string.history else R.string.downloads), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.search)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL", "RUNNING", "QUEUED", "COMPLETED", "FAILED", "CANCELLED").forEach { status -> FilterChip(selected = filter == status, onClick = { filter = status }, label = { Text(statusText(status)) }) }
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { newest = !newest }) { Text(stringResource(if(newest) R.string.newest else R.string.oldest)) }
            if (history) TextButton(onClick = { clear = true }) { Text(stringResource(R.string.clear_history)) }
        } }
        if(filtered.isEmpty()) item { EmptyState() }
        items(filtered, key = { it.id }) { DownloadCard(vm, it) }
    }
    if(clear) Confirm(R.string.clear_history, R.string.clear_body, { clear = false }) { clear = false; vm.clear() }
}
@Composable private fun DownloadCard(vm: MainViewModel, row: DownloadItem) {
    val context = LocalContext.current
    var deleting by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_download), null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(row.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(statusText(row.status), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (row.status == "RUNNING") {
            if (row.total > 0) LinearProgressIndicator(progress = { (row.bytes.toFloat() / row.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("${size(row.bytes)} / ${size(row.total)}" + if(row.speed > 0) " · ${size(row.speed)}/s" else "", style = MaterialTheme.typography.bodySmall)
            if (row.speed > 0 && row.total > row.bytes) Text(stringResource(R.string.eta, ((row.total-row.bytes)/row.speed).toString()), style = MaterialTheme.typography.labelSmall)
        } else Text("${size(if(row.status == "COMPLETED") row.bytes else row.total)} · ${DateFormat.getDateFormat(context).format(java.util.Date(row.created))}", style = MaterialTheme.typography.bodySmall)
        row.error?.let { Text(errorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            when(row.status) {
                "QUEUED", "RUNNING" -> TextButton(onClick = { vm.cancel(row.id) }) { Text(stringResource(R.string.cancel)) }
                "FAILED", "CANCELLED" -> TextButton(onClick = { vm.retry(row.id) }) { Text(stringResource(R.string.retry)) }
                "COMPLETED" -> {
                    TextButton(onClick = { vm.fileAction(row.fileUri) { value -> context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(value), row.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null)) } }) { Text(stringResource(R.string.open)) }
                    TextButton(onClick = { vm.fileAction(row.fileUri) { value ->
                        val send = Intent(Intent.ACTION_SEND).setType(row.mime).putExtra(Intent.EXTRA_STREAM, Uri.parse(value)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        send.clipData = ClipData.newRawUri("media", Uri.parse(value)); context.startActivity(Intent.createChooser(send, null))
                    } }) { Text(stringResource(R.string.share)) }
                }
            }
            if(!row.active) TextButton(onClick = { deleting = true }) { Text(stringResource(R.string.delete)) }
        }
    } }
    if(deleting) Confirm(R.string.delete, R.string.delete_body, { deleting = false }) { deleting = false; vm.delete(row.id) }
}
@Composable private fun SettingsScreen(vm: MainViewModel, prefs: Preferences) {
    val context = LocalContext.current
    var clear by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf("en" to "English", "ar" to "العربية").forEach { (code, label) ->
            FilterChip(selected = prefs.language == code, onClick = { vm.update(prefs.copy(language = code)); AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code)) }, label = { Text(label) })
        } }
        HorizontalDivider()
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("dark" to R.string.dark, "light" to R.string.light, "system" to R.string.system).forEach { (code, label) -> FilterChip(selected = prefs.theme == code, onClick = { vm.update(prefs.copy(theme = code)) }, label = { Text(stringResource(label)) }) } }
        HorizontalDivider()
        SettingSwitch(R.string.wifi_only, prefs.wifiOnly) { vm.update(prefs.copy(wifiOnly = it)) }
        Text(stringResource(R.string.network_hint), style = MaterialTheme.typography.bodySmall)
        SettingSwitch(R.string.notifications, prefs.notifications) { vm.update(prefs.copy(notifications = it)) }
        Text(stringResource(R.string.notification_hint), style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text(stringResource(R.string.quality), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ask" to R.string.ask_quality, "original" to R.string.original_quality).forEach { (code, label) ->
                FilterChip(selected = prefs.preferredQuality == code, onClick = { vm.update(prefs.copy(preferredQuality = code)) }, label = { Text(stringResource(label)) })
            }
        }
        Text(stringResource(R.string.original_only), style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
        Text(stringResource(R.string.location), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.private_location),style=MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { clear = true }) { Text(stringResource(R.string.clear_history)) }
        TextButton(onClick = { privacy = true }) { Text(stringResource(R.string.privacy)) }
        Text("J Video Downloader · 1.1.0-dev", style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.developer), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if(clear) Confirm(R.string.clear_history, R.string.clear_body, { clear = false }) { clear = false; vm.clear() }
    if(privacy) AlertDialog(onDismissRequest = { privacy = false }, title = { Text(stringResource(R.string.privacy)) }, text = { Text(stringResource(R.string.privacy_body), Modifier.verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { privacy = false }) { Text(stringResource(R.string.close)) } })
}
@Composable private fun SettingSwitch(label: Int, checked: Boolean, change: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(label), Modifier.weight(1f)); Switch(checked, change) } }
@Composable private fun Confirm(title: Int, body: Int, dismiss: () -> Unit, confirm: () -> Unit) { AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(title)) }, text = { Text(stringResource(body)) }, confirmButton = { TextButton(onClick = confirm) { Text(stringResource(R.string.confirm)) } }, dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } }) }
@Composable private fun EmptyState() { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(painterResource(R.drawable.ic_download), null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)); Text(stringResource(R.string.empty), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
fun size(bytes: Long): String = when { bytes < 0 -> "—"; bytes < 1024 -> "$bytes B"; bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0); bytes < 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1048576.0); else -> String.format(Locale.getDefault(), "%.2f GB", bytes / 1073741824.0) }
@Composable private fun statusText(status: String) = stringResource(when(status) { "QUEUED" -> R.string.queued; "RUNNING" -> R.string.downloading; "COMPLETED" -> R.string.completed; "FAILED" -> R.string.failed; "CANCELLED" -> R.string.cancelled; else -> R.string.all })
@Composable private fun errorText(code: String) = stringResource(when(code) {
    "INVALID_URL" -> R.string.err_url; "PLATFORM_RESTRICTED" -> R.string.err_platform;
    "NO_FORMATS", "QUALITY_UNAVAILABLE", "MUX_UNSUPPORTED" -> R.string.err_formats
    "ENGINE_UNAVAILABLE", "EXTRACTION_FAILED", "EXTRACTOR_TIMEOUT", "METADATA_TOO_LARGE" -> R.string.err_extractor
    "SELECT_QUALITY" -> R.string.choose_quality; "UNSAFE_HOST", "UNSAFE_REDIRECT" -> R.string.err_unsafe
    "UNSUPPORTED_MEDIA", "MEDIA_CHANGED" -> R.string.err_media; "RESTRICTED" -> R.string.err_restricted; "UNAVAILABLE" -> R.string.err_unavailable
    "STORAGE_FULL" -> R.string.err_space; "STORAGE_ACCESS" -> R.string.err_storage; "DUPLICATE" -> R.string.err_duplicate
    "QUEUE_FULL" -> R.string.err_queue; "FILE_MISSING" -> R.string.err_missing; "ADDED" -> R.string.added
    "NETWORK", "INTERRUPTED", "INCOMPLETE" -> R.string.err_network; "TLS_ERROR" -> R.string.err_tls; "RATE_LIMIT" -> R.string.err_rate
    else -> R.string.err_generic
})
