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

package jm.audio.io;

import java.io.*;
import jm.music.data.Note;
import jm.audio.AudioObject;
import jm.audio.Instrument;
import jm.audio.AOException;

/**
 * Reads in Audio data in the AU/SND file format and casts that data
 * into 32 floating point values which is the internal format used in
 * the jMusic audio architecture.<br>
 * Currently support is only provided for reading in 8bit, 16bit 24 bitand
 * 32bit(floating point) PCM files although any sampleing rate is supported.
 * @author Andrew Sorensen 
 * @version 1.0,Sun Feb 25 18:42:41  2001
 */
public final class SampleIn extends AudioObject implements jm.JMC{
	//----------------------------------------------
	// ATTRIBUTES
	//----------------------------------------------
	/** Debug constant specific to AUIn file */
	private boolean DEBUG_AUIn = DEBUG && true;
    
	/** The offset of sound data from the beginning of the file */
	private int offset;
	/** The number of bytes in this AU file */
	private int numOfBytes;
	/** The sound format to use 
		(5 = 32 bit no compression)
		(4 = 24 bit no compression)
		(3 = 16 bit no compression) 
		(2 = 8 bit no compression)*/
	private int format;
	/** The BIT selection to use */
	private int bitNum;
	/** The file associated with this class */
	private String fileName;
	/** number of sound samples */
	private int size;
	/** do we write the header in formation: useful for appending*/
	private boolean header = true;
	/** The input stream to read the file from */
	private FileInputStream fis;
	/** Buffer the input stream */
	private BufferedInputStream bis;
	/** A data input stream used to make reading the file easier */
	private DataInputStream dis;
	// the start timme in seconds
	private double offsetTime;
	// the length to be read
	private double audioDuration;
	// the number of bytes to read
	private int endSamples;
	//check the end of the
	private int endCount = 0;
	/** have we reached the end of the audio file */
	public boolean fin = false;
	/** should we cache the input file in memory (only read in once!) */
	public boolean cacheInput = false;
	/** if cache is set the true this is the cache */
	private float[] cache;
        /** play whole file ?   or only note length? */
        private boolean wholeFile = false;
        /** The size of the buffered input stream buffer */
        private int bisBufferSize = 4096;

	
	//----------------------------------------------
	// Constructors
	//----------------------------------------------
	/**
	 * This Constructor whose single argument is the name
	 * of the file to read data from.
         * This constructor is not used for instruments, but for
         * other sample reading tasks that applications might want to do.
	 * @param fileName the name of the file to read.
	 */
	//public SampleIn(String fileName){
		//this(new SimpleSampleInst(), fileName, 0.0);
               // this.fileName = fileName;
	//}
        
        /**
	 * This Constructor that names the instrument and the name
	 * of the file to read data from.
	 * @param Instrument The instrument that this is an audio object of.
	 * @param fileName the name of the file to read.
	 */
	public SampleIn(Instrument inst, String fileName){
		this(inst, fileName, 0.0);
	}
	
	/**
	 * This Constructor that names the instrument and a filename as an argument but
	 * also assigns an offset time for reading sample data
	 * from the file. This is set so that information start
	 * being read from any place in the file.
	 * @param Instrument The instrument that this is an audio object of.
	 * @param fileName the name of the file to read in.
	 * @param offsetTime the offset time is the place in 
	 * the file to start reading from expressed as a jMusic
	 * rhythmValue.
	 */
	public SampleIn(Instrument inst, String fileName, double offsetTime){
		super(inst, 0, "[SampleIn]"); //sampleRate instead 0
		this.fileName = fileName;
		this.offsetTime = offsetTime;
		this.read();
	}

	//----------------------------------------------
	// Public methods
	//----------------------------------------------
    /**
    * Return the current status for playing all the
    * file before moving on or else ending file
    * reading when the note duration is up.
    */
    public boolean getWholeFile(){
        return this.wholeFile;
    }
    
    /**
    * Will the whole file be read or will the read
    * cut off after the note duration ends?
    * @param wholeFile True if the whole file is to be read.
    */
    public void setWholeFile(boolean wholeFile){
        this.wholeFile = wholeFile;
    }
    
    /**
    * Change the file that is read by this audio object
    */
    public void setFileName(String fn){
        this.fileName = fn;
    }
    
    /**
    * Return the length of the sample in Bytes.
    * Call the read() method before calling this method!
    */
    public int getNumOfBytes() {
        return this.numOfBytes;
    }
    
    /**
    * Read in a sample from the file and pass it down
    * the audio chain. We need to have the switch statement
    * so that we can read in multple bit sizes. The input to
    * this method is bogus as it is always the first in the
    * audio chain and therefore receives no input.
    * @param input bogus input here to fit in.
    */
    public int work(float[] buffer)throws AOException{
		int count=0;
                float tmp = (float)0.0;
		if(cacheInput){ //if cacheing don't read from disk
			int ret = 0;
			for(;ret<buffer.length;ret++){
				buffer[ret] = cache[ret];
			}
			return ret;
		}
		//we must not be cacheing so we need to read from disk
		for(; count<buffer.length; count++){
			try{
				switch(format){
				case 2:
					tmp = ((float)this.dis.readByte()/(float)EIGHT_BIT);
					break;
				case 3:
					buffer[count] = ((float)this.dis.readShort()/(float)SIXTEEN_BIT);
					//System.out.println(count+"  "+buffer[count]);
					break;
				case 5:
					tmp = ((float)this.dis.readInt()/(float)THIRTY_TWO_BIT);
					break;
				default:
					System.out.println("jMusic does not currently support this format");
					System.exit(1);
				}
			}catch(EOFException eofe){
				this.fin = true;
                                this.finished = true;
				count = buffer.length;
				break;
				//this is supposed to happen ;)
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		return count;
	}

	/**
	 * This method does pretty much the same thing as the work 
	 * method but is included as an easy way to seed the 
	 * wave table of the WaveTable object. It is different 
	 * from the work method in that it does not send one sample
	 * at a time to the next audio object but instead returns
	 * a complete array of samples to whichever method calls it
	 * @return the array of samples which form this AU/SND file.
	 */
	public float[] getSamples(){
		float[] tmp = new float[this.numOfSamples];
		return this.getSamples(tmp);
	}
	
        /**
        * This method reads samples as specified by the
        * array size, starting from a position in the file
        * that is 'startPos' number of samples from the current
        * poisition in the file.
        * To ensure this is a count from the beginning of the
        * file make a call to read() before calling this method.
        * @param tmp An array to be filled with samples.
        * @param startPos The sample position to start reading from.
        */
        public float[] getSamples(float[] tmp, int startPos){
            //System.out.println("Getting samples" + format);
            try {
                // 8 bit file
                if (format == 2) {
                    if (startPos * channels > bisBufferSize) {
                        //System.out.println("skipping");
                        fis.skip((long)(startPos * channels - bisBufferSize));
                        float[] temp = getSamples(new float[bisBufferSize]);
                    }
                }
                // 16 bit file
                if (format == 3) {
                    if (startPos * 2 * channels > bisBufferSize) {
                        //System.out.println("skipping");
                        fis.skip((long)(startPos * 2 * channels - bisBufferSize));
                        float[] temp = getSamples(new float[bisBufferSize]);
                    }
                }
                // 24 bit file
                if (format == 4) {
                    if (startPos * 3 * channels > bisBufferSize) {
                        //System.out.println("skipping");
                        fis.skip((long)(startPos * 3 * channels - bisBufferSize));
                        float[] temp = getSamples(new float[bisBufferSize]);
                    }
                }
                // 32 bit file
                if (format == 5) {
                    if (startPos * 4 * channels > bisBufferSize) {
                        //System.out.println("skipping");
                        fis.skip((long)(startPos * 4 * channels - bisBufferSize));
                        float[] temp = getSamples(new float[bisBufferSize]);
                    }
                }
                // get the samples
            } catch (IOException e) {System.err.println("jMusic SampleIn error: Can't skip.");}
            return getSamples(tmp);
        }
        
	/**
	* This method does pretty much the same thing as the work
	* method but is included as an easy way to seed the
	* wave table of the WaveTable object. It is different
	* from the work method in that it does not send one sample
	* at a time to the next audio object but instead returns
	* a complete array of samples to whichever method calls it
	* @param tmp An array to be filled with samples.
	* @return the array of samples which form this AU/SND file.
	*/
	public float[] getSamples(float[] tmp){
		int i = 0;
		while(i<tmp.length){
			try{
				switch(format){
				case 2:
					tmp[i++] = ((float)this.dis.readByte()/(float)EIGHT_BIT);
					break;
				case 3:
					tmp[i++] = ((float)this.dis.readShort()/(float)SIXTEEN_BIT);
					break;
				case 5:
					tmp[i++] = ((float)this.dis.readInt()/(float)THIRTY_TWO_BIT);
					break;
				default:
					System.out.println("jMusic does not currently support this file format");
					System.exit(1);
				}
			}catch(EOFException eofe){
				this.fin = true;
				this.finished = true;
                                System.out.println ("Sample In: End of file");
				return tmp;
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		return tmp;
	}		

	/**
	 * How many channels are there in this audio file?
	 */
	public int getChannels(){
		return this.channels;
	}
	
	/**
	 * What is the sample rate of this audio file?
	 */
	public int getSampleRate(){
		return this.sampleRate;
	}
        
        /**
        * What is the sample bit size of this audio file?
        */
	public int getBitResolution(){
            int size = 0;
            if (format == 2) size = 8;
            if (format == 3) size = 16;
            if (format == 4) size = 24;
            if (format == 5) size = 32;
            return size;
	}
        
        /*
        * Returns the number of samples in one track of the file.
        * i.e., the wave length, or size.
        */
        public int getWaveSize() {
            return (int)((double)numOfBytes / (double)channels / (double)(format -1));
        }
        
        /**
        * Returns the format (bit resolution) of this file.
        * (5 = 32 bit no compression)
        * (4 = 24 bit no compression)
        * (3 = 16 bit no compression) 
        * (2 = 8 bit no compression)
        * @return int The format, subtract 1 for the bumber of bytes per sample.
        */
        public int getFormat() {
            return this.format;
        }

	/**
	 * Reads an AU file header and puts us in the right
	 * place to start reading the file from if we want to 
	 * impose an Audio offset (i.e. if we don't want to 
	 * start reading samples from the start of the file)
	 * @param fileName the file to read from
	 */
	public void read(){
		try{
			if (this.dis != null) this.dis.close();
			if (this.bis != null) this.bis.close();
			if (this.fis != null) this.fis.close();
			this.fis = new FileInputStream(this.fileName);
			this.bis = new BufferedInputStream(fis, bisBufferSize);
			this.dis = new DataInputStream(bis);
			//Check files validity
			if(dis.readInt() != 0x2E736E64){
				System.out.println("This file is NOT .au/.snd file format");
				return;
			}
			offset = dis.readInt();
			if(DEBUG) System.out.println("The offset is " + offset);
			this.numOfBytes = dis.readInt();
			if(DEBUG) System.out.println("The number of bytes in this file is "+numOfBytes);
			this.format = dis.readInt();
			if(DEBUG) System.out.println("The format of this file is " + format);
			this.sampleRate = dis.readInt();
			if(DEBUG) System.out.println("Sample rate of input file is " + sampleRate);
			this.channels = dis.readInt();
			if(DEBUG) System.out.println("The number of channels for this file "+channels);
			//skip the rest of the header
			fis.skip(offset - 24);
                        //
			/* set the bit depth */
		    switch(this.format){
			case 0:
				System.out.println("Not a valid au bit and compression format");
				return;
			case 1:
				System.out.println("8 Bit u-Lax G.711 is not currently supported");
				return;
			case 2:
				if(DEBUG)System.out.println("Setting 8bit linear sound ....");
				this.bitNum = EIGHT_BIT;
				break;
			case 3:
				if(DEBUG)System.out.println("Setting 16bit linear sound ....");
				this.bitNum = SIXTEEN_BIT;
				break;
			case 4:
				System.out.println("24 Bit linear sound is not currently supported");
				return;
			case 5:
				if(DEBUG)System.out.println("Setting 32bit linear sound ....");
				this.bitNum = THIRTY_TWO_BIT;
				break;
			default:
				System.out.println("This bit rate and compression format is not currently " +
											  "supported.");
				return;
			}
			if(cacheInput){ //if cacheing audio data
				int samples = this.numOfBytes/2;
				cache = new float[samples];
				for(int i=0;i<samples;i++){
					cache[i] = (float)dis.readShort()/(float)SIXTEEN_BIT; 
				}
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.out.println(ioe);
		}
	}

    /**
    * Retrieve the offset time from note
    */
    public void build(){
        if(wholeFile)this.finished = false;
        if(!cacheInput)this.read();
        
        try {
            long bytes = ((long)((double)currentNote.getSampleStartTime()*
                (double)this.sampleRate * (this.format - 1) * this.channels));
            // make sure its at the start of a sample
            while( bytes%4 != 0) bytes++;
            // jump to that location
            fis.skip(bytes);
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
    
    }
}
