package com.example.gymbud.ui.viewbuilder

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.gymbud.R
import com.example.gymbud.databinding.*
import com.example.gymbud.model.*
import com.example.gymbud.ui.viewmodel.ItemViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


private const val TAG = "SetTemplateEV"

class SetTemplateEditView(
    private val context: Context
): EditItemView {

    private var _nameBinding: LayoutEditTextFieldBinding? = null
    private val nameBinding get() = _nameBinding!!

    private var _exerciseListBinding: FragmentItemListBinding? = null
    private val exerciseListBinding get() = _exerciseListBinding!!

    private var addExerciseTemplateButton = MaterialButton(context)
    private var addRestPeriodButton = MaterialButton(context)

    private var _addItemBinding: FragmentItemEditBinding? = null
    private val addItemBinding get() = _addItemBinding!!

    private var _itemSelectionBinding: LayoutEditDropdownFieldBinding? = null
    private val itemSelectionBinding get() = _itemSelectionBinding!!

    private var addingItemOfType = ItemType.EXERCISE_TEMPLATE

    // the exercises(+rest periods) in the set
    private var setTemplateEditableItems: MutableList<Item> = mutableListOf()
    private val exerciseListAdapter = SetTemplateRecyclerViewAdapter(Functionality.Edit)

    // available exercise templates to chose from
    private var exerciseTemplates: List<Item>? = null
    private var exerciseTemplatesSelectionAdapter: ArrayAdapter<String> = ArrayAdapter(context, R.layout.dropdown_list_item, listOf())

    // available rest periods to chose from
    private var restPeriods: List<Item>? = null
    private var restPeriodsSelectionAdapter: ArrayAdapter<String> = ArrayAdapter(context, R.layout.dropdown_list_item, listOf())

    init {
        exerciseListAdapter.setOnItemClickedCallback { item ->
            if(setTemplateEditableItems.remove(item)) {
                exerciseListAdapter.submitList(setTemplateEditableItems.toList())
            }
        }

        addExerciseTemplateButton.setIconResource(R.drawable.ic_add_24)
        addExerciseTemplateButton.text = context.getString(R.string.exercise)
        addExerciseTemplateButton.setOnClickListener{
            onAddNewExercise()
        }

        addRestPeriodButton.setIconResource(R.drawable.ic_add_24)
        addRestPeriodButton.text = context.getString(R.string.rest_period)
        addRestPeriodButton.setOnClickListener{
            onAddNewRestPeriod()
        }
    }


    override fun inflate(inflater: LayoutInflater): List<View> {
        return listOf(
            inflateName(inflater),
            inflateExerciseList(inflater),
            LayoutDetailDividerBinding.inflate(inflater).root,
            inflateAddItem(inflater),
            addExerciseTemplateButton,
            addRestPeriodButton,
        )
    }

    private fun inflateName(inflater: LayoutInflater): View {
        _nameBinding = LayoutEditTextFieldBinding.inflate(inflater)

        nameBinding.label.hint = context.getString(R.string.item_name)
        nameBinding.input.setOnClickListener {
            nameBinding.label.error = null
        }

        return nameBinding.root
    }

    private fun inflateExerciseList(inflater: LayoutInflater): View {
        _exerciseListBinding = FragmentItemListBinding.inflate(inflater)

        exerciseListBinding.addItemFab.isVisible  = false
        exerciseListBinding.recyclerView.adapter = exerciseListAdapter

        return exerciseListBinding.root
    }

    private fun inflateAddItem(inflater: LayoutInflater): View {
        _itemSelectionBinding = LayoutEditDropdownFieldBinding.inflate(inflater)

        itemSelectionBinding.input.setOnClickListener {
            itemSelectionBinding.label.error = null
        }

        _addItemBinding = FragmentItemEditBinding.inflate(inflater)

        addItemBinding.apply {
            // move buttons closer
            val constraintSet = ConstraintSet()
            constraintSet.clone(layout)
            constraintSet.connect(buttonsLayout.id, ConstraintSet.TOP, editFieldsLayout.id, ConstraintSet.BOTTOM)
            constraintSet.applyTo(layout)

            editFieldsLayout.addView(itemSelectionBinding.root)

            confirmBtn.text = context.getString(R.string.bnt_add)

            confirmBtn.setOnClickListener {
                setAddItemSectionVisibility(false)

                val name = itemSelectionBinding.input.text.toString()
                if (name.isEmpty()) {
                    itemSelectionBinding.label.error = "Name is required"
                    return@setOnClickListener
                }

                val item = when (addingItemOfType) {
                    ItemType.EXERCISE_TEMPLATE -> exerciseTemplates?.find { it.name == name }
                    ItemType.REST_PERIOD -> restPeriods?.find {it.name == name }
                    else -> null
                }

                if (item == null) {
                    Log.e(TAG, "Failed to retrieve item to be added to set")
                    return@setOnClickListener
                }

                setTemplateEditableItems.add(item)
                exerciseListAdapter.submitList(setTemplateEditableItems.toList())
            }

            cancelBtn.text = context.getString(R.string.btn_cancel)
            cancelBtn.setOnClickListener {
                setAddItemSectionVisibility(false)
            }
        }

        addItemBinding.root.isVisible = false

        return addItemBinding.root
    }


    private fun onAddNewExercise() {
        addingItemOfType = ItemType.EXERCISE_TEMPLATE

        setAddItemSectionVisibility(true)

        itemSelectionBinding.label.setStartIconDrawable(R.drawable.ic_equipment_24)
        itemSelectionBinding.label.hint = "Exercise"

        itemSelectionBinding.input.setAdapter(exerciseTemplatesSelectionAdapter)
        itemSelectionBinding.input.setText(exerciseTemplates?.get(0)?.name ?: "", false) // todo all these ? look kinda funky xd
    }


    private fun onAddNewRestPeriod() {
        addingItemOfType = ItemType.REST_PERIOD

        setAddItemSectionVisibility(true)

        itemSelectionBinding.label.setStartIconDrawable(R.drawable.ic_timer_24)
        itemSelectionBinding.label.hint = "Rest"

        itemSelectionBinding.input.setAdapter(restPeriodsSelectionAdapter)
        itemSelectionBinding.input.setText(restPeriods?.get(0)?.name ?: "", false) // todo all these ? look kinda funky xd
    }

    private fun setAddItemSectionVisibility(visible: Boolean) {
        addItemBinding.root.isVisible = visible
        addExerciseTemplateButton.isVisible = !visible
        addRestPeriodButton.isVisible = !visible

    }


    override fun populateForNewItem(lifecycle: LifecycleCoroutineScope, viewModel: ItemViewModel) {
        setTemplateEditableItems = mutableListOf()
        exerciseListAdapter.submitList(setTemplateEditableItems.toList())

        populateItemsThatCanBeAdded(lifecycle, viewModel)
    }

    override fun populate(
        lifecycle: LifecycleCoroutineScope,
        viewModel: ItemViewModel,
        item: Item
    ) {
        if (item !is SetTemplate) {
            Log.e(TAG, "Can't populate view because item " + item.name +"(" + item.id + ") is not a set template!")
            return
        }

        nameBinding.input.setText(item.name)

        setTemplateEditableItems = item.items.toMutableList()
        exerciseListAdapter.submitList(setTemplateEditableItems.toList())

        populateItemsThatCanBeAdded(lifecycle, viewModel)
    }

    private fun populateItemsThatCanBeAdded(
        lifecycle: LifecycleCoroutineScope,
        viewModel: ItemViewModel
    ) {
        lifecycle.launch {
            viewModel.getItemsByType(ItemType.EXERCISE_TEMPLATE).collect {
                exerciseTemplates = it

                val exerciseTemplatesByName = it.map { ex ->
                    ex.name
                }

                exerciseTemplatesSelectionAdapter =
                    ArrayAdapter(context, R.layout.dropdown_list_item, exerciseTemplatesByName)
            }
        }

        lifecycle.launch {
            viewModel.getItemsByType(ItemType.REST_PERIOD).collect {
                restPeriods = it

                val restPeriodsByName = it.mapNotNull { rest ->
                    if ((rest as RestPeriod).isIntraWorkoutRestPeriod())
                        rest.name
                    else
                        null
                }

                restPeriodsSelectionAdapter =
                    ArrayAdapter(context, R.layout.dropdown_list_item, restPeriodsByName)
            }
        }
    }


    override fun getContent(): ItemContent? {
        if (!validateInput()) {
            return null
        }

        val name = nameBinding.input.text.toString()
        val setItems = setTemplateEditableItems.toList()

        return SetTemplateContent(
            name,
            setItems
        )
    }

    private fun validateInput(): Boolean {
        if (nameBinding.input.text.isNullOrEmpty()) {
            nameBinding.label.error = context.getString(R.string.item_name_err)
            return false
        }

        if (setTemplateEditableItems.size == 0) {
            nameBinding.label.error = "Set needs at least one item"
            return false
        }

        return true
    }
}