package com.app.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.app.R
import com.app.util.showToast
import java.lang.StringBuilder


class VideoFragment : Fragment() {

    private lateinit var videoView: VideoView
    private lateinit var loadBtn: Button
    private lateinit var playBtn: Button
    private lateinit var pauseBtn: Button
    private lateinit var replayBtn: Button
    private lateinit var videoPath: EditText
    private lateinit var videoPathIsPlaying: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video, container, false)
        videoView = view.findViewById(R.id.video_view)
        loadBtn = view.findViewById(R.id.video_load)
        playBtn = view.findViewById(R.id.video_play)
        pauseBtn = view.findViewById(R.id.video_pause)
        replayBtn = view.findViewById(R.id.video_replay)
        videoPath = view.findViewById(R.id.video_path)
        videoPathIsPlaying = view.findViewById(R.id.video_path_is_playing)
        return view
    }

    private fun loadResource() {
        // 默认的的视频资源路径是 /Pictures/movie.mp4   文件名是movie格式是mp4
        val videoPath = Environment.getExternalStorageDirectory().path + videoPath.text
        videoView.setVideoPath(videoPath)
        // 将当前正在播放的视频路径显示在界面上
        val stringBuilder = StringBuilder("当前播放:")
        stringBuilder.append(videoPath)
        videoPathIsPlaying.text = stringBuilder.toString()

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // 加载新视频资源
        loadBtn.setOnClickListener {
            loadResource()
        }
        // 播放
        playBtn.setOnClickListener {
            if (!videoView.isPlaying) {
                videoView.start()
            }
        }
        // 暂停
        pauseBtn.setOnClickListener {
            if (videoView.isPlaying) {
                videoView.pause()
            }
        }
        // 重新播放
        replayBtn.setOnClickListener {
            if (videoView.isPlaying) {
                videoView.resume()
            }
        }

        // 如果把资源放在SD卡上，需要判断是否有 WRITE_EXTERNAL_STORAGE权限才能播放视频
        activity?.let {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            val result = ContextCompat.checkSelfPermission(it, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                val arr = arrayOf(permission)
                ActivityCompat.requestPermissions(it, arr, 1)
            } else {
                loadResource()
                videoView.start()
            }
        }
        //  显示视频进度控制栏
        videoView.setMediaController(MediaController(activity))
        //  解决只有声音没有图像的问题 https://www.jianshu.com/p/b8c060ce50b0
        videoView.setZOrderOnTop(true)
    }

    // 申请完权限之后
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadResource()
                videoView.start()
            } else {
                "拒绝权限将无法使用程序".showToast()
                activity?.finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放资源
        videoView.suspend()
    }

}