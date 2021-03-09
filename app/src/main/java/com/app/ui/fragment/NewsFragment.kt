package com.app.ui.fragment

import android.annotation.SuppressLint
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
import com.app.util.showToast
import com.bumptech.glide.Glide
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.Exception
import java.lang.StringBuilder
import kotlin.concurrent.thread

class NewsFragment(private var newType: String) : Fragment() {

    companion object {
        // 两种不同的新闻列表项
        const val ONE_IMAGE = 1
        const val THREE_IMAGES = 3
    }

    private lateinit var newsRecyclerView: RecyclerView

    private lateinit var refresh: SwipeRefreshLayout

    private val newsList = ArrayList<News>()

    @SuppressLint("NotifyDataSetChanged")
    private fun refresh() {
        thread {
            //创建一个子线程，在里面执行网络请求这种耗时操作
            val request =
                Request.Builder()
                    .url("http://v.juhe.cn/toutiao/index?type=" + newType + "&key=" + NewsApplication.KEY)
                    .build()
            val response = OkHttpClient().newCall(request).execute()
            val json = response.body?.string()
            val newsResponse = Gson().fromJson(json, NewsResponse::class.java)
            if (newsResponse != null) {
                try {
                    val data = newsResponse.result.data
                    newsList.clear()
                    newsList.addAll(data)
                    activity?.runOnUiThread {
                        // 切换回UI线程(即 主线程) 执行刷新UI的操作
                        newsRecyclerView.adapter?.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    // 如果接口出了问题，就弹出一个toast
                    activity?.runOnUiThread {
                        // 切换回UI线程(即 主线程) 执行刷新UI的操作
                        "请检查网络接口是否正常".showToast()
                    }
                }

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
        refresh = view.findViewById(R.id.news_refresh)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //
        newsRecyclerView.layoutManager = LinearLayoutManager(NewsApplication.context)
        newsRecyclerView.adapter = NewsAdapter(newsList)

        // 创建页面后立即刷新一次页面上的新闻数据
        refresh()

        refresh.setColorSchemeColors(Color.parseColor("#03A9F4"))
        refresh.setOnRefreshListener {
            thread {
                Thread.sleep(700)
                activity?.runOnUiThread {
                    refresh()
                    refresh.isRefreshing = false
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
            return if (news.thumbnail_pic_s02 == null || news.thumbnail_pic_s03 == null) {
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
                // holder.adapterPosition 到底是什么？ 当前点击的新闻在newsList中的下标
                val intent = Intent(NewsApplication.context, DetailActivity::class.java)
                val selectedNews = newsList[holder.adapterPosition]
                intent.putExtra("news_from=", selectedNews.author_name)
                intent.putExtra("url=", selectedNews.url)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return newsList.size
        }
    }
}