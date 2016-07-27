/*

<This Java Class is part of the jMusic API version 1.4, February 2003.>

Copyright (C) 2000 Andrew Sorensen & Andrew Brown

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or any
later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package jm.audio.synth;

import java.io.IOException;
import jm.audio.AOException;
import jm.audio.AudioObject;
import jm.audio.Instrument;
import jm.music.data.Note;

/**
* The Noise class contains various noise waveform generators, 
* incluidng white noise and fractal noise.
 * @author Andrew Brown
 * @version 1.0,Sun Feb 25 18:42:52  2001
 */
public class Noise extends AudioObject{
	//----------------------------------------------
	// Attributes
	//----------------------------------------------
	/** 
	* A variable to choose different noise properties
	* 0 = white noise
	* 1 = low resolution (and frequency) noise
	* 2 = smoothed noise
        * 3 = brown noise
        * 4 = fractal noise
        * 5 = gaussian noise
	*/
	private int noiseType = 0;
	private int noiseDensity = 10;
	private float amp = 1.0f;
        // for fractal math
        private static float sum;
        private static float[] rg = new float[16];
        private static int k, kg, ng, threshold;
        private static int np = 1;
        private static int nbits = 1;
        private static int numbPoints = 48000; //number of notes
        private static float nr = (float)(numbPoints);
        private static float result;
        private static int counter = 0;
        // for gaussian noise
        private double standardDeviation = 0.25;
        private double mean = 0.0;
	
	// constants
	public static final int WHITE_NOISE = 0,
		STEP_NOISE = 1,
		SMOOTH_NOISE = 2,
		BROWN_NOISE = 3,
                FRACTAL_NOISE = 4,
                GAUSSIAN_NOISE = 5;
		
	//----------------------------------------------
	// Constructors
	//----------------------------------------------
	/**
	 * Tdefault constructor
	 * @param Instrument the class instance 'this'
	 */
	public Noise(Instrument inst){
	    this(inst, WHITE_NOISE);
	}
	
	/**
	 * This constructor sets this object up as a noise generator
	 * allowing you to specify the type of noise
	 * @param Instrument the class instance 'this'
	 * @param sampleRate the sampling rate
	 */
	public Noise(Instrument inst, int noiseType){
	    this(inst, noiseType, 44100);
	}
	
	/**
	 * This constructor sets this object up as a noise generator
	 * allowing you to specify the type of noise and sample rate
	 * @param Instrument the class instance 'this'
	 * @param sampleRate the sampling rate
	 * @param noiseType the flavour of noise to use
	 */
	public Noise(Instrument inst, int noiseType, int sampleRate){
	    this(inst, noiseType, sampleRate, 1);
	}
	
	/**
	 * This constructor sets this object up as a noise generator
	 * with all parameters
	 * @param Instrument the class instance 'this'
	 * @param sampleRate the sampling rate
	 * @param noiseType the flavour of noise to use
	 * @param channels the number of channels to use
	 */
	public Noise(Instrument inst, int noiseType, int sampleRate, int channels){
		super(inst, sampleRate, "[WaveTable]");
		this.noiseType = noiseType;
		this.channels = channels;
                // setup math for fractal noise
                if (noiseType == FRACTAL_NOISE) setUpFractalMath();
	}

	/**
	* Set the fixed amp of this Noise instance
	 * @param amp Fixed value amplitude
	 */
	public void setAmp(float amp){
		this.amp = amp;
	}

	/**
	* Get the fixed amp of this Noise instance
	 */
	public float getAmp(){
		return this.amp;
	}
	
	
	private void setUpFractalMath() {
            // setup math for fractal noise
            nr = nr/2;
    
            while (nr > 1) {
                nbits++;
                np = 2 * np;
                nr = nr/2;
            }
    
            for(kg=0; kg<nbits; kg++) {
                rg[kg] = (float)(Math.random());
            }
        }
	


	//----------------------------------------------
	// Methods
	//----------------------------------------------
	/**
	 * Returns a random sample value to each channel 
	 * @param buffer The sample buffer.
	 */
	public int work(float[] buffer)throws AOException{
	    int ret=0; //the number of samples to return
	    // run the appropiate code for the chosen noise type
		switch(this.noiseType){
		case WHITE_NOISE: 
			for(;ret<buffer.length;){
                            for(int j=0;j<channels;j++){ 
                                    buffer[ret++] = (float)(Math.random()*2.0 - 1.0) * amp;
                            }
			};
			break;
		case BROWN_NOISE: 
			float prev0 = 0.0f;
			float prev1 = 0.0f;
			float prev2 = 0.0f;
			float brownValue, current;
			for(;ret<buffer.length;){
                            for(int j=0;j<channels;j++){ 
                                current = (float)(Math.random()*2.0 - 1.0) * amp;
                                brownValue = (prev0 + prev1 + prev2  + current) / 4.0f;
                                buffer[ret++] = brownValue;
                                // update values
                                prev0 = prev1;
                                prev1 = prev2;
                                prev2 = current;
                            }
			};
			break;
		case STEP_NOISE:
			// low sample resolution noise (RandH noise)
			// has greater energy in the low frequency spectrum
			int density = this.noiseDensity;
			float temp = (float)(Math.random()*2.0 - 1.0) * amp;
			for(;ret<buffer.length;){
                            for(int j=0;j<channels;j++){
                                if (ret % density == 0) temp = 
                                    (float)(Math.random()*2.0 - 1.0) * amp;
                                buffer[ret++] = temp;
                            }
			};
			break;
		case SMOOTH_NOISE:
			// interpolated noise (RandI noise)
			// has an even greater emphasis on low frrquency energy
			density = this.noiseDensity;
			temp = (float)(Math.random()*2.0 - 1.0) * amp;
			float temp2 = (float)(Math.random()*2.0 - 1.0) * amp;
			for(;ret<buffer.length;){
                            for(int j=0;j<channels;j++){
                                if ((ret + 1) % density == 0) {
                                    buffer[ret++] = temp2;
                                    temp = temp2;
                                    temp2 = (float)(Math.random()*2.0 - 1.0) * amp;
                                } else {
                                    buffer[ret++] = temp + 
                                            ((temp2 - temp) / density * (ret % density));
                                }
                            }
			};
			break;
                case FRACTAL_NOISE:
                    for(;ret < buffer.length;){
                        for(int j=0;j<channels;j++){ 
                            if (counter%noiseDensity == 0) { //recalculate
                                threshold = np;
                                ng = nbits;
                                while(k%threshold != 0) {
                                    ng--;
                                    threshold = threshold / 2;
                                }
                                sum = 0;
                                for(kg=0; kg<nbits; kg++) {
                                    if(kg<ng) {rg[kg]=(float)(Math.random());}
                                    sum += rg[kg];
                                }
                                result = (float)(((sum/nbits) - 0.17) * 2.85 - 1.0);
                                if(result > 1.0) result = (float)1.0;
                                else if(result < -1.0) result = (float)-1.0;
                            }
                            counter++;
                            buffer[ret++] = result * amp;
                        }
                        if (counter > 67000) counter = 0;      
                    }
                    break;
                case GAUSSIAN_NOISE:
                    java.util.Random RNG = new java.util.Random();
                    float gaussValue;
                    for(;ret<buffer.length;){
                        for(int j=0;j<channels;j++){ 
                            gaussValue = (float)(RNG.nextGaussian() *
                                    standardDeviation + mean);
                            if (gaussValue < -1.0f) gaussValue = -1.0f;
                            else if (gaussValue > 1.0f) gaussValue = 1.0f;
                            buffer[ret++] = gaussValue * amp;
                        }
                    };
                    break;
		default:
                    System.err.println(this.name+" Noise type " + noiseType + 
                            " not supported yet");
                    System.exit(1);
		}
		
		return ret; 
	}
	
	/**
	* Specify the number of samples to set the same in 
	* the low and high noise wave forms.
	* The greater the value the less high frequency spectrum
	* will be in the LOW and SMOOTH noise types.
	*/
	public void setNoiseDensity(int newDensity) {
		this.noiseDensity = newDensity;
	}
        
        /**
        * Specify the standard deviation for gaussian noise.
        * The dafault for this in 0.25.
        */
        public void setStandardDeviation(double newValue) {
            this.standardDeviation = newValue;
        }
        
        /**
        * Specify the mean for gaussian noise.
        * The dafault for this in 0.0.
        */
        public void setMean(double newValue) {
            this.mean = newValue;
        }
}
