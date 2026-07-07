package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatarEmoji: String = "👤",
    val colorHex: String = "#6750A4"
)
