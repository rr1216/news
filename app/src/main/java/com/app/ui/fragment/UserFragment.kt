package com.app.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.app.R
import com.app.model.JuHeKEY
import org.litepal.LitePal

class UserFragment: Fragment() {

    private lateinit var apiUseCount: TextView
    private lateinit var btn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)
        apiUseCount = view.findViewById(R.id.api_use_count)
        btn = view.findViewById(R.id.test_btn)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        btn.setOnClickListener {
            // 显示聚合数据API的使用次数
            val key = LitePal.find(JuHeKEY::class.java, 1)
            if (key != null) {
                apiUseCount.text = key.count.toString()
            } else {
                apiUseCount.text = "0"
            }
        }
    }
}