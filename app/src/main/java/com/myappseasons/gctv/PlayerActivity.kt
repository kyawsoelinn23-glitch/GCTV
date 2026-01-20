package com.myappseasons.gctv

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_exo_player)
        hideSystemUI()


        playerView = findViewById(R.id.playerView)

        val isAndroidTV = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        //resizeMode
        playerView.post {
            playerView.setResizeMode(
                if (isAndroidTV)
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                else
                    AspectRatioFrameLayout.RESIZE_MODE_FILL
            )
        }

        val videoList = intent.getStringArrayListExtra("VIDEO_LIST")
            ?.filter { File(it).exists() }
            ?: return

        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            exo.repeatMode = Player.REPEAT_MODE_ALL
            exo.setMediaItems(
                videoList.map { path ->
                    MediaItem.fromUri(Uri.fromFile(File(path)))
                }
            )
            exo.prepare()
            exo.playWhenReady = false
        }
    }

    private fun hideSystemUI() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) player.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onStop() {
        super.onStop()
        if (::player.isInitialized) player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) player.release()
    }

}
