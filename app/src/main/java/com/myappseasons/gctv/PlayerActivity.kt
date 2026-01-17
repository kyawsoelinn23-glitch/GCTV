
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

        // Fullscreen content to draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_exo_player)
        hideSystemUI()

        playerView = findViewById(R.id.playerView)

        // Android TV detection
        val isAndroidTV = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        // Default resize mode per platform
        playerView.resizeMode = if (isAndroidTV) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        } else {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }

        // VIDEO_LIST from intent extras
        val videoList = intent.getStringArrayListExtra("VIDEO_LIST")
            ?.filter { File(it).exists() }
            ?: run {
                //Toast.makeText(this, "No playable videos.", Toast.LENGTH_SHORT).show()
                //finish()
                return
            }

        // Player
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Playlist behavior
        player.repeatMode = Player.REPEAT_MODE_ALL
        //player.shuffleModeEnabled = false

        // Build media items
        val mediaItems = videoList.map { path ->
            MediaItem.fromUri(Uri.fromFile(File(path)))
        }
        player.setMediaItems(mediaItems)
        player.prepare()
        player.playWhenReady = true
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onStart() {
        super.onStart()
        if (::player.isInitialized) player.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) player.playWhenReady = false
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