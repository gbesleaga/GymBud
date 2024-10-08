package com.gymbud.gymbud.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.PanZoom
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYSeries
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gymbud.gymbud.BaseApplication
import com.gymbud.gymbud.R
import com.gymbud.gymbud.databinding.FragmentExerciseProgressionBinding
import com.gymbud.gymbud.model.Exercise
import com.gymbud.gymbud.model.ExerciseProgression
import com.gymbud.gymbud.model.WeightUnit
import com.gymbud.gymbud.ui.viewmodel.ExerciseFiltersViewModel
import com.gymbud.gymbud.ui.viewmodel.ExerciseFiltersViewModelFactory
import com.gymbud.gymbud.ui.viewmodel.ExerciseResultEvaluator
import com.gymbud.gymbud.ui.viewmodel.ProgressionChartViewModel
import com.gymbud.gymbud.ui.viewmodel.ProgressionChartViewModelFactory
import com.gymbud.gymbud.ui.viewmodel.StatsViewModel
import com.gymbud.gymbud.ui.viewmodel.StatsViewModelFactory
import com.gymbud.gymbud.ui.viewmodel.TimeWindowLength
import com.gymbud.gymbud.utility.TimeFormatter
import com.gymbud.gymbud.utility.addDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import java.util.Date
import kotlin.math.roundToLong


private const val CHART_RANGE_LIMIT_PADDING = 5.0 // weight


class ExerciseProgressionFragment : Fragment() {
    private val navigationArgs: ExerciseProgressionFragmentArgs by navArgs()

    private val statsViewModel: StatsViewModel by activityViewModels {
        val app = activity?.application as BaseApplication
        StatsViewModelFactory(app.sessionRepository, app.exerciseTemplateRepository)
    }

    private val exerciseFiltersViewModel: ExerciseFiltersViewModel by activityViewModels {
        ExerciseFiltersViewModelFactory((activity?.application as BaseApplication).programRepository)
    }

    private val chartViewModel: ProgressionChartViewModel by activityViewModels {
        val app = activity?.application as BaseApplication
        ProgressionChartViewModelFactory(app.exerciseRepository)
    }

    private var weightUnit: WeightUnit = WeightUnit.KG

    private var _binding: FragmentExerciseProgressionBinding? = null
    private val binding get() = _binding!!

    private var filterByProgram: TextView? = null
    private var filterByPeriod: TextView? = null

    private var exerciseProgression: ExerciseProgression? = null

    private lateinit var seriesFormatterLight: LineAndPointFormatter
    private lateinit var seriesFormatterDark: LineAndPointFormatter

    private var useDarkTheme = true

    private lateinit var chartPan: PanZoom
    private var timeWindowCenter: Long = -1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseProgressionBinding.inflate(inflater, container, false)

        // todo these values come from an included fragment,
        //  if I try to bind them by assigning an id to the <include/>, the parent constraint layout can't find stuff anymore.
        //  Is there a better way to do this?
        filterByProgram = binding.root.findViewById(R.id.program_value)
        filterByPeriod = binding.root.findViewById(R.id.period_value)

        binding.root.findViewById<FloatingActionButton>(R.id.edit_filters_btn).setOnClickListener {
            val action = ExerciseProgressionFragmentDirections.actionExerciseProgressionFragmentToExerciseFiltersFragment()
            findNavController().navigate(action)
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // line style
        seriesFormatterDark = LineAndPointFormatter(requireContext(), R.xml.exercise_progression_chart_formatter_dark)
        seriesFormatterDark.pointLabelFormatter.vOffset = -30f

        seriesFormatterLight = LineAndPointFormatter(requireContext(), R.xml.exercise_progression_chart_formatter_light)
        seriesFormatterLight.pointLabelFormatter.vOffset = -30f

        binding.apply {
            progressionPlot.clear()

            // show x labels as dates
            progressionPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format = object : Format() {
                override fun format(
                    obj: Any,
                    toAppendTo: StringBuffer,
                    pos: FieldPosition?
                ): StringBuffer? {
                    val label = TimeFormatter.getFormattedDateDDMM(Date(
                        (obj as Number).toFloat().roundToLong()
                    ))
                    return toAppendTo.append(label)
                }

                override fun parseObject(source: String?, pos: ParsePosition?): Any? {
                    return null
                }
            }

            // hide legend
            progressionPlot.legend.isVisible = false

            // hide grid lines
            progressionPlot.graph.rangeGridLinePaint = null
            progressionPlot.graph.domainGridLinePaint = null

            // Y labels
            progressionPlot.rangeStepValue = 5.0

            // pan and zoom
            chartPan = PanZoom.attach(binding.progressionPlot, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.NONE)

            exerciseInput.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, p2, _ ->
                        selectExerciseFromOptions(p2)
                }

            when (chartViewModel.exerciseEvaluatorType) {
                ExerciseResultEvaluator.OneRepMax -> evaluatorSelection.check(R.id.evaluator_option_1rm)
                ExerciseResultEvaluator.MaxWeight -> evaluatorSelection.check(R.id.evaluator_option_max_weight)
            }

            evaluatorSelection.setOnCheckedChangeListener { _, id ->
                when (id) {
                    R.id.evaluator_option_1rm -> chartViewModel.exerciseEvaluatorType = ExerciseResultEvaluator.OneRepMax
                    R.id.evaluator_option_max_weight -> chartViewModel.exerciseEvaluatorType = ExerciseResultEvaluator.MaxWeight
                }

                updateGraph()
            }

            when (chartViewModel.timeWindow) {
                TimeWindowLength.Week -> timeWindowSelection.check(R.id.time_window_option_week)
                TimeWindowLength.Month -> timeWindowSelection.check(R.id.time_window_option_month)
                TimeWindowLength.Year -> timeWindowSelection.check(R.id.time_window_option_year)
            }

            timeWindowSelection.setOnCheckedChangeListener { _, id ->
                when (id) {
                    R.id.time_window_option_week -> chartViewModel.timeWindow = TimeWindowLength.Week
                    R.id.time_window_option_month -> chartViewModel.timeWindow = TimeWindowLength.Month
                    R.id.time_window_option_year -> chartViewModel.timeWindow = TimeWindowLength.Year
                }

                updateGraph()
            }
        }

        val appRepository = (activity?.application as BaseApplication).appRepository
        viewLifecycleOwner.lifecycleScope.launch {
            appRepository.weightUnit.collect {
                weightUnit = it
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            appRepository.useDarkTheme.collect {
                useDarkTheme = it
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            filterByProgram?.text = exerciseFiltersViewModel.getFilterForProgramAsText().first()
            filterByPeriod?.text = exerciseFiltersViewModel.getFilterForPeriodAsText()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (navigationArgs.exerciseId > 0) {
                chartViewModel.selectExercise(navigationArgs.exerciseId)
            }

            // need to set selected exercise text here before populating, otherwise it overwrites dropdown in exerciseInput :(
            val selectedExercise = chartViewModel.getSelectedExercise()
            if (selectedExercise != null) {
                binding.exerciseInput.setText(selectedExercise.name)
            }

            val exerciseOptions = chartViewModel.getExerciseOptions()

            if (exerciseOptions.isNotEmpty()) {
                val exerciseOptionsAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_list_item, exerciseOptions.map { it.name })
                binding.exerciseInput.setAdapter(exerciseOptionsAdapter)
            }

            if (selectedExercise != null) {
                updateProgression(selectedExercise)
            }
        }
    }


    private fun selectExerciseFromOptions(index: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val selectedExercise = chartViewModel.selectExercise(index)
            if (selectedExercise != null) {
                updateProgression(selectedExercise)
            }
        }
    }


    private fun updateProgression(exercise: Exercise) {
        viewLifecycleOwner.lifecycleScope.launch {
            val filters = exerciseFiltersViewModel.getFilters()
            exerciseProgression = statsViewModel.getExerciseProgression(exercise, filters)

            // remove timeWindowCenter constraint after progression changes from initial setup
            timeWindowCenter = if (timeWindowCenter > 0) {
                -1
            } else {
                navigationArgs.timeWindowCenter
            }

            updateGraph()
        }
    }


    private fun updateGraph() {
        if (exerciseProgression == null) {
            return
        }

        val (time, results) = chartViewModel.generateXYSeries(exerciseProgression!!, weightUnit)
        val series: XYSeries = SimpleXYSeries(time.reversed(), results.reversed(), "ExerciseProgressionSeries")
        binding.progressionPlot.clear()

        if (useDarkTheme) {
            binding.progressionPlot.addSeries(series, seriesFormatterDark)
        } else{
            binding.progressionPlot.addSeries(series, seriesFormatterLight)
        }

        if (time.isEmpty() || results.isEmpty()) {
            binding.progressionPlot.redraw()
            return
        }

        // time window (x)
        val timeWindowInDays = when (chartViewModel.timeWindow) {
            TimeWindowLength.Week -> 7
            TimeWindowLength.Month -> 30
            TimeWindowLength.Year -> 365
        }

        val upperBound = if (timeWindowCenter > 0) {
            addDays(timeWindowCenter, timeWindowInDays / 2)
        } else {
            time.first()
        }

        val rangeMinValue = results.minOf { it.toDouble() } - CHART_RANGE_LIMIT_PADDING
        val rangeMaxValue = results.maxOf { it.toDouble() } + CHART_RANGE_LIMIT_PADDING

        binding.progressionPlot.apply {
            setDomainBoundaries(addDays(upperBound.toLong(), -timeWindowInDays), upperBound,  BoundaryMode.FIXED)
            setRangeBoundaries(
                rangeMinValue,
                rangeMaxValue,
                BoundaryMode.FIXED
            )

            // allow some space for horizontal panning
            // for vertical we just use the range boundaries, since there is no vertical panning
            outerLimits.set(
                addDays(time.minOf { it.toLong() }, -timeWindowInDays / 7),
                addDays(time.maxOf { it.toLong() }, timeWindowInDays / 7),
                rangeMinValue,
                rangeMaxValue
            )

            rangeTitle.text = weightUnit.toString()

            redraw()
        }
    }
}