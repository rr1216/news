package com.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

class NewsApplication : Application() {
    companion object {
        // 别担心，用全局的context不会内存泄露
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        // 聚合数据 https://www.juhe.cn/ 免费提供的 头条新闻API,
        // 在运行项目前先去注册，申请一个KEY赋值给这个静态变量
        // 示例  const val KEY = "b4************************037"
        const val KEY =
    }

    override fun onCreate() {
        super.onCreate()
        context = baseContext
    }
}