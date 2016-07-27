package org.jkcsoft.music.synthesis;

import java.nio.ByteBuffer;

/**
 * Created by jcoles on 7/26/16.
 * Copyright 2016 Jim Coles (jameskcoles@gmail.com).
 */
public class SinWaveForm implements WaveForm {

    @Override
    public float[] genForm(int numberOfSamples) {
        float[] formFloats = new float[numberOfSamples];
        double sampleInc = (2 * Math.PI) / numberOfSamples;
//        int samplesPerQuarterCycle = numberOfSamples / 4;
        double radSample = 0.0d;
        // only compute sin( ) as many times as needed and the copy values into place
        for (int idxSample = 0; idxSample < numberOfSamples; idxSample++) {
            float sinValue = (float) Math.sin(radSample);
            formFloats[idxSample] = sinValue;
//            formFloats[idxSample + samplesPerQuarterCycle] = sinValue;
//            formFloats[idxSample + 2 * samplesPerQuarterCycle] = -sinValue;
//            if (idxSample + 3 * samplesPerQuarterCycle < numberOfSamples)
//                formFloats[idxSample + 3 * samplesPerQuarterCycle] = -sinValue;
            radSample += sampleInc;
        }
        return formFloats;
    }
}
