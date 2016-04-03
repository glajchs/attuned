package com.attuned.main;

import com.google.common.base.Strings;
import org.eclipse.jface.preference.PreferenceStore;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class PreferencesAndSongDB {
    private String defaultPreferencesFolderString = System.getProperty("user.home") + System.getProperty("file.separator")
            + ".config" + System.getProperty("file.separator")
            + "attuned" + System.getProperty("file.separator");
    private AttunedMainWindow attunedMainWindow;
    private Connection songDBConnectionReadOnly;
    // Don't forget to close this one immediatelly after you've written to it, otherwise other instances of Attuned could conflict
    private Connection songDBConnectionWritable;

    public PreferencesAndSongDB(AttunedMainWindow attunedMainWindow) {
        this.attunedMainWindow = attunedMainWindow;
    }

    public void initializeSongDBConnection() {
        try {
            songDBConnectionWritable = DriverManager.getConnection("jdbc:hsqldb:file:" + defaultPreferencesFolderString + "attunedSongDB", "SA", "");
            String songDBCreateString = "create table if not exists songinfos"
                    + " (trackNum varchar(10),"
                    + "  artist varchar(256),"
                    + "  album varchar(256),"
                    + "  trackName varchar(256),"
                    + "  filename varchar(1024))";
            Statement createSongDBStatement = songDBConnectionWritable.createStatement();
            createSongDBStatement.execute(songDBCreateString);
        } catch (SQLException e) {
            // TODO: log here
        } finally {
            try {
                if (songDBConnectionWritable != null && !songDBConnectionWritable.isClosed()) {
                    songDBConnectionWritable.close();
                }
            } catch (SQLException e) {
                // TODO: log here
            }
        }
        try {
            songDBConnectionReadOnly = DriverManager.getConnection("jdbc:hsqldb:file:" + defaultPreferencesFolderString + "attunedSongDB;readonly=true", "SA", "");
        } catch (SQLException e) {
            // TODO: log here
        }
    }

    public void loadPreferences() {
        PreferenceStore preferenceStore = loadPreferenceStore();
        String musicLibraryFolderFromPreferences = preferenceStore.getString("musicLibraryFolder");
        if (!Strings.isNullOrEmpty(musicLibraryFolderFromPreferences)) {
            attunedMainWindow.musicLibraryFolder = musicLibraryFolderFromPreferences;
        }
        int volumePercentPreferences = preferenceStore.getInt("volumePercent");
        if (volumePercentPreferences > 0) {
            attunedMainWindow.volumePercent = volumePercentPreferences;
        }
    }

    private PreferenceStore loadPreferenceStore() {
        File defaultPreferencesFolder = new File(defaultPreferencesFolderString);
        defaultPreferencesFolder.mkdirs();
        PreferenceStore preferenceStore = new PreferenceStore(defaultPreferencesFolder.getAbsolutePath() + System.getProperty("file.separator") + "attuned.preferences");
        try {
            preferenceStore.load();
        } catch (IOException e) {
            // Ignore
        }
        return preferenceStore;
    }

    public void storePreferences() {
        PreferenceStore preferenceStore = loadPreferenceStore();
        preferenceStore.setValue("musicLibraryFolder", attunedMainWindow.musicLibraryFolder);
        preferenceStore.setValue("volumePercent", attunedMainWindow.volumePercent);
        try {
            preferenceStore.save();
        } catch (IOException e) {
            // TODO: log here
        }
    }

    public void loadSongDB() {
        if (songDBConnectionReadOnly == null) {
            return;
        }

        PreparedStatement getSongsFromSongDBStatement;
        try {
            getSongsFromSongDBStatement = songDBConnectionReadOnly.prepareStatement("select * from songInfos");
            ResultSet resultSet = getSongsFromSongDBStatement.executeQuery();
            if (resultSet != null) {
                while (resultSet.next()) {
                    String trackNum = resultSet.getString("trackNum");
                    String artist = resultSet.getString("artist");
                    String album = resultSet.getString("album");
                    String trackName = resultSet.getString("trackName");
                    String filename = resultSet.getString("filename");
                    attunedMainWindow.musicLibrary.addSongToTable(trackNum, artist, album, trackName, filename, attunedMainWindow.songTable);
                }
            }
        } catch (SQLException e) {
            return;
        }
    }

    public void storeSongDB() {
        try {
            songDBConnectionWritable = DriverManager.getConnection("jdbc:hsqldb:file:" + defaultPreferencesFolderString + "attunedSongDB", "SA", "");
        } catch (SQLException e) {
            try {
                if (songDBConnectionWritable != null && !songDBConnectionWritable.isClosed()) {
                    songDBConnectionWritable.close();
                }
            } catch (SQLException exception) {
                // TODO: log here
            } finally {
                return;
            }
        }
        ArrayList<SongEntry> songEntries = attunedMainWindow.musicLibrary.getSongEntries();
        if (songEntries.size() == 0) {
            return;
        }

        PreparedStatement deleteFromSongDBStatement;
        try {
            deleteFromSongDBStatement = songDBConnectionWritable.prepareStatement("delete from songInfos");
            deleteFromSongDBStatement.execute();
        } catch (SQLException e) {
            return;
        }

        PreparedStatement insertIntoSongDBStatement;
        try {
            insertIntoSongDBStatement = songDBConnectionWritable.prepareStatement("insert into songInfos (trackNum, artist, album, trackName, filename) values(?, ?, ?, ?, ?)");

            for (SongEntry songEntry : songEntries) {
                insertIntoSongDBStatement.setString(1, songEntry.trackNumber);
                insertIntoSongDBStatement.setString(2, songEntry.artist);
                insertIntoSongDBStatement.setString(3, songEntry.album);
                insertIntoSongDBStatement.setString(4, songEntry.trackName);
                insertIntoSongDBStatement.setString(5, songEntry.filename);
                insertIntoSongDBStatement.addBatch();
            }
            insertIntoSongDBStatement.executeBatch();
        } catch (SQLException e) {
            return;
        } finally {
            try {
                if (songDBConnectionWritable != null && !songDBConnectionWritable.isClosed()) {
                    songDBConnectionWritable.close();
                }
            } catch (SQLException exception) {
                // TODO: log here
            }
        }

        // TODO: store artists and idtags once we start using those
    }
}
