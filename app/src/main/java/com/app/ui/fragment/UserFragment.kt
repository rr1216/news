package com.app.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.app.R
import com.app.model.NetWorkLog
import org.litepal.LitePal
import java.lang.StringBuilder

class UserFragment : Fragment() {

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
            // 显示今日调用聚合数据API的次数
            val newsTypes = arrayOf(
                "shehui", "guonei", "guoji", "yule",
                "tiyu", "junshi", "keji", "caijing", "shishang"
            )
            val withinToday =
                "timestamp>=datetime('now','+8 hour','start of day','+0 day') and timestamp<datetime('now','+8 hour','start of day','+1 day')"
            // 在数据库中查询
            val stringBuilder = StringBuilder("\t\t\t\t\t\t")
            for (newsType in newsTypes) {
                val count =
                    LitePal.where("type='${newsType}' and $withinToday")
                        .count(NetWorkLog::class.java)
                stringBuilder.append(newsType).append(" : ").append(count).append("次")
                    .append("\n\t\t\t\t\t\t")
            }
            val count =
                LitePal.where(withinToday).count(NetWorkLog::class.java)
            stringBuilder.append("合计 : ").append(count).append("次")
            // 显示在界面上
            apiUseCount.text = stringBuilder.toString()
        }
    }
}