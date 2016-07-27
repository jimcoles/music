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
You should have received a copy of the GNU General Public Licens
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/
package jm.music.rt;

import jm.audio.Instrument;
import jm.audio.AOException;
import jm.music.data.*;
import jm.audio.AudioChainListener;
import jm.audio.RTMixer;


/**
 * @author Andrew Sorensen
 * @version 1.0,Sun Feb 25 18:43:31  2001
 */

public abstract class RTLine implements AudioChainListener{
	//--------------------------------------
	//Attributes	
	//--------------------------------------
	/** The instrument associated with this real time line */
	protected Instrument[] inst;
	/** if clear is true the threads wait call will be skipped */ 
	protected boolean clear = false;
	/** control rate */
	protected double controlRate;
	/** counter for how far we have gone between notes player */
	private double localCounter = 0.0;
	/** buffer size for the instrument */
	private int bufferSize = 0;
	/** number of instruments being run by this RTLine (polyphony) */
	private int lines;
	/** are we after a new note ? */
	private boolean newNote = true;
	/** tempo value */
	private double tempo = 60.0;
	/** test position ???? */
	private double testpos;
	/** This value is equal to (sampleRate * channels) */
	private double size;

	//--------------------------------------
	//Constructors
	//--------------------------------------
        /**
        * The RTLine constructor is primarily responsible for initialising its
        * associated instrument and the instruments audio chain.
        * @param inst the instrument associated with this RTLine
        * @param controlRate the frequency of control parameter updating
        */
        public RTLine(Instrument[] inst, double controlRate) {
            this(inst, controlRate, inst[0].getBufSize());
        }
        
        public RTLine(Instrument[] inst, int sampleRate, int channels, double controlRate) {
            this(inst, controlRate, inst[0].getBufSize(), sampleRate, channels);
        }
        
        /**
	 * The RTLine constructor is primarily responsible for initialising its
	 * associated instrument and the instruments audio chain.
	 * @param inst the instrument associated with this RTLine
          * @param controlRate the frequency of control parameter updating
          * @param bufferSize the sample size of the buffer, effects latency and efficiency
	 */
	public RTLine(Instrument[] inst, double controlRate, int bufferSize){
            this(inst, controlRate, bufferSize, 44100, 1);
        }
        
        public RTLine(Instrument[] inst, double controlRate, int bufferSize, int sampleRate, int channels){
		this.inst = inst;
		for(int i=0;i<inst.length;i++){
			inst[i].addRTLine(this);
		}
		this.controlRate = controlRate;
		this.bufferSize = bufferSize;
		this.lines = inst.length;
		this.size = sampleRate * channels; //((double)bufferSize)/controlRate; 
	}		

	//--------------------------------------
	//Public Methods
	//--------------------------------------
	/**
	 * Returns the instrument associated with this RTLine
	 * @return Instrument the instrument associated with this RTLine
	 */
	public Instrument[] getInstrument(){
		return inst;
	}

	/** Return the number of lines (note polyphony for this RTLine */
	public int getNumLines(){
		return this.lines;
	}

	/** sets the tempo of this RTLine */
	public void setTempo(double tempo){
		this.tempo = tempo;
	}

	/**
	 * External action is called by RTMixer whenever an external event is sent
	 * to trigger a real time audio event.  The event will commonly be
	 * triggered by a GUI widget such as a button or slider.
	 * @param obj and undetermined Object which will need to be cast locally to
	 * whatever type is expected. 
	 * @param actionNumber is an serial index value for the source of the
	 * event.
	 */
	public void externalAction(Object obj, int actionNumber){
		//Is there anything we want done by default ??
	}

	/**
	 * controlChange is called by RTLine's instrument everytime it completes
	 * filling its current buffer. The regularity at which controlChange is
	 * called is what determines the control rate. The control rate is used
	 * therefore to set the buffer sizes of the instrument sample buffers.
	 * @param buffer a buffer of samples passed from an instrument
	 * @param returned the number of samples in the buffer
	 * @param finished this boolean indicates whether the instrument has
	 * finished processing its current note.
	 */
	public synchronized void controlChange(float[] buffer, int returned, boolean finished){
		//do nothing here unless overriden
	}

	/**
	 * This method is called from Instrument and in return calls that
	 * instrument's renderNote method. When this method returns, the instruments
	 * iterateChain method is called and the note is processed.  This method
	 * is responsible for either fetching a "playable" note from the getNote()
	 * method or else for inserting a rest of an appropriate amount of time.
	 */
	public void instNote(Instrument inst,long samplesProcessed){
		Note note = null;
		double scorePos = ((double)samplesProcessed)/size;
		double temp = 60.0 / this.tempo;
		if(scorePos > (testpos - 0.001) && scorePos < (testpos + 0.001)){
			note = getNote().copy();
			note.setRhythmValue(note.getRhythmValue()*temp);
			note.setDuration(note.getDuration()*temp);
			testpos += note.getRhythmValue();	
		}else{
			note = new Note(jm.JMC.REST,(testpos-scorePos));
			note.setRhythmValue(note.getRhythmValue()*temp);
			note.setDuration(note.getRhythmValue());
		}
		inst.renderNote(note,scorePos);
	}

	/**
	 * start this RTLines instrument call for the setting of the first note
	 */
	public void start(double scorePosition, RTMixer rta){		
		for(int i=0;i<inst.length;i++){
			try{
				inst[i].createChain();
				inst[i].setBufSize(bufferSize);
				inst[i].addAudioChainListener(rta);
			}catch(AOException aoe){
				aoe.printStackTrace();
			}
		}
		for(int i=0;i<inst.length;i++){
			inst[i].start();
		}
	}
        
        /*
        * Halt the playback.
        */
        public void pause() {
            for(int i=0;i<inst.length;i++){
			inst[i].pause();
		}
        }
        
        /*
        * Continue the playback.
        */
        public void unPause() {
            for(int i=0;i<inst.length;i++){
			inst[i].unPause();
		}
        }

	/**
	 * Override this method to set the next method to be called.
	 */
	public abstract Note getNote();
}
