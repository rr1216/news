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

// 假设所有新闻的标题互不重复,为了方便查询建一下索引
// 真实的业务千万不要这样处理！！！
data class News(
    // id 默认自增
    var id: Long,
    @Column(unique = true, index = true)
    val title: String,
    val date: String,
    val category: String,
    val author_name: String,
    val thumbnail_pic_s: String,
    val thumbnail_pic_s02: String?,
    val thumbnail_pic_s03: String?,
    val url: String
) : LitePalSupport()
// 为 News添加 LitePal 支持，使之作为一张表存入数据库中


// 记录一下发送网络请求的次数
data class JuHeKEY(
    // id 默认自增
    var id: Long = 0,
    var value: String = "",
    var count: Long = 0
) : LitePalSupport()