package com.gymbud.gymbud.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.gymbud.gymbud.data.ItemIdentifierGenerator
import com.gymbud.gymbud.data.datasource.database.ExerciseSessionRecordDao
import com.gymbud.gymbud.data.datasource.database.WorkoutSessionRecordDao
import com.gymbud.gymbud.model.*
import com.gymbud.gymbud.ui.viewmodel.ExerciseFilters
import com.gymbud.gymbud.utility.*
import kotlinx.coroutines.flow.first
import kotlin.math.max


//private const val TAG = "SessionsRepo"


class SessionsRepository(
    private var exerciseSessionRecordDao: ExerciseSessionRecordDao,
    private var workoutSessionRecordDao: WorkoutSessionRecordDao,
    private val workoutTemplateRepository: WorkoutTemplateRepository
) {
    fun setDao(exerciseSessionRecordDao: ExerciseSessionRecordDao, workoutSessionRecordDao: WorkoutSessionRecordDao) {
        this.exerciseSessionRecordDao = exerciseSessionRecordDao
        this.workoutSessionRecordDao = workoutSessionRecordDao
    }


    suspend fun addExerciseSessionRecord(record: ExerciseSessionRecord) {
        exerciseSessionRecordDao.insert(record)
    }


    suspend fun updateExerciseSessionRecord(record: ExerciseSessionRecord) {
        exerciseSessionRecordDao.update(record.id, record.resistance, record.reps, record.notes)
    }


    suspend fun hasExerciseSessionRecord(record: ExerciseSessionRecord): Boolean {
        return exerciseSessionRecordDao.exists(record.id)
    }


    suspend fun addWorkoutSessionRecord(record: WorkoutSessionRecord) {
        workoutSessionRecordDao.insert(record)
    }


    suspend fun updateWorkoutSessionRecord(record: WorkoutSessionRecord) {
        workoutSessionRecordDao.update(record.id, record.durationMs, record.notes)
    }


    suspend fun getWorkoutSession(workoutSessionId: ItemIdentifier): WorkoutSession? {
        val record = workoutSessionRecordDao.get(workoutSessionId)
            ?: return null

        val template = workoutTemplateRepository.retrieveWorkoutTemplate(record.workoutTemplateId).first()
            ?: return null

        val exerciseRecords = exerciseSessionRecordDao.getFromSession(record.id)

        return WorkoutSession.fromRecord(record, template, exerciseRecords)
    }


    suspend fun getWorkoutSessionDate(workoutSessionId: ItemIdentifier): Long? {
        return workoutSessionRecordDao.getSessionDate(workoutSessionId)
    }


    suspend fun getPreviousWorkoutSession(workoutTemplateId: ItemIdentifier, activeSessionId: ItemIdentifier): WorkoutSession? {
        val prevSesRecord = workoutSessionRecordDao.getPreviousSession(workoutTemplateId, activeSessionId)
            ?: return null

        val template = workoutTemplateRepository.retrieveWorkoutTemplate(prevSesRecord.workoutTemplateId).first()
            ?: return null

        val prevSesExerciseRecords = exerciseSessionRecordDao.getFromSession(prevSesRecord.id)
        if (prevSesExerciseRecords.isEmpty()) {
            return null
        }

        return WorkoutSession.fromRecord(prevSesRecord, template, prevSesExerciseRecords)
    }


    suspend fun getSessionsByMonth(year: Int, month: Int, daySpan: Int): List<DayOfTheMonth> {
        // determine start and end date of month span
        val (startDate, endDate, daysInMonth) = getMonthSpan(year, month, daySpan)

        // fill with empty days
        val sessionDays = mutableListOf<DayOfTheMonth>()
        daysInMonth.forEach {
            sessionDays.add(DayOfTheMonth(it.second, it.first, ItemIdentifierGenerator.NO_ID, ""))
        }

        // query for all sessions within that time period (sessions are in order)
        val sessions = workoutSessionRecordDao.getPreviousSessions(startDate, endDate)

        // fill days with sessions
        var atDay = 0
        sessions.forEach { session ->
            for (day in atDay until daySpan) {
                if (getMonth(session.date) == sessionDays[day].month && getDayOfMonth(session.date) == sessionDays[day].day) {
                    sessionDays[day] = DayOfTheMonth(sessionDays[day].day, sessionDays[day].month, session.id, session.name)
                    atDay = day + 1
                    break
                }
            }
        }

        return sessionDays.toList()
    }

    /**
     * exerciseTemplates: the templates associated with this exercise
     */
    suspend fun getExercisePersonalBest(exerciseTemplates: List<ItemIdentifier>, filters: ExerciseFilters): ExerciseResult? {
        // restrict usable workout sessions by filters
        val sessions = getSessionIdsByFilters(filters)

        return exerciseSessionRecordDao.getExercisePersonalBest(exerciseTemplates, sessions)
    }


    /**
     * exerciseTemplates: the templates associated with this exercise
     */
    suspend fun getExerciseResults(exerciseTemplates: List<ItemIdentifier>, filters: ExerciseFilters): List<ExerciseResult> {
        // restrict usable workout sessions by filters
        val sessions = getSessionIdsByFilters(filters)

        return exerciseSessionRecordDao.getExerciseResults(exerciseTemplates, sessions)
    }


    private suspend fun getSessionIdsByFilters(filters: ExerciseFilters): List<ItemIdentifier> {
        // construct query based on filters
        var queryString = ""
        val queryArgs = mutableListOf<Any>()
        var containsCondition = false

        queryString += "SELECT id from workout_session"

        if (filters.programTemplateId != null) {
            queryString += " WHERE"
            queryString += " program_template_id IS ?"
            queryArgs.add(filters.programTemplateId)
            containsCondition = true
        }

        if (filters.periodStart != null) {
            queryString += if (containsCondition) {
                " AND"
            } else {
                containsCondition = true
                " WHERE"
            }

            queryString += " date >= ?"
            queryArgs.add(filters.periodStart)
        }

        if (filters.periodEnd != null) {
            queryString += if (containsCondition) {
                " AND"
            } else {
                // suppressing this, value is set intentionally to avoid future bugs when extending :)
                @Suppress("UNUSED_VALUE")
                containsCondition = true
                " WHERE"
            }

            queryString += " date <= ?"
            queryArgs.add(filters.periodEnd)
        }

        queryString += ";"

        val query = SimpleSQLiteQuery(queryString, queryArgs.toTypedArray())
        return workoutSessionRecordDao.getPreviousSessionsByFilters(query)
    }


    suspend fun getTodaySession(): WorkoutSessionRecord? {
        // query for all sessions within that time period (sessions are in order)
        val today = getStartOfDay(System.currentTimeMillis())
        val tomorrow = addDays(today, 1)

        val sessions = workoutSessionRecordDao.getPreviousSessions(today, tomorrow)

        return if (sessions.isEmpty()) {
            null
        } else {
            sessions[0]
        }
    }


    suspend fun removeSession(id: ItemIdentifier): Boolean {
        return workoutSessionRecordDao.delete(id) > 0
    }


    suspend fun getMaxId(): ItemIdentifier = max(exerciseSessionRecordDao.getMaxId(), workoutSessionRecordDao.getMaxId())
}