package com.gymbud.gymbud.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gymbud.gymbud.data.ItemIdentifierGenerator
import com.gymbud.gymbud.utility.TimeFormatter

@Entity(tableName = "rest_period")
data class RestPeriod(
    @PrimaryKey(autoGenerate = false) override val id: ItemIdentifier,
    override var name: String,
    @ColumnInfo(name = "target_in_seconds") var targetRestPeriodSec: IntRange
): Item {

    fun getTargetRestPeriodAsString(): String {
        return if (targetRestPeriodSec.first == targetRestPeriodSec.last) {
            TimeFormatter.getFormattedTimeMMSS(targetRestPeriodSec.first.toLong())
        } else {
            "${TimeFormatter.getFormattedTimeMMSS(targetRestPeriodSec.first.toLong())} .. ${TimeFormatter.getFormattedTimeMMSS(targetRestPeriodSec.last.toLong())}"
        }
    }


    fun isIntraWorkoutRestPeriod(): Boolean = (this != RestDay)


    companion object {
        val RestDay = RestPeriod(
            ItemIdentifierGenerator.REST_DAY_ID,
            "Rest Day",
            IntRange(86400, 86400)
        )
    }
}


data class RestPeriodContent(
    override var name: String,
    var targetRestPeriodSec: IntRange
): ItemContent

