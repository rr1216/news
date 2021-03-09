package com.app.model

import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport

data class NewsResponse(
    val reason: String,
    val result: NewsResult,
    val error_code: Int
)

data class NewsResult(
    val stat: String,
    val data: List<News>
)

// 为 News添加 LitePal 支持，使之作为一张表存入数据库中
data class News(
    @Column(unique = true)
    val uniquekey: String,
    val title: String,
    val date: String,
    val category: String,
    val author_name: String,
    val thumbnail_pic_s: String,
    val thumbnail_pic_s02: String?,
    val thumbnail_pic_s03: String?,
    val url: String
) : LitePalSupport()