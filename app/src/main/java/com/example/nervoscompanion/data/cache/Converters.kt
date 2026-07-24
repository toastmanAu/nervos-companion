package com.example.nervoscompanion.data.cache

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
  @TypeConverter
  fun fromStringList(value: List<String>?): String {
    if (value == null) return "[]"
    val array = JSONArray()
    for (item in value) {
      array.put(item)
    }
    return array.toString()
  }

  @TypeConverter
  fun toStringList(value: String?): List<String> {
    if (value.isNullOrEmpty()) return emptyList()
    val list = mutableListOf<String>()
    try {
      val array = JSONArray(value)
      for (i in 0 until array.length()) {
        list.add(array.getString(i))
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list
  }

  @TypeConverter
  fun fromLongList(value: List<Long>?): String {
    if (value == null) return "[]"
    val array = JSONArray()
    for (item in value) {
      array.put(item)
    }
    return array.toString()
  }

  @TypeConverter
  fun toLongList(value: String?): List<Long> {
    if (value.isNullOrEmpty()) return emptyList()
    val list = mutableListOf<Long>()
    try {
      val array = JSONArray(value)
      for (i in 0 until array.length()) {
        list.add(array.getLong(i))
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list
  }
}
