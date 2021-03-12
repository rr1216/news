package com.app

import android.annotation.SuppressLint
import android.content.Context
import org.litepal.LitePalApplication

class NewsApplication : LitePalApplication() {
    companion object {
        // 别担心，用全局的context不会内存泄露
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        // 聚合数据 https://www.juhe.cn/ 免费提供的 头条新闻API,
        // 注意:在运行项目前先去 https://www.juhe.cn/注册，申请一个KEY赋值给这个静态变量
        // 否则获取不到新闻数据

        // 示例  const val KEY = "b4************************037"
        const val KEY = "58ff2000704bbf8e31ce031c1b555a33"
    }

    override fun onCreate() {
        super.onCreate()
        context = baseContext
    }
}