package com.gymbud.gymbud.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gymbud.gymbud.BaseApplication
import com.gymbud.gymbud.BuildConfig
import com.gymbud.gymbud.R
import com.gymbud.gymbud.data.repository.AppRepository
import com.gymbud.gymbud.databinding.FragmentSettingsBinding
import com.gymbud.gymbud.model.WeightUnit
import com.gymbud.gymbud.ui.viewmodel.ItemViewModel
import com.gymbud.gymbud.ui.viewmodel.ItemViewModelFactory
import com.gymbud.gymbud.utility.BackupException
import com.gymbud.gymbud.utility.createBackup
import com.gymbud.gymbud.utility.distributeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels {
        ItemViewModelFactory(
            (activity?.application as BaseApplication).itemRepository
        )
    }

    private lateinit var appRepository: AppRepository


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        binding.apply {
            // Disable dev options in production
            if (!BuildConfig.DEBUG) {
                devOptions.visibility = View.GONE
            }
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appRepository = (activity?.application as BaseApplication).appRepository

        setupWeightUnitDisplay(appRepository)
        setupKeepScreenOnDuringWorkout(appRepository)
        setupAppThemeDisplay(appRepository)
        setupGuides()
        setupData()
        setupAbout()
        setupDevDisplay(appRepository)
    }


    private fun setupWeightUnitDisplay(appRepository: AppRepository) {
        binding.apply {
            viewLifecycleOwner.lifecycleScope.launch {
                appRepository.weightUnit.collect {
                    when (it) {
                        WeightUnit.KG -> weightUnitGroup.check(R.id.weight_unit_kg)
                        WeightUnit.LB -> weightUnitGroup.check(R.id.weight_unit_lb)
                    }
                }
            }

            weightUnitKg.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        appRepository.updateWeightUnit(WeightUnit.KG)
                    }
                }
            }

            weightUnitLb.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        appRepository.updateWeightUnit(WeightUnit.LB)
                    }
                }
            }
        }
    }


    private fun setupKeepScreenOnDuringWorkout(appRepository: AppRepository) {
        binding.apply {
            viewLifecycleOwner.lifecycleScope.launch {
                appRepository.liveSessionKeepScreenOn.collect {
                    liveSessionKeepScreenOnSwitch.isChecked = it
                }
            }

            liveSessionKeepScreenOnSwitch.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        appRepository.updateLiveSessionKeepScreenOn(liveSessionKeepScreenOnSwitch.isChecked)
                    }
                }
            }
        }
    }


    private fun setupAppThemeDisplay(appRepository: AppRepository) {
        binding.apply {
            viewLifecycleOwner.lifecycleScope.launch {
                appRepository.useDarkTheme.collect {
                    when (it) {
                        true -> themeGroup.check(R.id.theme_dark)
                        false -> themeGroup.check(R.id.theme_light)
                    }
                }
            }

            themeDark.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        appRepository.updateUseDarkTheme(true)
                    }
                }
            }

            themeLight.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        appRepository.updateUseDarkTheme(false)
                    }
                }
            }
        }
    }


    private fun setupGuides() {
        binding.apply {
            overviewGuide.setOnClickListener {
                val action = SettingsFragmentDirections.actionSettingsFragmentToGettingStartedGuideFragment()
                findNavController().navigate(action)
            }

            templatesGuide.text = getString(R.string.templatesGuideLabel) + " >>"
            templatesGuide.setOnClickListener {
                val action = SettingsFragmentDirections.actionSettingsFragmentToCreatingProgramGuideFragment()
                findNavController().navigate(action)
            }

            liveSessionGuide.text = getString(R.string.liveSessionGuideLabel) + " >>"
            liveSessionGuide.setOnClickListener {
                val action = SettingsFragmentDirections.actionSettingsFragmentToTrackingWorkoutGuideFragment()
                findNavController().navigate(action)
            }

            statsGuide.text = getString(R.string.statsGuideLabel) + " >>"
            statsGuide.setOnClickListener {
                val action = SettingsFragmentDirections.actionSettingsFragmentToMonitoringProgressGuideFragment()
                findNavController().navigate(action)
            }
        }
    }


    private fun setupData() {
        binding.createBackupButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        // create backup
                        val bakFile = createBackup(activity?.application as BaseApplication)

                        // distribute backup
                        val intent = distributeFile(bakFile, requireContext(), "application/x-sqlite3", "GymBud backup", "Share File")
                        startActivity(intent)
                    } catch (e: BackupException) {
                        showDataBackupToast(e.message?: "Failed to create backup")
                    } catch (e: Exception) {
                        showDataBackupToast("Failed to create backup")
                        if (BuildConfig.DEBUG) {
                            Log.e("Backup", e.stackTrace.toString())
                        }
                    }
                }
            }
        }
    }


    private fun showDataBackupToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun setupAbout() {
        binding.versionLabel.text = "Version: ${BuildConfig.VERSION_NAME}"
    }


    private fun setupDevDisplay(appRepository: AppRepository) {
        binding.apply {
            resetDbButton.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        viewModel.removeAll()
                    }
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    appRepository.reset()
                }
            }
        }
    }
}