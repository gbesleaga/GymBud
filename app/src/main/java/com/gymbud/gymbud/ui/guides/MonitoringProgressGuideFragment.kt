package com.gymbud.gymbud.ui.guides

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gymbud.gymbud.databinding.FragmentMonitoringProgressGuideBinding


class MonitoringProgressGuideFragment : Fragment() {
    private var _binding: FragmentMonitoringProgressGuideBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonitoringProgressGuideBinding.inflate(inflater, container, false)
        return binding.root
    }
}