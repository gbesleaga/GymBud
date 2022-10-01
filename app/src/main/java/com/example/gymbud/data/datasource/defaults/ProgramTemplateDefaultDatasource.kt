package com.example.gymbud.data.datasource.defaults

import com.example.gymbud.data.ItemIdentifierGenerator
import com.example.gymbud.model.ProgramTemplate
import com.example.gymbud.model.RestPeriod


const val DEFAULT_HYPERTROPHY_PROGRAM = "Default Hypertrophy Program"


object ProgramTemplateDefaultDatasource {
    val programs: List<ProgramTemplate>

    init {
        val programForHypertrophy = ProgramTemplate(ItemIdentifierGenerator.generateId(), DEFAULT_HYPERTROPHY_PROGRAM)
            .add(WorkoutTemplateDefaultDatasource.getWorkoutTemplateForHypertrophyByName(WORKOUT_1)!!)
            .add(WorkoutTemplateDefaultDatasource.getWorkoutTemplateForHypertrophyByName(WORKOUT_2)!!)
            .add(RestPeriod.RestDay)
            .add(WorkoutTemplateDefaultDatasource.getWorkoutTemplateForHypertrophyByName(WORKOUT_3)!!)
            .add(WorkoutTemplateDefaultDatasource.getWorkoutTemplateForHypertrophyByName(WORKOUT_4)!!)
            .add(WorkoutTemplateDefaultDatasource.getWorkoutTemplateForHypertrophyByName(WORKOUT_5)!!)
            .add(RestPeriod.RestDay)

        programs = listOf(programForHypertrophy as ProgramTemplate)
    }
}