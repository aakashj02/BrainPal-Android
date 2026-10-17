package com.brainpal.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.brainpal.app.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF18211F)
private val Muted = Color(0xFF687570)
private val Mint = Color(0xFFB9F1D5)
private val Canvas = Color(0xFFF5F7F4)
private val Border = Color(0xFFE1E8E2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = BrainPalDatabase.getInstance(applicationContext).brainPalDao()
        val repository = BrainPalRepository(dao)
        setContent { BrainPalTheme { val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository, dao)); BrainPalApp(vm, onEnableTracking = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, onExport = { csv ->
            val send = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_SUBJECT, "Brain Pal watch history"); putExtra(Intent.EXTRA_TEXT, csv) }
            startActivity(Intent.createChooser(send, "Export Brain Pal data"))
        }) } }
    }
    override fun onResume() { super.onResume() }
}

class HomeViewModel(private val repository: BrainPalRepository, dao: BrainPalDao) : ViewModel() {
    val home = repository.observeHome().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSnapshot())
    val todayPlatforms = dao.observePlatformCountsForDate(java.time.LocalDate.now().toString()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lifetimePlatforms = dao.observePlatformLifetimeCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentEvents = repository.observeRecentEvents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dailyCounts = dao.observeDailyCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun setGoal(target: Int, onError: (String) -> Unit) { viewModelScope.launch { runCatching { repository.setDailyGoal(target) }.onFailure { onError(it.message ?: "Could not save goal.") } } }
    fun export(onReady: (String) -> Unit) { viewModelScope.launch { val rows = repository.allEvents(); val csv = buildString { appendLine("id,platform,watchedAt,localDate,wasAd,contentKey"); rows.forEach { appendLine("${it.id},${it.platform},${it.watchedAtEpochMillis},${it.localDate},${it.wasAd},\"${(it.contentKey ?: "").replace("\"", "\"\"")}\"") } }; onReady(csv) } }
    class Factory(private val repository: BrainPalRepository, private val dao: BrainPalDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T { if (modelClass.isAssignableFrom(HomeViewModel::class.java)) return HomeViewModel(repository, dao) as T; throw IllegalArgumentException("Unknown ViewModel") }
    }
}

@Composable private fun BrainPalTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = lightColorScheme(primary = Ink, onPrimary = Color.White, secondary = Color(0xFF39775D), background = Canvas, surface = Color.White, onSurface = Ink), content = content) }

@Composable private fun BrainPalApp(vm: HomeViewModel, onEnableTracking: () -> Unit, onExport: (String) -> Unit) {
    val snapshot by vm.home.collectAsState(); val todayPlatforms by vm.todayPlatforms.collectAsState(); val lifetimePlatforms by vm.lifetimePlatforms.collectAsState(); val events by vm.recentEvents.collectAsState(); val dailyCounts by vm.dailyCounts.collectAsState()
    var tab by remember { mutableStateOf("Home") }; var showGoalDialog by remember { mutableStateOf(false) }; var goalText by remember(snapshot.goal) { mutableStateOf(snapshot.goal?.toString() ?: "100") }; var errorText by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val trackingEnabled = remember { mutableStateOf(false) }
    fun refreshTracking() { val enabled = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty(); trackingEnabled.value = enabled.contains("ReelTrackingService", ignoreCase = true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshTracking() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(tab) { refreshTracking() }
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Mint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Bolt, null, tint = Ink, modifier = Modifier.size(26.dp)) }
                    Column(Modifier.weight(1f).padding(start = 11.dp)) { Text("Brain Pal", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Your attention, made visible", fontSize = 13.sp, color = Muted) }
                    Icon(Icons.Filled.Lock, contentDescription = "Local-only", tint = Muted)
                }
                when (tab) {
                    "Home" -> {
                        Text("TODAY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.5.sp)
                        Text("Make scrolling intentional.", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink, lineHeight = 31.sp)
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Text("Reels / Shorts watched today", color = Color.White, modifier = Modifier.weight(1f)); StatusPill(if (trackingEnabled.value) "TRACKING ON" else "TRACKING OFF", trackingEnabled.value) }
                                Text(snapshot.todayCount.toString(), color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                                Text(if (trackingEnabled.value) "Automatic tracking is enabled; counts are estimates." else "Enable accessibility tracking below to start automatic counting.", color = Color(0xFFD7E2DC), fontSize = 13.sp)
                                if (snapshot.goal != null) { LinearProgressIndicator(progress = { snapshot.progress }, modifier = Modifier.fillMaxWidth(), color = Mint); Text("${(snapshot.progress * 100).toInt()}% of daily goal", color = Color.White, fontSize = 12.sp) }
                                Divider(color = Color(0xFF35433D))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { MiniMetric("YESTERDAY", snapshot.yesterdayCount.toString()); MiniMetric("GOAL STREAK", "${snapshot.streak} days"); MiniMetric("DAILY GOAL", snapshot.goal?.toString() ?: "Not set") }
                            }
                        }
                        Card(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Border), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Platform breakdown · today", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                                PlatformRow("Instagram Reels", todayPlatforms.firstOrNull { it.platform == "instagram" }?.total ?: 0, lifetimePlatforms.firstOrNull { it.platform == "instagram" }?.total ?: 0)
                                PlatformRow("Facebook Reels", todayPlatforms.firstOrNull { it.platform == "facebook" }?.total ?: 0, lifetimePlatforms.firstOrNull { it.platform == "facebook" }?.total ?: 0)
                                PlatformRow("YouTube Shorts", todayPlatforms.firstOrNull { it.platform == "youtube" }?.total ?: 0, lifetimePlatforms.firstOrNull { it.platform == "youtube" }?.total ?: 0)
                            }
                        }
                        Card(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Border), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Flag, null, tint = Color(0xFF39775D)); Text("Your daily goal", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp), color = Ink) }
                                Text("Streaks are based on days when your saved count reaches the goal.", color = Muted, fontSize = 13.sp)
                                Text("Reminder milestones: every 5 counted videos beyond your goal. Notifications require notification permission.", color = Ink, fontSize = 12.sp)
                                Button(onClick = { goalText = snapshot.goal?.toString() ?: "100"; errorText = null; showGoalDialog = true }, modifier = Modifier.fillMaxWidth()) { Text(if (snapshot.goal == null) "Set daily goal" else "Change daily goal") }
                            }
                        }
                        Card(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Border), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Automatic tracking", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                                Text("Brain Pal uses Android Accessibility to inspect visible UI labels. It does not capture video pixels or send data to a server. Detection can miss videos or count incorrectly when apps change their interface.", color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
                                Button(onClick = onEnableTracking, modifier = Modifier.fillMaxWidth()) { Text(if (trackingEnabled.value) "Open accessibility settings" else "Enable tracking in Settings") }
                            }
                        }
                    }
                    "History" -> {
                        Text("Watch history", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Estimated totals and recent saved detections across supported platforms.", color = Muted, fontSize = 13.sp)
                        val todayDate = java.time.LocalDate.now()
                        val weekTotal = dailyCounts.filter { runCatching { java.time.LocalDate.parse(it.localDate) }.getOrNull()?.let { d -> !d.isBefore(todayDate.minusDays(6)) && !d.isAfter(todayDate) } == true }.sumOf { it.total }
                        val monthTotal = dailyCounts.filter { runCatching { java.time.LocalDate.parse(it.localDate) }.getOrNull()?.let { d -> d.year == todayDate.year && d.month == todayDate.month } == true }.sumOf { it.total }
                        val yearTotal = dailyCounts.filter { runCatching { java.time.LocalDate.parse(it.localDate) }.getOrNull()?.year == todayDate.year }.sumOf { it.total }
                        val lifetimeTotal = dailyCounts.sumOf { it.total }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Border)) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatLine("Today", snapshot.todayCount); StatLine("Last 7 days", weekTotal); StatLine("This month", monthTotal); StatLine("This year", yearTotal); StatLine("Lifetime", lifetimeTotal)
                        } }
                        Button(onClick = { vm.export(onExport) }, modifier = Modifier.fillMaxWidth()) { Text("Export all events as CSV") }
                        if (events.isEmpty()) Text("No saved events yet.", color = Muted)
                        events.forEach { event ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Border)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { Text(platformName(event.platform), fontWeight = FontWeight.Bold, color = Ink); Text(event.localDate + " · " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(event.watchedAtEpochMillis)), fontSize = 12.sp, color = Muted); Text(event.contentKey?.take(36) ?: "No content key", fontSize = 10.sp, color = Muted) }
                                    Text("+1", fontWeight = FontWeight.Bold, color = Color(0xFF39775D))
                                }
                            }
                        }
                    }
                    else -> {
                        Text("Settings & privacy", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Border)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Local storage", fontWeight = FontWeight.Bold, color = Ink); Text("Your watch events and daily goal are stored in the app's local Room database. There is no account or cloud sync.", color = Muted, fontSize = 13.sp)
                            Divider(); Text("Tracking permission", fontWeight = FontWeight.Bold, color = Ink); Text(if (trackingEnabled.value) "Accessibility service appears enabled." else "Accessibility service is currently off.", color = Muted, fontSize = 13.sp); Button(onClick = onEnableTracking, modifier = Modifier.fillMaxWidth()) { Text("Open Android accessibility settings") }
                            Divider(); Text("Data export", fontWeight = FontWeight.Bold, color = Ink); Text("Export a CSV containing saved timestamps, platform, and heuristic content keys. Content keys are hashes, not video links.", color = Muted, fontSize = 13.sp); OutlinedButton(onClick = { vm.export(onExport) }, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }
                            Text("Accuracy note: accessibility heuristics cannot know the exact watch count from the social apps. Review History and treat counts as estimates.", color = Muted, fontSize = 12.sp)
                        } }
                    }
                }
            }
            NavigationBar(containerColor = Color.White) {
                listOf("Home", "History", "Settings").forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item; refreshTracking() }, icon = { when(item) { "Home" -> Icon(Icons.Filled.Bolt, null); "History" -> Icon(Icons.Filled.Insights, null); else -> Icon(Icons.Filled.Settings, null) } }, label = { Text(item) }) }
            }
        }
    }
    if (showGoalDialog) AlertDialog(onDismissRequest = { showGoalDialog = false }, title = { Text("Set daily goal") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("How many Reels/Shorts per day?"); OutlinedTextField(value = goalText, onValueChange = { goalText = it.filter(Char::isDigit).take(6); errorText = null }, label = { Text("Videos per day") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); if (errorText != null) Text(errorText!!, color = MaterialTheme.colorScheme.error); Text("Allowed range: 1–100,000", color = Muted, fontSize = 12.sp) } }, confirmButton = { Button(onClick = { val target = goalText.toIntOrNull(); if (target == null || target !in 1..100_000) errorText = "Enter a number from 1 to 100000." else { vm.setGoal(target) { errorText = it }; showGoalDialog = false } }) { Text("Save goal") } }, dismissButton = { OutlinedButton(onClick = { showGoalDialog = false }) { Text("Cancel") } })
}

@Composable private fun StatLine(label: String, value: Int) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted); Text(value.toString(), color = Ink, fontWeight = FontWeight.Bold) } }
@Composable private fun StatusPill(text: String, enabled: Boolean) { Box(Modifier.background(if (enabled) Color(0xFF35433D) else Color(0xFF454545), CircleShape).padding(horizontal = 9.dp, vertical = 5.dp)) { Text(text, color = if (enabled) Mint else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun PlatformRow(name: String, today: Int, total: Int) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(name, color = Ink, fontWeight = FontWeight.SemiBold); Text("$total lifetime", color = Muted, fontSize = 11.sp) }; Text("$today today", color = Ink, fontWeight = FontWeight.Bold) } }
@Composable private fun MiniMetric(label: String, value: String) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(label, color = Color(0xFFB7C7BF), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp); Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) } }
private fun platformName(platform: String) = when(platform) { "instagram" -> "Instagram Reels"; "facebook" -> "Facebook Reels"; "youtube" -> "YouTube Shorts"; else -> platform }
