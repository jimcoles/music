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

import jm.audio.AudioChainListener;
import jm.audio.Instrument;
import jm.audio.AOException;
import jm.music.rt.RTLine;
import java.util.*;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.*;

 /**
  * RTMixer is responsible for convolving the audio signals being pulled from
  * n number of RTLines.  RTMixer uses a single JMF Java Sound SourceDataLine
  * object for writing the newly convolved signal to the audio device.
  * Buffers of audio sample data are passed to the SourceDataLine at a rate
  * set by the Control Rate value.  The contol rate sets the size of the audio
  * buffers used by RTMixer, SourceDataLine and Instrument.   
  *
  * @author Andrew Sorensen 
  * @version 1.0,Sun Feb 25 18:42:43  2001
  */
public class RTMixer implements AudioChainListener{
	//------------------------------------------
	//Attributes
	//------------------------------------------
	/** The number of lines in the RTLine array */
	private int totLines = 0;
	/** count shows how many RTLines have passed RTMixer their full buffers */
	private int count = 0;
	/** sampleArray contains the convolution of all RTLines buffers */ 
	private float[] sampleArray;
	/** bos is used to convert sampleArray into a byte stream */
	private ByteArrayOutputStream bos;
	/** dos is used to help convert sampleArray into bos */
	private DataOutputStream dos;
	/** dline is the JFM java sound object which we write sampleArray to */
	private SourceDataLine dline;
	/** The sampleRate to be used when establishing the JMF SourceDataLine */
	protected int sampleRate;
	/** The number of channels to be used when setting up SourceDataLine */
	protected int channels;
	/** A Timer which keeps track of how many samples have been written since
	 * this object started */
	public long currentTime = 0;
	/** The control rate is used to set how often in instrument returns a full
	 * buffer.  This is acheived by changing the size of the instrument buffers
	 */
	protected double controlRate;
	/** How far into the score we are in terms of beats */
	private double scorePosition = 0.0;
	/** RTLines associated with this RTMixer object */
	private RTLine[] rtlines;
	/** buffer size */
	private int bufferSize;

	//-------------------------------------
	//Constructors
	//-------------------------------------
	/**
	 * The RTMixer constructor sets a number of attributes and opens a JMF java
	 * sound sourceDataLine.
	 * @param inst the Instruments to be processed by this object.
	 * @param bufferSize sets the size of the SourceDataLines buffer
	 * @param sampleRate the sampleRate for the SourceDataLine
	 * @param channels the number of channels for the SourceDataLine
	 * @param controlRate sets the controlRate for this object.
	 */
	public RTMixer(RTLine[] rtlines, int bufferSize, int sampleRate, 
                        int channels, double controlRate){
		this.rtlines = rtlines;
		for(int i=0;i<rtlines.length;i++){
			this.totLines += this.rtlines[i].getNumLines();
		}
		//System.out.println("Total Lines: "+this.totLines);
		this.controlRate = controlRate;
		this.sampleRate = sampleRate;
		this.channels = channels;
		this.bufferSize = bufferSize;
		initJMFSound(bufferSize);
		bos = new ByteArrayOutputStream();
		dos = new DataOutputStream(bos);
	}

	//-------------------------------------
	//Abstract Methods
	//-------------------------------------

	//--------------------------------------
	//Public Methods
	//---------------------------------------
	/**
	 * The controlChange method is called every time an instrument fills a
	 * sample buffer.  This method is responsible for receiving the sample
	 * buffer and convolving it with the data in sampleArray.
	 * @param buffer a sample array filled by an instrument
	 * @param returned the number of samples in the buffer
	 * @param finished indicates whether the instruments current note is 
	 * finished or not.
	 */
	public synchronized void controlChange(float[] buffer, int returned, boolean finished){
		for(int i=0;i<returned;i++){
			sampleArray[i] += buffer[i];
		}	
		if(++count == totLines){
			this.scorePosition += controlRate;
			for(int j=0;j<this.rtlines.length;j++){
				Instrument[] inst = rtlines[j].getInstrument();
				for(int i=0;i<inst.length;i++){
					inst[i].release();
				}
			}
			count = 0;
			this.writeOutAudio(sampleArray.length);
		}
	}

	/**
	 * Begin starts RTMixer.
	 */
	public void begin(){
		this.sampleArray = new float[bufferSize];
		for(int i=0;i<rtlines.length;i++){
			rtlines[i].start(this.scorePosition,this);
		}
	}
        
        /**
	 * Pauses RTMixer playback.
	 */
	public void pause(){
		for(int i=0;i<rtlines.length;i++){
			rtlines[i].pause();
		}
	}
        
        /**
	 * Continues the RTMixer playback.
	 */
	public void unPause(){
		for(int i=0;i<rtlines.length;i++){
			rtlines[i].unPause();
		}
	}

	/**
	 * This method passes on external action requests (i.e. gui based action
	 * events) to each RTLines externalAction method).
	 * @param obj an unspecified object type (externalAction will cast)
	 * @param actionNumber an indentifyer for the originator of the action
	 * request (i.e. if there are three buttons there would actionNumbers 1,2
	 * and 3)
	 */
	public void actionLines(Object obj, int actionNumber){
		for(int i=0;i<rtlines.length;i++){
			rtlines[i].externalAction(obj,actionNumber);
		}
		//I need to develop some action listeners for this
	}

	//-----------------------------------------
	//Private Methods
	//-----------------------------------------
	/**
	 * This method writes out the convolved sampleArray to the SourceDataLine 
	 * @param length the number of samples to write
	 */
	private void writeOutAudio(int length){
		bos.reset();
		for(int i=0; i<length; i++){
                        // scale to avoid clipping
                        if (this.totLines > 1)
                            this.sampleArray[i] = this.sampleArray[i] / (this.totLines * 0.75f);
			try{
				dos.writeShort((short)(this.sampleArray[i]*32767));
			}catch(IOException ioe){ioe.printStackTrace();}
			this.sampleArray[i] = (float)0.0;
		}
		int returned = this.dline.write(bos.toByteArray(), 0, bos.size());
		this.currentTime += (long)length;
	}


	/**
	 * This method creates an instance of the JMF SourceDataLine object.  This
	 * becomes the sink for all sample data leaving jMusic.
	 * @param bufferSize the size to set the SourceDataLine's buffer size to.
	 */
	private void initJMFSound(int bufferSize){
		//Set up jmf audio stuff
	//	AudioFormat af = new AudioFormat((float)this.sampleRate,16,
	//	this.channels,true,true);
		AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    (float)this.sampleRate, 16, this.channels, this.channels*2, 
                    this.sampleRate,true);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class,af);
		//System.out.println("Setting for audio line: "+info);
		if(!AudioSystem.isLineSupported(info)){
			System.out.println(info);
			System.err.println("JMF Line not supported. Real time audio must be 16 bit stereo ... exiting .. so there : (");
			System.exit(1);
		}
		try{
			this.dline = (SourceDataLine)AudioSystem.getLine(info);
			//multiply buffersize by 2 because this is bytes not shorts
			this.dline.open(af, bufferSize*8);
			this.dline.start();
		}catch(Exception e){e.printStackTrace();}
	}
        
        public void finalize() {
            try {
                dos.close();
                bos.close();
            } catch(IOException e) {}
        }
}
