package com.attuned.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.io.File;
import java.util.ArrayList;

public class JumpToSongDialog {
    private AttunedMainWindow attunedMainWindow;

    public JumpToSongDialog(AttunedMainWindow attunedMainWindow) {
        this.attunedMainWindow = attunedMainWindow;
    }

    public void jumpTo() {
        final Shell jumpToDialog = new Shell(AttunedMainWindow.display);
        jumpToDialog.setText("Attuned - Jump To Song");
        jumpToDialog.setSize(300, 200);
        jumpToDialog.setImage(attunedMainWindow.attunedIconImage);

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
                            attunedMainWindow.songTable.setSelection((Integer) item.getData("index"));
                            attunedMainWindow.playSongFromScratch(new File((String) item.getData("filename")));
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
                AttunedMainWindow.display.asyncExec(new Runnable() {
                    public void run() {
                        ArrayList<TableItem> songNameStartsWithMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> artistNameStartsWithMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> albumNameStartsWithMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> songNameOtherMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> fileNameMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> artistNameOtherMatches = new ArrayList<TableItem>();
                        ArrayList<TableItem> albumNameOtherMatches = new ArrayList<TableItem>();
                        String search = searchText.getText();
                        for (TableItem tableItem : attunedMainWindow.songTable.getItems()) {
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
                        attunedMainWindow.addItemsToList(songNameStartsWithMatches, resultsTable);
                        attunedMainWindow.addItemsToList(artistNameStartsWithMatches, resultsTable);
                        attunedMainWindow.addItemsToList(albumNameStartsWithMatches, resultsTable);
                        attunedMainWindow.addItemsToList(songNameOtherMatches, resultsTable);
                        attunedMainWindow.addItemsToList(fileNameMatches, resultsTable);
                        attunedMainWindow.addItemsToList(artistNameOtherMatches, resultsTable);
                        attunedMainWindow.addItemsToList(albumNameOtherMatches, resultsTable);
                        resultsTable.select(0);
                    }
                });
            }
        });

        jumpToDialog.open();
        while (!jumpToDialog.isDisposed()) {
            if (!AttunedMainWindow.display.readAndDispatch()) {
                AttunedMainWindow.display.sleep();
            }
        }
    }

}
