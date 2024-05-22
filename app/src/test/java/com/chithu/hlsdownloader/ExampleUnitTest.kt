package com.chithu.hlsdownloader

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {


    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    @Before
    fun before(){

    }
    @Test
    fun hlsParseTest() {
        val content = "#EXTM3U\n" +
                "    #EXT-X-STREAM-INF:BANDWIDTH=154493,RESOLUTION=256x144,NAME=\"Low\",FRAME-RATE=20.0\n" +
                "    144/output.m3u8\n" +
                "    #EXT-X-STREAM-INF:BANDWIDTH=281175,RESOLUTION=426x240,NAME=\"Medium\",FRAME-RATE=20.0\n" +
                "    240/output.m3u8\n" +
                "    #EXT-X-STREAM-INF:BANDWIDTH=654063,RESOLUTION=640x360,NAME=\"High\",FRAME-RATE=25.0\n" +
                "    360/output.m3u8"

        val result = HLSDownloader.Parser().parseMasterPlayList(content)
        assertTrue(result?.variantList?.size == 3)
    }
}