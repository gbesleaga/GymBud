package com.gymbud.gymbud.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.gymbud.gymbud.data.datasource.database.WorkoutTemplateDao
import com.gymbud.gymbud.data.datasource.database.WorkoutTemplateWithItemDao
import com.gymbud.gymbud.data.datasource.defaults.WorkoutTemplateDefaultDatasource
import com.gymbud.gymbud.model.Item
import com.gymbud.gymbud.model.ItemIdentifier
import com.gymbud.gymbud.model.ItemType
import com.gymbud.gymbud.model.TaggedItem
import com.gymbud.gymbud.model.Tags
import com.gymbud.gymbud.model.WorkoutTemplate
import com.gymbud.gymbud.model.WorkoutTemplateWithItem
import com.gymbud.gymbud.model.getItemType
import com.gymbud.gymbud.model.getValidName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


private const val TAG = "WorkoutTemplateRepo"


// todo lots of duplication with SetTemplateRepository atm (basically copy-pasta) -> can we do better? (check after adding real data source)
class WorkoutTemplateRepository(
    private var workoutTemplateDao: WorkoutTemplateDao,
    private var workoutTemplateWithItemDao: WorkoutTemplateWithItemDao,
    private val setTemplateRepository: SetTemplateRepository,
    private val restPeriodRepository: RestPeriodRepository
) {
    fun setDao(workoutTemplateDao: WorkoutTemplateDao, workoutTemplateWithItemDao: WorkoutTemplateWithItemDao) {
        this.workoutTemplateDao = workoutTemplateDao
        this.workoutTemplateWithItemDao = workoutTemplateWithItemDao
    }


    val workoutTemplates: Flow<List<WorkoutTemplate>> = workoutTemplateDao.getAll().map { workouts ->
        workouts.map {
            populateWorkoutTemplateItems(it)
            it
        }
    }


    suspend fun populateWithDefaults() {
        WorkoutTemplateDefaultDatasource.workoutTemplatesForHyperTrophy.forEach {
            addWorkoutTemplate(it.id, it.name, it.items)
        }
    }


    fun retrieveWorkoutTemplate(id: ItemIdentifier): Flow<WorkoutTemplate?> {
        return workoutTemplateDao.get(id).map { workoutTemplate ->
            if (workoutTemplate != null) {
                populateWorkoutTemplateItems(workoutTemplate)
            }

            return@map workoutTemplate
        }
    }


    private suspend fun populateWorkoutTemplateItems(workoutTemplate: WorkoutTemplate) {
        val workoutItems = workoutTemplateWithItemDao.getAll(workoutTemplate.id)

        // get workout items from db in bulk
        val workoutSetTemplates = setTemplateRepository.retrieveSetTemplates(
            workoutItems.filter { it.isWithSetTemplate() }.map { it.setTemplateId!! }
        )

        val workoutRestPeriods = restPeriodRepository.retrieveRestPeriods(
            workoutItems.filter { it.isWithRestPeriod() }.map{ it.restPeriodId!! }
        )

        // put in order
        workoutItems.forEachIndexed { workoutItemIndex, workoutItem ->
            workoutItem.workoutItemPosition = workoutItemIndex // ensure no gaps

            val itemToBeAdded: Item? = when {
                workoutItem.isWithSetTemplate() -> {
                    workoutSetTemplates.find { setTemplate -> setTemplate.id == workoutItem.setTemplateId  }
                }
                workoutItem.isWithRestPeriod() -> {
                    workoutRestPeriods.find { restPeriod -> restPeriod.id == workoutItem.restPeriodId  }
                }
                else -> {
                    null
                }
            }

            if (itemToBeAdded == null) {
                //Log.e(TAG,"The workout item with id: $workoutItemIndex could not be retrieved from the DB.")
                assert(false) // should never happen
            }

            // check for tags
            if (workoutItem.tags.isNotEmpty()) {
                workoutTemplate.add(TaggedItem(itemToBeAdded!!, workoutItem.tags))
            } else {
                workoutTemplate.add(itemToBeAdded!!)
            }
        }
    }


    suspend fun retrieveWorkoutTemplatesByItem(id: ItemIdentifier): List<Item> {
        return workoutTemplateDao.getByItem(id)
    }


    suspend fun retrieveWorkoutTemplates(ids: List<ItemIdentifier>): List<WorkoutTemplate> {
        val templates = workoutTemplateDao.get(ids)
        templates.forEach {
            populateWorkoutTemplateItems(it)
        }

        return templates
    }


    suspend fun hasWorkoutTemplateWithSameContent(pendingEntry: WorkoutTemplate): WorkoutTemplate? {
        val emptyTemplate = workoutTemplateDao.hasWorkoutTemplateWithSameContent(pendingEntry.name)
            ?: return null

        val template = retrieveWorkoutTemplate(emptyTemplate.id).first()

        return if (pendingEntry.items == template!!.items) {
            template
        } else {
            null
        }
    }


    suspend fun addWorkoutTemplate(
        id: ItemIdentifier,
        name: String,
        items: List<Item>
    ): WorkoutTemplate {
        return withContext(Dispatchers.IO) {
            val validName = getValidName(id, name, workoutTemplateDao.getAll().first())
            val entry = WorkoutTemplate(id, validName)
            entry.replaceAllWith(items)

            try {
                workoutTemplateDao.insert(entry)
            } catch (e: SQLiteConstraintException) {
                //Log.e(TAG, "Workout template with id: $id already exists!")
                throw e
            }

            items.forEachIndexed { itemIndex, item ->
                try {
                    var tags: Tags = mapOf()
                    if (item is TaggedItem) {
                        tags = item.tags
                    }

                    when (getItemType(item)) {
                        ItemType.SET_TEMPLATE -> workoutTemplateWithItemDao.insert(
                            WorkoutTemplateWithItem(id, itemIndex, setTemplateId = item.id, tags = tags)
                        )
                        ItemType.REST_PERIOD -> workoutTemplateWithItemDao.insert(
                            WorkoutTemplateWithItem(id, itemIndex, restPeriodId = item.id, tags = tags)
                        )
                        else -> assert(false)
                    }
                } catch (e: SQLiteConstraintException) {
                    //Log.e(TAG, "Failed to link item with id: ${item.id} to workout $id")
                    throw e
                }
            }

            return@withContext entry
        }
    }


    suspend fun updateWorkoutTemplate(
        id: ItemIdentifier,
        name: String,
        items: List<Item>
    ) {
        withContext(Dispatchers.IO) {
            val validName = getValidName(id, name, workoutTemplateDao.getAll().first())
            workoutTemplateDao.update(id, validName)

            // first remove older links
            workoutTemplateWithItemDao.deleteAll(id)

            // then add new links
            val workoutItemsToAdd = items.mapIndexedNotNull { index, item ->
                var tags: Tags = mapOf()
                if (item is TaggedItem) {
                    tags = item.tags
                }

                when (getItemType(item)) {
                    ItemType.SET_TEMPLATE -> WorkoutTemplateWithItem(id, index, setTemplateId = item.id, tags = tags)
                    ItemType.REST_PERIOD -> WorkoutTemplateWithItem(id, index, restPeriodId = item.id, tags = tags)
                    else -> null
                }
            }

            workoutTemplateWithItemDao.insert(workoutItemsToAdd)
        }
    }



    suspend fun removeWorkoutTemplate(id: ItemIdentifier): Boolean {
        return workoutTemplateDao.delete(id) > 0
    }


    suspend fun getMaxId(): ItemIdentifier = workoutTemplateDao.getMaxId()
}