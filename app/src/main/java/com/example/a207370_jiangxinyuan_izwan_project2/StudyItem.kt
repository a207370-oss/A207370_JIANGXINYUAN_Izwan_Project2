package com.example.a207370_jiangxinyuan_izwan_project2

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "study_items")
data class StudyItem(
    @PrimaryKey val id: String,
    val subject: String,
    val content: String,
    val learnDate: String,
    val reviewDates: List<String>,
    val completed: List<Boolean>
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromBooleanList(value: List<Boolean>): String = gson.toJson(value)

    @TypeConverter
    fun toBooleanList(value: String): List<Boolean> {
        val listType = object : TypeToken<List<Boolean>>() {}.type
        return gson.fromJson(value, listType)
    }
}