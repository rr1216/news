package com.app.ui.fragment

import android.annotation.SuppressLint
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
import com.app.model.NetWorkLog
import com.app.ui.adapter.NewsAdapter
import com.app.ui.adapter.NewsAdapter.Companion.FAILED
import com.app.ui.adapter.NewsAdapter.Companion.FINISHED
import com.app.ui.adapter.NewsAdapter.Companion.HAS_MORE
import com.app.util.isNetworkAvailable
import com.app.util.showToast
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.litepal.LitePal
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

// newType是拼音：用于url ;  category是汉字：用于sql
class NewsFragment(private var newType: String, private var category: String) : Fragment() {

    private lateinit var newsRecyclerView: RecyclerView

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val newsList = ArrayList<News>()

    private val newsAdapter = NewsAdapter(newsList, this)

    // 保证 loadNewData() 和 loadCacheData() 这两个函数同一时间只有一个正在执行
    private var isLoading = false

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

        newsRecyclerView.layoutManager = LinearLayoutManager(NewsApplication.context)
        newsRecyclerView.adapter = newsAdapter

        // 功能1:创建页面后立即从网络获取新的数据，并刷新到UI上
        loadNewData()

        // 功能2:下拉刷新
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#03A9F4"))
        swipeRefreshLayout.setOnRefreshListener {
            thread {
                Thread.sleep(700) // 这个延迟0.7秒只是为了实现视觉效果，与逻辑无关
                activity?.runOnUiThread {
                    loadNewData()
                    // 让圆形进度条停下来
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }

        //  功能3:滑动到底部加载更多数据，数据来自本地数据库的缓存
        newsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) return // 不监听向上滑动的动作，只监听向下滑动的动作
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val position = layoutManager.findLastVisibleItemPosition()
                if (position == newsAdapter.itemCount - 1) {
                    // 向下滑动到底部时，立即加载缓存数据
                    loadCacheData()
                }
            }
        })
    }

    private fun loadNewData() {
        if (isLoading) return
        isLoading = true
        val networkAvailable = isNetworkAvailable(NewsApplication.context)
        if (networkAvailable) {
            // 如果网络可用,就从网络中获取数据
            writeLog() //记录网络请求日志
            thread {
                val dataFromNetwork = getDataFromNetwork()
                if (dataFromNetwork != null && dataFromNetwork.isNotEmpty()) {
                    // 如果成功从网络获取到多条新数据,就立即刷新到UI上
                    activity?.runOnUiThread {
                        replaceDataInRecyclerView(dataFromNetwork)
                        // 将数据缓存到数据库中,耗时操作单独开一个子线程
                        thread {
                            insertNewsToDataBase()
                        }
                        newsAdapter.footerViewStatus = HAS_MORE
                        isLoading = false
                    }
                } else {
                    // 如果从网络获取到 0条数据，改从本地数据库中获取数据
                    val dataFromDatabase = getDataFromDatabase(6)
                    // 刷新UI
                    activity?.runOnUiThread {
                        replaceDataInRecyclerView(dataFromDatabase)
                        newsAdapter.footerViewStatus = HAS_MORE
                        isLoading = false
                    }
                }
            }
        } else {
            // 如果网络不可用,只能从数据库中获取数据
            thread {
                val dataFromDatabase = getDataFromDatabase(6)
                // 刷新UI
                activity?.runOnUiThread {
                    "网络不可用".showToast()
                    replaceDataInRecyclerView(dataFromDatabase)
                    newsAdapter.footerViewStatus = HAS_MORE
                    isLoading = false
                }
            }
        }
    }

    fun loadCacheData() {
        if (isLoading) return
        if (newsAdapter.footerViewStatus != HAS_MORE) return
        isLoading = true
        thread {
            try {
                Thread.sleep(1000) // 这个延迟1秒只是为了实现视觉效果，与逻辑无关
                // 注意在缓存时让越早的新闻,id越小
                val newData = getDataFromDatabase(6, minIdInNewsList() - 1)
                if (newData.isEmpty()) {
                    newsAdapter.footerViewStatus = FINISHED
                    activity?.runOnUiThread {
                        newsAdapter.notifyItemChanged(newsAdapter.itemCount - 1)
                        isLoading = false
                    }
                } else {
                    // 将旧数据和新数据合并到一个list中
                    val list = listOf(newsList, newData).flatten()
                    activity?.runOnUiThread {
                        replaceDataInRecyclerView(list)
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                newsAdapter.footerViewStatus = FAILED
                activity?.runOnUiThread {
                    newsAdapter.notifyItemChanged(newsAdapter.itemCount - 1)
                    isLoading = false
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

    private fun getDataFromDatabase(limitCount: Int = 6, maxId: Long = -10): List<News> {
        // 由于在保存来自网络的数据时将列表翻转了一次，而插入数据库时id是自增的
        // 因此越旧的新闻 id越小
        // 从数据库中按新闻类型(汉字)读取最多30条新闻,按id降序排列
        return if (maxId < 0) {
            // 小于 0的id是无意义的，不拼接到 SQL中
            LitePal.where("category=?", category)
                .order("id desc")
                .limit(limitCount)
                .find(News::class.java)
        } else {
            LitePal.where("category=? and id<=?", category, maxId.toString())
                .order("id desc")
                .limit(limitCount)
                .find(News::class.java)
        }
    }

    // 获取当前newsList中所有新闻中id的最小值
    private fun minIdInNewsList(): Long {
        return if (newsList.isNullOrEmpty()) {
            -1
        } else {
            var minId = newsList[0].id
            for (i in newsList.indices) {
                val id = newsList[i].id
                if (id < minId) {
                    minId = id
                }
            }
            minId
        }
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
        // simpleDateFormat 是线程不安全的，但这里只用于主线程就没问题
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        val netWorkLog = NetWorkLog(NewsApplication.KEY, newType, simpleDateFormat.format(Date()))
        netWorkLog.save()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun replaceDataInRecyclerView(newData: List<News>) {
        try {
            newsList.clear()
            newsList.addAll(newData)
            newsAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}