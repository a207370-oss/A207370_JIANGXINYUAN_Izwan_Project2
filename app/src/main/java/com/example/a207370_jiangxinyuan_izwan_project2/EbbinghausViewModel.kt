package com.example.a207370_jiangxinyuan_izwan_project2

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class ApiState {
    object Idle : ApiState()
    object Loading : ApiState()
    data class Success(val quote: String, val author: String) : ApiState()
    data class Error(val message: String) : ApiState()
}

class EbbinghausViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val studyDao = database.studyDao()

    val studyList: StateFlow<List<StudyItem>> = studyDao.getAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var apiState = mutableStateOf<ApiState>(ApiState.Idle)
        private set

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    val selectedItem = mutableStateOf<StudyItem?>(null)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun addStudyItem(subject: String, content: String) {
        if (subject.isBlank() || content.isBlank()) return
        val itemId = UUID.randomUUID().toString()
        val today = dateFormat.format(Date())
        val calendar = Calendar.getInstance()
        val reviewDayOffsets = listOf(0, 1, 3, 6, 14)
        val reviewDates = reviewDayOffsets.map { offset ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, offset)
            dateFormat.format(calendar.time)
        }

        val newItem = StudyItem(
            id = itemId,
            subject = subject,
            content = content,
            learnDate = today,
            reviewDates = reviewDates,
            completed = List(reviewDayOffsets.size) { false }
        )
        viewModelScope.launch { studyDao.insertItem(newItem) }
    }

    fun markReviewCompleted(itemId: String, reviewIndex: Int) {
        val currentList = studyList.value
        val item = currentList.find { it.id == itemId }
        if (item != null && reviewIndex in item.completed.indices) {
            val updatedCompleted = item.completed.toMutableList()
            updatedCompleted[reviewIndex] = true
            val updatedItem = item.copy(completed = updatedCompleted)
            viewModelScope.launch { studyDao.updateItem(updatedItem) }
        }
    }

    fun getTodayReviewItems(): List<Pair<StudyItem, Int>> {
        val today = dateFormat.format(Date())
        val todayItems = mutableListOf<Pair<StudyItem, Int>>()
        studyList.value.forEach { item ->
            item.reviewDates.forEachIndexed { index, date ->
                if (date == today && !item.completed[index]) {
                    todayItems.add(Pair(item, index))
                }
            }
        }
        return todayItems
    }

    fun getOverallCompletionRate(): Float {
        val currentList = studyList.value
        val totalReviews = currentList.sumOf { it.completed.size }
        val completedReviews = currentList.sumOf { item -> item.completed.count { it } }
        return if (totalReviews == 0) 0f else completedReviews.toFloat() / totalReviews.toFloat()
    }

    fun fetchEducationalQuote(topic: String) {
        viewModelScope.launch {
            apiState.value = ApiState.Loading
            try {
                kotlinx.coroutines.delay(1000)
                apiState.value = ApiState.Success(
                    quote = "Education is the most powerful weapon which you can use to change the world.",
                    author = "Nelson Mandela"
                )
            } catch (e: Exception) {
                apiState.value = ApiState.Error("API Link Failure: ${e.localizedMessage}")
            }
        }
    }

    fun uploadPlanToCloud(item: StudyItem, onSuccess: () -> Unit) {
        val cloudData = hashMapOf(
            "subject" to item.subject,
            "content" to item.content,
            "learnDate" to item.learnDate,
            "uploader" to "Student_207370"
        )
        firestore.collection("public_plans").document(item.id).set(cloudData).addOnSuccessListener { onSuccess() }
    }
}