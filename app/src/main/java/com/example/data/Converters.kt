package com.example.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromListIntToString(list: List<Int>?): String {
        return list?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun fromStringToListInt(data: String?): List<Int> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
