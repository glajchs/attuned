package com.attuned.main;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.freedesktop.gstreamer.elements.PlayBin;

// Unused as of now
class PlayerThread extends Thread {
        private PlayBin player;
        private SourceDataLine line;
        PlayerThread(PlayBin player) {
            this.player = player;
        }
        
        public void setVolume(int volume) {
            FloatControl masterGain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            masterGain.setValue(volume);
        }

        public void run() {
            player.play();
            
        }
    }