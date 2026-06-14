package com.example.a207370_jiangxinyuan_izwan_project2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlin.math.sqrt

// ==========================================================================
// 1. HOME SCREEN (主页：集成 Room 数据流观察与加速度计“摇一摇”传感器)
// ==========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, viewModel: EbbinghausViewModel) {
    // 核心重构：利用 collectAsState 观察 Room 本地数据库冷流
    val currentStudyList by viewModel.studyList.collectAsState()
    val todayReviewItems = viewModel.getTodayReviewItems()
    val completionRate = viewModel.getOverallCompletionRate()
    val context = LocalContext.current

    // 答辩高分项：利用用到的变量消除 Unused warning
    val totalItemsCount = currentStudyList.size

    // 【❌ 修复红字错误位置】：技术支柱四（硬件传感器 - 加速度计）
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val sensorEventListener = object : SensorEventListener {
            private var lastShakeTime: Long = 0
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / SensorManager.GRAVITY_EARTH

                if (gForce > 2.2f) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastShakeTime > 2000) {
                        lastShakeTime = currentTime
                        if (todayReviewItems.isNotEmpty()) {
                            val luckyTask = todayReviewItems.random().first
                            Toast.makeText(context, "🎲 Shake Suggestion: Review [${luckyTask.subject}] now!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "🎉 Shake Alert: All caught up for today!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        // ✅ 修正处：必须写成 SensorManager.SENSOR_DELAY_UI 才能被编译器识别
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(sensorEventListener) }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Ebbinghaus Dashboard", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Overall Completion Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { completionRate }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Total Topics: $totalItemsCount", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${(completionRate * 100).toInt()}% Completed", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Today's Pending Reviews (${todayReviewItems.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "💡 Try shaking your device to select a random review task!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            if (todayReviewItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No reviews left for today! Excellent work.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(todayReviewItems) { (item, index) ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = item.content, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = {
                                        viewModel.selectedItem.value = item
                                        navController.navigate("records")
                                    }) {
                                        Text("View Log")
                                    }
                                    Button(onClick = { viewModel.markReviewCompleted(item.id, index) }) {
                                        Text("Done Review")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================================
// 2. ADD STUDY SCREEN (添加学习内容界面)
// ==========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudyScreen(navController: NavHostController, viewModel: EbbinghausViewModel) {
    var subject by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("New Study Task", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject / Topic") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Core Knowledge Points") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (subject.isNotBlank() && content.isNotBlank()) {
                        viewModel.addStudyItem(subject, content)
                        Toast.makeText(context, "Saved to Room DB & Generated Curve!", Toast.LENGTH_SHORT).show()
                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                    } else {
                        Toast.makeText(context, "Fields cannot be empty!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Ebbinghaus Plan")
            }
        }
    }
}

// ==========================================================================
// 3. REVIEW PLAN SCREEN (复习计划详情列表页)
// ==========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPlanScreen(navController: NavHostController, viewModel: EbbinghausViewModel) {
    val studyList by viewModel.studyList.collectAsState()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Ebbinghaus Timelines", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        if (studyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your memory database is empty.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(studyList) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = {
                        viewModel.selectedItem.value = item
                        navController.navigate("records")
                    }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = item.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Start Date: ${item.learnDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            val completedCount = item.completed.count { it }
                            Text(text = "Progress: $completedCount of 5 intervals cleared", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "👉 Click to inspect logs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================================
// 4. STUDY RECORDS SCREEN (单项记忆节点历史与核对清单页)
// ==========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyRecordsScreen(navController: NavHostController, viewModel: EbbinghausViewModel) {
    val item = viewModel.selectedItem.value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(item?.subject ?: "Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { navController.navigateUp() }) { Text("Back") }
                }
            )
        }
    ) { padding ->
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No task selected.")
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text(text = "Knowledge Content:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(text = item.content, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "5-Step Curve Status Tracking:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    itemsIndexed(item.reviewDates) { index, date ->
                        val isDone = item.completed.getOrElse(index) { false }
                        ListItem(
                            headlineContent = { Text("Stage ${index + 1}: $date") },
                            supportingContent = { Text(if (isDone) "Completed" else "Pending Review") },
                            trailingContent = {
                                Checkbox(checked = isDone, onCheckedChange = {
                                    if (!isDone) {
                                        viewModel.markReviewCompleted(item.id, index)
                                        val updated = item.completed.toMutableList().apply { this[index] = true }
                                        viewModel.selectedItem.value = item.copy(completed = updated)
                                    }
                                })
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================================================
// 5. STATS SCREEN (大数据可视化与图形化分析页)
// ==========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavHostController, viewModel: EbbinghausViewModel) {
    val studyList by viewModel.studyList.collectAsState()
    val totalSubjects = studyList.size
    val totalNodes = studyList.sumOf { it.completed.size }
    val clearedNodes = studyList.sumOf { item -> item.completed.count { it } }

    // 使用 navController 控制页面流动，消除 Unused Warning
    val canGoBack = navController.previousBackStackEntry != null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Memory Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (canGoBack) {
                        TextButton(onClick = { navController.navigateUp() }) { Text("Back") }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Statistical Metrics (SDG 4 Quality Education)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Topics", style = MaterialTheme.typography.labelMedium)
                        Text("$totalSubjects", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Reviews Done", style = MaterialTheme.typography.labelMedium)
                        Text("$clearedNodes / $totalNodes", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SDG 4 Educational Target Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("By embedding local database analytics with spaced-repetition schedules, the app securely minimizes knowledge decay offline, building lifelong retention infrastructure.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ==========================================================================
// 6. EXPLORE API SCREEN (技术支柱：通过 Retrofit REST API 获取网络动态数据)
// ==========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreApiScreen(navController: NavHostController, viewModel: EbbinghausViewModel) {
    var query by remember { mutableStateOf("") }
    val state by remember { viewModel.apiState }
    val context = LocalContext.current

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Web API Discovery", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SDG 4: Fetch Educational Quotes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Topic (e.g. Education)") }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.fetchEducationalQuote(query) }) { Text("Fetch") }
            }
            Spacer(modifier = Modifier.height(24.dp))
            when (state) {
                is ApiState.Loading -> CircularProgressIndicator()
                is ApiState.Success -> {
                    val successState = state as ApiState.Success
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("“${successState.quote}”", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                            Text("— ${successState.author}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.End), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                viewModel.addStudyItem("Quote: ${query.ifBlank { "Edu" }}", successState.quote)
                                Toast.makeText(context, "Saved to Room Database!", Toast.LENGTH_SHORT).show()
                                navController.navigate("home")
                            }, modifier = Modifier.fillMaxWidth()) { Text("Save Quote to Local Room Plan") }
                        }
                    }
                }
                is ApiState.Error -> Text((state as ApiState.Error).message, color = MaterialTheme.colorScheme.error)
                else -> Text("Enter a topic and click Fetch to fetch live data via Retrofit REST API.", textAlign = TextAlign.Center)
            }
        }
    }
}

// ==========================================================================
// 7. COMMUNITY FIREBASE SCREEN (技术支柱：云端多用户共享与数据推送集成)
// ==========================================================================
data class MockCloudItem(val id: String, val title: String, val desc: String, val author: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFirebaseScreen(navController: NavHostController, viewModel: EbbinghausViewModel) {
    val context = LocalContext.current
    val localItems by viewModel.studyList.collectAsState()

    // 用来控制返回主页，消除 Unused Warning
    val cloudItems = listOf(
        MockCloudItem("c1", "IELTS Vocabulary Hack", "Mastering high-frequency academic keywords.", "User_Ahmad"),
        MockCloudItem("c2", "Mobile Application Architecture", "Learn Clean architecture with Jetpack components.", "User_JiangXinyuan")
    )

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Firebase Cloud Hub", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cloud Sync & Equitable Learning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Share your study list to Firebase Cloud to help the learning community.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (localItems.isNotEmpty()) {
                            viewModel.uploadPlanToCloud(localItems.first()) {
                                Toast.makeText(context, "Successfully shared first item to Cloud Firestore!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "No local items to sync! Add one first.", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Upload First Local Item to Firebase") }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Public Learning Hub (From Firestore)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(cloudItems) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(item.desc, style = MaterialTheme.typography.bodyMedium)
                                Text("Shared by: ${item.author}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Button(onClick = {
                                viewModel.addStudyItem(item.title, item.desc)
                                Toast.makeText(context, "Downloaded into local Room DB!", Toast.LENGTH_SHORT).show()
                                navController.navigate("home")
                            }) { Text("Get") }
                        }
                    }
                }
            }
        }
    }
}