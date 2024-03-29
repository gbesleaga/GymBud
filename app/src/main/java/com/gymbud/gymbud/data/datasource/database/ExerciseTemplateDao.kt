package com.gymbud.gymbud.data.datasource.database

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymbud.gymbud.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseTemplateDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exerciseTemplate: ExerciseTemplate)

    @Query("UPDATE exercise_template SET" +
            " name = :name," +
            " target_rep_range = :targetRepRange" +
            " WHERE id = :id")
    suspend fun update(
        id: ItemIdentifier,
        name: String,
        targetRepRange: IntRange
    )

    @Query("DELETE from exercise_template WHERE id = :id")
    suspend fun delete(id: ItemIdentifier): Int

    @Query("SELECT * from exercise_template WHERE id = :id")
    fun get(id: ItemIdentifier): Flow<ExerciseTemplate?>

    @Query("SELECT * from exercise_template WHERE id IN (:ids)")
    suspend fun get(ids: List<ItemIdentifier>): List<ExerciseTemplate>

    @Query("SELECT id, name from exercise_template WHERE exercise_id = :exerciseId")
    suspend fun getByExercise(exerciseId: ItemIdentifier): List<ItemFromDao>

    @Query("SELECT * from exercise_template ORDER BY name ASC")
    fun getAll(): Flow<List<ExerciseTemplate>>

    @Query("SELECT * from exercise_template WHERE name = :name AND exercise_id = :exerciseId AND target_rep_range = :targetRepRange")
    suspend fun hasExerciseTemplateWithSameContent(
        name: String,
        exerciseId: ItemIdentifier,
        targetRepRange: IntRange
        ): ExerciseTemplate?

    @Query("SELECT * from exercise_template WHERE id = :id")
    fun getRows(id: ItemIdentifier): Cursor

    @Query("SELECT id from exercise_template ORDER BY id DESC LIMIT 1")
    suspend fun getMaxId(): ItemIdentifier
}