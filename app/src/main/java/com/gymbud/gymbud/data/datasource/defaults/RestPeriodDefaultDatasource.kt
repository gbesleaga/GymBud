package com.gymbud.gymbud.data.datasource.defaults

import com.gymbud.gymbud.data.ItemIdentifierGenerator
import com.gymbud.gymbud.model.RestPeriod

object RestPeriodDefaultDatasource {
    val rest30 = RestPeriod(
        ItemIdentifierGenerator.generateId(),
        "Rest 30s",
        IntRange(30, 30)
    )

    val rest60 = RestPeriod(
        ItemIdentifierGenerator.generateId(),
        "Rest 60s",
        IntRange(60, 60)
    )

    val rest60to120 = RestPeriod(
        ItemIdentifierGenerator.generateId(),
        "Rest 1-2 min",
        IntRange(60,120)
    )

    val rest180to300 = RestPeriod(
        ItemIdentifierGenerator.generateId(),
        "Rest 3-5 min",
        IntRange(180, 300)
    )

    val restPeriods = listOf(
        rest30,
        rest60,
        rest60to120,
        rest180to300
    )
}