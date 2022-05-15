package com.erha.calander.fragment.task

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erha.calander.R
import com.erha.calander.activity.SelectSimpleTaskTimeActivity
import com.erha.calander.adapter.TaskAdapter
import com.erha.calander.dao.TaskDao
import com.erha.calander.databinding.FragmentTaskBinding
import com.erha.calander.model.SimpleTaskWithID
import com.erha.calander.model.TaskStatus
import com.erha.calander.type.EventType
import com.qmuiteam.qmui.recyclerView.QMUIRVItemSwipeAction
import com.qmuiteam.qmui.recyclerView.QMUISwipeAction
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class TaskBaseFragment : Fragment(R.layout.fragment_task) {
    lateinit var binding: FragmentTaskBinding

    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View {
        binding = FragmentTaskBinding.inflate(inflater, container, false)
        binding.recyclerViewQMUILinearLayout.radius =
            resources.getDimensionPixelSize(R.dimen.listview_radius)
        adapter = TaskAdapter(binding.root.context)

        val swipeAction = QMUIRVItemSwipeAction(true, object : QMUIRVItemSwipeAction.Callback() {

            override fun getSwipeDirection(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return QMUIRVItemSwipeAction.SWIPE_LEFT
            }

            override fun onClickAction(
                swipeAction: QMUIRVItemSwipeAction,
                selected: RecyclerView.ViewHolder,
                action: QMUISwipeAction
            ) {
                super.onClickAction(swipeAction, selected, action)
                when (action) {
                    adapter.mFinishAction -> {
                        adapter.getData()[selected.adapterPosition].apply {
                            if (this.status != TaskStatus.FINISHED) {
                                this.status = TaskStatus.FINISHED
                                selected.itemView.findViewById<com.buildware.widget.indeterm.IndeterminateCheckBox>(
                                    R.id.checkBox
                                ).isChecked = true
                                updateTask(this)
                            } else {
                                Log.e(this.javaClass.name, "已完成，不要更新")
                            }
                        }
                        swipeAction.clear()
                    }
                    adapter.mDeleteAction -> {
                        adapter.getData()[selected.adapterPosition].apply {
                            deleteTask(this)
                            adapter.remove(selected.adapterPosition)
                            reloadBackgroundTip()
                        }
                    }
                    adapter.mCalendarAction -> {
                        adapter.getData()[selected.adapterPosition].apply {
                            val i = Intent(
                                binding.root.context,
                                SelectSimpleTaskTimeActivity::class.java
                            )
                            i.putExtra("fromQuickModifySimpleTask", true)
                            i.putExtra("simpleTaskId", this.id)
                            startActivity(i)
                        }
                    }
                }
            }
        })
        swipeAction.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.layoutManager = object : LinearLayoutManager(context) {
            override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
                return RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        binding.recyclerView.adapter = adapter
        adapter.append(TaskDao.getAllSimpleTasks())
        binding.smallTitle.text = getSmallTitleText()
        return binding.root
    }

    abstract fun getSmallTitleText(): String

    final override fun onResume() {
        super.onResume()
        adapter.setData(getDate4ThisPage())
        reloadBackgroundTip()
        EventBus.getDefault().register(this)
    }

    private fun reloadBackgroundTip() {
        if (adapter.getData().isEmpty()) {
            binding.taskListPane.visibility = View.INVISIBLE
            binding.nothingTipLinearLayout.visibility = View.VISIBLE
        } else {
            binding.taskListPane.visibility = View.VISIBLE
            binding.nothingTipLinearLayout.visibility = View.INVISIBLE
        }
    }

    final override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    private var isPostEventChangeMyself = false

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(str: String?) {
        when (str) {
            EventType.EVENT_CHANGE -> {
                //如果是自己发布了事件变化Event，不要刷新，减少开销。
                if (isPostEventChangeMyself) {
                    isPostEventChangeMyself = false
                } else {
                    adapter.setData(getDate4ThisPage())
                    reloadBackgroundTip()
                }
            }
        }
    }

    abstract fun getDate4ThisPage(): ArrayList<SimpleTaskWithID>

//    private fun getDate4ThisPage() : ArrayList<SimpleTaskWithID>{
//        val list = ArrayList<SimpleTaskWithID>()
//        //过滤数据，只关心部分数据
//        for (i in TaskDao.getAllSimpleTasks()){
//            if (i.status == TaskStatus.ONGOING){
//                list.add(SimpleTaskWithID.copy(i))
//            }
//        }
//        return list
//    }

    private fun updateTask(task: SimpleTaskWithID) {
        Thread {
            TaskDao.updateSimpleTask(task)
            isPostEventChangeMyself = true
            EventBus.getDefault().post(EventType.EVENT_CHANGE)
        }.start()
    }

    private fun deleteTask(task: SimpleTaskWithID) {
        Thread {
            TaskDao.removeSimpleTask(task)
            isPostEventChangeMyself = true
            EventBus.getDefault().post(EventType.EVENT_CHANGE)
        }.start()
    }
}
