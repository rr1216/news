# News App
```
git clone https://github.com/hjzf/news.git
```
## 项目描述:
一个Android开发基础学习阶段的新闻App实战项目

![效果图见README.assets/test.png](https://gitee.com/ashinigit/news/raw/main/README.assets/test.png)

![视频播放Demo效果图见README.assets/test_1.png](https://gitee.com/ashinigit/news/raw/main/README.assets/test_1.png)

## 开发思路:
#### 1.设计的缺陷: 只能显示一条新闻(即写在布局文件中的那一条特定的新闻)

解决方案: 用 "一个RecyclerView标签" 代替 "ScrollView-LinearLayout-include这个复杂的嵌套结构"

|效果               |之前的实现方法                |现在的实现方法                                                   |
|                  -|                            -|                                                              -|
|滚动               | ScrollView                  |   RecyclerView                                                |
|垂直方向线性排列    | LinearLayout                |   newsRecyclerView.layoutManager = LinearLayoutManager(this)  |
|展示新闻            | include                    |    newsRecyclerView.adapter = MyAdapter(newsList)             |



#### 2.设计的缺陷: 新闻数据被硬编码在kotlin代码中

解决方案: 调用 聚合数据 https://www.juhe.cn/ 免费提供的新闻API, 通过网络请求获取实时的新闻数据    

步骤一：  添加网络权限  

`   <uses-permission android:name="android.permission.INTERNET"></uses-permission>`

步骤二：  添加http支持(高版本安卓默认仅支持https)  

```
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

`android:networkSecurityConfig="@xml/network_security_config"`

步骤三：  使用Okhttp库和Gson库发送网络请求，并解析返回的JSON数据     

```
dependencies {
    .......
    implementation  'com.squareup.okhttp3:okhttp:4.9.0'
    implementation  'com.google.code.gson:gson:2.8.6'
    
    .......
}
```


## 遇到的问题:
Q: **Context类型参数怎么填？**  
A: 方案一 ，MainActivity 本身就是一个Context，直接填  this 或者  this@MainActivity  
    方案二 ， parent.context  (在Adapter中使用这个方案，parent是一个viewGroup)  
    方案三 ， MyApplication.context  ，这是最终的解决方案



Q: **ViewPager是能够通过触摸左右切换页面的，如何禁用这个功能呢？**  
A: 方案一 ，自定义一个 NoScrollViewPager  



Q: **ViewPager在跨页切换时，会出现多页闪烁的问题**   
A: 方案一 ，使用 ViewPager.setCurrentItem(0, false)       代替     ViewPager.setCurrentItem(0)      即添加一个参数false  
方案二，自定义一个 NewsViewPager ，这个方案用在了 TabLayout与新闻列表ViewPager的联动上  



Q: **如何去掉点击时的水波纹动画效果？**  
A: 基本思路是将背景色设置为透明色。  
去掉TabLayout的水波纹效果app:tabRippleColor="@color/transparent"      
（其中transparent是定义的透明色）      
去掉BottomNavigationView的水波纹效果app:itemRippleColor="@color/transparent"   
去掉标题栏的menu的水波纹效果 :

```
<!--设置菜单项的水波纹颜色为透明 v21以上有效，见src/main/res/values-v21/themes.xml-->
<item name="android:selectableItemBackgroundBorderless">@color/transparent</item>
```



Q: **如何在RecyclerView中展示多种菜单项，比如有三张图片的新闻列表项？**  
A:在适配器中重写getItemViewType(position: Int)函数，判断第position条新闻应该用哪一种列表项展示，返回viewType常量，在 onCreateViewHolder 中 根据不同的viewType加载不同的列表项布局并创建对应类型的ViewHolder ，最后在onBindViewHolder 中根据不同的ViewHolder类型进行数据绑定 。    



Q: **TabLayout的菜单项太多显得很拥挤怎么办？**  
A: 添加属性  app:tabMode="scrollable"  让标签栏能横向滑动      




Q: **如何实现BiliBili安卓客户端的上滑自动隐藏标题栏效果？**  
A: 使用 CoordinatorLayout + AppBarLayout + app:layout_scrollFlags +  app:layout_behavior 实现标题栏与新闻列表的联动效果




Q: **如何播放视频？**
A: android 原生的 VideoView 控件，或者开源库 https://github.com/google/ExoPlayer.git






## BUG记录:

|BUG描述|原因|解决方案|
|   -   | - |   -   |
|Permission denied (missing INTERNET permission?)      | 没有网络权限，加载不了来自网络的图片 |    `<uses-permission android:name="android.permission.INTERNET"></uses-permission>`    |
|lateinit property context has not been initialized    | MyApplication没有注册              |   `android:name=".MyApplication"`   |
|Unable to start activity ComponentInfo{com.app/com.app.MainActivity}: android.os.NetworkOnMainThreadException   |  网络请求不能放在主线程中 | thread { ... }    |
| Only the original thread that created a view hierarchy can touch its views.       | 不能在子线程中刷新UI  | runOnUiThread { ... } |
| have you declared this activity in your AndroidManifest.xml?     | 新活动没有注册  |  `<activity android:name=".DetailActivity"></activity>`   |



### 已完成的内容  

- 新闻类别标签栏
- 底部导航栏 
- home页面的标题栏布局
- home页面中，点击搜索框打开搜索页面(**注意：在代码中实现了搜索页面的内容，但是忘记录制视频**) 
- 用数据库做本地数据缓存,用到了 LitePal 和 Glance


### 未完成的地方
- 使用MVVM架构        









