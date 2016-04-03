package com.attuned.main;

public class SongEntry {
    String trackNumber;
    String artist;
    String album;
    String trackName;
    String filename;

    public SongEntry(String trackNumber, String artist, String album, String trackName, String filename) {
        this.trackNumber = trackNumber;
        this.artist = artist;
        this.album = album;
        this.trackName = trackName;
        this.filename = filename;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SongEntry) {
            if (((SongEntry) obj).trackNumber.equalsIgnoreCase(this.trackNumber)
                    && ((SongEntry) obj).artist.equalsIgnoreCase(this.artist)
                    && ((SongEntry) obj).album.equalsIgnoreCase(this.album)
                    && ((SongEntry) obj).trackName.equalsIgnoreCase(this.trackName)
                    && ((SongEntry) obj).filename.equalsIgnoreCase(this.filename)) {
                return true;
            }
        }
        return false;
    }
}
