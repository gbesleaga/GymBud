package com.example.gymbud.ui.stats

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gymbud.BaseApplication
import com.example.gymbud.databinding.FragmentSessionDetailBinding
import com.example.gymbud.model.WorkoutSession
import com.example.gymbud.ui.SessionExerciseListRecyclerViewAdapter
import com.example.gymbud.ui.viewmodel.StatsViewModel
import com.example.gymbud.ui.viewmodel.StatsViewModelFactory
import com.example.gymbud.utility.getFormattedTimeHHMMSS
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class SessionDetailFragment : Fragment() {
    private val navigationArgs: SessionDetailFragmentArgs by navArgs()

    private val statsViewModel: StatsViewModel by activityViewModels {
        StatsViewModelFactory((activity?.application as BaseApplication).sessionRepository)
    }

    private var _binding: FragmentSessionDetailBinding? = null
    private val binding get() = _binding!!

    private val sessionAdapter = SessionExerciseListRecyclerViewAdapter()

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.US)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionDetailBinding.inflate(inflater, container, false)
        binding.apply{
            resultsRecyclerView.adapter = sessionAdapter

            deleteBtn.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    statsViewModel.removeSession(navigationArgs.id)
                    findNavController().navigateUp()
                }
            }
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            val session = statsViewModel.getSession(navigationArgs.id)
            if (session != null) {
                updateSession(session)
            }
        }
    }


    private fun updateSession(session: WorkoutSession) {
        binding.apply {
            workoutLabel.text = session.getShortName()
            dateValue.text = dateFormat.format(session.getStartTime())
            durationValue.text = getFormattedTimeHHMMSS(session.getDuration() / 1000)
            sessionAdapter.submitList(session.getExerciseSessions())
            notesValue.text = session.notes
        }
    }
}