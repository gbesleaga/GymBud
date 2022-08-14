package com.example.gymbud.data.datasource.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymbud.model.ItemIdentifier
import com.example.gymbud.model.RestPeriod
import kotlinx.coroutines.flow.Flow


@Dao
interface RestPeriodDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(restPeriod: RestPeriod)

    @Query("UPDATE rest_period SET" +
            " name = :name," +
            " target_in_seconds = :targetPeriodSec" +
            " WHERE id = :id")
    suspend fun update(
        id: ItemIdentifier,
        name: String,
        targetPeriodSec: IntRange
    )

    @Query("SELECT * from rest_period WHERE id = :id")
    fun get(id: ItemIdentifier): Flow<RestPeriod?>

    @Query("SELECT * from rest_period WHERE id IN (:ids)")
    suspend fun get(ids: List<ItemIdentifier>): List<RestPeriod>

    @Query("SELECT * from rest_period ORDER BY name ASC")
    fun getAll(): Flow<List<RestPeriod>>

    @Query("DELETE from rest_period WHERE id = :id")
    suspend fun delete(id: ItemIdentifier): Int
}