package com.nexday.app.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nexday.app.R
import com.nexday.app.data.database.DifficultyLevel
import com.nexday.app.data.repository.Task
import com.nexday.app.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying tasks with premium UI styling
 * Handles task display, difficulty indicators, and interaction callbacks
 */
class TaskAdapter(
    private val onTaskClick: (Task) -> Unit = {},
    private val onTaskComplete: (Task) -> Unit = {},
    private val onTaskUncomplete: (Task) -> Unit = {},
    private val onTaskSwipeLeft: (Task) -> Unit = {},
    private val onTaskSwipeRight: (Task) -> Unit = {},
    private val onTaskReordered: (List<Task>) -> Unit = {},
    private val onTaskDelete: (Task) -> Unit = {}
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    var isDragEnabled: Boolean = false
        private set
    private var itemTouchHelper: ItemTouchHelper? = null
    private var isDragging: Boolean = false
    private var dragStartList: List<Task>? = null
    private var isDragFinishing: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
        
        // Add drag functionality if enabled
        if (isDragEnabled) {
            // Enable long press to start drag
            holder.itemView.setOnLongClickListener {
                itemTouchHelper?.startDrag(holder)
                true
            }
        } else {
            // Remove long press listener when drag is disabled
            holder.itemView.setOnLongClickListener(null)
        }
    }
    
    fun setDragEnabled(enabled: Boolean) {
        isDragEnabled = enabled
        notifyDataSetChanged() // Refresh to update drag behavior
    }
    
    fun attachItemTouchHelper(itemTouchHelper: ItemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper
    }
    
    fun moveItem(fromPosition: Int, toPosition: Int) {
        val mutableList = currentList.toMutableList()
        if (fromPosition < mutableList.size && toPosition < mutableList.size && fromPosition != toPosition) {
            // Remove the task from its current position
            val task = mutableList.removeAt(fromPosition)
            // Insert it at the new position
            mutableList.add(toPosition, task)
            
            submitList(mutableList) {
                // Notify parent about reordering only if not dragging
                if (!isDragging) {
                    onTaskReordered(mutableList)
                }
            }
        }
    }
    
    fun moveItemVisually(fromPosition: Int, toPosition: Int) {
        val mutableList = currentList.toMutableList()
        if (fromPosition < mutableList.size && toPosition < mutableList.size && fromPosition != toPosition) {
            // Remove the task from its current position
            val task = mutableList.removeAt(fromPosition)
            // Insert it at the new position
            mutableList.add(toPosition, task)
            
            // Only update the visual list, don't trigger repository update
            submitList(mutableList)
        }
    }
    
    fun startDrag() {
        isDragging = true
        dragStartList = currentList.toList() // Save the starting state
    }
    
    fun finalizeDragReorder() {
        isDragging = false
        val finalList = currentList.toList()
        
        // Only notify about reordering if the list actually changed
        if (dragStartList != finalList) {
            isDragFinishing = true
            onTaskReordered(finalList)
            
            // Safety timeout to ensure drag finishing state clears even if repository update is missed
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                isDragFinishing = false
            }, 1000) // 1 second timeout
        }
        
        dragStartList = null
    }
    
    fun isDragInProgress(): Boolean {
        return isDragging || isDragFinishing
    }
    
    fun completeDragFinishing() {
        isDragFinishing = false
    }
    
    fun getItemAt(position: Int): Task {
        return getItem(position)
    }
    
    
    fun handleTaskSwipeLeft(task: Task) {
        onTaskSwipeLeft(task)
    }
    
    fun handleTaskSwipeRight(task: Task) {
        onTaskSwipeRight(task)
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var lastClickTime = 0L
        private val clickDebounceMs = 300L // Prevent rapid clicks within 300ms

        fun bind(task: Task) {
            // Set task title
            binding.taskTitle.text = task.title
            
            // Set task description (show/hide based on availability)
            if (task.description.isNullOrBlank()) {
                binding.taskDescription.visibility = View.GONE
            } else {
                binding.taskDescription.text = task.description
                binding.taskDescription.visibility = View.VISIBLE
            }
            
            // Set scheduled time (show/hide based on availability)
            if (task.scheduledTime == null) {
                binding.taskTimeContainer.visibility = View.GONE
            } else {
                binding.taskTimeContainer.visibility = View.VISIBLE
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                binding.taskTime.text = timeFormat.format(Date(task.scheduledTime))
            }
            
            // Set difficulty indicator color and XP badge
            val difficulty = task.difficulty // Task.difficulty is already DifficultyLevel
            val difficultyColor = getDifficultyColor(difficulty)
            
            binding.difficultyIndicator.backgroundTintList = 
                ContextCompat.getColorStateList(binding.root.context, difficultyColor)
            
            binding.xpBadge.text = "+${difficulty.xpValue} XP"
            
            // Handle completion state
            if (task.isCompleted) {
                binding.completionOverlay.visibility = View.VISIBLE
                binding.completionCheck.visibility = View.VISIBLE
                binding.taskCard.alpha = 0.85f // Slightly less dimmed for better readability
            } else {
                binding.completionOverlay.visibility = View.GONE
                binding.completionCheck.visibility = View.GONE
                binding.taskCard.alpha = 1.0f
            }
            
            // Set click listeners with debouncing to prevent rapid tapping
            binding.taskCard.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > clickDebounceMs) {
                    lastClickTime = currentTime
                    
                    if (!task.isCompleted) {
                        // Tap incomplete task to complete it - immediate response
                        onTaskComplete(task)
                    } else {
                        // Tap completed task to uncomplete it - immediate response
                        onTaskUncomplete(task)
                    }
                }
            }
            
            // Edit button click listener
            binding.editButton.setOnClickListener {
                onTaskClick(task)
            }
            
            // Delete button click listener
            binding.deleteButton.setOnClickListener {
                onTaskDelete(task)
            }
        }
        
        private fun getDifficultyColor(difficulty: DifficultyLevel): Int {
            return when (difficulty) {
                DifficultyLevel.VERY_EASY -> R.color.difficulty_very_easy
                DifficultyLevel.EASY -> R.color.difficulty_easy
                DifficultyLevel.MEDIUM -> R.color.difficulty_medium
                DifficultyLevel.HARD -> R.color.difficulty_hard
                DifficultyLevel.VERY_HARD -> R.color.difficulty_very_hard
            }
        }
    }
}

/**
 * DiffUtil callback for efficient RecyclerView updates
 */
class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem
    }
}

/**
 * ItemTouchHelper callback for handling drag-and-drop
 */
class TaskDragDropCallback(
    private val adapter: TaskAdapter
) : ItemTouchHelper.Callback() {
    
    private var recyclerView: RecyclerView? = null
    
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Enable vertical dragging only when drag is enabled
        val dragFlags = if (adapter.isDragEnabled) {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        } else {
            0
        }
        
        // Keep existing swipe functionality
        val swipeFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        
        return makeMovementFlags(dragFlags, swipeFlags)
    }
    
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition
        
        // Use visual-only move during drag operation
        adapter.moveItemVisually(fromPosition, toPosition)
        return true
    }
    
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Delegate to existing swipe handling
        val position = viewHolder.bindingAdapterPosition
        val task = adapter.getItemAt(position)
        
        when (direction) {
            ItemTouchHelper.LEFT -> adapter.handleTaskSwipeLeft(task)
            ItemTouchHelper.RIGHT -> adapter.handleTaskSwipeRight(task)
        }
    }
    
    override fun isLongPressDragEnabled(): Boolean {
        return adapter.isDragEnabled
    }
    
    override fun isItemViewSwipeEnabled(): Boolean {
        return true // Keep swipe functionality always enabled
    }
    
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                // Store reference to RecyclerView
                recyclerView = viewHolder?.itemView?.parent as? RecyclerView
                
                // Start drag operation
                adapter.startDrag()
                
                // Visual feedback during drag
                viewHolder?.itemView?.apply {
                    alpha = 0.8f
                    scaleX = 1.05f
                    scaleY = 1.05f
                    elevation = 8f
                }
            }
        }
    }
    
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        
        // Finalize drag operation and persist changes immediately
        adapter.finalizeDragReorder()
        
        // Reset visual feedback after drag
        viewHolder.itemView.apply {
            alpha = 1.0f
            scaleX = 1.0f
            scaleY = 1.0f
            elevation = 2f
        }
    }
}