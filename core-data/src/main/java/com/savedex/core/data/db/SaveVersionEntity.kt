package com.savedex.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "save_versions",
    foreignKeys = [
        ForeignKey(
            entity = SaveSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("slotId")],
)
data class SaveVersionEntity(
    @PrimaryKey val id: String,
    val slotId: String,
    val timestampEpochMillis: Long,
    val hash: String,
    val backupPath: String,
)
