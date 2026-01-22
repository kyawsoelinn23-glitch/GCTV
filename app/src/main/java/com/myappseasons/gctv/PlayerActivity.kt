package com.myappseasons.gctv

import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private var mediaPlayer: MediaPlayer? = null
    private var currentIndex = 0
    private var playList: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_exo_player)
        hideSystemUI()

        // Keep screen always on (XML + code double sure)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        videoView = findViewById(R.id.videoView)
        videoView.keepScreenOn = true

        val mediaController = android.widget.MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)


        val isAndroidTV = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        playList = intent.getStringArrayListExtra("VIDEO_LIST")
            ?.filter { File(it).exists() } ?: emptyList()

        if (playList.isEmpty()) {
            finish()
            return
        }

        // Prepare first item
        prepareAndPlay(currentIndex, isAndroidTV)
    }

    private fun prepareAndPlay(index: Int, isAndroidTV: Boolean) {
        releasePlayer()

        val path = playList[index]
        val uri = Uri.fromFile(File(path))

        // Use underlying MediaPlayer to get looping, wake lock, attrs
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@PlayerActivity, uri)

            // Keep CPU awake while playing
            setWakeMode(this@PlayerActivity, PowerManager.PARTIAL_WAKE_LOCK)

            // Optional: don't mess with audio focus on TV signage
            val attrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            setAudioAttributes(attrs)

            // Loop each item or you can loop playlist manually
            isLooping = false // we will loop the whole playlist instead

            setOnPreparedListener { mp ->
                // Keep screen on while playing
                mp.setScreenOnWhilePlaying(true)
                videoView.start()
            }

            setOnCompletionListener {
                // Go next; loop playlist
                currentIndex = (currentIndex + 1) % playList.size
                prepareAndPlay(currentIndex, isAndroidTV)
            }

            setOnErrorListener { _, what, extra ->
                // Skip to next on error
                currentIndex = (currentIndex + 1) % playList.size
                prepareAndPlay(currentIndex, isAndroidTV)
                true
            }
        }

        // Simpler, VideoView-only approach (recommended for minimal code):
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { mp ->
            // Ensure wake + screen on for the platform MediaPlayer that VideoView uses internally
            try {
                mp.setScreenOnWhilePlaying(true)
                mp.isLooping = false // we handle playlist looping
            } catch (_: Throwable) {}
            videoView.start()
        }
        videoView.setOnCompletionListener {
            currentIndex = (currentIndex + 1) % playList.size
            prepareAndPlay(currentIndex, isAndroidTV)
        }
        videoView.setOnErrorListener { _, _, _ ->
            currentIndex = (currentIndex + 1) % playList.size
            prepareAndPlay(currentIndex, isAndroidTV)
            true
        }
    }

    private fun hideSystemUI() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Also avoid focus stealing
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        try {
            if (videoView.isPlaying) videoView.stopPlayback()
        } catch (_: Throwable) {}
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
