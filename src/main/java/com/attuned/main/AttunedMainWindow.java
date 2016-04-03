package com.attuned.main;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import com.google.common.base.Strings;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.gstreamer.ClockTime;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.State;
import org.gstreamer.elements.PlayBin2;

import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.Provider;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.id3.AbstractTagItem;

public class AttunedMainWindow {
    private Table songTable;
    private String musicLibraryFolder;
    public PlayBin2 theSoundPlayer = null;
    private Button play;
    private static Display display;
    private Shell shell;
    private Button volumeButton;
    public Scale trackSeek;
    private boolean manualPlaybackEvent = true;
    private final Provider provider = Provider.getCurrentProvider(false);
    private int currentPosition;
    private ScheduledFuture<?> trackSeekFuture;
    private boolean repeat = false;
    private boolean shuffle = false;
    private List<Integer> shuffleHistory = new ArrayList<Integer>();
    private Integer shufflePointer = 0;
    private int volumePercent;

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

    private void quit(Shell shell) {
        try {
            if (provider != null) {
                provider.reset();
            }
        } finally {
            try {
                if (shell != null && !shell.isDisposed()) {
                    shell.close();
                }
            } finally {
                try {
                    if (display != null && !display.isDisposed()) {
                        display.dispose();
                    }
                } finally {
                    System.exit(0);
                }
            }
        }
    }

    private void loadPreferences() {
        PreferenceStore preferenceStore = loadPreferenceStore();
        String musicLibraryFolderFromPreferences = preferenceStore.getString("musicLibraryFolder");
        if (!Strings.isNullOrEmpty(musicLibraryFolderFromPreferences)) {
            musicLibraryFolder = musicLibraryFolderFromPreferences;
        }
        int volumePercentPreferences = preferenceStore.getInt("volumePercent");
        if (volumePercentPreferences > 0) {
            volumePercent = volumePercentPreferences;
        }
    }

    private PreferenceStore loadPreferenceStore() {
        String defaultPreferencesFolderString = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".config" + System.getProperty("file.separator") + "attuned" + System.getProperty("file.separator");
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

    private void storePreferences() {
        PreferenceStore preferenceStore = loadPreferenceStore();
        preferenceStore.setValue("musicLibraryFolder", musicLibraryFolder);
        preferenceStore.setValue("volumePercent", volumePercent);
        try {
            preferenceStore.save();
        } catch (IOException e) {
            // TODO: log here
        }
    }

    private void setupMenus(final Shell shell) {
        Menu menuBar = new Menu(shell, SWT.BAR);
        MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        fileMenuHeader.setText("&File");

        Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
        fileMenuHeader.setMenu(fileMenu);

        MenuItem fileOpenFolderItem = new MenuItem(fileMenu, SWT.PUSH);
        fileOpenFolderItem.setText("&Open");
        fileOpenFolderItem.setAccelerator(SWT.CTRL | 'o');

        MenuItem fileQuitItem = new MenuItem(fileMenu, SWT.PUSH);
        fileQuitItem.setText("&Quit");
        fileQuitItem.setAccelerator(SWT.CTRL | 'q');

        MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText("&Help");

        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        helpMenuHeader.setMenu(helpMenu);

        MenuItem helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
        helpAboutItem.setText("&About");

        fileQuitItem.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                quit(shell);
            }
            public void widgetDefaultSelected(SelectionEvent event) {
                quit(shell);
            }
        });
        fileOpenFolderItem.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                openMusicLibraryFolder();
                System.out.println("TODO: open music library folder.");
            }
            public void widgetDefaultSelected(SelectionEvent event) {
                openMusicLibraryFolder();
                System.out.println("TODO: open music library folder.");
            }
            private void openMusicLibraryFolder() {
                DirectoryDialog directoryOpenDialog = new DirectoryDialog(shell);

                directoryOpenDialog.setFilterPath(musicLibraryFolder);
                directoryOpenDialog.setText("Music Library Folder");
                directoryOpenDialog.setMessage("Select a directory");
                String directoryString = directoryOpenDialog.open();
                if (directoryString != null) {
                    File directory = new File(directoryString);
                    musicLibraryFolder = directoryString;
                    storePreferences();
                    readMusicLibraryFromFolder(directory);
                }
            }
        });

        shell.setMenuBar(menuBar);
    }


    public AttunedMainWindow(final Display display) {
        shell = new Shell(display);
        try {
        loadPreferences();
        if (musicLibraryFolder == null) {
            musicLibraryFolder = "/media/scott/Shared/Music/Amazon MP3";
            storePreferences();
        }
        if (volumePercent == 0) {
            volumePercent = 10;
            storePreferences();
        }
        setupMenus(shell);
        shell.setText("Attuned");
//        shell.setSize(300, 500);

        Logger logger = Logger.getLogger("org.jaudiotagger");
        logger.setLevel(Level.WARNING);

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 6;
        shell.setLayout(gridLayout);

        previous = new Button(shell, SWT.PUSH);
        previous.setImage(new Image(
                display,
                Class.class
                        .getResourceAsStream("/crystal_icons/backward/gif/backward-24.gif")));
        previous.setLayoutData(new GridData(GridData.BEGINNING,
                GridData.BEGINNING, false, false));
        previous.setSize(16, 16);
        previous.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent arg0) {
                previous();
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
                previous();
            }
        });

        play = new Button(shell, SWT.PUSH);
        play.setImage(new Image(display, Class.class
                .getResourceAsStream("/crystal_icons/play/gif/play-24.gif")));
        play.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false,
                false));
        play.setSize(16, 16);
        play.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                playSong();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                playSong();
            }

        });

        stop = new Button(shell, SWT.PUSH);
        stop.setImage(new Image(display, Class.class
                .getResourceAsStream("/crystal_icons/stop/gif/stop-24.gif")));
        stop.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false,
                false));
        stop.setSize(16, 16);
        stop.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent arg0) {
                stopSong();
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
                stopSong();
            }
        });

        next = new Button(shell, SWT.PUSH);
        next.setImage(new Image(
                display,
                Class.class
                        .getResourceAsStream("/crystal_icons/forward/gif/forward-24.gif")));
        next.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false,
                false));
        next.setSize(16, 16);
        next.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent arg0) {
                next();
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
                next();
            }
        });

        trackSeek = new Scale(shell, SWT.HORIZONTAL);
        trackSeek.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
                true, false));
        trackSeek.setPageIncrement(5);
        trackSeek.setMinimum(0);
        trackSeek.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                doTrackSeek(e);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                doTrackSeek(e);
            }

            private void doTrackSeek(SelectionEvent e) {
                currentPosition = trackSeek.getSelection();
                theSoundPlayer.seek(ClockTime.fromSeconds(currentPosition));
            }
        });

        volumeButton = new Button(shell, SWT.PUSH);
        volumeButton
                .setImage(new Image(
                        display,
                        Class.class
                                .getResourceAsStream("/kde_crystal_diamond_icons/16x16/actions/player_volume.png")));
        volumeButton.setLayoutData(new GridData(GridData.END, GridData.FILL,
                false, false));
        volumeButton.setSize(16, 16);

        class VolumeSelectionListener implements SelectionListener {
            private Scale volumeSlider;

            public VolumeSelectionListener() {
            }

            public void widgetSelected(SelectionEvent e) {
                createVolumeSlider(e);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                createVolumeSlider(e);
            }

            private void createVolumeSlider(SelectionEvent e) {
                Shell volumeShell = new Shell(shell, SWT.NO_FOCUS | SWT.NO_TRIM);
                volumeShell.setLayout(new RowLayout());

                class VolumeShellFocusListener implements FocusListener {
                    Shell myVolumeshell;

                    public VolumeShellFocusListener(Shell myVolumeshell) {
                        this.myVolumeshell = myVolumeshell;
                    }

                    public void focusGained(FocusEvent e) {
                    }

                    public void focusLost(FocusEvent e) {
                        myVolumeshell.close();
                    }

                }

                volumeSlider = new Scale(volumeShell, SWT.VERTICAL);
                Point volumeSliderSize = new Point(150, 20);
                volumeSlider.setMinimum(0);
                volumeSlider.addFocusListener(new VolumeShellFocusListener(
                        volumeShell));
                volumeSlider.setMaximum(100);
                volumeSlider.setIncrement(2);
                volumeSlider.setPageIncrement(10);
                volumeSlider.setSelection(100 - theSoundPlayer.getVolumePercent());
                volumeSlider.addSelectionListener(new SelectionListener() {
                    public void widgetSelected(SelectionEvent e) {
                        handleVolumeChange(e);
                    }

                    public void widgetDefaultSelected(SelectionEvent e) {
                        handleVolumeChange(e);
                    }

                    private void handleVolumeChange(SelectionEvent e) {
                        volumePercent = (100 - volumeSlider.getSelection());
                        theSoundPlayer.setVolumePercent(volumePercent);
                        storePreferences();
                    }
                });
                Point volumeButtonLocation = ((Button) e.getSource())
                        .toDisplay(0, 0);
                Point finalVolumeSliderLocation = volumeButtonLocation;
                if (volumeButtonLocation.y - volumeSliderSize.y - 50 >= 0) {
                    finalVolumeSliderLocation.y = finalVolumeSliderLocation.y
                            - volumeSliderSize.y;
                } else {
                    finalVolumeSliderLocation.y = finalVolumeSliderLocation.y
                            + volumeSliderSize.y;
                }

                volumeShell.setLocation(finalVolumeSliderLocation);
                volumeShell.pack();
                volumeShell.open();
            }
        }

        volumeButton.addSelectionListener(new VolumeSelectionListener());

        Composite tableComposite = new Composite(shell, SWT.NONE);
        songTable = new Table(tableComposite, SWT.MULTI | SWT.BORDER
            | SWT.FULL_SELECTION);
        songTable.setLinesVisible(true);
        songTable.setHeaderVisible(true);
        GridData songTableGridData = new GridData(SWT.FILL, SWT.FILL, true,
                true);
        songTableGridData.heightHint = 400;
        songTableGridData.widthHint = 410;
        songTableGridData.horizontalSpan = 6;
        songTable.setLayoutData(songTableGridData);

        TableColumn trackNumberColumn = new TableColumn(songTable, SWT.RIGHT);
        trackNumberColumn.setText("Track #");
        trackNumberColumn.setMoveable(true);
        TableColumn artistColumn = new TableColumn(songTable, SWT.LEFT);
        artistColumn.setText("Artist");
        artistColumn.setMoveable(true);
        TableColumn albumColumn = new TableColumn(songTable, SWT.LEFT);
        albumColumn.setText("Album");
        albumColumn.setMoveable(true);
        TableColumn songNameColumn = new TableColumn(songTable, SWT.LEFT);
        songNameColumn.setText("Title");
        songNameColumn.setMoveable(true);

        trackNumberColumn.pack();
        artistColumn.pack();
        albumColumn.pack();
        songNameColumn.pack();

        songTable.setSortColumn(songNameColumn);
        songTable.setSortDirection(SWT.UP);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableLayout.setColumnData(trackNumberColumn, new ColumnWeightData(0, 30, true));
        tableLayout.setColumnData(artistColumn, new ColumnWeightData(20, 100, true));
        tableLayout.setColumnData(albumColumn, new ColumnWeightData(15, 90, true));
        tableLayout.setColumnData(songNameColumn, new ColumnWeightData(45, 150, true));
        tableComposite.setLayout(tableLayout);
        tableComposite.setLayoutData(songTableGridData);


        initPlayer();
        shell.pack();
        shell.open();

        theSoundPlayer.setVideoSink(ElementFactory
                .make("fakesink", "videosink"));
        theSoundPlayer.connect(new PlayBin2.ABOUT_TO_FINISH() {
            public void aboutToFinish(final PlayBin2 element) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        TableItem nextItem = songTable
                                .getItem(determineNextSongIndex());
                        songTable.setSelection(nextItem);
                        File song = new File((String) nextItem
                                .getData("filename"));
                        playSongFromScratch(song);
                    }
                });
            }
        });

            theSoundPlayer.connect(new PlayBin2.AUDIO_CHANGED() {
                public void audioChanged(final PlayBin2 element) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            setupTrackSeekForSong();
                            if (!manualPlaybackEvent) {
                                int nextSongIndex = determineNextSongIndex();
                                songTable.setSelection(nextSongIndex);
                            }
                        }
                    });
                }
            });

            attachHotkeys();

            File baseDir = new File(musicLibraryFolder);
            readMusicLibraryFromFolder(baseDir);

            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        } finally {
            quit(shell);
        }
    }

    private void readMusicLibraryFromFolder(File baseDir) {
        songTable.removeAll();
        musicLibrary.resetMusicLibrary();
        shufflePointer = 0;
        shuffleHistory = new ArrayList<Integer>();
        theSoundPlayer.stop();
        recurseDirectory(baseDir, songTable);
    }

    private void attachHotkeys() {
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_F9,
                InputEvent.ALT_DOWN_MASK), new HotKeyListener() {
            @Override
            public void onHotKey(HotKey hotKey) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        previous();
                    }
                });
            }
        });
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_F10,
                InputEvent.ALT_DOWN_MASK), new HotKeyListener() {
            @Override
            public void onHotKey(HotKey hotKey) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        playSong();
                    }
                });
            }
        });
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_F11,
                InputEvent.ALT_DOWN_MASK), new HotKeyListener() {
            @Override
            public void onHotKey(HotKey hotKey) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        stopSong();
                    }
                });
            }
        });
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_F12,
                InputEvent.ALT_DOWN_MASK), new HotKeyListener() {
            @Override
            public void onHotKey(HotKey hotKey) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        next();
                    }
                });
            }
        });
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
                InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK),
                new HotKeyListener() {
                    @Override
                    public void onHotKey(HotKey hotKey) {
                        display.asyncExec(new Runnable() {
                            public void run() {
                                seekBack();
                            }
                        });
                    }
                });
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
                InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK),
                new HotKeyListener() {
                    @Override
                    public void onHotKey(HotKey hotKey) {
                        display.asyncExec(new Runnable() {
                            public void run() {
                                seekForward();
                            }
                        });
                    }
                });
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_UP,
                InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK),
                new HotKeyListener() {
                    @Override
                    public void onHotKey(HotKey hotKey) {
                        display.asyncExec(new Runnable() {
                            public void run() {
                                volumeUp();
                            }
                        });
                    }
                });
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
                InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK),
                new HotKeyListener() {
                    @Override
                    public void onHotKey(HotKey hotKey) {
                        display.asyncExec(new Runnable() {
                            public void run() {
                                volumeDown();
                            }
                        });
                    }
                });
        provider.register(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK
                        + InputEvent.ALT_DOWN_MASK), new HotKeyListener() {
                    @Override
                    public void onHotKey(HotKey hotKey) {
                        display.asyncExec(new Runnable() {
                            public void run() {
                                toggleShuffle();
                            }
                        });
                    }
                });
        provider.register(
                KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK
                        + InputEvent.ALT_DOWN_MASK), new HotKeyListener() {
                    @Override
                    public void onHotKey(HotKey hotKey) {
                        display.asyncExec(new Runnable() {
                            public void run() {
                                toggleRepeat();
                            }
                        });
                    }
                });
        provider.register(
                KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK
                        + InputEvent.ALT_DOWN_MASK), new HotKeyListener() {
                    @Override
                    public void onHotKey(HotKey hotKey) {
                        display.asyncExec(new Runnable() {
                            public void run() {
                                jumpTo();
                            }
                        });
                    }
                });
    }

    private void initPlayer() {
        Gst.init("AudioPlayer", new String[] { "" });
        theSoundPlayer = new PlayBin2("AudioPlayer");
        theSoundPlayer.setVolumePercent(volumePercent);
    }

    class TrackSeeker implements Runnable {
        public void run() {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    trackSeek.setSelection((int) theSoundPlayer.queryPosition()
                            .toSeconds());
                }
            });
        }
    }

    private void playSong() {
        manualPlaybackEvent = true;
        TableItem[] selectedItems = songTable.getSelection();
        File song;
        if (selectedItems.length > 0) {
            song = new File((String) selectedItems[0].getData("filename"));
        } else {
            song = new File((String) songTable.getItem(0).getData("filename"));
            songTable.setSelection(0);
        }

        if (theSoundPlayer.isPlaying()) {
            theSoundPlayer.pause();
            play.setImage(new Image(display, Class.class
                    .getResourceAsStream("/crystal_icons/play/gif/play-24.gif")));
        } else if (theSoundPlayer.getState() == State.PAUSED) {
            theSoundPlayer.play();
            play.setImage(new Image(
                    display,
                    Class.class
                            .getResourceAsStream("/crystal_icons/pause/gif/pause-24.gif")));
        } else {
            playSongFromScratch(song);
        }
    }

    private void playSongFromScratch(File song) {
        theSoundPlayer.stop();
        theSoundPlayer.setInputFile(song);
        theSoundPlayer.play();
        while (theSoundPlayer.queryDuration().toSeconds() <= 0) {
        }
        setupTrackSeekForSong();
        play.setImage(new Image(display, Class.class
                .getResourceAsStream("/crystal_icons/pause/gif/32.gif")));
    }

    private void setupTrackSeekForSong() {
        trackSeek.setMaximum((int) theSoundPlayer.queryDuration().toSeconds());
        currentPosition = (int) theSoundPlayer.queryPosition().toSeconds();
        trackSeek.setSelection(currentPosition);
        setupTrackSeekFuture();
    }

    private void setupTrackSeekFuture() {
        if (trackSeekFuture != null) {
            trackSeekFuture.cancel(true);
        }
        trackSeekFuture = Gst.getScheduledExecutorService().scheduleAtFixedRate(new Runnable() {
            public void run() {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        if (!trackSeek.isDisposed()) {
                            currentPosition = (int) theSoundPlayer.queryPosition().toSeconds();
                            trackSeek.setSelection(currentPosition);
                        }
                    }
                });
            }
        }, 250, 250, TimeUnit.MILLISECONDS);
    }

    private void next() {
        manualPlaybackEvent = true;
        int nextItemIndex = determineNextSongIndex();
        songTable.setSelection(nextItemIndex);
        TableItem nextItem = songTable.getItem(nextItemIndex);
        File song = new File((String) nextItem.getData("filename"));
        if (theSoundPlayer.isPlaying()) {
            playSongFromScratch(song);
        }
    }

    private int findNextShuffleSongIndex() {
        int numSongs = songTable.getItemCount();
        int newShuffleIndex = (int) (Math.random() * (numSongs + 1));
        shuffleHistory.add(newShuffleIndex);
        shufflePointer++;
        return newShuffleIndex;
    }

    private int determineNextSongIndex() {
        if (shuffle) {
            if (shufflePointer + 1 < shuffleHistory.size()) {
                shufflePointer++;
                return shuffleHistory.get(shufflePointer);
            } else {
                return findNextShuffleSongIndex();
            }
        }
        int[] selectedItemIndicies = songTable.getSelectionIndices();
        if (selectedItemIndicies.length > 0) {
            int nextItemIndex;
            if (repeat) {
                nextItemIndex = selectedItemIndicies[0];
            } else {
                nextItemIndex = selectedItemIndicies[0] + 1;
            }
            if (nextItemIndex >= songTable.getItemCount()) {
                nextItemIndex = 0;
            }
            return nextItemIndex;
        } else {
            return 0;
        }
    }

    private int determinePreviousSongIndex() {
        if (shuffle) {
            if (shufflePointer > 0) {
                shufflePointer--;
                return shuffleHistory.get(shufflePointer);
            } else {
                // Not sure what we want to do here yet
                return 0;
            }
        }
        int[] selectedItemIndicies = songTable.getSelectionIndices();
        if (selectedItemIndicies.length > 0) {
            int previousItemIndex;
            if (repeat) {
                previousItemIndex = selectedItemIndicies[0];
            } else {
                previousItemIndex = selectedItemIndicies[0] - 1;
            }
            if (previousItemIndex < 0) {
                previousItemIndex = songTable.getItemCount() - 1;
            }
            return previousItemIndex;
        } else {
            return 0;
        }
    }

    private void previous() {
        manualPlaybackEvent = true;
        int previousItemIndex = determinePreviousSongIndex();
        TableItem previousItem = songTable.getItem(previousItemIndex);
        File song = new File((String) previousItem.getData("filename"));
        songTable.setSelection(previousItemIndex);
        if (theSoundPlayer.isPlaying()) {
            playSongFromScratch(song);
        }
    }

    private void stopSong() {
        play.setImage(new Image(display, Class.class
                .getResourceAsStream("/crystal_icons/play/gif/32.gif")));
        theSoundPlayer.stop();
    }

    private void seekBack() {
        int minTrackLength = trackSeek.getMinimum();
        if (currentPosition - 5 <= minTrackLength) {
            theSoundPlayer.seek(minTrackLength, TimeUnit.SECONDS);
            setupTrackSeekFuture();
        } else {
            currentPosition -= 5;
            theSoundPlayer.seek(currentPosition, TimeUnit.SECONDS);
            setupTrackSeekFuture();
        }
    }

    private void seekForward() {
        int maxTrackLength = trackSeek.getMaximum();
        if (currentPosition + 5 >= maxTrackLength) {
            next();
        } else {
            currentPosition += 5;
            theSoundPlayer.seek(currentPosition, TimeUnit.SECONDS);
            setupTrackSeekFuture();
        }
    }

    private void volumeUp() {
        int currentVolume = theSoundPlayer.getVolumePercent();
        if (currentVolume + 2 >= 100) {
            volumePercent = 100;
            theSoundPlayer.setVolumePercent(volumePercent);
            storePreferences();
        } else {
            volumePercent = currentVolume + 2;
            theSoundPlayer.setVolumePercent(volumePercent);
            storePreferences();
        }
    }

    private void volumeDown() {
        int currentVolume = theSoundPlayer.getVolumePercent();
        if (currentVolume - 2 <= 0) {
            volumePercent = 0;
            theSoundPlayer.setVolumePercent(volumePercent);
            storePreferences();
        } else {
            volumePercent = currentVolume - 2;
            theSoundPlayer.setVolumePercent(volumePercent);
            storePreferences();
        }
    }

    private void toggleShuffle() {
        shuffle = (!shuffle);
    }

    private void toggleRepeat() {
        repeat = (!repeat);
    }

    private void jumpTo() {
        final Shell jumpToDialog = new Shell(display);
        jumpToDialog.setText("Attuned - Jump To Song");
        jumpToDialog.setSize(300, 200);

        GridLayout gridLayout = new GridLayout(2, true);
        gridLayout.numColumns = 1;
        jumpToDialog.setLayout(gridLayout);
        final Text searchText = new Text(jumpToDialog, SWT.NONE);
        
        final Table resultsTable = new Table(jumpToDialog, SWT.MULTI | SWT.BORDER
                | SWT.FULL_SELECTION);
        resultsTable.setLinesVisible(true);
        resultsTable.setHeaderVisible(true);
        resultsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        jumpToDialog.addListener(SWT.Traverse, new Listener() {
            @Override
            public void handleEvent(Event event) {
                switch (event.detail) {
                    case SWT.TRAVERSE_ESCAPE:
                        jumpToDialog.close();
                        event.detail = SWT.TRAVERSE_NONE;
                        event.doit = false;
                        break;
                    case SWT.TRAVERSE_RETURN:
                        if (resultsTable.getSelection().length > 0) {
                            TableItem item = resultsTable.getSelection()[0];
                            songTable.setSelection((Integer) item.getData("index"));
                            playSongFromScratch(new File((String) item.getData("filename")));
                            jumpToDialog.close();
                            event.detail = SWT.TRAVERSE_NONE;
                            event.doit = false;
                        }
                        break;
                }
            }
        });

        TableColumn songNameColumn = new TableColumn(resultsTable, SWT.NONE);
        songNameColumn.setText("Song");
        songNameColumn.pack();
        
        searchText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        searchText.addKeyListener(new KeyListener() {
            
            @Override
            public void keyReleased(org.eclipse.swt.events.KeyEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void keyPressed(org.eclipse.swt.events.KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN) {
                    int selectionTable = resultsTable.getSelectionIndex();
                    if (selectionTable + 1 < resultsTable.getItemCount()) {
                        resultsTable.setSelection(selectionTable + 1);
                    }
                    
                } else if (e.keyCode == SWT.ARROW_UP) {
                    int selectionTable = resultsTable.getSelectionIndex();
                    if (selectionTable > 0) {
                        resultsTable.setSelection(selectionTable - 1);
                    }
                }
            }
        });
        searchText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        ArrayList<TableItem> songNameStartsWithMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> artistNameStartsWithMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> albumNameStartsWithMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> songNameOtherMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> fileNameMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> artistNameOtherMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> albumNameOtherMatches = new ArrayList<TableItem>();
                        String search = searchText.getText();
                        for (TableItem tableItem : songTable.getItems()) {
                            if (tableItem.getText(3).toLowerCase().startsWith(search)) {
                                songNameStartsWithMatches.add(tableItem);
                            } else if (tableItem.getText(1).toLowerCase().startsWith(search)) {
                                artistNameStartsWithMatches.add(tableItem);
                            } else if (tableItem.getText(2).toLowerCase().startsWith(search)) {
                                albumNameStartsWithMatches.add(tableItem);
                            } else if (tableItem.getText(3).toLowerCase().contains(search)) {
                                songNameOtherMatches.add(tableItem);
                            } else if (tableItem.getText(1).toLowerCase().contains(search)) {
                                artistNameOtherMatches.add(tableItem);
                            } else if (tableItem.getText(2).toLowerCase().contains(search)) {
                                albumNameOtherMatches.add(tableItem);
                            } else {
                                String fileName = ((String) tableItem.getData("filename")).toLowerCase();
                                if (fileName.contains("/")) {
                                    fileName = fileName.substring(fileName.lastIndexOf("/"));
                                }
                                if (fileName.contains(search)) {
                                    fileNameMatches.add(tableItem);
                                }
                            }
                        }
                        resultsTable.removeAll();
                        addItemsToList(songNameStartsWithMatches, resultsTable);
                        addItemsToList(artistNameStartsWithMatches, resultsTable);
                        addItemsToList(albumNameStartsWithMatches, resultsTable);
                        addItemsToList(songNameOtherMatches, resultsTable);
                        addItemsToList(fileNameMatches, resultsTable);
                        addItemsToList(artistNameOtherMatches, resultsTable);
                        addItemsToList(albumNameOtherMatches, resultsTable);
                        resultsTable.select(0);
                    }
                });
            }
        });

        jumpToDialog.open();
        while (!jumpToDialog.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private void addItemsToList(ArrayList<TableItem> items, Table resultsTable) {
        for (TableItem tableItem : items) {
            TableItem item = new TableItem(resultsTable, SWT.NONE);
            item.setText(0, tableItem.getText(3) + " - " + tableItem.getText(1) + " - " + tableItem.getText(2));
            item.setData("filename", tableItem.getData("filename"));
            item.setData("index", songTable.indexOf(tableItem));
        }
    }

    private void recurseDirectory(File currentDirectory, Table songTable) {
        File[] directories = currentDirectory
                .listFiles(new DirectoryFileFilter());
        File[] songs = currentDirectory.listFiles(new NonDirectoryFileFilter());
        Arrays.sort(directories, new FileComparator());
        Arrays.sort(songs, new FileComparator());
        for (File directory : directories) {
            recurseDirectory(directory, songTable);
        }
        for (File song : songs) {
            musicLibrary.parseSong(song, songTable);
        }
    }

    public MusicLibrary musicLibrary = new MusicLibrary();
    private Button stop;
    private Button previous;
    private Button next;

    public static void main(String[] args) {
        silenceThirdpartyLoggers();
        display = new Display();
        try {
            new AttunedMainWindow(display);
        } finally {
            display.dispose();
            System.exit(0);
        }
    }

    private static void silenceThirdpartyLoggers() {
        Provider.logger.setLevel(Level.WARNING);
        AbstractTagItem.logger.setLevel(Level.SEVERE);
        MP3File.logger.setLevel(Level.SEVERE);
    }

    public void run() {
        // TODO Auto-generated method stub
    }
}
