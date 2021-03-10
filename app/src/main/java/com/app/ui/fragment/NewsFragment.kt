package com.app.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.app.model.News
import com.app.NewsApplication
import com.app.model.NewsResponse
import com.app.R
import com.app.model.JuHeKEY
import com.app.ui.adapter.NewsAdapter
import com.app.util.isNetworkAvailable
import com.app.util.showToast
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.litepal.LitePal
import org.litepal.extension.find
import java.lang.Exception
import kotlin.concurrent.thread

class NewsFragment(private var newType: String, private var category: String) : Fragment() {
    // newType是拼音 用于url ,  category是汉字 用于sql


    private lateinit var newsRecyclerView: RecyclerView

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val newsList = ArrayList<News>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)
        newsRecyclerView = view.findViewById(R.id.news_recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.news_refresh)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //
        newsRecyclerView.layoutManager = LinearLayoutManager(NewsApplication.context)
        newsRecyclerView.adapter = NewsAdapter(newsList)

        // 创建页面后立即刷新一次页面上的新闻数据
        refresh()

        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#03A9F4"))
        swipeRefreshLayout.setOnRefreshListener {
            thread {
                Thread.sleep(700)
                activity?.runOnUiThread {
                    refresh()
                    swipeRefreshLayout.isRefreshing = false
                }
            }

        }
    }

    private fun refresh() {
        val networkAvailable = isNetworkAvailable(NewsApplication.context)
        if (networkAvailable) {
            // 如果网络可用,就从网络中获取数据
            writeLog() // 记录一下聚合数据API的使用次数
            thread {
                val dataFromNetwork = getDataFromNetwork()
                if (dataFromNetwork != null && dataFromNetwork.isNotEmpty()) {
                    // 如果成功从网络获取到多条新数据,就立即刷新到UI上
                    activity?.runOnUiThread {
                        val preSize = newsList.size
                        newsList.clear()
                        newsRecyclerView.adapter?.notifyItemRangeChanged(0, preSize)
                        newsList.addAll(dataFromNetwork)
                        newsRecyclerView.adapter?.notifyItemRangeChanged(0, dataFromNetwork.size)
                        // 将数据缓存到数据库中,耗时操作单独开一个子线程
                        thread {
                            insertNewsToDataBase()
                        }
                    }
                } else {
                    // 如果从网络获取到 0条数据，改从本地数据库中获取数据
                    val dataFromDatabase = getDataFromDatabase()
                    activity?.runOnUiThread {
                        val preSize = newsList.size
                        newsList.clear()
                        newsRecyclerView.adapter?.notifyItemRangeChanged(0, preSize)
                        newsList.addAll(dataFromDatabase)
                        newsRecyclerView.adapter?.notifyItemRangeChanged(0, dataFromDatabase.size)
                    }
                }
            }
        } else {
            // 如果网络不可用,只能从数据库中获取数据
            thread {
                val dataFromDatabase = getDataFromDatabase()
                activity?.runOnUiThread {
                    "网络不可用".showToast()
                    val preSize = newsList.size
                    newsList.clear()
                    newsRecyclerView.adapter?.notifyItemRangeChanged(0, preSize)
                    newsList.addAll(dataFromDatabase)
                    newsRecyclerView.adapter?.notifyItemRangeChanged(0, dataFromDatabase.size)
                }
            }
        }
    }

    private fun getDataFromNetwork(): List<News>? {
        var newsArray: List<News>? = null
        val request =
            Request.Builder()
                .url("http://v.juhe.cn/toutiao/index?type=" + newType + "&key=" + NewsApplication.KEY)
                .build()
        try {
            // 发送请求
            val response = OkHttpClient().newCall(request).execute()
            val json = response.body?.string()
            val newsResponse = Gson().fromJson(json, NewsResponse::class.java)
            if (newsResponse != null) {
                when (newsResponse.error_code) {
                    0 -> {
                        try {
                            // 错误码为 0代表成功
                            newsArray = newsResponse.result.data
                        } catch (e: Exception) {
                            activity?.runOnUiThread {
                                // 切换回UI线程(即 主线程) 执行刷新UI的操作
                                "数据获取失败".showToast()
                            }
                        }
                    }
                    10012, 10013 -> {
                        activity?.runOnUiThread {
                            // 切换回UI线程(即 主线程) 执行刷新UI的操作
                            "当前的KEY请求次数超过限制,每天免费次数为100次".showToast()
                        }
                    }
                    else -> {
                        activity?.runOnUiThread {
                            // 切换回UI线程(即 主线程) 执行刷新UI的操作
                            "网络接口异常".showToast()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            activity?.runOnUiThread {
                // 切换回UI线程(即 主线程) 执行刷新UI的操作
                "网络请求失败".showToast()
            }
        }
        return newsArray
    }

    private fun getDataFromDatabase(limitCount: Int = 30): List<News> {
        // 由于在保存来自网络的数据时将列表翻转了一次，而插入数据库时id是自增的
        // 因此越旧的新闻 id越小
        // 从数据库中按新闻类型(汉字)读取最多30条新闻,按id降序排列
        return LitePal.where("category=?", category)
            .order("id desc")
            .limit(limitCount)
            .find()
    }

    @Deprecated(message = "这个函数的设计很糟糕,必须优化一下,以后再说")
    private fun insertNewsToDataBase() {
        // 将 UI中的数据逐条插入到数据库中
        try {
            // 逆序插入的目的是让越早的新闻 id越小
            for (i in newsList.size - 1 downTo 0) {
                // 先在数据库中按标题查一遍
                val news = newsList[i]
                val resultList = LitePal.where("title=?", news.title).find(News::class.java)
                if (resultList.isEmpty()) {
                    // 如果本地数据库中没有同一标题的新闻，就执行插入操作
                    news.save()
                } else {
                    // 如果已经有同一标题的新闻
                    news.id = resultList[0].id
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            activity?.runOnUiThread {
                // 切换回UI线程(即 主线程) 执行刷新UI的操作
                "数据缓存失败".showToast()
            }
        }
    }

    private fun writeLog() {
        // 记录一下聚合数据API的使用次数
        val old = LitePal.find(JuHeKEY::class.java, 1)
        if (old != null) {
            JuHeKEY(1, NewsApplication.KEY, old.count + 1).update(1)
        } else {
            JuHeKEY(1, NewsApplication.KEY, 1).save()
        }
    }
}