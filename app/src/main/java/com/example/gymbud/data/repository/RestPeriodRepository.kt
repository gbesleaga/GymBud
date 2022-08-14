package com.example.gymbud.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.example.gymbud.data.datasource.database.RestPeriodDao
import com.example.gymbud.data.datasource.defaults.RestPeriodDefaultDatasource
import com.example.gymbud.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


private const val TAG = "RestPeriodRepo"


class RestPeriodRepository(
    private val restPeriodDao: RestPeriodDao
) {
    // RestDay is a special value, needs to be retrieved explicitly by id
    val restPeriods: Flow<List<RestPeriod>> = restPeriodDao.getAll().map {
        it.mapNotNull { rest ->
            if (rest.isIntraWorkoutRestPeriod()) rest else null
        }
    }

    // todo We always want to have default rest periods in the DB. Where should this be called to ensure that?
    //  can we deploy a pre-populated db? what about interaction with purge() (it would also drop this stuff)
    suspend fun populateWithDefaults() {
        RestPeriodDefaultDatasource.restPeriods.forEach {
            restPeriodDao.insert(it)
        }

        restPeriodDao.insert(RestPeriod.RestDay)
    }


    fun retrieveRestPeriod(id: ItemIdentifier): Flow<RestPeriod?> = restPeriodDao.get(id)


    suspend fun retrieveRestPeriods(ids: List<ItemIdentifier>): List<RestPeriod> {
        return restPeriodDao.get(ids)
    }


    suspend fun updateRestPeriod(
        id: ItemIdentifier,
        name: String,
        targetPeriodSec: IntRange
    ) {
        // RestDay is not editable
        assert(id != RestPeriod.RestDay.id)

        withContext(Dispatchers.IO) {
            val validName = getValidName(id, name, restPeriodDao.getAll().first())
            restPeriodDao.update(id, validName, targetPeriodSec)
        }
    }


    suspend fun addRestPeriod(
        id: ItemIdentifier,
        name: String,
        targetPeriodSec: IntRange
    ) {
        withContext(Dispatchers.IO) {
            val validName = getValidName(id, name, restPeriodDao.getAll().first())

            try {
                restPeriodDao.insert(RestPeriod(id, validName, targetPeriodSec))
            } catch (e: SQLiteConstraintException) {
                Log.e(TAG, "RestPeriod with id: $id already exists!")
                throw e
            }
        }
    }


    suspend fun removeRestPeriod(id: ItemIdentifier): Boolean {
        // RestDay is not removable
        assert(id != RestPeriod.RestDay.id)

        return restPeriodDao.delete(id) > 0
    }
}