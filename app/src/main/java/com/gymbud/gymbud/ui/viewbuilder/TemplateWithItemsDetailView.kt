package com.gymbud.gymbud.ui.viewbuilder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import com.gymbud.gymbud.databinding.FragmentBasicItemListBinding
import com.gymbud.gymbud.databinding.LayoutDetailDividerBinding
import com.gymbud.gymbud.databinding.LayoutDetailNameBinding
import com.gymbud.gymbud.model.Item
import com.gymbud.gymbud.model.ItemContainer
import com.gymbud.gymbud.ui.viewmodel.ItemViewModel


private const val TAG = "TemplateWithItemsDV"


class TemplateWithItemsDetailView(
    val context: Context,
    private val onDetailsCallback: (Item) -> Unit
): ItemView {
    private var _nameBinding: LayoutDetailNameBinding? = null
    private val nameBinding get() = _nameBinding!!

    private var _itemListBinding: FragmentBasicItemListBinding? = null
    private val itemListBinding get() = _itemListBinding!!

    private val itemListAdapter =  TemplateWithItemsRecyclerViewAdapter(context, Functionality.Detail)

    init {
        itemListAdapter.setOnItemClickedCallback { item, _ ->
            onDetailsCallback(item)
        }
    }


    override fun inflate(inflater: LayoutInflater): List<View> {
        _nameBinding = LayoutDetailNameBinding.inflate(inflater)

        val divider1 = LayoutDetailDividerBinding.inflate(inflater).root

        _itemListBinding = FragmentBasicItemListBinding.inflate(inflater)
        itemListBinding.root.setPadding(0,0,0,0)
        itemListBinding.recyclerView.adapter = itemListAdapter

        return listOf(
            nameBinding.root,
            divider1,
            itemListBinding.root
        )
    }


    override fun populate(
        lifecycle: Lifecycle,
        lifecycleScope: LifecycleCoroutineScope,
        viewModel: ItemViewModel,
        item: Item
    ) {
        if (item !is ItemContainer) {
            //Log.e(TAG, "Can't populate view because item " + item.name +"(" + item.id + ") is not a Template with Items (ItemContainer)!")
            return
        }

        nameBinding.name.text = item.name

        itemListAdapter.submitList(item.items)
    }
}

