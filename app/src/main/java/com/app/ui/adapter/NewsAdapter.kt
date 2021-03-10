package com.app.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.app.NewsApplication
import com.app.R
import com.app.model.News
import com.app.ui.activity.DetailActivity
import com.bumptech.glide.Glide
import java.lang.StringBuilder

class NewsAdapter(private val newsList: List<News>) :
    RecyclerView.Adapter<NewsAdapter.BaseViewHolder>() {
    companion object {
        // 两种不同的新闻列表项
        const val ONE_IMAGE = 1
        const val THREE_IMAGES = 3
    }

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
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(NewsApplication.context, intent, null)
        }
    }

    override fun getItemCount(): Int {
        return newsList.size
    }
}
