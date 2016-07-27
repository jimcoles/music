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

Enhanced by the Derryn McMaster


*/ 

package jm.util;

import jm.midi.MidiSynth;
import jm.music.data.*;
import jm.JMC;


public class Play implements JMC{
        /**
        * The current (only) one time playback thread.
        */
        private static PlayThread pt;
        /**
        * The current (only) repeated playback thread.
        */
        private static PlayCycle pc;
	/**
	* True if a midiCycle is currently playing
	*/
	private static boolean cyclePlaying = false;
        /**
	* True if a one time MIDI playback is currently playing
	*/
	private static boolean midiPlaying = false;
	
	/**
	* Constructor
	*/
	public Play() {}
	
	/**
	* Used by infinite cycle players (eg midiCycle())
	* to stop the playback loop.
	*/
	public static void stopCycle(){
		cyclePlaying = false;
                if (pc != null) pc.stopPlayCycle();
	}
	
	/**
	* Used by infinite cycle player threads to check
	* cyclePlaying status.
	*/
	public static boolean cycleIsPlaying(){
		return cyclePlaying;
	}
	
 /**
	* Thread.sleeps for a period of 1 score play length
	* (i.e. the time it would take for the specified 
	* score to play).
	* Can be used in conjunction with midiCycle() if the 
	* score requires a re-compute just before being
	* replayed.  (i.e. sleeps for one loop)  Should 
	* be placed immediately after the Play.midiCycle() 
	* command to ensure that the time waited is for 
	* the currently playing score, and not subject 
	* to any changes made to the score since.
	* @param Score The score used for timing the sleep.
	*/
	public static void waitCycle(Score s){
		try{	// wait duration plus 2 second for construction and reverb trail
			Thread.sleep((int)(1000.0 * 60.0 / s.getTempo() * s.getEndTime() + 2000));
		}catch(Exception e){e.printStackTrace();}
	}
	
	
	//----------------------------------------------
	// MidiSynth - JavaSound MIDI playback
	//----------------------------------------------
	/**
	* Playback the jMusic score JavaSound MIDI
	* @param Note The note to be played
	*/
	public static void midi(Note n) {
                midi(n, true);
	}	

	/**
	* Playback the jMusic score JavaSound MIDI
	* @param Phrase The Phrase to be played
	*/
	public static void midi(Phrase phr) {
                midi(phr, true);
	}

	/**
	* Playback the jMusic score JavaSound MIDI
	* @param Part The Part to be played
	*/
	public static void midi(Part p) {
                midi(p, true);
	}
	
	
	/**
	* Playback the jMusic score JavaSound MIDI using the default value of 
	* true for 'exit' - See Play.midi(Score,boolean)
	* @param Score: The score to be played.
	*/ 
	public static void midi(Score s) {
		midi(s,true);
	}
	
	
        /**
	* Playback the jMusic score JavaSound MIDI
	* @param Note The note to be played
        * @param exit Crash program after playabck? true or false
	*/
	public static void midi(Note n, boolean exit) {
		Score s = new Score("One note score", 120);
                s.addPart(new Part(new Phrase(n)));
                midi(s,exit);
	}	

	/**
	* Playback the jMusic score JavaSound MIDI
	* @param Phrase The Phrase to be played
        * @param exit Crash program after playabck? true or false
	*/
	public static void midi(Phrase phr, boolean exit) {
		Score s = new Score(phr.getTitle() + " score", 120);
                if (phr.getTempo() != Phrase.DEFAULT_TEMPO) s.setTempo(phr.getTempo());
                s.addPart(new Part(phr));
                midi(s,exit);
	}

	/**
	* Playback the jMusic score JavaSound MIDI
	* @param Part The Part to be played
        * @param exit Crash program after playabck? true or false
	*/
	public static void midi(Part p, boolean exit) {
		Score s = new Score(p.getTitle() + " score", 120);
                if (p.getTempo() != Part.DEFAULT_TEMPO) s.setTempo(p.getTempo());
                s.addPart(p);
                midi(s,exit);
	}
	
        /**
        * Playback the jMusic score JavaSound MIDI.
        * This method exits the application on completion.
        * To avoid this exit call, pass false as the second argument.
        * @param Score: The score to be played.
        * @param boolean exit: If true, System.exit(0) will be called at the end.
        */ 
	public static void midi(Score s, boolean exit) {
            if(midiPlaying) stopMidi();
            midiPlaying = true;
            Score defensiveCopy = s.copy();
            System.out.println("-------- Playing a jMusic Score with JavaSound MIDI ----------");
            System.out.println("Constructing '" + defensiveCopy.getTitle() + "'...");
            pt = new PlayThread(defensiveCopy);
            new Thread(pt).start();
            System.out.println("Playing '" + s.getTitle() + "' ...");
            if (exit) {
                try {
                    waitCycle(defensiveCopy);
                    System.out.println("-------------------- Completed Playback ----------------------");
                    System.exit(0); // horrid but less confusing for beginners
                }catch (Exception e) {
                    System.err.println("MIDI Playback Error:" + e);
                    return;
                }
            }
	}
        
        public static void stopMidi() {
            if (pt != null) {
                pt.stopPlayThread();
                midiPlaying = false;
            }
        }	
	
	/**
	* Repeated playback the jMusic score JavaSound MIDI
	* @param Note The note to be played. See midiCycle(Score s)
	*/
	public static void midiCycle(Note n) {
		Score s = new Score("One note score");
                s.addPart(new Part(new Phrase(n)));
                midiCycle(s);
	}	

	/**
	* Repeated playback the jMusic score JavaSound MIDI
	* @param Phrase The Phrase to be played. See midiCycle(Score s)
	*/
	public static void midiCycle(Phrase phr) {
		Score s = new Score(phr.getTitle() + " score");
                s.addPart(new Part(phr));
                midiCycle(s);
	}

	/**
	* Repeated playback the jMusic score JavaSound MIDI
	* @param Part The Part to be played.  See midiCycle(Score s)
	*/
	public static void midiCycle(Part p) {
		Score s = new Score(p.getTitle() + " score");
                s.addPart(p);
                midiCycle(s);
	}

	/**
	* Continually repeat-plays a Score object (i.e. loops).  If the Score object 
	* reference is altered, that alteration will be heard on the following cycle.
	* Score should be a minimum of 1 beat long.
	* NB: It takes a small amount of time to load the Score.  Due to the use of 
	* threads and a timer, no delay is heard between loops (i.e. the score is 
	* prepared just in time to be played).  However, this means that alterations 
	* made to the Score object near the end of the segment (ie final beats) may not
	* be heard for an extra loop.  Putting this another way, if a 1-bar Score is 
	* being looped, the next bar may be pre-loaded as the last beat is being played.
	* This means that any alterations made after this point will not appear in the 
	* very next loop.
	*/
	public static void midiCycle(Score s){
		if (cyclePlaying == true) stopCycle();
                cyclePlaying = true;
                pc = new PlayCycle(s);
		new Thread(pc).start();
	}
	
        
    /**
    * Playback an audio file jMusic audio playback via javaSound.
    * This method requires the javax.sound packages in Java 1.3 or higher.
    * @param String The name of the file to be played.
    */ 
    public static void au(String fileName) {
        au(fileName, true);
    }
    /**
    * Playback an audio file jMusic audio playback via javaSound.
    * This method requires the javax.sound packages in Java 1.3 or higher.
    * @param filename The name of the file to be played.
    * @param autoClose A flag for exiting java after the file has played.
    */ 
    public static void au(String fileName, boolean autoClose) {
        jm.audio.io.SampleIn si = new jm.audio.io.SampleIn(new jm.gui.wave.DummyInst(), fileName);
        jm.music.rt.RTLine[] lineArray = {new jm.util.AudioRTLine(fileName)};	
        jm.audio.RTMixer mixer = new jm.audio.RTMixer(lineArray, 4096, si.getSampleRate(), 
            si.getChannels(), 0.01);	
        mixer.begin();
        System.out.println("---------- Playing '" + fileName + "'... Sample rate = "
            +si.getSampleRate() + " Channels = " + si.getChannels() + " ----------");
       if (autoClose) {
            java.io.File audioFile = new java.io.File(fileName);
            try {
                int byteSize = si.getFormat() - 1;
                // bytes, sample rate, channels, milliseconds, cautious buffer
                Thread.sleep((int)((double)audioFile.length() / byteSize / 
                    si.getSampleRate() / si.getChannels() * 1000.0) + 1000);
            } catch (InterruptedException e) {
                System.err.println("jMusic play.au error: Thread sleeping interupted");
            }
            System.out.println("-------------------- Completed Audio Playback ----------------------");
            System.exit(0); // horrid but less confusing for beginners
        }
    }
        
                
    /**
    * Playback an audio file using Java Applet audioclip playback.
    * A audioClip limitation is that the file must be small enough to fit into RAM.
    * This method is compatibl with Java 1.1 and higher.
    * @param String The name of the file to be played.
    */ 
    public static void audioClip(String fileName) {
	  System.out.println("-------- Playing an audio file ----------");
	  System.out.println("Loading sound into memory, please wait...");
	  java.io.File audioFile = new java.io.File(fileName);
	  try {
	      java.applet.AudioClip sound = java.applet.Applet.newAudioClip(audioFile.toURL());
	      System.out.println("Playing '" + fileName + "' ...");
	      sound.play();
	  } catch (java.net.MalformedURLException e) {
	      System.err.println("jMusic play.au error: malformed URL or filename");
	  }
	  try {
	      // bytes, sample rate, channels, milliseconds, cautious buffer
	      Thread.sleep((int)(audioFile.length() / 2.0 / 44100.0 / 2.0 * 1000.0) + 1000);
	  } catch (InterruptedException e) {
	      System.err.println("jMusic play.au error: Thread sleeping interupted");
	  }
	  System.out.println("-------------------- Completed Playback ----------------------");
	  System.exit(0); // horrid but less confusing for beginners
	}
	
}


