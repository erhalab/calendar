package com.erha.calander.timeline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.erha.calander.R
import com.erha.calander.timeline.model.OrderStatus
import com.erha.calander.timeline.model.TimeLineModel
import com.erha.calander.timeline.utils.VectorDrawableUtils
import com.github.vipulasri.timelineview.TimelineView


interface SingleTimeLineCallback {
    fun onclick(id: Int)
}

class SimpleTimeLineAdapter(private val mFeedList: List<TimeLineModel>) :
    RecyclerView.Adapter<SimpleTimeLineAdapter.TimeLineViewHolder>() {

    private lateinit var mLayoutInflater: LayoutInflater

    override fun getItemViewType(position: Int): Int {
        return TimelineView.getTimeLineViewType(position, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeLineViewHolder {

        if (!::mLayoutInflater.isInitialized) {
            mLayoutInflater = LayoutInflater.from(parent.context)
        }

        return TimeLineViewHolder(
            mLayoutInflater.inflate(R.layout.item_timeline, parent, false),
            viewType
        )
    }

    override fun onBindViewHolder(holder: TimeLineViewHolder, position: Int) {

        val timeLineModel = mFeedList[position]

        when {
            timeLineModel.status == OrderStatus.INACTIVE -> {
                setMarker(holder, R.drawable.ic_marker_inactive, R.color.gray_500)
            }
            timeLineModel.status == OrderStatus.ACTIVE -> {
                setMarker(holder, R.drawable.ic_marker_active, R.color.warningColor)
            }
            else -> {
                setMarker(holder, R.drawable.ic_marker, R.color.successColor)
            }
        }

        if (timeLineModel.date.isNotEmpty()) {
            holder.date.visibility = View.VISIBLE
            holder.date.text = timeLineModel.date
        } else
            holder.date.visibility = View.GONE

        holder.message.text = timeLineModel.message

        holder.root.setOnClickListener {
            timeLineModel.callback?.apply {
                this.onclick(timeLineModel.id)
            }
        }
    }

    private fun setMarker(holder: TimeLineViewHolder, drawableResId: Int, colorFilter: Int) {
        holder.timeline.marker = VectorDrawableUtils.getDrawable(
            holder.itemView.context,
            drawableResId,
            ContextCompat.getColor(holder.itemView.context, colorFilter)
        )
    }

    override fun getItemCount() = mFeedList.size

    inner class TimeLineViewHolder(itemView: View, viewType: Int) :
        RecyclerView.ViewHolder(itemView) {

        val date = itemView.findViewById<TextView>(R.id.text_timeline_date)
        val message = itemView.findViewById<TextView>(R.id.text_timeline_title)
        val timeline = itemView.findViewById<TimelineView>(R.id.timeline)
        val root = itemView.findViewById<LinearLayout>(R.id.timelinePane)

        init {
            timeline.initLine(viewType)
        }
    }

}
