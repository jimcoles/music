package org.jkcsoft.music.groovy;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jcoles on 7/22/16.
 * Copyright 2016 Jim Coles (jameskcoles@gmail.com).
 */
public class GNotes {

    public static void main(String[] args) {

        /*
         note peaks per ref

         3 / 2 =>
         4 / 2 ref (octave above ref)

         4 / 3
         5 / 3

         5 / 4
         6 / 4
         7 / 4

         6 / 5
         7 / 5
         8 / 5
         9 / 5



         not interested in fraction that is > 2 because that is an octave

         */
        int notesPerScale = 12;

        double refFreqHz = 110.0d;

        List<Integer> refNotePeaks = new LinkedList();
        for(int numPeaks = 1; numPeaks <= 100; numPeaks++) {
            refNotePeaks.add(numPeaks);
        }

        List<Integer> possibleNotePeaksPerRefPeaks;

        println("hello groovy")

    }
}
