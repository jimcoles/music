package org.jkcsoft.music.util;

import org.jkcsoft.music.theory.Notes;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * Created by jcoles on 7/26/16.
 * Copyright 2016 Jim Coles (jameskcoles@gmail.com).
 */
public class AudioUtil {

    public static Mixer.Info[] dumpMixerInfo() {
        Mixer.Info[] mixersInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixersInfos) {
            print("mixalot: " + mixerInfo);
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] sourceLineInfos = mixer.getSourceLineInfo();
            for (Line.Info sourceLineInfo : sourceLineInfos) {
                print("  source line info: " + sourceLineInfo);
                try {
                    Line line = mixer.getLine(sourceLineInfo);
                    print("    line: " + line.getLineInfo());
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
            }
        }
        return mixersInfos;
    }

    public static void print(String msg) {
        System.out.println(Thread.currentThread().getName() + "|" + msg);
    }
}
