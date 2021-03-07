package com.app.model

data class NewsResponse(
    val reason: String,
    val result: NewsResult,
    val error_code: Int
)

data class NewsResult(
    val stat: String,
    val data: List<News>
)

data class News(
    val uniquekey: String,
    val title: String,
    val date: String,
    val category: String,
    val author_name: String,
    val thumbnail_pic_s: String,
    val thumbnail_pic_s02: String?,
    val thumbnail_pic_s03: String?,
    val url: String
)