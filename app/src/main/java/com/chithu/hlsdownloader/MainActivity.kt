package com.chithu.hlsdownloader

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.chithu.hlsdownloader.HLSDownloader.PlayListVariantListener
import com.chithu.hlsdownloader.databinding.ActivityMainBinding
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDownload.setOnClickListener {
            if (!binding.urlText.text.toString().isNullOrEmpty()){
               // downloadVideo(binding.urlText.text.toString())
                val doownloader = HLSDownloader.Builder()
                    .setUrl(binding.urlText.text.toString())
                    .setFilePath("hls_downloads/xyz")
                    .build()

                doownloader.getVariants(object : PlayListVariantListener{
                    override fun onVariantObtained(masterPlayList: HLSDownloader.Parser.MasterPlayList) {
                        super.onVariantObtained(masterPlayList)
                        doownloader.download(context = this@MainActivity, masterPlayList.variantList.first())

                    }

                    override fun onError(message: String) {
                        super.onError(message)
                    }
                })

            }
        }
        playVideo(null)

    }

    fun playVideo(url: String?){
        var player: ExoPlayer? = null

        player = SimpleExoPlayer.Builder(this)
            .setLoadControl(DefaultLoadControl())
            .build()
        // player.setMediaSource()
        player.setMediaSource(buildMediaSource())
        player.prepare()


        // Attach the player to the player view
        binding.player.player = player

        // Start playback
        player?.playWhenReady = true
    }

    private fun buildMediaSource(): HlsMediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, "YourApp")
        )
        val outputDir = File(this.filesDir, "hls_downloads/xyz")
        // URI pointing to the local HLS playlist file
        val uri = Uri.parse("${outputDir.path}/output.m3u8")
        val mediaItem = MediaItem.fromUri(uri)
        // Create HLS media source
        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }

}

