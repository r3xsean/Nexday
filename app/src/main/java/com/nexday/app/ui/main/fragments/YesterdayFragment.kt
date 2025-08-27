package com.nexday.app.ui.main.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.nexday.app.R
import com.nexday.app.data.database.DayCategory
import com.nexday.app.data.repository.Task
import com.nexday.app.databinding.FragmentTaskListBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment displaying yesterday's tasks
 * Shows completed tasks with summary information
 */
@AndroidEntryPoint
class YesterdayFragment : BaseTaskListFragment<FragmentTaskListBinding, TaskListViewModel>() {
    
    override val viewModel: TaskListViewModel by viewModels()
    override val dayCategory: DayCategory = DayCategory.YESTERDAY
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTaskListBinding {
        return FragmentTaskListBinding.inflate(inflater, container, false)
    }
    
    override fun setupUI() {
        super.setupUI()
        
        // Set up empty state for yesterday
        binding.emptyStateIcon.setImageResource(R.drawable.ic_yesterday_24)
        binding.emptyStateTitle.text = "No tasks from yesterday"
        binding.emptyStateMessage.text = "Yesterday's tasks will appear here.\nCompleted tasks help you track your progress!"
    }
    
    override fun setupObservers() {
        // Observe tasks for yesterday with current sort preferences
        viewModel.getTasksForDayWithSort(DayCategory.YESTERDAY).observe(viewLifecycleOwner) { tasks ->
            updateTaskList(tasks)
            
            // Update task count summary with custom message for yesterday
            if (tasks.isNotEmpty()) {
                val completedCount = tasks.count { it.isCompleted }
                val totalCount = tasks.size
                
                (getTaskCountSummary() as TextView).text = if (completedCount == totalCount) {
                    "All $totalCount tasks completed! 🎉"
                } else {
                    "$completedCount of $totalCount tasks completed"
                }
            }
        }
        
        // Observe sort type to enable/disable drag functionality
        viewModel.taskSortType.observe(viewLifecycleOwner) { sortType ->
            val isDragEnabled = sortType == "MANUAL"
            taskAdapter.setDragEnabled(isDragEnabled)
        }
        
        // Observe level up events
        viewModel.levelUpEvent.observe(viewLifecycleOwner) { levelUpResult ->
            levelUpResult?.let {
                showLevelUpDialog(
                    previousLevel = it.previousLevel,
                    newLevel = it.newLevel,
                    onDismiss = {
                        viewModel.clearLevelUpEvent()
                    }
                )
            }
        }
    }
    
    // Implementation of abstract methods
    override fun getRecyclerView(): RecyclerView = binding.tasksRecyclerView
    override fun getEmptyStateContainer(): View = binding.emptyStateContainer
    override fun getTaskCountSummary(): View = binding.taskCountSummary
    
    override fun onTaskComplete(task: Task) {
        viewModel.completeTask(task)
    }
    
    override fun onTaskUncomplete(task: Task) {
        viewModel.uncompleteTask(task)
    }
    
    override fun onTaskSwipeLeft(task: Task) {
        // Yesterday tasks can't be moved further LEFT (no action)
        // This should not happen due to canSwipeLeft() returning false
    }
    
    override fun onTaskSwipeRight(task: Task) {
        // Swipe RIGHT: Move yesterday's task to TODAY
        viewModel.moveTaskRight(task)
    }
    
    override fun canSwipeLeft(): Boolean {
        // Yesterday is leftmost, can't swipe left
        return false
    }
    
    override fun canSwipeRight(): Boolean {
        // Yesterday can swipe right to Today
        return true
    }
    
    override fun onTaskReordered(reorderedTasks: List<Task>) {
        // Handle manual reordering - auto-switches to manual sort
        viewModel.reorderTasks(DayCategory.YESTERDAY, reorderedTasks)
    }
    
    override fun onTaskDelete(task: Task) {
        viewModel.deleteTask(task)
    }
}