package org.jkcsoft.music.theory;

import javax.sound.sampled.AudioFormat;

/**
 * @author Jim Coles
 */
public class Formats {

    // from a debug session dive into a "Line" object. ==>
    //12 = {AudioFormat@731} "PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, little-endian"
    //13 = {AudioFormat@732} "PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, big-endian"

    public static final AudioFormat BEST_MUSIC_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, true);
}
