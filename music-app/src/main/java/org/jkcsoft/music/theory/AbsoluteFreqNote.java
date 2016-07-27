package org.jkcsoft.music.theory;

/**
 * Created by jcoles on 7/22/16.
 * Copyright 2016 Jim Coles (jameskcoles@gmail.com).
 */
public class AbsoluteFreqNote extends AbstractNote {
    private double freq;

    public AbsoluteFreqNote(double freq) {
        this.freq = freq;
    }

    public double getFrequency() {
        return freq;
    }

    boolean isRational() {
        return false;
    }

    @Override
    public String toString() {
        return "AbsoluteFreqNote{" +
                "freq=" + freq +
                '}';
    }
}
