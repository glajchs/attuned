package com.attuned.main;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import com.patrikdufresne.fontawesome.FontAwesome;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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

// Requires libgstreamer0.10-dev and libgstreamer-plugins-base0.10-dev packages
public class AttunedMainWindow {
    public Table songTable;
    public String musicLibraryFolder;
    public PlayBin2 theSoundPlayer = null;
    private Button play;
    public static Display display;
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
    public int volumePercent;
    private static PreferencesAndSongDB preferencesAndSongDB;
    private static JumpToSongDialog jumpToSongDialog;

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
                    preferencesAndSongDB.storePreferences();
                    readMusicLibraryFromFolder(directory);
                }
            }
        });

        shell.setMenuBar(menuBar);
    }


    public AttunedMainWindow(final Display display) {
        preferencesAndSongDB = new PreferencesAndSongDB(this);
        jumpToSongDialog = new JumpToSongDialog(this);
        shell = new Shell(display);
        try {
        preferencesAndSongDB.initializeSongDBConnection();
        preferencesAndSongDB.loadPreferences();

        if (musicLibraryFolder == null) {
            musicLibraryFolder = "/media/scott/Shared/Music/Amazon MP3";
            preferencesAndSongDB.storePreferences();
        }
        if (volumePercent == 0) {
            volumePercent = 10;
            preferencesAndSongDB.storePreferences();
        }
        setupMenus(shell);
        shell.setText("Attuned");
//        shell.setSize(300, 500);

        Logger logger = Logger.getLogger("org.jaudiotagger");
        logger.setLevel(Level.WARNING);

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 8;
        shell.setLayout(gridLayout);

        previous = new Button(shell, SWT.PUSH);
        previous.setFont(getFontAwesomeWithSize(16));
        previous.setText(FontAwesome.backward);
        previous.setForeground(new Color(display.getCurrent(), 36, 143, 187));
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
        play.setFont(getFontAwesomeWithSize(16));
        play.setText(FontAwesome.play);
        play.setForeground(new Color(display.getCurrent(), 36, 143, 187));
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
        stop.setFont(getFontAwesomeWithSize(16));
        stop.setText(FontAwesome.stop);
        stop.setForeground(new Color(display.getCurrent(), 36, 143, 187));
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
        next.setFont(getFontAwesomeWithSize(16));
        next.setText(FontAwesome.forward);
        next.setForeground(new Color(display.getCurrent(), 36, 143, 187));
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

        shuffleButton = new Button(shell, SWT.PUSH);
        shuffleButton.setFont(getFontAwesomeWithSize(16));
        shuffleButton.setText(FontAwesome.random);
        shuffleButton.setForeground(new Color(display.getCurrent(), 36, 143, 187));
        shuffleButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false,
                false));
        shuffleButton.setSize(16, 16);
        shuffleButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent arg0) {
                toggleShuffle();
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
                toggleShuffle();
            }
        });

        repeatButton = new Button(shell, SWT.PUSH);
        repeatButton.setFont(getFontAwesomeWithSize(16));
        repeatButton.setText(FontAwesome.repeat);
        repeatButton.setForeground(new Color(display.getCurrent(), 36, 143, 187));
        repeatButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false,
                false));
        repeatButton.setSize(16, 16);
        repeatButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent arg0) {
                toggleRepeat();
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
                toggleRepeat();
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
        volumeButton.setFont(getFontAwesomeWithSize(16));
        volumeButton.setText(FontAwesome.volume_up);
        volumeButton.setForeground(new Color(display.getCurrent(), 36, 143, 187));
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
                        preferencesAndSongDB.storePreferences();
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
        songTableGridData.horizontalSpan = 8;
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
                                .getItem(determineNextSongIndex(false));
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
                                int nextSongIndex = determineNextSongIndex(false);
                                songTable.setSelection(nextSongIndex);
                            }
                        }
                    });
                }
            });

            attachHotkeys();

            preferencesAndSongDB.loadSongDB();

            // Set focus on the first table row on startup, instead of on the "back" button, which just happens to be the first UI control.
            songTable.setFocus();

            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
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
        preferencesAndSongDB.storeSongDB();
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
                                jumpToSongDialog.jumpTo();
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
            play.setText(FontAwesome.play);
        } else if (theSoundPlayer.getState() == State.PAUSED) {
            theSoundPlayer.play();
            play.setText(FontAwesome.pause);
        } else {
            playSongFromScratch(song);
        }
    }

    public void playSongFromScratch(File song) {
        theSoundPlayer.stop();
        theSoundPlayer.setInputFile(song);
        theSoundPlayer.play();
        while (theSoundPlayer.queryDuration().toSeconds() <= 0) {
        }
        setupTrackSeekForSong();
        play.setText(FontAwesome.pause);
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
        int nextItemIndex = determineNextSongIndex(true);
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

    private int determineNextSongIndex(boolean ignoreRepeat) {
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
            if (repeat && !ignoreRepeat) {
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
            previousItemIndex = selectedItemIndicies[0] - 1;
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
        play.setText(FontAwesome.play);
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
            preferencesAndSongDB.storePreferences();
        } else {
            volumePercent = currentVolume + 2;
            theSoundPlayer.setVolumePercent(volumePercent);
            preferencesAndSongDB.storePreferences();
        }
    }

    private void volumeDown() {
        int currentVolume = theSoundPlayer.getVolumePercent();
        if (currentVolume - 2 <= 0) {
            volumePercent = 0;
            theSoundPlayer.setVolumePercent(volumePercent);
            preferencesAndSongDB.storePreferences();
        } else {
            volumePercent = currentVolume - 2;
            theSoundPlayer.setVolumePercent(volumePercent);
            preferencesAndSongDB.storePreferences();
        }
    }

    private void toggleShuffle() {
        if (shuffle) {
            shuffleButton.setForeground(new Color(display.getCurrent(), 36, 143, 187));
            shuffleButton.setFont(getFontAwesomeWithSize(16));
        } else {
            shuffleButton.setForeground(new Color(display.getCurrent(), 0, 128, 0));
            shuffleButton.setFont(getFontAwesomeWithStyleAndSize(SWT.BOLD, 16));
        }
        shuffle = (!shuffle);
    }

    private void toggleRepeat() {
        if (repeat) {
            repeatButton.setForeground(new Color(display.getCurrent(), 36, 143, 187));
            repeatButton.setFont(getFontAwesomeWithSize(16));
        } else {
            repeatButton.setForeground(new Color(display.getCurrent(), 0, 128, 0));
            repeatButton.setFont(getFontAwesomeWithStyleAndSize(SWT.BOLD, 16));
        }
        repeat = (!repeat);
    }

    /****** START I should contribute this back to https://github.com/ikus060/fontawesome/ ******/
    public Font getFontAwesomeWithStyleAndSize(int style, int size) {
        String name = "FONTAWESOME" + "_style_" + style + "_size_" + size;
        return getFontWithName(name, style, size);
    }

    public Font getFontAwesomeWithStyle(int style) {
        String name = "FONTAWESOME" + "_style_" + style;
        return getFontWithName(name, style, null);
    }

    public Font getFontAwesomeWithSize(int size) {
        String name = "FONTAWESOME" + "_size_" + size;
        return getFontWithName(name, null, size);
    }

    private Font getFontWithName(String name, Integer style, Integer size) {
        if (!JFaceResources.getFontRegistry().hasValueFor(name)) {
            FontData[] data = FontAwesome.getFont().getFontData();
            for (FontData d : data) {
                if (style != null) {
                    d.setStyle(style);
                }
                if (size != null) {
                    d.setHeight(size);
                }
            }
            JFaceResources.getFontRegistry().put(name, data);
        }
        return JFaceResources.getFontRegistry().get(name);
    }
    /****** END I should contribute this back to https://github.com/ikus060/fontawesome/ ******/

    public void addItemsToList(ArrayList<TableItem> items, Table resultsTable) {
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
    private Button shuffleButton;
    private Button repeatButton;

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
