package com.attuned.main;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;

public class MusicLibrary {
    // artists isn't used yet
    private ArrayList<Artist> artists = new ArrayList<Artist>();
    // idTags aren't used yet
    private ArrayList<ID3v24Tag> idTags = new ArrayList<ID3v24Tag>();
    private ArrayList<SongEntry> songEntries = new ArrayList<SongEntry>();

    public void resetMusicLibrary() {
        artists = new ArrayList<Artist>();
        idTags = new ArrayList<ID3v24Tag>();
        songEntries = new ArrayList<SongEntry>();
    }

    public void addSongToLibrary(ID3v24Tag id3Tag, Table songTable, File song) {
        String artist = convertNullToEmpty(id3Tag.getFirst(ID3v24Frames.FRAME_ID_ARTIST));
        String album = convertNullToEmpty(id3Tag.getFirst(ID3v24Frames.FRAME_ID_ALBUM));
        String trackName = convertNullToEmpty(id3Tag.getFirst(ID3v24Frames.FRAME_ID_TITLE));
        if (trackName.equals("")) {
            trackName = song.getName();
        }
        String trackNumber = convertNullToEmpty(id3Tag.getFirst(ID3v24Frames.FRAME_ID_TRACK));

        addSongToTable(trackNumber, artist, album, trackName, song.getAbsolutePath(), songTable);
        songEntries.add(new SongEntry(trackNumber, artist, album, trackName, song.getAbsolutePath()));
        
        Artist currentArtistInLibrary = null;
        for (Artist currentArtistInLoop : artists) {
            if (currentArtistInLoop.equals(artist)) {
                currentArtistInLibrary = currentArtistInLoop;
            }
        }
        if (currentArtistInLibrary == null) {
            currentArtistInLibrary = new Artist(artist);
            artists.add(currentArtistInLibrary);
        }
    }

    public void addSongToTable(String trackNumber, String artist, String album, String trackName, String filename, Table songTable) {
        TableItem item = new TableItem(songTable, SWT.NONE);
        item.setText(0, trackNumber);
        item.setText(1, artist);
        item.setText(2, album);
        item.setText(3, trackName);
        item.setData("filename", filename);
    }

    public ArrayList<Artist> getArtists() {
        return artists;
    }

    public ArrayList<ID3v24Tag> getIdTags() {
        return idTags;
    }

    public ArrayList<SongEntry> getSongEntries() {
        return songEntries;
    }

    public void parseSong(File song, Table songTable) {
        try {
            AudioFile audioFile = AudioFileIO.read(song);
            if (audioFile instanceof MP3File) {
                MP3File currentSongMP3Id = (MP3File) audioFile;
                ID3v24Tag id3Tag = currentSongMP3Id.getID3v2TagAsv24();
                if (id3Tag != null) {
                    addSongToLibrary(id3Tag, songTable, song);
                    idTags.add(id3Tag);
                } else {
                    id3Tag = new ID3v24Tag();
                    id3Tag.setField(FieldKey.TITLE, song.getName());
                    addSongToLibrary(id3Tag, songTable, song);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CannotReadException e) {
            // usually a bad file type (jpg or something), ignore
        } catch (TagException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String convertNullToEmpty(String value) {
        if (value == null || value.equals("null")) {
            return "";
        } else {
            return value;
        }
    }
}
