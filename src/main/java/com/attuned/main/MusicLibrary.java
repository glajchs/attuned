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
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;

public class MusicLibrary {
    ArrayList<Artist> artists = new ArrayList<Artist>();
    private ArrayList<ID3v24Tag> idTags = new ArrayList<ID3v24Tag>();
    
    public void addSongToLibrary(ID3v24Tag id3Tag, Table songTable, File song) {
        String artist = convertNullToEmpty(id3Tag.getFirst(ID3v24Frames.FRAME_ID_ARTIST));
        String album = convertNullToEmpty(id3Tag.getFirst(ID3v24Frames.FRAME_ID_ALBUM));
        String trackName = convertNullToEmpty(id3Tag.getFirst(ID3v24Frames.FRAME_ID_TITLE));
        String trackNumber = convertNullToEmpty(id3Tag.getFirst(ID3v24Frames.FRAME_ID_TRACK));
        
        TableItem item = new TableItem(songTable, SWT.NONE);
        item.setText(0, trackName);
        item.setText(1, artist);
        item.setText(2, album);
        item.setText(3, trackNumber);
        item.setData("filename", song.getAbsolutePath());
        
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

    public void parseSong(File song, Table songTable) {
        try {
            AudioFile audioFile = AudioFileIO.read(song);
            if (audioFile instanceof MP3File) {
                MP3File currentSongMP3Id = (MP3File) audioFile;
                ID3v24Tag id3Tag = currentSongMP3Id.getID3v2TagAsv24();
                if (id3Tag != null) {
                    addSongToLibrary(id3Tag, songTable, song);
                    idTags.add(id3Tag);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CannotReadException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    public void recurseDirectory(File currentDirectory, Table songTable) {
        File[] directories = currentDirectory.listFiles(new DirectoryFileFilter());
        File[] songs = currentDirectory.listFiles(new NonDirectoryFileFilter());
        Arrays.sort(directories, new FileComparator());
        Arrays.sort(songs, new FileComparator());
        for (File directory : directories) {
            recurseDirectory(directory, songTable);
        }
        for (File song : songs) {
            parseSong(song, songTable);
        }
    }

    class FileComparator implements Comparator<File> {
        public int compare(File file1, File file2) {
            return file1.getName().compareToIgnoreCase(file2.getName());
        }
    }
    class DirectoryFileFilter implements FileFilter {
        public boolean accept(File filename) {
            return filename.isDirectory();
        }
    }
    class NonDirectoryFileFilter implements FileFilter {
        public boolean accept(File filename) {
            return !filename.isDirectory();
        }
    }

}
