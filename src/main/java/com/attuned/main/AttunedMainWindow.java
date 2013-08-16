package com.attuned.main;

import static com.attuned.main.KeyCodeTranslator.translateSWTKey;
import static com.attuned.main.KeyCodeTranslator.translateSWTModifiers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jxgrabkey.HotkeyConflictException;
import jxgrabkey.HotkeyListener;
import jxgrabkey.JXGrabKey;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gstreamer.ClockTime;
import org.gstreamer.Gst;
import org.gstreamer.State;
import org.gstreamer.elements.PlayBin2;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;


public class AttunedMainWindow {
    protected static final int HOTKEY_INDEX_PLAY_PAUSE = 1;
    protected static final int HOTKEY_INDEX_PLAY = 2;
    protected static final int HOTKEY_INDEX_STOP = 3;
    protected static final int HOTKEY_INDEX_PREVIOUS = 4;
    protected static final int HOTKEY_INDEX_NEXT = 5;
    protected static final int HOTKEY_INDEX_VOLUME_UP = 6;
    protected static final int HOTKEY_INDEX_VOLUME_DOWN = 7;
    protected static final int HOTKEY_INDEX_SEEK_BACK = 8;
    protected static final int HOTKEY_INDEX_SEEK_FORWARD = 9;
    private static final int HOTKEY_INDEX_TOGGLE_SHUFFLE = 10;
    private static final int HOTKEY_INDEX_TOGGLE_REPEAT = 11;
    private static final int HOTKEY_INDEX_JUMP = 12;
    protected boolean autoIncrementNextSong = false;
    private Table songTable;
    public PlayBin2 theSoundPlayer = null;
    private Button play;
    private static Display display;
    private Shell shell;
    private Button volumeButton;
    public Scale trackSeek; 

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
    
    public AttunedMainWindow(final Display display) {
        shell = new Shell(display);
        shell.setText("Attuned");

        Logger logger = Logger.getLogger("org.jaudiotagger");
        logger.setLevel(Level.WARNING);
        
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 6;
        shell.setLayout(gridLayout);

        previous = new Button(shell, SWT.PUSH);
        previous.setImage(new Image(display, Class.class.getResourceAsStream("/crystal_icons/backward/gif/backward-24.gif")));
        previous.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
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
        play.setImage(new Image(display, Class.class.getResourceAsStream("/crystal_icons/play/gif/play-24.gif")));
        play.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
        play.setSize(16, 16);
        play.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                playSong();
            }
            public void widgetDefaultSelected(SelectionEvent e) {
                playSong();
            } 
        });
        
        stop = new Button(shell, SWT.PUSH);
        stop.setImage(new Image(display, Class.class.getResourceAsStream("/crystal_icons/stop/gif/stop-24.gif")));
        stop.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
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
        next.setImage(new Image(display, Class.class.getResourceAsStream("/crystal_icons/forward/gif/forward-24.gif")));
        next.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
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
        trackSeek.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
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
                theSoundPlayer.seek(ClockTime.fromSeconds(trackSeek.getSelection()));
            }
        });
        
        volumeButton = new Button(shell, SWT.PUSH);
        volumeButton.setImage(new Image(display, Class.class.getResourceAsStream("/kde_crystal_diamond_icons/16x16/actions/player_volume.png")));
        volumeButton.setLayoutData(new GridData(GridData.END, GridData.FILL, false, false));
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
                Point volumeSliderSize = volumeShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                volumeSlider.setMinimum(0);
                volumeSlider.addFocusListener(new VolumeShellFocusListener(volumeShell));
                volumeSlider.setMaximum(100);
                volumeSlider.setIncrement(2);
                volumeSlider.setPageIncrement(10);
                volumeSlider.setSelection(90);
                volumeSlider.addSelectionListener(new SelectionListener() {
                    public void widgetSelected(SelectionEvent e) {
                        handleVolumeChange(e);
                    }

                    public void widgetDefaultSelected(SelectionEvent e) {
                        handleVolumeChange(e);
                    }

                    private void handleVolumeChange(SelectionEvent e) {
                        theSoundPlayer.setVolumePercent((100 - volumeSlider.getSelection()));
                    }
                });
                Point volumeButtonLocation = ((Button) e.getSource()).toDisplay(0, 0);
                Point finalVolumeSliderLocation = volumeButtonLocation;
                if (volumeButtonLocation.y - volumeSliderSize.y - 50 >= 0) {
                    finalVolumeSliderLocation.y = finalVolumeSliderLocation.y - volumeSliderSize.y;
                } else {
                    finalVolumeSliderLocation.y = finalVolumeSliderLocation.y + volumeSliderSize.y;
                }
                
                volumeShell.setLocation(finalVolumeSliderLocation);
                volumeShell.pack();
                volumeShell.open();
            }
        }
        
        volumeButton.addSelectionListener(new VolumeSelectionListener());
        
        songTable = new Table(shell, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
        songTable.setLinesVisible(true);
        songTable.setHeaderVisible(true);
        GridData songTableGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        songTableGridData.heightHint = 400;
        songTableGridData.horizontalSpan = 6;
        songTable.setLayoutData(songTableGridData);
        
        TableColumn songNameColumn = new TableColumn(songTable, SWT.NONE);
        songNameColumn.setText("Title");
        songNameColumn.setMoveable(true);
        TableColumn artistColumn = new TableColumn(songTable, SWT.NONE);
        artistColumn.setText("Artist");
        artistColumn.setMoveable(true);
        TableColumn albumColumn = new TableColumn(songTable, SWT.NONE);
        albumColumn.setText("Album");
        albumColumn.setMoveable(true);
        TableColumn trackNumberColumn = new TableColumn(songTable, SWT.NONE);
        trackNumberColumn.setText("Track #");
        trackNumberColumn.setMoveable(true);
        
        
        File baseDir = new File("/media/Shared/Music/iTunes/Tool");
        recurseDirectory(baseDir, songTable);
        
        songNameColumn.pack();
        artistColumn.pack();
        albumColumn.pack();
        trackNumberColumn.pack();
        
        songTable.setSortColumn(songNameColumn);
        songTable.setSortDirection(SWT.UP);
        
        File[] directories = baseDir.listFiles(new DirectoryFileFilter());
        File[] songs = baseDir.listFiles(new NonDirectoryFileFilter());
        Arrays.sort(directories, new FileComparator());
        Arrays.sort(songs, new FileComparator());
        
        initPlayer();
        shell.pack();
        shell.open();
        
        theSoundPlayer.connect(new PlayBin2.ABOUT_TO_FINISH() {
            public void aboutToFinish(final PlayBin2 element) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        System.out.println("about 2 finish");
                        System.out.println(element.getState());
                        TableItem nextItem = songTable.getItem(determineNextSongIndex());
                        File song = new File((String) nextItem.getData("filename"));
                        element.setInputFile(song);
                        autoIncrementNextSong = true;
                        element.play();
                    }
                });
            }
        });
        
        theSoundPlayer.connect(new PlayBin2.AUDIO_CHANGED() {
            public void audioChanged(final PlayBin2 element) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        setupTrackSeekForSong();
                        if (autoIncrementNextSong) {
                            System.out.println("autoInc");
                            int nextSongIndex = determineNextSongIndex();
                            songTable.setSelection(nextSongIndex);
                            autoIncrementNextSong = false;
                        }
                    }
                });
            }
        });
        
//        Gst.getScheduledExecutorService().scheduleAtFixedRate(new Runnable() {
//            public void run() {
//                Display.getDefault().syncExec(new Runnable() {
//                    public void run() {
//                        if (volumeSlider != null) {
//                            volumeSlider.setSelection((int) theSoundPlayer.getVolumePercent());
//                        }
//                    }
//                });
//            }
//        }, 250, 250, TimeUnit.MILLISECONDS);
        
        attachHotkeys();
        while (!shell.isDisposed()) {
            if(!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }
    
    private void attachHotkeys() {
        try {
            System.load(new File("target/classes/libJXGrabKey.so").getCanonicalPath());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        JXGrabKey.setDebugOutput(false);
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_PLAY_PAUSE, translateSWTModifiers(SWT.ALT), translateSWTKey(SWT.F10));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_STOP, translateSWTModifiers(SWT.ALT), translateSWTKey(SWT.F11));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_PREVIOUS, translateSWTModifiers(SWT.ALT), translateSWTKey(SWT.F9));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_NEXT, translateSWTModifiers(SWT.ALT), translateSWTKey(SWT.F12));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_SEEK_BACK, translateSWTModifiers(SWT.ALT + SWT.CTRL), translateSWTKey(SWT.ARROW_LEFT));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_SEEK_FORWARD, translateSWTModifiers(SWT.ALT + SWT.CTRL), translateSWTKey(SWT.ARROW_RIGHT));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_VOLUME_UP, translateSWTModifiers(SWT.ALT + SWT.CTRL), translateSWTKey(SWT.ARROW_UP));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_VOLUME_DOWN, translateSWTModifiers(SWT.ALT + SWT.CTRL), translateSWTKey(SWT.ARROW_DOWN));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_TOGGLE_SHUFFLE, translateSWTModifiers(SWT.ALT + SWT.CTRL), translateSWTKey('s'));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_TOGGLE_REPEAT, translateSWTModifiers(SWT.ALT + SWT.CTRL), translateSWTKey('r'));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        try {
            JXGrabKey.getInstance().registerAwtHotkey(HOTKEY_INDEX_JUMP, translateSWTModifiers(SWT.ALT + SWT.CTRL), translateSWTKey('j'));
        } catch (HotkeyConflictException hotkeyConflictException) {
            hotkeyConflictException.printStackTrace();
        }
        
        HotkeyListener hotkeyPlayPauseListener = new jxgrabkey.HotkeyListener(){
            public void onHotkey(int hotkey_idx) {
                if (hotkey_idx == HOTKEY_INDEX_PLAY_PAUSE) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            playSong();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_PLAY) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            playSong();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_STOP) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            stopSong();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_PREVIOUS) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            autoIncrementNextSong = false;
                            previous();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_NEXT) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            autoIncrementNextSong = false;
                            next();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_VOLUME_UP) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            volumeUp();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_VOLUME_DOWN) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            volumeDown();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_SEEK_BACK) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            seekBack();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_SEEK_FORWARD) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            seekForward();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_TOGGLE_SHUFFLE) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            toggleShuffle();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_TOGGLE_REPEAT) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            toggleRepeat();
                        }
                    });
                } else if (hotkey_idx == HOTKEY_INDEX_JUMP) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            jumpTo();
                        }
                    });
                } else {
                    return;
                }
            }
        };
        
        //Add HotkeyListener
        JXGrabKey.getInstance().addHotkeyListener(hotkeyPlayPauseListener);
    }
    
    private void initPlayer() {
        Gst.init("VideoPlayer", new String[] {""});
        theSoundPlayer = new PlayBin2("VideoPlayer");
    }
    
    class TrackSeeker implements Runnable {
        public void run() {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    trackSeek.setSelection((int) theSoundPlayer.queryPosition().toSeconds());
                }
            });
        }
    }
    
    private void playSong() {
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
            play.setImage(new Image(display, Class.class.getResourceAsStream("/crystal_icons/play/gif/play-24.gif")));
        }
        else if (theSoundPlayer.getState() == State.PAUSED) {
            theSoundPlayer.play();
            play.setImage(new Image(display, Class.class.getResourceAsStream("/crystal_icons/pause/gif/pause-24.gif")));
        }
        else {
            playSongFromScratch(song);
        }
    }
    
    private void playSongFromScratch(File song) {
        theSoundPlayer.stop();
        theSoundPlayer.setInputFile(song);
        theSoundPlayer.setVolumePercent(10);
        theSoundPlayer.play();
        while (theSoundPlayer.queryDuration().toSeconds() <= 0) {
        }
//        setupTrackSeekForSong();
        play.setImage(new Image(display, Class.class.getResourceAsStream("/crystal_icons/pause/gif/32.gif")));
    }
    
    private void setupTrackSeekForSong() {
        trackSeek.setMaximum((int) theSoundPlayer.queryDuration().toSeconds());
        trackSeek.setSelection((int) theSoundPlayer.queryPosition().toSeconds());
        Gst.getScheduledExecutorService().scheduleAtFixedRate(new Runnable() {
            public void run() {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        trackSeek.setSelection((int) theSoundPlayer.queryPosition().toSeconds());
                    }
                });
            }
        }, 250, 250, TimeUnit.MILLISECONDS);
    }
    
    private void next() {
        int nextItemIndex = determineNextSongIndex();
        songTable.setSelection(nextItemIndex);
        TableItem nextItem = songTable.getItem(nextItemIndex);
        File song = new File((String) nextItem.getData("filename"));
        playSongFromScratch(song);
    }
    
    private int determineNextSongIndex() {
        int[] selectedItemIndicies = songTable.getSelectionIndices();
        if (selectedItemIndicies.length > 0) {
            int nextItemIndex = selectedItemIndicies[0] + 1;
            if (nextItemIndex >= songTable.getItemCount()) {
                nextItemIndex = 0;
            }
            return nextItemIndex;
        } else {
            return 0;
        }
    }
    
    private void previous() {
        int[] selectedItemIndicies = songTable.getSelectionIndices();
        if (selectedItemIndicies.length > 0) {
            int previousItemIndex = selectedItemIndicies[0] - 1;
            if (previousItemIndex < 0) {
                previousItemIndex = songTable.getItemCount() - 1;
            }
            TableItem previousItem = songTable.getItem(previousItemIndex);
            File song = new File((String) previousItem.getData("filename"));
            songTable.setSelection(previousItemIndex);
            
            playSongFromScratch(song);
        }
    }
    
    private void stopSong() {
        play.setImage(new Image(display, Class.class.getResourceAsStream("/crystal_icons/play/gif/32.gif")));
        theSoundPlayer.stop();
    }
    
    private void seekBack() {
        int minTrackLength = trackSeek.getMinimum();
        int currentPosition = trackSeek.getSelection();
        if (currentPosition - 5 <= minTrackLength) {
            theSoundPlayer.seek(minTrackLength, TimeUnit.SECONDS);
        } else {
            theSoundPlayer.seek(currentPosition - 5, TimeUnit.SECONDS);
        }
    }
    
    private void seekForward() {
        int maxTrackLength = trackSeek.getMaximum();
        int currentPosition = trackSeek.getSelection();
        if (currentPosition + 5 >= maxTrackLength) {
            next();
        } else {
            theSoundPlayer.seek(currentPosition + 5, TimeUnit.SECONDS);
        }
    }
    
    private void volumeUp() {
        int currentVolume = theSoundPlayer.getVolumePercent();
        if (currentVolume + 2 >= 100) {
            theSoundPlayer.setVolumePercent(100);
        } else {
            theSoundPlayer.setVolumePercent(currentVolume + 2);
        }
    }
    
    private void volumeDown() {
        int currentVolume = theSoundPlayer.getVolumePercent();
        if (currentVolume - 2 <= 0) {
            theSoundPlayer.setVolumePercent(0);
        } else {
            theSoundPlayer.setVolumePercent(currentVolume - 2);
        }
    }
    
    private void toggleShuffle() {
        System.out.println("toggle shuffle");
    }
    
    private void toggleRepeat() {
        System.out.println("toggle repeat");
    }
    
    private void jumpTo() {
        System.out.println("jump to");
    }
    
    private void recurseDirectory(File currentDirectory, Table songTable) {
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
    
    private ArrayList<ID3v24Tag> idTags = new ArrayList<ID3v24Tag>();
    
    private void parseSong(File song, Table songTable) {
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
    
    private void addSongToLibrary(ID3v24Tag id3Tag, Table songTable, File song) {
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
        
        ArrayList<Artist> artists = musicLibrary.artists;
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
    
    public MusicLibrary musicLibrary = new MusicLibrary();
    private Button stop;
    private Button previous;
    private Button next;
    class MusicLibrary {
        ArrayList<Artist> artists = new ArrayList<Artist>();
    }
    
    class Artist {
        String name;
        ArrayList<Album> albums = new ArrayList<Album>();
        public Artist (String name) {
            this.name = name;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Artist) {
                if (((Artist) obj).name.equalsIgnoreCase(this.name)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    class Album {
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

    public static void main(String[] args) {
        display = new Display();
        new AttunedMainWindow(display);
        display.dispose();
    }

    public void run() {
        // TODO Auto-generated method stub
        
    }
}
