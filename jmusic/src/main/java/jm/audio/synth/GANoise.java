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
* The GANoise class produces noise that varies by random mutatation. 
 * @author Andrew Brown 2003
 */
public class GANoise extends AudioObject{
	//----------------------------------------------
	// Attributes
	//----------------------------------------------
	
	private float amp = 1.0f;
        private boolean firstBuffer = true;
        private float[] buf;
	//----------------------------------------------
	// Constructors
	//----------------------------------------------
	/**
	 * Tdefault constructor
	 * @param Instrument the class instance 'this'
	 */
	public GANoise(Instrument inst){
	    this(inst, 44100);
	}
	
	/**
	 * This constructor sets this object up as a noise generator
	 * allowing you to specify the type of noise and sample rate
	 * @param Instrument the class instance 'this'
	 * @param sampleRate the sampling rate
	 */
	public GANoise(Instrument inst, int sampleRate){
	    this(inst, sampleRate, 1);
	}
	
	/**
	 * This constructor sets this object up as a noise generator
	 * with all parameters
	 * @param Instrument the class instance 'this'
	 * @param sampleRate the sampling rate
	 * @param channels the number of channels to use
	 */
	public GANoise(Instrument inst, int sampleRate, int channels){
		super(inst, sampleRate, "[GANoise]");
		this.channels = channels;
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

	//----------------------------------------------
	// Methods
	//----------------------------------------------
	/**
	 * Returns a random sample value to each channel 
	 * @param buffer The sample buffer.
	 */
	public int work(float[] buffer)throws AOException{
            if (firstBuffer) initialise(buffer);
            // process
            int ret=0;
            /*
            for(;ret<buffer.length;ret++){
                    if(Math.random() < 0.001) buf[ret] = (float)(Math.random()*2.0 - 1.0) * amp;
            }
            */
            int width = 50;
            float peak = (float)((Math.random()*2.0 - 1.0) * amp);
            
            int loc = (int)(Math.random() * (buffer.length - width * 2));
            if(loc < 0) loc = 0;
            float startVal = buf[loc];
            float endVal = buf[loc + width * 2];
            float incUp = (peak - startVal) / (float)width;
            float incDown = (peak - endVal) / (float)width;
            for (int i=0; i<width; i++) {
                buf[loc +i] = startVal + incUp * (float)i;
            }
            for (int i=0; i<width; i++) {
                buf[loc + width + i] = peak - incDown * (float)i;
            }
            for(ret=0; ret<buffer.length; ret++) {
                buffer[ret] = buf[ret];
            }
            return ret;
        }
	
        // Set up for new note
        public void build() {
            firstBuffer = true;
        }
        
        // fill with silence
        private void initialise(float[] buffer) {
            buf = new float[buffer.length];
            for(int i=0; i<buf.length; i++) {
                buf[i] = 0;
            }
            firstBuffer = false;
        }
}
