package com.nexday.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.nexday.app.R
import kotlin.math.abs
import kotlin.math.min

/**
 * ItemTouchHelper.Callback for handling swipe gestures on task items
 * Left swipe: Move task LEFT (Today → Yesterday)
 * Right swipe: Move task RIGHT (Today → Tomorrow)
 * Context-aware: boundaries prevent invalid moves
 */
class SwipeGestureCallback(
    private val context: Context,
    private val onSwipeLeft: (position: Int) -> Unit,  // Move LEFT (towards Yesterday)
    private val onSwipeRight: (position: Int) -> Unit, // Move RIGHT (towards Tomorrow)
    private val canSwipeLeft: Boolean = true,  // Can move towards Yesterday
    private val canSwipeRight: Boolean = true  // Can move towards Tomorrow
) : ItemTouchHelper.SimpleCallback(0, getSwipeFlags(canSwipeLeft, canSwipeRight)) {

    private val leftArrowIcon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_24)
    private val rightArrowIcon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_forward_24)
    
    private val leftMoveBackground = ColorDrawable()
    private val rightMoveBackground = ColorDrawable()
    
    private val clearPaint = Paint().apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) }
    
    companion object {
        private fun getSwipeFlags(canSwipeLeft: Boolean, canSwipeRight: Boolean): Int {
            var flags = 0
            if (canSwipeLeft) flags = flags or ItemTouchHelper.LEFT
            if (canSwipeRight) flags = flags or ItemTouchHelper.RIGHT
            return flags
        }
    }
    
    init {
        // Blue for both directions - moving between days
        leftMoveBackground.color = ContextCompat.getColor(context, R.color.primary)
        rightMoveBackground.color = ContextCompat.getColor(context, R.color.primary)
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return makeMovementFlags(0, getSwipeFlags(canSwipeLeft, canSwipeRight))
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        when (direction) {
            ItemTouchHelper.LEFT -> onSwipeLeft(position)
            ItemTouchHelper.RIGHT -> onSwipeRight(position)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Determine swipe direction and draw background
        if (dX > 0 && canSwipeRight) {
            // Swiping right (move towards Tomorrow)
            drawRightMoveBackground(c, itemView, dX, itemHeight)
        } else if (dX < 0 && canSwipeLeft) {
            // Swiping left (move towards Yesterday)
            drawLeftMoveBackground(c, itemView, dX, itemHeight)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun drawRightMoveBackground(c: Canvas, itemView: android.view.View, dX: Float, itemHeight: Int) {
        rightMoveBackground.setBounds(
            itemView.left, 
            itemView.top, 
            itemView.left + dX.toInt(), 
            itemView.bottom
        )
        rightMoveBackground.draw(c)

        // Draw right arrow icon (move to Tomorrow)
        rightArrowIcon?.let { icon ->
            val iconMargin = (itemHeight - icon.intrinsicHeight) / 2
            val iconTop = itemView.top + (itemHeight - icon.intrinsicHeight) / 2
            val iconBottom = iconTop + icon.intrinsicHeight
            
            val iconLeft = itemView.left + iconMargin
            val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
            
            if (dX > iconRight + iconMargin) {
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.setTint(Color.WHITE)
                icon.draw(c)
            }
        }
    }

    private fun drawLeftMoveBackground(c: Canvas, itemView: android.view.View, dX: Float, itemHeight: Int) {
        leftMoveBackground.setBounds(
            itemView.right + dX.toInt(), 
            itemView.top, 
            itemView.right, 
            itemView.bottom
        )
        leftMoveBackground.draw(c)

        // Draw left arrow icon (move to Yesterday)
        leftArrowIcon?.let { icon ->
            val iconMargin = (itemHeight - icon.intrinsicHeight) / 2
            val iconTop = itemView.top + (itemHeight - icon.intrinsicHeight) / 2
            val iconBottom = iconTop + icon.intrinsicHeight
            
            val iconRight = itemView.right - iconMargin
            val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
            
            if (abs(dX) > icon.intrinsicWidth + iconMargin) {
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.setTint(Color.WHITE)
                icon.draw(c)
            }
        }
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.7f
    }
}