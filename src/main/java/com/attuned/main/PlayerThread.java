package com.attuned.main;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.gstreamer.elements.PlayBin2;

class PlayerThread extends Thread {
        private PlayBin2 player;
        private SourceDataLine line;
        PlayerThread(PlayBin2 player) {
            this.player = player;
        }
        
        public void setVolume(int volume) {
            FloatControl masterGain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            masterGain.setValue(volume);
        }

        public void run() {
            player.play();
//            try {
//                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(player);
//                AudioInputStream decodedInputStream = null;
//                AudioFormat sourceFormat = audioInputStream.getFormat();
//                AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
//                                                    sourceFormat.getSampleRate(),
//                                                    16,
//                                                    sourceFormat.getChannels(),
//                                                    sourceFormat.getChannels() * (16 / 8),
//                                                    sourceFormat.getSampleRate(),
//                                                    false);
//                decodedInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
//                byte[] data = new byte[4096];
//
//                DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
//                try
//                {
//                    Mixer mixer = null;
//                    Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
//                    for (int i = 0; i < aInfos.length; i++) {
//                        if (aInfos[i].getName().equals("Intel [plughw:0,0]")) {
//                            mixer = AudioSystem.getMixer(aInfos[i]);
//                        }
//                    }
////                    line = (SourceDataLine) AudioSystem.getLine(info);
//                    line = (SourceDataLine) mixer.getLine(info);
//                    line.isOpen();
//                    line.open(targetFormat);
//                    setVolume(-35);
//                }
//                catch (LineUnavailableException e)
//                {
//                    e.printStackTrace();
//                }
//                catch (Exception e)
//                {
//                    e.printStackTrace();
//                }
//                if (line != null)
//                {
//                  line.start();
//                  int nBytesRead = 0;
//                  while (nBytesRead != -1)
//                  {
//                      nBytesRead = decodedInputStream.read(data, 0, data.length);
//                      if (nBytesRead != -1) line.write(data, 0, nBytesRead);
//                  }
//                  line.drain();
//                  line.stop();
//                  line.close();
//                  decodedInputStream.close();
//                } 
//                audioInputStream.close();
//            } catch (UnsupportedAudioFileException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }
//    
//    class MyPlaybackListener extends PlaybackListener {
//        
//    }