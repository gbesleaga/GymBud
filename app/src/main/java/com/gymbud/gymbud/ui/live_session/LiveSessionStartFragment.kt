package com.gymbud.gymbud.ui.live_session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gymbud.gymbud.BaseApplication
import com.gymbud.gymbud.R
import com.gymbud.gymbud.data.repository.AppRepository
import com.gymbud.gymbud.databinding.FragmentLiveSessionStartBinding
import com.gymbud.gymbud.model.ItemType
import com.gymbud.gymbud.model.WorkoutSessionItemType
import com.gymbud.gymbud.model.WorkoutSessionState
import com.gymbud.gymbud.model.WorkoutTemplate
import com.gymbud.gymbud.ui.SessionExerciseListRecyclerViewAdapter
import com.gymbud.gymbud.ui.viewmodel.ItemViewModel
import com.gymbud.gymbud.ui.viewmodel.ItemViewModelFactory
import com.gymbud.gymbud.ui.viewmodel.LiveSessionViewModel
import com.gymbud.gymbud.ui.viewmodel.LiveSessionViewModelFactory
import kotlinx.coroutines.launch


class LiveSessionStartFragment : Fragment() {
    private val navigationArgs: LiveSessionStartFragmentArgs by navArgs()

    private val liveSessionViewModel: LiveSessionViewModel by activityViewModels {
        LiveSessionViewModelFactory(
            (activity?.application as BaseApplication).sessionRepository,
            (activity?.application as BaseApplication).appRepository,
        )
    }

    private val itemViewModel: ItemViewModel by activityViewModels {
        ItemViewModelFactory(
            (activity?.application as BaseApplication).itemRepository
        )
    }

    private lateinit var appRepository: AppRepository

    private var _binding: FragmentLiveSessionStartBinding? = null
    private val binding get() = _binding!!

    private val previousSessionAdapter = SessionExerciseListRecyclerViewAdapter(showProgression = false, showNotes = true)

    private var sessionStarted = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveSessionStartBinding.inflate(inflater, container, false)
        binding.previousSessionRecyclerView.adapter = previousSessionAdapter
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appRepository = (activity?.application as BaseApplication).appRepository

        viewLifecycleOwner.lifecycleScope.launch {
            appRepository.weightUnit.collect {
                previousSessionAdapter.displayWeightUnit = it
            }
        }

        binding.continueBtn.setOnClickListener {
            val action = when(liveSessionViewModel.getNextItemType()) {
                WorkoutSessionItemType.Exercise ->
                    LiveSessionStartFragmentDirections.actionLiveSessionStartFragmentToLiveSessionExerciseFragment()
                WorkoutSessionItemType.Rest ->
                    LiveSessionStartFragmentDirections.actionLiveSessionStartFragmentToLiveSessionRestFragment()
                else ->
                    null
            }

            if (action != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    liveSessionViewModel.start()
                    sessionStarted = true

                    findNavController().navigate(action)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            liveSessionViewModel.state.collect {
                if (it == WorkoutSessionState.Ready) {
                    binding.continueBtn.isEnabled = true
                    binding.nextItemHint.text = liveSessionViewModel.getNextItemHint()
                } else {
                    binding.continueBtn.isEnabled = false
                    binding.nextItemHint.text = ""
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            itemViewModel.getItem(navigationArgs.workoutId, ItemType.WORKOUT_TEMPLATE).collect {
                binding.workoutLabel.text = it?.name
                liveSessionViewModel.prepare(it as WorkoutTemplate, navigationArgs.programId)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            liveSessionViewModel.previousSession.collect {
                if (it == null) {
                    binding.previousSessionRecyclerView.visibility = View.GONE
                    binding.previousSessionNoneValue.visibility = View.VISIBLE
                    binding.notesContent.text = getString(R.string.no_history_for_workout)
                } else {
                    previousSessionAdapter.submitList(it.getExerciseSessions())
                    binding.previousSessionRecyclerView.visibility = View.VISIBLE
                    binding.previousSessionNoneValue.visibility = View.GONE
                    binding.notesContent.text = it.notes
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()

        if (!sessionStarted) {
            liveSessionViewModel.cancel()
        }
    }
}