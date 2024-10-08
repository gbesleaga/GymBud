package com.gymbud.gymbud.ui.live_session

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gymbud.gymbud.BaseApplication
import com.gymbud.gymbud.data.repository.AppRepository
import com.gymbud.gymbud.databinding.FragmentLiveSessionEndBinding
import com.gymbud.gymbud.ui.SessionExerciseListRecyclerViewAdapter
import com.gymbud.gymbud.ui.viewmodel.LiveSessionViewModel
import com.gymbud.gymbud.ui.viewmodel.LiveSessionViewModelFactory
import com.gymbud.gymbud.utility.SoftKeyboardVisibilityListener
import com.gymbud.gymbud.utility.TimeFormatter
import kotlinx.coroutines.launch


class LiveSessionEndFragment : Fragment() {
    private val liveSessionViewModel: LiveSessionViewModel by activityViewModels {
        LiveSessionViewModelFactory(
            (activity?.application as BaseApplication).sessionRepository,
            (activity?.application as BaseApplication).appRepository,
        )
    }

    private lateinit var appRepository: AppRepository

    private var _binding: FragmentLiveSessionEndBinding? = null
    private val binding get() = _binding!!

    private val sessionResultsAdapter = SessionExerciseListRecyclerViewAdapter(showProgression = true, showNotes = false)

    private lateinit var keyboardVisibilityListener: SoftKeyboardVisibilityListener

    private var needsUserConfirmationOnDiscard = true


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        liveSessionViewModel.finish()

        _binding = FragmentLiveSessionEndBinding.inflate(inflater, container, false)

        keyboardVisibilityListener = SoftKeyboardVisibilityListener(binding.root) {
            onKeyboardVisibilityChanged(it)
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardVisibilityListener)

        binding.resultsRecyclerView.adapter = sessionResultsAdapter

        return binding.root
    }


    private fun onKeyboardVisibilityChanged(visible: Boolean) {
        if (visible) {
            binding.resultsContainer.visibility = View.GONE
            binding.notesInput.maxLines = 10
        } else {
            binding.resultsContainer.visibility = View.VISIBLE
            binding.notesInput.maxLines = 2
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appRepository = (activity?.application as BaseApplication).appRepository

        viewLifecycleOwner.lifecycleScope.launch {
            appRepository.weightUnit.collect {
                sessionResultsAdapter.displayWeightUnit = it
            }
        }

        val results = liveSessionViewModel.getResults()
        sessionResultsAdapter.submitList(results)

        binding.apply {
            durationValue.text = TimeFormatter.getFormattedTimeHHMMSS(liveSessionViewModel.getDuration() / 1000)

            if (results.isEmpty()) {
                sessionCompletedLabel.text = "\n\n\n\n That was a short one.. \n\n\n\n We won't record it :)"
                resultsContainer.visibility = View.GONE
                notesInput.visibility = View.GONE
                continueBtn.visibility = View.GONE
                discardBtn.text = "Exit"

                needsUserConfirmationOnDiscard = false
            }

            continueBtn.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch{
                    liveSessionViewModel.saveSession(notesInput.text.toString())
                    val action = LiveSessionEndFragmentDirections.actionLiveSessionEndFragmentToDashboardFragment()
                    findNavController().navigate(action)
                }
            }

            discardBtn.setOnClickListener {
                if (needsUserConfirmationOnDiscard) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Discard Workout Session?")
                        .setPositiveButton("Ok") { _, _ ->
                            discardSession()
                        }
                        .setNegativeButton("Cancel") {_,_ ->
                        }
                        .show()
                } else {
                    discardSession()
                }
            }
        }
    }


    private fun discardSession() {
        viewLifecycleOwner.lifecycleScope.launch{
            liveSessionViewModel.discardSession()
            val action = LiveSessionEndFragmentDirections.actionLiveSessionEndFragmentToDashboardFragment()
            findNavController().navigate(action)
        }
    }


    override fun onStart() {
        super.onStart()

        // set keyboard mode to adjust resize to ensure all fields all visible on a single screen
        //Log.d("keyboard_mode", "adjust resize")
        @Suppress("DEPRECATION")
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }


    override fun onStop() {
        super.onStop()

        // return keyboard mode to previous val
        //Log.d("keyboard_mode", "adjust nothing")
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(keyboardVisibilityListener)
    }
}