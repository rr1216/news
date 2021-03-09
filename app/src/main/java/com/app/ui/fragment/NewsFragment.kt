package com.app.ui.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.app.model.News
import com.app.NewsApplication
import com.app.model.NewsResponse
import com.app.R
import com.app.ui.activity.DetailActivity
import com.app.util.isNetworkAvailable
import com.app.util.showToast
import com.bumptech.glide.Glide
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.litepal.LitePal
import org.litepal.extension.find
import java.lang.Exception
import java.lang.StringBuilder
import kotlin.concurrent.thread

class NewsFragment(private var newType: String, private var category: String) : Fragment() {
    // newType是拼音 用于url ,  category是汉字 用于sql
    companion object {
        // 两种不同的新闻列表项
        const val ONE_IMAGE = 1
        const val THREE_IMAGES = 3
    }

    private lateinit var newsRecyclerView: RecyclerView

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val newsList = ArrayList<News>()

    private fun getDataFromNetwork(): List<News>? {
        var newsArray: List<News>? = null
        val request =
            Request.Builder()
                .url("http://v.juhe.cn/toutiao/index?type=" + newType + "&key=" + NewsApplication.KEY)
                .build()
        val response = OkHttpClient().newCall(request).execute()
        val json = response.body?.string()
        val newsResponse = Gson().fromJson(json, NewsResponse::class.java)
        if (newsResponse != null) {
            when (newsResponse.error_code) {
                0 -> {
                    try {
                        // 错误码为 0代表成功
                        newsArray = newsResponse.result.data
                        // 把数据次序翻转一下，再保存到本地SQLite数据库中作为缓存
                        LitePal.saveAll(newsArray.asReversed())
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
        return newsArray
    }


    private fun getDataFromDatabase(): List<News> {
        // 由于在保存来自网络的数据时将列表翻转了一次，而插入数据库时id是自增的
        // 因此越旧的新闻 id越小
        // 从数据库中按新闻类型(汉字)读取30条新闻,按id降序排列
        return LitePal.where("category=?", category)
            .order("id desc")
            .limit(30)
            .find()
    }

    private fun refresh() {
        val data = ArrayList<News>()
        // 访问数据库也是耗时操作，放在子线程中
        thread {
            // (2)再加入数据库中的最多30条数据
            data.addAll(getDataFromDatabase())
            // (3)如果网络可用,通过网络请求获取最新的数据
            val networkAvailable = isNetworkAvailable(NewsApplication.context)
            if (networkAvailable) {
                val dataFromNetwork = getDataFromNetwork()
                if (dataFromNetwork != null && dataFromNetwork.isNotEmpty()) {
                    if (data.isEmpty()) {
                        data.addAll(dataFromNetwork)
                    } else {
                        // 如果网络数据没有被缓存过
                        if (data[0].uniquekey != dataFromNetwork[0].uniquekey) {
                            data.addAll(dataFromNetwork)
                        }
                    }
                }
            } else {
                activity?.runOnUiThread {
                    "网络不可用".showToast()
                }
            }
            activity?.runOnUiThread {
                val preSize = newsList.size
                newsList.clear()
                newsRecyclerView.adapter?.notifyItemRangeRemoved(0, preSize)
                newsList.addAll(data)
                newsRecyclerView.adapter?.notifyItemRangeInserted(0, data.size)
            }
        }
    }

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

    inner class NewsAdapter(private val newsList: List<News>) :
        RecyclerView.Adapter<NewsAdapter.BaseViewHolder>() {

        // 新闻列表项基类(无论是哪种类型的新闻列表项，都有标题和描述)
        open inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.news_title)
            val description: TextView = itemView.findViewById(R.id.news_desc)
        }

        // 只有一张图片的新闻列表项
        inner class OneImageViewHolder(itemView: View) : BaseViewHolder(itemView) {
            val image: com.makeramen.roundedimageview.RoundedImageView =
                itemView.findViewById(R.id.news_image)
        }

        // 有三张图片的新闻列表项
        inner class ThreeImagesViewHolder(itemView: View) : BaseViewHolder(itemView) {
            val image1: com.makeramen.roundedimageview.RoundedImageView =
                itemView.findViewById(R.id.news_image_1)
            val image2: com.makeramen.roundedimageview.RoundedImageView =
                itemView.findViewById(R.id.news_image_2)
            val image3: com.makeramen.roundedimageview.RoundedImageView =
                itemView.findViewById(R.id.news_image_3)
        }

        // 判断第position条新闻应该用哪一种列表项展示，即返回viewType
        override fun getItemViewType(position: Int): Int {
            val news = newsList[position]
            // news.thumbnail_pic_s02 == "null"必不可少
            // 因为有些离谱的库会把 null序列化为"null"
            return if (news.thumbnail_pic_s02 == null
                || news.thumbnail_pic_s02 == ""
                || news.thumbnail_pic_s02 == "null"
                || news.thumbnail_pic_s03 == null
                || news.thumbnail_pic_s03 == ""
                || news.thumbnail_pic_s03 == "null"
            ) {
                ONE_IMAGE
            } else {
                THREE_IMAGES
            }
        }

        // 根据不同的viewType加载不同的列表项布局
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            when (viewType) {
                THREE_IMAGES -> {
                    val itemView =
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.news_item_three_images, parent, false)
                    return ThreeImagesViewHolder(itemView)
                }
                ONE_IMAGE -> {
                    val itemView =
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.news_item_one_image, parent, false)
                    return OneImageViewHolder(itemView)
                }
                else -> {
                    // 其它的情况待拓展...暂时用一张图片的方案代替
                    val itemView =
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.news_item_one_image, parent, false)
                    return OneImageViewHolder(itemView)
                }
            }
        }

        // 将数据展示在列表项中
        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            val news = newsList[position]
            // (1)显示新闻标题
            holder.title.text = news.title
            // (2)显示新闻描述:用 author_name和 date两个字段拼接出来
            val stringBuilder = StringBuilder()
            stringBuilder.append(news.author_name)
                .append("      ").append(news.date)
            holder.description.text = stringBuilder.toString()
            // (3)加载新闻图片
            when (holder) {
                is OneImageViewHolder -> {
                    Glide.with(NewsApplication.context).load(news.thumbnail_pic_s)
                        .into(holder.image)
                }
                is ThreeImagesViewHolder -> {
                    Glide.with(NewsApplication.context).load(news.thumbnail_pic_s)
                        .into(holder.image1)
                    Glide.with(NewsApplication.context).load(news.thumbnail_pic_s02)
                        .into(holder.image2)
                    Glide.with(NewsApplication.context).load(news.thumbnail_pic_s03)
                        .into(holder.image3)
                }
            }
            // (4)列表项点击事件
            holder.itemView.setOnClickListener {
                // holder.adapterPosition 到底是什么? 当前点击的新闻在newsList中的下标
                // 什么?  holder.adapterPosition被划线不推荐使用了?
                // 参考  https://blog.csdn.net/guolin_blog/article/details/105606409
                val intent = Intent(NewsApplication.context, DetailActivity::class.java)
                val currentNews = newsList[holder.adapterPosition]
                intent.putExtra("news_from=", currentNews.author_name)
                intent.putExtra("url=", currentNews.url)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return newsList.size
        }
    }
}