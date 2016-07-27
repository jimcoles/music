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

package jm.audio;

import java.io.IOException;
import jm.JMC;
import jm.music.data.Note;
import java.util.Vector;
import java.util.Enumeration;
import jm.music.rt.RTLine;

/**
 * An Instrument in jMusic is a chain of AudioObjects which are strung
 * together to form a signal processing chain.  Instruments chains are each
 * run in their own thread.
 *
 * @author Andrew Sorensen
 * @version 1.0,Sun Feb 25 18:42:44  2001
 */
public abstract class Instrument extends Thread implements Runnable, jm.JMC{
	//----------------------------------------------
	// Attributes
	//----------------------------------------------
	/** the number of samples processed by this instrument for this note */
	public int iterations;
	/** the primary audio chain objects (ie the first in each chain) */
	protected Vector primaryAO;
	/** the final audio chaine object */
	protected AudioObject finalAO = null;
	/** the number of samples which this Instrument needs to write */
	protected int numOfSamples = 0;
	/** the number of channels which this Instrument must supply */
	protected int numOfChannels = 0;
	/** buffer size for passing samples between work methods */
	protected int bufsize = 4096;
	/** Vector for holding AudioChainListeners */
	protected volatile Vector listeners = new Vector(); 
	/** number of samples processed by this instrument in its lifetime */
	protected long samplesProcessed = 0;
	/** This shows whether we can just substitue blank buffers (all zeros)*/
	private boolean restNote = false;
	/** 
	 * Real time export Buffer which is ALWAYS bufsize (i.e., not 
	 * changed to reflect a change in the notes duration which is why 
	 * we need an rtBufferIndex).
	 */
	private float[] rtBuffer = new float[bufsize];
  	/** Signifies whether this Instruments audio chain finished processing
	 * NOTE: This may be longer than both the notes duration and the
	 * notes rhythmvalue !
	 */ 
  	private boolean finished = true;
	/** if clear is true the threads wait call will be skipped */
	private boolean clear = false;
	/** should the inst block or not (RT should block, non-RT should not*/
	private boolean block = true;
	/** This is the rtline that this instrument is associated with (RT Only) */
	private RTLine rtline;
	/** Index value for rtBuffer */
	private int index = 0;
	public boolean finishedNewData = false;
	/** Has the creatChain method been called yet? */
	private boolean initialised = false;
        /** The instrument is free to render or play */
        private boolean okToRun = true;


	//----------------------------------------------
	// Constructors
	//----------------------------------------------
	/**
	 * Simple default constructor
	 */
	protected Instrument(){
		primaryAO = new Vector();
	}

	//----------------------------------------------
	// Class Methods
	//----------------------------------------------
	/**
	 * The renderNote method is called whenever the instrument is asked to
	 * render a new note.  This method is responsible for caclulating the
	 * number of samples required for the note and calls any AudioObjects 
	 * build methods prior to starting the chain.
	 * @param note The note to render
	 * @param startTime The startTime of the note
	 * @return true if available for a new note and false if still rendering
	 */
	public void renderNote(Note note, double startTime){
		this.finalAO = null;
		Enumeration enumElem = primaryAO.elements();
		//set the numOfSamples variable
		AudioObject pao = (AudioObject)primaryAO.elementAt(0);
		
		// continue onward
		this.numOfSamples = (int)((float)pao.getSampleRate()*(float)((float)note.getDuration()));
		this.numOfChannels = pao.channels;
								  
		//do any note specific initialization on the audio chain headers
		//which will propogate the build(note) method through the chain
		if(note.getFrequency() == (double)REST){
			restNote = true;
		}else{
			// calc frequency
			double frequency = 0.0;
			if (note.getPitchType() == Note.MIDI_PITCH  && note.getPitch() != JMC.REST) {
				frequency = jm.JMC.FRQ[note.getPitch()];
			} else {
				frequency = note.getFrequency();
			}
			if((double)pao.getSampleRate()*0.5 < frequency) {
				System.out.println("Sorry, can't render a note above the Nyquist frequency.");
				System.out.println("Sample rate = " + pao.getSampleRate() + " Pitch = " + note.getFrequency());
				System.exit(1);
			}

			// process
			restNote = false;
			while(enumElem.hasMoreElements()){
				AudioObject ao = (AudioObject)enumElem.nextElement();
				ao.newNote(note,(startTime-note.getOffset()),
						this.numOfSamples);
			}
		}
	}

	/**
	 * Add primary Audio Objects to the primaryAO vector
	 */
	public void addPrimaryAO(AudioObject ao){
		primaryAO.addElement(ao);
	}

	/**
	 * Sets the finalAO for this instrument
	 */
	public void setFinalAO(AudioObject ao) throws AOException{
		if(finalAO == null || finalAO == ao){
			this.finalAO = ao;
		}else{
			throw new AOException(ao.name,finalAO.name+
					" is already set as finalAO.\n"+
					"  There can only be one finalAO.");
		}
	}

        /** Is the instrument still processing a note? */
        public void setFinished(boolean state) {
            if(this.finished == false)return;
            this.finished = state;
        }
        
        /** Return the finished state of this note */
        public boolean getFinished(){
            return this.finished;
        }

        
	/**
	 * Sets the buffer size which is used to pass
	 * sample data between work methods.
	 * @param bufsize The buffer size to set for this instrument
    	 */
	public void setBufSize(int bufsize){
		this.bufsize = bufsize;
		rtBuffer = new float[bufsize];
	}

	/**
	 * Returns the buffer size being used by this instrument.
	 * @return bufsize The size of the buffer being used by this instrument
	 */
	public int getBufSize(){
		return this.bufsize;
	}

	/**
	* Tells if the instrument's creatchain method has been called or not.
	 */
	
	public void setInitialised(boolean val) {
		this.initialised = val;
	}

	/**
	* Returns theinitialsed status of the instrument.
	 * @return initialised
	 */	
	public boolean getInitialised() {
		return this.initialised;
	}

	/**
	 * Associates an RTLine with this Instrument
	 */
	public void addRTLine(RTLine rtline){
		this.rtline = rtline;
	}

	/**
	 * Return this instruments audio chain listeners
	 * @return listeners return all audio chain listeners
	 */
	public Enumeration getListeners(){
		return this.listeners.elements();
	}

	/**
	 * Attaches an AudioChainListener to this instrument
	 * @param listener the AudioChainListener to add
	 */
	public void addAudioChainListener(AudioChainListener listener){
		this.listeners.addElement(listener);
	}

	/**
	 * An array of double values which can be used as controller messages.
	 * Instruments that wish to implement controllers should override this
	 * method.  Controller values which are not doubles will need to be 
	 * cast to their appropriate types in the overriding method. 
	 * @param controlValues
	 */
	public void setController(double[] controlValues){}

	/**
	 * This threads run method
	 */
	public void run(){
            while(true) { //(this.okToRun){
                //??? called in real time only when idle??
                //Start pulling samples through the audio chain.
                this.finished = false;
                rtline.instNote(this,samplesProcessed);
                this.iterateChain();
            }
	}
        
        /*
        * Halts the playback thread.
        */
        public void pause() {
            this.block(); // ugly but effective for now
            //this.okToRun = false;
        }
        
        /*
        * Continues the playback thread.
        */
        public void unPause() {
            this.release(); // ugly but effective for now
            //this.okToRun = true;
        }

	/**
	 * Set whether the instrument should block or not.  This setting should be
	 * set to true if working in real-time and false if not working in real
	 * time.
	 */
	public void setBlock(boolean block){
		this.block = block;
	}

	/**
	 * If clear is set to true the iterate method will not block after calling
	 * controlChange on all its listeners.
	 */
	public void setClear(boolean clear){
		this.clear = clear;
	}

	/**
	 * release is called by RTAudio when the RTLine is ready to start
	 * processing data again.  This is required so that all instruments
	 * in turn provide the buffer they are currently processing to the
	 * real time audio stream
	 */
	public synchronized void release(){
		this.notify();
		this.clear = true;
	}

	/**
	 * block is called to hold any further processing untill all instruments
	 * are in the same place (in time that is).  Block calls wait on this
	 * instrument and is released by a call to release.  The RTAudio class is
	 * responsible for handling all block and release calls.
	 */
	public synchronized void block(){
		if(!this.clear && this.block){
			try{
				this.wait();
			}catch(InterruptedException ie){
				//Do anything ?
			}
		}
		this.clear = false;
	}
	//----------------------------------------------
	// Abstract Methods
	//----------------------------------------------
	/**
	 * This method is automatically called on startup to initialise
	 * any AudioObjects used by this instrument
	 */
	public abstract void createChain() throws AOException;

	//----------------------------------------------
	// Private Methods
	//----------------------------------------------
	/**
	 * The iterate chain method is resposible for pulling the appropriate
	 * number of samples through the audio chain.
	 */
	public void iterateChain() {		
		iterations = 0;
		if(this.numOfSamples > 0)
	   		iterations = this.numOfSamples * this.numOfChannels;
		int returned=0;
	 	while(!finished){
        	finished = true; //finish unless proved otherwise
			//finished = false;
			float[] buffer = null;
			if(iterations > bufsize || iterations <= 0){
				buffer = new float[bufsize];
                for(int i=0; i<buffer.length; i++){
                    buffer[i] = 0.0f;
                }
			}else {
				buffer = new float[iterations];
                for(int i=0; i<buffer.length; i++){
                    buffer[i] = 0.0f;
                }
			}
			try{
				if(restNote){
					returned = buffer.length;
				}else{
					returned = finalAO.nextWork(buffer); 
				}
			}catch(AOException aoe){
				System.out.println(aoe);
				System.exit(1);
			}
			iterations-=returned;
        	if(iterations > 0){
			finished = false;
		}
			samplesProcessed += returned;
			//put any new samples into rtBuffer and pass rtBuffer onto any 
			//listeners if index has reached bufsize
			for(int i=0;i<returned;i++){
				rtBuffer[index++] = buffer[i];
				if(index == bufsize){
					index = 0;
					Enumeration enumElem = listeners.elements();
					while(enumElem.hasMoreElements()){
						AudioChainListener acl=(AudioChainListener)enumElem.nextElement();
						acl.controlChange(rtBuffer,returned,finished);
					}
					this.block();
				}
			}
		}
	}
}
