package com.erha.calander.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.erha.calander.R
import com.erha.calander.activity.LoginActivity
import com.erha.calander.databinding.FragmentAccountBinding


class AccountFragment : Fragment(R.layout.fragment_account) {
    private lateinit var binding: FragmentAccountBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater, container, false)


        //创建ActivityResultLauncher
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Log.e("registerForActivityResult", it.resultCode.toString())
//            if(it.resultCode == Activity.RESULT_OK){
//                val result = it.data?.getStringExtra("testBean") ?: null
//            }
            }

        binding.loginTest.setOnClickListener { v ->
            run {
                resultLauncher.launch(Intent(activity, LoginActivity::class.java))
            }
        }



        return binding.root
    }

}