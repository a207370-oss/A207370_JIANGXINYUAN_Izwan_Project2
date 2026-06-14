package com.example.a207370_jiangxinyuan_izwan_project2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val navController = rememberNavController()
                val viewModel: EbbinghausViewModel = viewModel()
                EbbinghausApp(navController = navController, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun EbbinghausApp(navController: NavHostController, viewModel: EbbinghausViewModel) {
    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    LazyRow(modifier = Modifier.padding(horizontal = 4.dp)) {
                        item { TextButton(onClick = { navController.navigate("home") }) { Text("Home") } }
                        item { TextButton(onClick = { navController.navigate("plan") }) { Text("Plan") } }
                        item { TextButton(onClick = { navController.navigate("records") }) { Text("Records") } }
                        item { TextButton(onClick = { navController.navigate("stats") }) { Text("Stats") } }
                        item { TextButton(onClick = { navController.navigate("explore") }) { Text("API Explore", color = MaterialTheme.colorScheme.primary) } }
                        item { TextButton(onClick = { navController.navigate("community") }) { Text("Community", color = MaterialTheme.colorScheme.primary) } }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { navController.navigate("add") }) { Text("Add") }
                }
            )
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(innerPadding)) {
            composable("home") { HomeScreen(navController, viewModel) }
            composable("add") { AddStudyScreen(navController, viewModel) }
            composable("plan") { ReviewPlanScreen(navController, viewModel) }
            composable("records") { StudyRecordsScreen(navController, viewModel) }
            composable("stats") { StatsScreen(navController, viewModel) }
            composable("explore") { ExploreApiScreen(navController, viewModel) }
            composable("community") { CommunityFirebaseScreen(navController, viewModel) }
        }
    }
}