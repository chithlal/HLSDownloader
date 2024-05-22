package com.chithu.hlsdownloader

import android.content.Context
import android.util.Log
import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class HLSDownloader(val hlsUrl: String, val outputFilePath: String){
    private var progressListener: ProgressListener? = null
    private var variant: Parser.Variant? = null
    private var baseUrl: String? = null

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private var downloadContext: DownloadContext? = null
    private val downloadSegmentQueMap = HashMap<Int, Parser.Segment>()

    init {
        baseUrl = extractBaseUrl(hlsUrl)
    }

    fun setVariant(variant: Parser.Variant){
        this.variant = variant
    }
    fun setProgressListener(listener: ProgressListener) = apply { this.progressListener = listener }

    class Builder(){
        var url: String = ""
        var outputPath: String = ""

        fun setUrl(url: String) = apply { this.url = url }
        fun setFilePath(outFile: String) = apply { this.outputPath = outFile }


        fun build(): HLSDownloader {
            return HLSDownloader(url,outputPath)
        }
    }

    val downloadListener: DownloadListener = object : DownloadListener{
        override fun taskStart(task: DownloadTask) {
            Log.d(TAG,"Task started ${task.tag}")
        }

        override fun connectTrialStart(
            task: DownloadTask,
            requestHeaderFields: MutableMap<String, MutableList<String>>
        ) {
            Log.d(TAG,"connectTrialStart ${task.tag}")
        }

        override fun connectTrialEnd(
            task: DownloadTask,
            responseCode: Int,
            responseHeaderFields: MutableMap<String, MutableList<String>>
        ) {
            Log.d(TAG,"connectTrialEnd ${task.tag}")
        }

        override fun downloadFromBeginning(
            task: DownloadTask,
            info: BreakpointInfo,
            cause: ResumeFailedCause
        ) {
            Log.d(TAG,"downloadFromBeginning ${task.tag}")
        }

        override fun downloadFromBreakpoint(task: DownloadTask, info: BreakpointInfo) {
            Log.d(TAG,"downloadFromBreakpoint ${task.tag}")
        }

        override fun connectStart(
            task: DownloadTask,
            blockIndex: Int,
            requestHeaderFields: MutableMap<String, MutableList<String>>
        ) {
            Log.d(TAG,"connectStart ${task.tag}")
        }

        override fun connectEnd(
            task: DownloadTask,
            blockIndex: Int,
            responseCode: Int,
            responseHeaderFields: MutableMap<String, MutableList<String>>
        ) {
            Log.d(TAG,"connectEnd ${task.filename}")
        }

        override fun fetchStart(task: DownloadTask, blockIndex: Int, contentLength: Long) {
            Log.d(TAG,"fetchStart ${task.tag}")
        }

        override fun fetchProgress(task: DownloadTask, blockIndex: Int, increaseBytes: Long) {
            Log.d(TAG,"fetchProgress ${task.tag} -> $increaseBytes")
        }

        override fun fetchEnd(task: DownloadTask, blockIndex: Int, contentLength: Long) {
            Log.d(TAG,"fetchEnd ${task.tag}")
        }

        override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: java.lang.Exception?) {
            Log.d(TAG,"taskEnd ${task.tag}")
        }

    }




    fun download(context: Context, variant: Parser.Variant){

        scope.launch {
            val playLIst = downloadPlaylist(baseUrl+variant.path)
           // savePlayListFile(stringToInputStream(playLIst ?: ""), dir = context.filesDir,variant.name?:DEFAULT_PLAYLIST_FILE_NAME,)
            val segments = Parser().parseVariantPlayList(playLIst ?: "")
            createBulkDownloadRequest(context,segments, variant)
        }

    }

    fun stringToInputStream(content: String): ByteArrayInputStream {
        return ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_8))
    }

    private fun createBulkDownloadRequest(
        context: Context,
        segments: List<Parser.Segment>,
        variant: Parser.Variant
    ){
        if (segments.isEmpty()) return
        val dir = File(context.filesDir,outputFilePath)
        val builder = DownloadContext.QueueSet()
            .setParentPathFile(dir)
            .commit()
        builder.bind(baseUrl+variant.pathDir+"/"+ DEFAULT_PLAYLIST_FILE_NAME).addTag(INDEX_TAG, Int.MAX_VALUE)
        for (i in segments.indices) {
            val fullUrl = baseUrl+variant.pathDir+"/"+segments[i].path
            builder.bind(fullUrl).addTag(INDEX_TAG, i)
            downloadSegmentQueMap[i] = segments[i]
        }

        downloadContext = builder.setListener(object : DownloadContextListener{
            override fun taskEnd(
                context: DownloadContext,
                task: DownloadTask,
                cause: EndCause,
                realCause: java.lang.Exception?,
                remainCount: Int
            ) {
               //on task completed
            }

            override fun queueEnd(context: DownloadContext) {
               // onQue completed
            }

        }).build()


        downloadContext?.start(downloadListener,true)
    }

    fun stopDownload(){
        if (downloadContext != null){
            downloadContext?.stop()
        }
    }

    private fun downloadFile(){

    }


    fun getVariants( variantListener: PlayListVariantListener){

        val job = scope.launch {

            val masterContent = if (!hlsUrl.isNullOrEmpty()) downloadPlaylist(hlsUrl!!) else ""
            try {
                if (variantListener != null && !masterContent.isNullOrEmpty()){
                    variantListener.onVariantObtained(Parser().parseMasterPlayList(masterContent))
                } else variantListener.onError("Unable to get variants")
            } catch (e: Exception){
                variantListener.onError(e.toString())
            }
            finally {

            }
        }


    }

    private suspend fun downloadPlaylist(playlistUrl: String, name: String? = null, outputDir: File? =null, shouldSaveFile: Boolean = false): String? {
        val request = Request.Builder()
            .url(playlistUrl)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Unexpected response code: ${response.code}")

            val responseBody = response.body ?: throw IllegalStateException("Response body is null")
            val inputStream = responseBody.byteStream()
            if (shouldSaveFile && outputDir != null && !name.isNullOrEmpty()){
                savePlayListFile(inputStream,outputDir,name )
            }
            val stringBuilder = StringBuilder()
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }

            return stringBuilder.toString()

        }
    }

    private fun savePlayListFile(contentStream: InputStream, dir: File, fileName: String){
        try {
            val playlistFile = File(dir, "${outputFilePath}/$fileName")
            val playlistOutputStream = FileOutputStream(playlistFile)
            contentStream.copyTo(playlistOutputStream)
        } catch ( e: IOException){
            Log.e(TAG, e.toString())
        }

    }

    private fun extractBaseUrl(url: String): String {
        val lastSlashIndex = url.lastIndexOf('/')
        if (lastSlashIndex != -1) {
            return url.substring(0, lastSlashIndex + 1)
        }
        return url
    }

    fun downloadFiles(){

    }

    companion object{
       val httpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS) // Set connection timeout to 5 seconds
                .readTimeout(15, TimeUnit.SECONDS) // Set socket timeout to 5 seconds
                .build()

        const val TAG = "HLS_DOWNLOADER"
        const val INDEX_TAG = 1
        const val DEFAULT_PLAYLIST_FILE_NAME = "output.m3u8"
    }

    class Parser{

        fun parseMasterPlayList(content: String): MasterPlayList{
            val variantList = mutableListOf<Variant>()
            val lines = content.lines()
            var name: String? = null
            var path: String? = null
            var resolution: String? = null
            var bitRate: String? = null

            for (line in lines) {
                if (line.trim().startsWith("#EXT-X-STREAM-INF")) {
                    val attributes = line.substringAfter("#EXT-X-STREAM-INF:").split(",")
                    for (attribute in attributes) {
                        val keyValue = attribute.split("=")
                        val key = keyValue[0].trim()
                        val value = keyValue[1].trim()

                        when (key) {
                            "RESOLUTION" -> resolution = value
                            "NAME" -> name = value
                            "BANDWIDTH" -> bitRate = value
                        }
                    }
                } else if (line.trim().isNotEmpty() && !line.trim().startsWith("#")) {
                    path = line.trim()
                    if (name != null && path != null && resolution != null && bitRate != null) {
                        // Extract the directory name from the path
                        val pathDir = path.substringBefore('/') ?: ""
                        variantList.add(Variant(name, path, resolution, bitRate, pathDir))
                        // Reset values for next variant
                        name = null
                        path = null
                        resolution = null
                        bitRate = null
                    }
                }
            }

            return MasterPlayList(variantList)

        }

        fun parseVariantPlayList(content: String): List<Segment>{
            val segmentList = mutableListOf<Segment>()
            val lines = content.lines()

            var expectSegmentPath = false
            for (line in lines) {
                val trimmedLine = line.trim()
                if (expectSegmentPath) {
                    if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                        val path = trimmedLine
                        val name = path.substringAfterLast("/")
                        segmentList.add(Segment(path, name))
                        expectSegmentPath = false
                    }
                } else if (trimmedLine.startsWith("#EXTINF:")) {
                    expectSegmentPath = true
                }
            }

            return segmentList
        }

        data class MasterPlayList(val variantList: List<Variant>)
        data class Variant(val name: String, val path: String, val resolution: String, val bitRate: String, val pathDir: String)
        data class Segment(val path: String, val name: String)
    }

    interface ProgressListener {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long)
    }

    interface PlayListVariantListener {
        fun onVariantObtained(masterPlayList: Parser.MasterPlayList){

        }
        fun onError(message: String){

        }
    }
}