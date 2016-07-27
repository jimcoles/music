package org.jkcsoft.music.theory;

/**
 * Created by jcoles on 7/22/16.
 * Copyright 2016 Jim Coles (jameskcoles@gmail.com).
 */
abstract public class AbstractNote {

    private double SLOP;

    abstract public double getFrequency();

    abstract boolean isRational();

    @Override
    public boolean equals(Object obj) {
        SLOP = .000001d;
        return Math.abs( ((AbstractNote) obj).getFrequency() - this.getFrequency() ) < SLOP;
    }
}
