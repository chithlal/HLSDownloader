/*
package com.chithu.hlsdownloader

import io.lindstrom.m3u8.model.MasterPlaylist
import io.lindstrom.m3u8.parser.MasterPlaylistParser
import java.nio.file.Path
import java.nio.file.Paths


class HLSParser() {

    fun parseHLSUrl(path: Path){
        val parser = MasterPlaylistParser()

// Parse playlist

// Parse playlist
        val playlist = parser.readPlaylist(path)

// Update playlist version

// Update playlist version
        val updated = MasterPlaylist.builder()
            .from(playlist)
            .version(2)
            .build()

// Write playlist to standard out

// Write playlist to standard out
        println(parser.writePlaylistAsString(updated))
    }
}*/
