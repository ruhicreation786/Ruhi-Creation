package com.ruhitek.downloader

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                ready = true
                // Keep yt-dlp fresh so downloads keep working when sites change
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel.STABLE)
                } catch (e: Exception) {
                    Log.w(TAG, "yt-dlp update skipped", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "init failed", e)
                initError = e.message ?: "Setup failed"
            }
        }.start()
    }

    companion object {
        const val TAG = "RuhiTekDL"
        @Volatile var ready = false
        @Volatile var initError: String? = null
    }
}
