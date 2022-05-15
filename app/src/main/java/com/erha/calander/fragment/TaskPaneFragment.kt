package com.erha.calander.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.erha.calander.R
import com.erha.calander.activity.MonitorPagerAdapter
import com.erha.calander.databinding.FragmentTaskPaneBinding
import com.erha.calander.fragment.task.*
import com.erha.calander.util.TinyDB

interface MenuEventCallback {
    fun menuOnclick()
}

class TaskPaneFragment : Fragment(R.layout.fragment_task_pane) {

    private lateinit var binding: FragmentTaskPaneBinding
    private var isLogin = false
    private lateinit var store: TinyDB
    var menuEventCallback: MenuEventCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private val titles = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View {
        binding = FragmentTaskPaneBinding.inflate(inflater, container, false)
        store = TinyDB(binding.root.context)

        val list = ArrayList<Fragment>()
        list.add(TaskTodayFragment())
        titles.add("今天")
        list.add(TaskAllInboxFragment())
        titles.add("收集箱")
        list.add(TaskAllUndoneFragment())
        titles.add("所有未完成")
        list.add(TaskAllDoneFragment())
        titles.add("所有已完成")
        list.add(TaskAllCancelFragment())
        titles.add("所有已放弃")

        binding.viewPager2.adapter = MonitorPagerAdapter(requireActivity(), list)
        binding.viewPager2.isUserInputEnabled = false

        binding.menuButton.apply {
            setOnClickListener {
                menuEventCallback?.menuOnclick()
            }
        }
        gotoPage(0)
        return binding.root
    }

    fun gotoPage(index: Int) {
        if (index >= 0 && index < titles.size) {
            if (binding.viewPager2.currentItem != index) {
                binding.viewPager2.setCurrentItem(index, false)
            }
            binding.title.text = titles[index]
        }
    }
}

class MonitorPagerAdapter(context: FragmentActivity, fragments: List<Fragment>) :
    FragmentStateAdapter(context) {
    var context: Context = context
    var fragments: List<Fragment> = ArrayList()

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun getFragment(position: Int): Fragment {
        return fragments[position]
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    init {
        this.fragments = fragments
    }
}
