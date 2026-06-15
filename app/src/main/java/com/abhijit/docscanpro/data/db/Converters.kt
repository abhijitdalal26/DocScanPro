package com.abhijit.docscanpro.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTagList(tags: List<String>): String = tags.joinToString(",")

    @TypeConverter
    fun toTagList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",").map { it.trim() }
}
