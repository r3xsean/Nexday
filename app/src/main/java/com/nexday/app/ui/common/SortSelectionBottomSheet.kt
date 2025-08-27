package com.nexday.app.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nexday.app.R
import com.nexday.app.data.database.TaskSortType
import com.nexday.app.databinding.BottomSheetSortSelectionBinding

/**
 * Bottom sheet dialog for selecting task sort preferences
 * Allows users to choose sort type (Manual, Difficulty, Time) and reverse order
 */
class SortSelectionBottomSheet(
    private val currentSortType: TaskSortType,
    private val currentReverseSort: Boolean,
    private val onSortSelectionChanged: (TaskSortType, Boolean) -> Unit
) : BottomSheetDialogFragment() {
    
    private var _binding: BottomSheetSortSelectionBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSortSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupUI() {
        // Set current sort type
        when (currentSortType) {
            TaskSortType.MANUAL -> binding.manualSortRadio.isChecked = true
            TaskSortType.DIFFICULTY -> binding.difficultySortRadio.isChecked = true
            TaskSortType.TIME -> binding.timeSortRadio.isChecked = true
        }
        
        // Set current reverse sort
        binding.reverseSortSwitch.isChecked = currentReverseSort
        
        // Update reverse switch description based on current selection
        updateReverseSortDescription()
        
        // Listen for sort type changes to update reverse description
        binding.sortTypeRadioGroup.setOnCheckedChangeListener { _, _ ->
            updateReverseSortDescription()
        }
    }
    
    private fun setupClickListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        
        binding.applyButton.setOnClickListener {
            val selectedSortType = when (binding.sortTypeRadioGroup.checkedRadioButtonId) {
                R.id.manualSortRadio -> TaskSortType.MANUAL
                R.id.difficultySortRadio -> TaskSortType.DIFFICULTY
                R.id.timeSortRadio -> TaskSortType.TIME
                else -> TaskSortType.MANUAL
            }
            
            val isReverse = binding.reverseSortSwitch.isChecked
            
            onSortSelectionChanged(selectedSortType, isReverse)
            dismiss()
        }
    }
    
    private fun updateReverseSortDescription() {
        val selectedSortType = when (binding.sortTypeRadioGroup.checkedRadioButtonId) {
            R.id.manualSortRadio -> TaskSortType.MANUAL
            R.id.difficultySortRadio -> TaskSortType.DIFFICULTY
            R.id.timeSortRadio -> TaskSortType.TIME
            else -> TaskSortType.MANUAL
        }
        
        // Update radio button descriptions based on reverse toggle
        val isReverse = binding.reverseSortSwitch.isChecked
        
        when (selectedSortType) {
            TaskSortType.MANUAL -> {
                // Manual sort doesn't really have a reverse, but we can show it
                binding.manualSortRadio.text = if (isReverse) {
                    "Manual (drag to reorder, bottom to top)"
                } else {
                    "Manual (drag to reorder)"
                }
            }
            TaskSortType.DIFFICULTY -> {
                binding.difficultySortRadio.text = if (isReverse) {
                    "Difficulty (easiest first)"
                } else {
                    "Difficulty (hardest first)"
                }
            }
            TaskSortType.TIME -> {
                binding.timeSortRadio.text = if (isReverse) {
                    "Time (latest first)"
                } else {
                    "Time (earliest first)"
                }
            }
        }
    }
}