package org.jkcsoft.music.render;

import org.jkcsoft.music.theory.Formats;
import org.jkcsoft.music.util.AudioUtil;

import javax.sound.sampled.*;

import static org.jkcsoft.music.util.AudioUtil.print;

/**
 * @author Jim Coles
 */
public class AudioPlayer {

    private Mixer mixer;
    private LineListener lineListener;
    private SourceDataLine sourceDataLine;

    public AudioPlayer() {
        // TODO Add mixer selection logic instead of delegating to AudioSystem.

//        mixer.
        Mixer.Info[] mixersInfos = AudioUtil.dumpMixerInfo();
        Mixer.Info mixerInfo = mixersInfos[0];
        mixer = AudioSystem.getMixer(mixerInfo);
        //
        lineListener = event -> {
            print("line event: " + event);
        };
    }

    public void initLine(AudioFormat audioFormat) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceDataLine.addLineListener(lineListener);
        sourceDataLine.open(Formats.BEST_MUSIC_FORMAT);
        sourceDataLine.start();
        return;
    }

    public void playRawAudio(byte[] formattedBuffer) {
        try {
            sourceDataLine.write(formattedBuffer, 0, formattedBuffer.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeLine() {
        sourceDataLine.drain();
        print("closing audio line");
        sourceDataLine.stop();
        sourceDataLine.close();
        sourceDataLine.removeLineListener(lineListener);
    }

}
