package com.nexday.app.ui.main.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.nexday.app.data.database.DayCategory
import com.nexday.app.data.repository.Task
import com.nexday.app.databinding.FragmentTaskListBinding
import com.nexday.app.ui.common.LevelUpDialog
import com.nexday.app.ui.common.SwipeGestureCallback
import com.nexday.app.ui.common.TaskAdapter
import com.nexday.app.ui.common.TaskDragDropCallback
import com.nexday.app.ui.tasks.AddTaskActivity

/**
 * Base class for all task list fragments (Yesterday, Today, Tomorrow)
 * Provides common functionality and MVVM structure
 */
abstract class BaseTaskListFragment<VB : ViewBinding, VM : ViewModel> : Fragment() {
    
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    
    protected abstract val viewModel: VM
    protected abstract val dayCategory: DayCategory
    
    // TaskAdapter for RecyclerView
    protected lateinit var taskAdapter: TaskAdapter
    private lateinit var dragDropCallback: TaskDragDropCallback
    private lateinit var itemTouchHelper: ItemTouchHelper
    
    // Activity launcher for editing tasks
    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Task was updated successfully, fragments will update via LiveData
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createBinding(inflater, container)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        setupRecyclerView()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * Create the ViewBinding for this fragment
     */
    protected abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    
    /**
     * Set up the basic UI components
     */
    protected open fun setupUI() {
        // Default implementation - can be overridden
    }
    
    /**
     * Set up observers for LiveData/Flow from ViewModel
     */
    protected abstract fun setupObservers()
    
    /**
     * Set up RecyclerView for task list
     */
    protected open fun setupRecyclerView() {
        // Initialize TaskAdapter with callbacks
        taskAdapter = TaskAdapter(
            onTaskClick = { task -> onTaskClick(task) },
            onTaskComplete = { task -> onTaskComplete(task) },
            onTaskUncomplete = { task -> onTaskUncomplete(task) },
            onTaskSwipeLeft = { task -> onTaskSwipeLeft(task) },
            onTaskSwipeRight = { task -> onTaskSwipeRight(task) },
            onTaskReordered = { reorderedTasks -> onTaskReordered(reorderedTasks) },
            onTaskDelete = { task -> onTaskDelete(task) }
        )
        
        // Initialize drag-drop callback that handles both drag and swipe
        dragDropCallback = TaskDragDropCallback(taskAdapter)
        
        // Create ItemTouchHelper and attach to adapter
        itemTouchHelper = ItemTouchHelper(dragDropCallback)
        taskAdapter.attachItemTouchHelper(itemTouchHelper)
        
        // Get RecyclerView from binding and set up
        val recyclerView = getRecyclerView()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
            
            // Attach ItemTouchHelper to RecyclerView
            itemTouchHelper.attachToRecyclerView(this)
        }
    }
    
    /**
     * Handle task list updates and visibility
     */
    protected fun updateTaskList(tasks: List<Task>) {
        // Ignore updates during drag operations to prevent race condition
        if (taskAdapter.isDragInProgress()) {
            // If this is the final update after drag (same task IDs in same order), complete the drag
            val currentTasks = taskAdapter.currentList
            if (tasks.size == currentTasks.size && 
                tasks.zip(currentTasks).all { (new, current) -> new.id == current.id }) {
                taskAdapter.completeDragFinishing()
            }
            return
        }
        
        taskAdapter.submitList(tasks)
        
        // Update empty state visibility
        val isEmpty = tasks.isEmpty()
        getEmptyStateContainer().visibility = if (isEmpty) View.VISIBLE else View.GONE
        getRecyclerView().visibility = if (isEmpty) View.GONE else View.VISIBLE
        
        // Update task count summary
        (getTaskCountSummary() as TextView).apply {
            text = "${tasks.size} ${if (tasks.size == 1) "task" else "tasks"}"
            visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }
    
    /**
     * Get the RecyclerView from the fragment's binding
     * Must be implemented by subclasses to provide their specific RecyclerView
     */
    protected abstract fun getRecyclerView(): RecyclerView
    
    /**
     * Get the empty state container from the fragment's binding
     */
    protected abstract fun getEmptyStateContainer(): View
    
    /**
     * Get the task count summary TextView from the fragment's binding
     */
    protected abstract fun getTaskCountSummary(): View
    
    
    /**
     * Handle task click (long press or click on completed task)
     */
    protected open fun onTaskClick(task: Task) {
        // Launch AddTaskActivity in edit mode
        val intent = Intent(requireContext(), AddTaskActivity::class.java).apply {
            putExtra(AddTaskActivity.EXTRA_EDITING_TASK, task)
        }
        editTaskLauncher.launch(intent)
    }
    
    /**
     * Handle task completion (tap on incomplete task)
     */
    protected abstract fun onTaskComplete(task: Task)
    
    /**
     * Handle task uncomplete (tap on completed task)
     */
    protected abstract fun onTaskUncomplete(task: Task)
    
    /**
     * Handle task swipe left (move towards Yesterday)
     */
    protected abstract fun onTaskSwipeLeft(task: Task)
    
    /**
     * Handle task swipe right (move towards Tomorrow)
     */
    protected abstract fun onTaskSwipeRight(task: Task)
    
    /**
     * Check if task can be swiped left (towards Yesterday)
     */
    protected abstract fun canSwipeLeft(): Boolean
    
    /**
     * Check if task can be swiped right (towards Tomorrow)
     */
    protected abstract fun canSwipeRight(): Boolean
    
    /**
     * Handle task reordering (when user drags tasks)
     */
    protected abstract fun onTaskReordered(reorderedTasks: List<Task>)
    
    /**
     * Handle task deletion (when user taps delete button)
     */
    protected abstract fun onTaskDelete(task: Task)
    
    
    /**
     * Get display name for this day category
     */
    protected fun getDayDisplayName(): String {
        return when (dayCategory) {
            DayCategory.YESTERDAY -> "Yesterday"
            DayCategory.TODAY -> "Today"
            DayCategory.TOMORROW -> "Tomorrow"
        }
    }
    
    /**
     * Show level up celebration dialog
     */
    protected fun showLevelUpDialog(previousLevel: Int, newLevel: Int, onDismiss: () -> Unit = {}) {
        val dialog = LevelUpDialog(
            context = requireContext(),
            previousLevel = previousLevel,
            newLevel = newLevel,
            onDismiss = onDismiss
        )
        dialog.show()
    }
}