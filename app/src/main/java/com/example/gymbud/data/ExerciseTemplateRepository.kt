package com.example.gymbud.data

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.example.gymbud.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

private const val TAG = "ExerciseTemplateRepo"

class ExerciseTemplateRepository(
    private val exerciseTemplateDao: ExerciseTemplateDao,
    private val exerciseRepository: ExerciseRepository
) {
    val exerciseTemplates: Flow<List<ExerciseTemplate>> =
        exerciseTemplateDao.getAll().map { templates ->
            templates.forEach {
                exerciseRepository.fillExerciseContent(it.exercise)
            }

            return@map templates
        }


    suspend fun populateWithDefaults() {
        ExerciseDefaultDatasource.exerciseTemplatesForHypertrophy.forEach {
            exerciseTemplateDao.insert(it)
        }
    }


    fun retrieveExerciseTemplate(id: ItemIdentifier): Flow<ExerciseTemplate?> {
        return exerciseTemplateDao.get(id).map { exerciseTemplate ->
            if (exerciseTemplate != null) {
                // the exercise template will have only the id of the associated exercise after retrieving it from the DAO
                exerciseRepository.fillExerciseContent(exerciseTemplate.exercise)
            }

            return@map exerciseTemplate
        }
    }


    suspend fun retrieveExerciseTemplatesOnce(ids: List<ItemIdentifier>): List<ExerciseTemplate> {
        val templates = exerciseTemplateDao.getOnce(ids)
        templates.forEach {
            exerciseRepository.fillExerciseContent(it.exercise)
        }

        return templates
    }



    suspend fun updateExerciseTemplate(
        id: ItemIdentifier,
        name: String,
        targetRepRange: IntRange
    ) {
        withContext(Dispatchers.IO) {
            val validName = getValidName(id, name, exerciseTemplateDao.getAllOnce())
            exerciseTemplateDao.update(id, validName, targetRepRange)
        }
    }


    suspend fun addExerciseTemplate(
        id: ItemIdentifier,
        name: String,
        exercise: Exercise,
        targetRepRange: IntRange
    ) {
        withContext(Dispatchers.IO) {
            val validName = getValidName(id, name, exerciseTemplateDao.getAllOnce())
            try {
                exerciseTemplateDao.insert(ExerciseTemplate(id, validName, exercise, targetRepRange))
            } catch (e: SQLiteConstraintException) {
                Log.e(TAG, "Exercise template with id: $id already exists!")
                throw e
            }
        }
    }


    suspend fun removeExerciseTemplate(id: ItemIdentifier): Boolean {
        return exerciseTemplateDao.delete(id) > 0
    }
}