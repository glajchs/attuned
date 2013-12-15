package com.attuned.main;

import java.util.ArrayList;

import org.jaudiotagger.audio.mp3.MP3File;

public class Album {
    String name;
    ArrayList<MP3File> songs = new ArrayList<MP3File>();
    public Album (String name) {
        this.name = name;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Album) {
            if (((Album) obj).name.equalsIgnoreCase(this.name)) {
                return true;
            }
        }
        return false;
    }
}
