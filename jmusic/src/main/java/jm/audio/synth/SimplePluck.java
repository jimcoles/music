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

import jm.music.data.Note;
import jm.audio.AudioObject;
import jm.audio.Instrument;
import jm.audio.AOException;

/**
 * @author Andrew Sorensen
 * @version 1.0,Sun Feb 25 18:42:48  2001
 */

public final class SimplePluck extends AudioObject{
	//----------------------------------------------
	// Attributes 
	//----------------------------------------------
	private int index = 0;
	float[] kernel = null;

	//----------------------------------------------
	// Constructors 
	//----------------------------------------------
	/**
	 */
	public SimplePluck(Instrument inst,int sampleRate, int channels){ 
		super(inst, sampleRate,"[FixedValue]");
		this.channels=channels;
	}
	
	/**
	 */
	public int work(float[] buffer)throws AOException{
            float prevSample = 0;
            int i=0;
            if(index >= kernel.length) index = 0;
            for(;i<buffer.length;i++){
                buffer[i] = kernel[index];
                kernel[index] = (kernel[index]+prevSample)*(float)0.5;
				prevSample = buffer[i];
                index++;
                if(index >= kernel.length) index = 0;
            }
            return i;
	}

	/**
	 */
   	public void build(){
		double freq = FRQ[currentNote.getPitch()];
		int length = (int)((double)sampleRate / freq);
		this.kernel = new float[length];
		for(int i=0;i<length;i++){
			kernel[i] = (float)(Math.random()*2.0 - 1.0);
		}
	}
}
