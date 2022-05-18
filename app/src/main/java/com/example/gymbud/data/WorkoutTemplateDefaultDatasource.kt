package com.example.gymbud.data

import com.example.gymbud.model.SetTemplate
import com.example.gymbud.model.WorkoutBlockType
import com.example.gymbud.model.WorkoutTemplate


const val WORKOUT_CHEST_BACK_SHOULDERS_1 = "Workout Chest, Back and Shoulders 1"
const val WORKOUT_CHEST_BACK_SHOULDERS_2 = "Workout Chest, Back and Shoulders 2"

const val WORKOUT_LEGS_ARMS_TRAPS_NECK_1 = "Workout Legs, Arms, Traps and Neck 1"
const val WORKOUT_LEGS_ARMS_TRAPS_NECK_2 = "Workout Legs, Arms, Traps and Neck 2"



object WorkoutTemplateDefaultDatasource {
    val workoutTemplatesForHyperTrophy: List<WorkoutTemplate>

    init {
        workoutTemplatesForHyperTrophy = generateDefaultWorkoutTemplatesForHypertrophy()
    }


    private fun generateDefaultWorkoutTemplatesForHypertrophy(): List<WorkoutTemplate> {
        val chestBack1 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(CHEST_BACK_SET_1)!!
        val chestBack2 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(CHEST_BACK_SET_2)!!
        val shoulders1 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(SHOULDERS_SET_1)!!

        val legs1 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(LEGS_SET_1)!!
        val legsArmsTraps1 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(LEGS_ARMS_TRAPS_SET_1)!!
        val calvesNeck1 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(CALVES_NECK_SET_1)!!

        val chestBack3 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(CHEST_BACK_SET_3)!!
        val chestBack4 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(CHEST_BACK_SET_4)!!
        val shoulders2 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(SHOULDERS_SET_2)!!

        val legsArmsTraps2 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(LEGS_ARMS_TRAPS_SET_2)!!
        val calvesNeck2 = SetTemplateDefaultDatasource.getSetTemplateForHypertrophyByName(CALVES_NECK_SET_2)!!

        return listOf(
            WorkoutTemplate(ItemIdentifierGenerator.generateId(), WORKOUT_CHEST_BACK_SHOULDERS_1)
                .addBlock(chestBack1, WorkoutBlockType.Warmup)
                .addBlock(chestBack1, WorkoutBlockType.Warmup)
                .addBlock(chestBack1, WorkoutBlockType.Working)
                .addBlock(chestBack1, WorkoutBlockType.Working)
                .addBlock(chestBack1, WorkoutBlockType.Working)
                .addBlock(chestBack2, WorkoutBlockType.Warmup)
                .addBlock(chestBack2, WorkoutBlockType.Working)
                .addBlock(chestBack2, WorkoutBlockType.Working)
                .addBlock(chestBack2, WorkoutBlockType.Working)
                .addBlock(shoulders1, WorkoutBlockType.Warmup)
                .addBlock(shoulders1, WorkoutBlockType.Working)
                .addBlock(shoulders1, WorkoutBlockType.Working)
                .addBlock(shoulders1, WorkoutBlockType.Working),
            WorkoutTemplate(ItemIdentifierGenerator.generateId(), WORKOUT_LEGS_ARMS_TRAPS_NECK_1)
                .addBlock(legs1, WorkoutBlockType.Warmup)
                .addBlock(legs1, WorkoutBlockType.Warmup)
                .addBlock(legs1, WorkoutBlockType.Working)
                .addBlock(legs1, WorkoutBlockType.Working)
                .addBlock(legs1, WorkoutBlockType.Working)
                .addBlock(legs1, WorkoutBlockType.Working)
                .addBlock(legsArmsTraps1, WorkoutBlockType.Warmup)
                .addBlock(legsArmsTraps1, WorkoutBlockType.Warmup)
                .addBlock(legsArmsTraps1, WorkoutBlockType.Working)
                .addBlock(legsArmsTraps1, WorkoutBlockType.Working)
                .addBlock(legsArmsTraps1, WorkoutBlockType.Working)
                .addBlock(calvesNeck1, WorkoutBlockType.Warmup)
                .addBlock(calvesNeck1, WorkoutBlockType.Working)
                .addBlock(calvesNeck1, WorkoutBlockType.Working)
                .addBlock(calvesNeck1, WorkoutBlockType.Working),
            WorkoutTemplate(ItemIdentifierGenerator.generateId(), WORKOUT_CHEST_BACK_SHOULDERS_2)
                .addBlock(chestBack3, WorkoutBlockType.Warmup)
                .addBlock(chestBack3, WorkoutBlockType.Warmup)
                .addBlock(chestBack3, WorkoutBlockType.Working)
                .addBlock(chestBack3, WorkoutBlockType.Working)
                .addBlock(chestBack3, WorkoutBlockType.Working)
                .addBlock(chestBack4, WorkoutBlockType.Warmup)
                .addBlock(chestBack4, WorkoutBlockType.Working)
                .addBlock(chestBack4, WorkoutBlockType.Working)
                .addBlock(chestBack4, WorkoutBlockType.Working)
                .addBlock(shoulders2, WorkoutBlockType.Warmup)
                .addBlock(shoulders2, WorkoutBlockType.Working)
                .addBlock(shoulders2, WorkoutBlockType.Working)
                .addBlock(shoulders2, WorkoutBlockType.Working),
            WorkoutTemplate(ItemIdentifierGenerator.generateId(), WORKOUT_LEGS_ARMS_TRAPS_NECK_2)
                .addBlock(legsArmsTraps2, WorkoutBlockType.Warmup)
                .addBlock(legsArmsTraps2, WorkoutBlockType.Warmup)
                .addBlock(legsArmsTraps2, WorkoutBlockType.Working)
                .addBlock(legsArmsTraps2, WorkoutBlockType.Working)
                .addBlock(legsArmsTraps2, WorkoutBlockType.Working)
                .addBlock(calvesNeck2, WorkoutBlockType.Warmup)
                .addBlock(calvesNeck2, WorkoutBlockType.Working)
                .addBlock(calvesNeck2, WorkoutBlockType.Working)
                .addBlock(calvesNeck2, WorkoutBlockType.Working)
        )
    }


    fun getWorkoutTemplateForHypertrophyByName(name: String): WorkoutTemplate? {
        return workoutTemplatesForHyperTrophy.find { it.name.contains(name) }
    }
}