package org.jkcsoft.music.theory;

/**
 * Created by jcoles on 7/22/16.
 * Copyright 2016 Jim Coles (jameskcoles@gmail.com).
 */
public class RationalNote extends AbstractNote {

    private Integer numPeaks;
    private Integer refNumPeaks;
    private AbsoluteFreqNote refNote;

    public RationalNote(AbsoluteFreqNote refNote, Integer numPeaks, Integer refNumPeaks) {
        this.numPeaks = numPeaks;
        this.refNumPeaks = refNumPeaks;
        this.refNote = refNote;
    }

    public Integer getNumPeaks() {
        return numPeaks;
    }

    public Integer getRefNumPeaks() {
        return refNumPeaks;
    }

    public AbsoluteFreqNote getRefNote() {
        return refNote;
    }

    public double getFrequency() {
        return (1.0d * numPeaks / refNumPeaks) * refNote.getFrequency();
    }

    boolean isRational() {
        return true;
    }


    @Override
    public String toString() {
        return "RationalNote{" +
                "" + numPeaks +
                " / " + refNumPeaks +
                ", frequency=" + getFrequency() +
                '}';
    }
}
