package com.gymbud.gymbud.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.gymbud.gymbud.BaseApplication
import com.gymbud.gymbud.databinding.FragmentItemListBinding
import com.gymbud.gymbud.model.ItemType
import com.gymbud.gymbud.ui.viewmodel.ItemViewModel
import com.gymbud.gymbud.ui.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.launch

/**
 * A fragment representing a list of Items.
 */
class ItemListFragment : Fragment() {
    private val navigationArgs: ItemListFragmentArgs by navArgs()

    private val viewModel: ItemViewModel by activityViewModels {
        ItemViewModelFactory(
            (activity?.application as BaseApplication).itemRepository
        )
    }

    private var _binding: FragmentItemListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ItemListRecyclerViewAdapter {
            val action = ItemListFragmentDirections.actionItemListFragmentToItemDetailFragment(it, navigationArgs.itemType)
            findNavController().navigate(action)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // is this needed? https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            //repeatOnLifecycle(Lifecycle.State.STARTED) {

            viewModel.getItemsByType(navigationArgs.itemType).collect{
                it.let {
                    adapter.submitList(it)
                }
            }
        }


        binding.apply {
            title.name.text = when (navigationArgs.itemType) {
                ItemType.EXERCISE -> "Exercises"
                ItemType.EXERCISE_TEMPLATE -> "Exercise Templates"
                ItemType.REST_PERIOD -> "Rest Periods"
                ItemType.SET_TEMPLATE -> "Set Templates"
                ItemType.WORKOUT_TEMPLATE -> "Workout Templates"
                ItemType.PROGRAM_TEMPLATE -> "Programs"
                ItemType.UNKNOWN -> ""
            }

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            addItemFab.setOnClickListener {
                val action = ItemListFragmentDirections.actionItemListFragmentToItemEditFragment(type=navigationArgs.itemType)
                findNavController().navigate(action)
            }
        }
    }
}