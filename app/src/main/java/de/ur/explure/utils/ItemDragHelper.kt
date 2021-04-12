package de.ur.explure.utils

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.R
import de.ur.explure.adapter.RouteEditAdapter

class ItemDragHelper(private val adapter: RouteEditAdapter) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Specify the directions of movement
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // not needed
    }

    override fun isLongPressDragEnabled(): Boolean {
        // true to allow dragging on long press
        return false
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if (viewHolder.itemViewType != target.itemViewType) {
            return false
        }
        // Notify adapter that an item is moved from x position to y position
        adapter.onRowMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is RouteEditAdapter.ViewHolder) {
                adapter.onRowSelected(viewHolder)

                viewHolder.itemView.elevation = 1f
                viewHolder.itemView.setBackgroundResource(R.drawable.ripple_effect_selected)
            }
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        viewHolder.itemView.elevation = 0f
        viewHolder.itemView.setBackgroundResource(R.drawable.ripple_effect)

        if (viewHolder is RouteEditAdapter.ViewHolder) {
            adapter.onRowClear(viewHolder)
        }
    }

    interface OnDragStartListener {
        fun onStartDrag(viewHolder: RouteEditAdapter.ViewHolder)
    }

    interface CustomDragListener {
        fun onRowMoved(fromPosition: Int, toPosition: Int)
        fun onRowSelected(itemViewHolder: RouteEditAdapter.ViewHolder)
        fun onRowClear(itemViewHolder: RouteEditAdapter.ViewHolder)
    }
}
