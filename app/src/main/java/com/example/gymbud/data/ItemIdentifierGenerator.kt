package com.example.gymbud.data

import com.example.gymbud.model.ItemIdentifier

// todo this is very simplistic atm :)
object ItemIdentifierGenerator {
    private var id: ItemIdentifier = 0

    const val REST30_ID: ItemIdentifier = -30
    const val REST60_ID: ItemIdentifier = -60
    const val REST120_ID: ItemIdentifier = -120
    const val REST180_ID: ItemIdentifier = -180

    const val REST30_TO_60_ID: ItemIdentifier = -3060
    const val REST60_TO_120_ID: ItemIdentifier = -60120
    const val REST60_TO_180_ID: ItemIdentifier = -60180
    const val REST120_TO_180_ID: ItemIdentifier = -120180
    const val REST180_TO_300_ID: ItemIdentifier = -180300

    fun generateId():  ItemIdentifier {
        return id++
    }
}