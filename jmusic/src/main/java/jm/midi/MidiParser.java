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
package jm.midi;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

import jm.JMC;
import jm.midi.*;
import jm.midi.event.*;
import jm.music.data.*;

/**
 * A MIDI parser 
 * @author Andrew Sorensen
 */


public final class MidiParser implements JMC{

	//-----------------------------------------------------------
	//Converts a SMF into jMusic Score data
	//-----------------------------------------------------------
	/**
	 * Convert a SMF into the jMusic data type
	 */
	public static void SMFToScore(Score score, SMF smf){
		System.out.println("Convert SMF to JM");
		Enumeration enumElem = smf.getTrackList().elements();
		//Go through tracks
		while(enumElem.hasMoreElements()){
			Part part = new Part();
			Track smfTrack = (Track) enumElem.nextElement();
			Vector evtList = smfTrack.getEvtList();
			Vector phrVct = new Vector();
			sortEvents(score,evtList,phrVct,smf,part);
			for(int i=0;i<phrVct.size();i++){
				part.addPhrase((Phrase)phrVct.elementAt(i));
			}
			score.addPart(part);
			score.clean();
		}
	}


	private static void sortEvents(Score score, Vector evtList, Vector phrVct, SMF smf, Part part){
		double startTime = 0.0;
		double[] currentLength = new double[100];
		Note[] curNote = new Note[100];
		int numOfPhrases = 0;
		double oldTime = 0.0;
		int phrIndex = 0;
		//Go through evts
		for(int i=0;i<evtList.size();i++){
			Event evt = (Event) evtList.elementAt(i);
			startTime+=(double)evt.getTime()/(double)smf.getPPQN();
			if(evt.getID() == 007){
				PChange pchg = (PChange)evt;
				part.setInstrument(pchg.getValue());
				//if this event is a NoteOn event go on
			}else if(evt.getID() == 020){
				Tempo t = (Tempo) evt;
				score.setTempo(t.getTempo());
			}else if(evt.getID() == 005){
				NoteOn noteOn = (NoteOn) evt;
				part.setChannel(noteOn.getMidiChannel());
				short pitch = noteOn.getPitch();
				int dynamic = noteOn.getVelocity();
				short midiChannel = noteOn.getMidiChannel();
				//if you're a true NoteOn
				if(dynamic > 0){
					noteOn(phrIndex,curNote,smf,i,
							currentLength, startTime, 
							phrVct,midiChannel,
							pitch,dynamic,evtList);
				}
			}
		}
	}

	private static void noteOn(int phrIndex, Note[] curNote,SMF smf,int i, 
			double[] currentLength, double startTime, Vector phrVct,
			short midiChannel, short pitch, int dynamic, Vector evtList){

		phrIndex = -1;
		//work out what phrase is ready to accept a note
		for(int p=0;p<phrVct.size();p++){
			//Warning 0.02 should really be fixed
			if(currentLength[p]<=(startTime+0.08)){
				phrIndex = p;
				break;
			}
		} 
		//need to create new phrase for a new voice?
		if(phrIndex == -1){
			phrIndex = phrVct.size();
			phrVct.addElement(new Phrase(startTime));
			currentLength[phrIndex] = startTime;
		}
		//Do we need to add a rest ?
		if((startTime > currentLength[phrIndex])&&
				(curNote[phrIndex] != null)){
			double newTime=startTime - currentLength[phrIndex];
			//perform a level of quantisation first
			if(newTime < 0.25){
				double length=
					curNote[phrIndex].getRhythmValue();
				curNote[phrIndex].setRhythmValue(
						length+newTime);
			}else{
				Note restNote =new Note(REST, newTime, 0);
				restNote.setPan(midiChannel);
				restNote.setDuration(newTime);
				restNote.setOffset(0.0);
				((Phrase) phrVct.elementAt(phrIndex)).
					addNote(restNote);
			}
			currentLength[phrIndex]+= newTime;
		}
		// get end time
		double time = MidiUtil.getEndEvt(pitch, evtList, i)/
			(double)smf.getPPQN();
		// create the new note
		Note tempNote = new Note(pitch,time, dynamic);
		tempNote.setDuration(time);
		curNote[phrIndex] = tempNote;
		((Phrase)phrVct.elementAt(phrIndex)).addNote(curNote[phrIndex]);
		currentLength[phrIndex] += curNote[phrIndex].getRhythmValue();
	}

	//------------------------------------------------------------------
	// Converts a score into a SMF
	//------------------------------------------------------------------
	// MODIFIED 6/12/2003 Ron Legere to avoid use of magic note values for Program CHanges
	// Etc.
	
	/**
	 * Converts jmusic score data into SMF  data
	 * @param Score score - data to change to SMF
	 * @exception Exception
	 */
	public static void scoreToSMF(Score score, SMF smf){
		if(VERBOSE) System.out.println("Converting to SMF data structure...");

		double scoreTempo = score.getTempo();
		double partTempoMultiplyer = 1.0;
		int phraseNumb;
		Phrase phrase1, phrase2;

		//Add a tempo track at the start of top of the list
		//Add time sig to the tempo track
		Track smfT = new Track();
		smfT.addEvent(new Tempo(0, score.getTempo()));
		smfT.addEvent(new TimeSig(0, score.getNumerator(),score.getDenominator()));
		smfT.addEvent(new KeySig(0, score.getKeySignature()));
		smfT.addEvent(new EndTrack());
		smf.getTrackList().addElement(smfT);
		//---------------------------------------------------
		int partCount = 0;
		Enumeration enumElem = score.getPartList().elements();
		Vector timeingList = new Vector();
		while(enumElem.hasMoreElements()){
			if(!timeingList.isEmpty()) timeingList.removeAllElements();

			Track smfTrack = new Track();
			Part inst = (Part) enumElem.nextElement();
			System.out.print("Part "+ partCount++ + " (Ch. " + inst.getChannel() + "): ");

			//order phrases based on their startTimes
			phraseNumb = inst.getPhraseList().size();
			for(int i=0; i< phraseNumb; i++){
				phrase1 = (Phrase) inst.getPhraseList().elementAt(i);
				for(int j=0; j<phraseNumb; j++){
					phrase2 = (Phrase)inst.getPhraseList().elementAt(j);
					if(phrase2.getStartTime() > phrase1.getStartTime()){
						inst.getPhraseList().setElementAt(phrase2, i);
						inst.getPhraseList().setElementAt(phrase1, j);
						break;
					}
				}
			}
			//break Note objects into NoteStart's and NoteEnd's
			//as well as combining all phrases into one list
			Vector startList = new Vector();


			if(inst.getTempo() != Part.DEFAULT_TEMPO){
				//System.out.println("Adding part tempo");
				timeingList.addElement(new Tempo(inst.getTempo()) );
				startList.addElement(new Double(0.0));
			}

			//if this part has a Program Change value of 16 or lessadd
			// Note < 16 is not checked
			if(inst.getInstrument() != NO_INSTRUMENT){
				
				timeingList.addElement(
					new PChange((short) inst.getInstrument(),(short) inst.getChannel(),0));
				startList.addElement(new Double(0.0));
			}

			if(inst.getNumerator() != NO_NUMERATOR){
				timeingList.addElement(new TimeSig(inst.getNumerator(),inst.getDenominator()));
				startList.addElement(new Double(0.0));
			}

			if(inst.getKeySignature() != NO_KEY_SIGNATURE){
			
				timeingList.addElement(new KeySig(inst.getKeySignature(),inst.getKeyQuality()));
				startList.addElement(new Double(0.0));
			}

			Enumeration e2 = inst.getPhraseList().elements();
			double max = 0;
			double startTime = 0.0;
			double offsetValue = 0.0;
			while(e2.hasMoreElements()) {
				Phrase phrase = (Phrase) e2.nextElement();
				Enumeration e3 = phrase.getNoteList().elements();
				startTime = phrase.getStartTime();


			

				//Shouldn't this section go after the next for loop ?????
				//As the next for loop sets a NoteOn partners NoteOn(NoteOFF)
				
				if(phrase.getInstrument() != NO_INSTRUMENT){
				//	Not sure completely that this is what is intended. untested
					timeingList.addElement(
					   new PChange((short)phrase.getInstrument(),(short)inst.getChannel(),0)); 
					startList.addElement(new Double(startTime));
				}

				/*
				   if(phrase.getTempo() != 0){
				   Note nn = new Note(TEMP_EVT,(double)phrase.getTempo());
				   timeingList.addElement(nn);
				   startList.addElement(new Double(startTime));
				   }
				 */
				////////////////////////////////////////////////

				while(e3.hasMoreElements()){

					Note note = (Note) e3.nextElement();
					offsetValue = note.getOffset();
					//check for frequency rather than MIDI notes
                                        int pitch = 0;
					if (note.getPitchType() == Note.FREQUENCY) {
						System.err.println("jMusic warning: converting note frequency to the closest MIDI pitch for SMF.");
						//System.exit(1);
                                                pitch = Note.freqToMidiPitch(note.getFrequency());
					} else pitch = note.getPitch();
					timeingList.addElement(new NoteOn((short)pitch, (short)note.getDynamic(), 
								(short)inst.getChannel(),0));
					startList.addElement(new Double(startTime + offsetValue));
				
					// Add a NoteOn for the END of the note with 0 dynamic, as recommended.
					timeingList.addElement(
					new NoteOn((short)pitch, (short)0, (short)inst.getChannel(),0));
					//create a timing event at the end of the notes duration
					double endTime = startTime + note.getDuration();
					// Add the note off time to the list
					startList.addElement(new Double(endTime + offsetValue));
					// move the note on time forward by the rhythmic value
					startTime += note.getRhythmValue(); //time between start times
                                        System.out.print(".");
				}
			}
			//Sort lists so start times are in the right order
			Enumeration start = startList.elements();
			Enumeration timeing = timeingList.elements();
			Vector sortedStarts = new Vector();
			Vector sortedEvents = new Vector();
			while(start.hasMoreElements()){
				double smallest = ((Double)start.nextElement()).doubleValue();
				Event anevent = (Event) timeing.nextElement();
				int index = 0, count = 0;
				while(start.hasMoreElements()){
					count++;
					double d1 = ((Double)start.nextElement()).doubleValue();
					Event event1 = (Event) timeing.nextElement();
					if(smallest == d1){ //if note time is equal
						if(zeroVelEventQ(event1)) {
							index = count;
						}
					}
					if(smallest > d1){
						smallest = d1;
						index = count;
					}
				}
				sortedStarts.addElement(startList.elementAt(index));
				sortedEvents.addElement(timeingList.elementAt(index));
				startList.removeElementAt(index);
				timeingList.removeElementAt(index);
				//reset lists for next run
				start = startList.elements();
				timeing = timeingList.elements();
			}
			
			//Add times to events, now that things are sorted 
			double st = 0.0; //start time
			int sortedSize = sortedEvents.size();
			for(int index=0;index<sortedSize;index++){
				Event event = (Event) sortedEvents.elementAt(index);
	
				// The remaining switch is needed to drop the 
				// rests and to change partTempMultiplier
				// Would prefer not to use 5 and 20 here. Perhaps
				// NoteOn.id, etc should be public statics so they could be used
				// instead of hard wired numbers
				
				switch(event.getID()){
					case 5:
						// check for rest, if so don't add this event
						if(((NoteOn)event).getPitch() == REST) continue;
						break;
					case 20:
						partTempoMultiplyer = scoreTempo / ((Tempo)event).getTempo();
						break;
					//default:
						
				}
				double sortStart=((Double)sortedStarts.elementAt(index)).doubleValue();
				int time = (int) ((((sortStart - st) * (double)smf.getPPQN()))* partTempoMultiplyer + 0.5);
				st = sortStart;
				event.setTime(time);
				smfTrack.addEvent(event);
			}
			smfTrack.addEvent(new EndTrack());
			//add this track to the SMF
			smf.getTrackList().addElement(smfTrack);
		}
		System.out.println();
	}
	// Helper function 
	//
	private static	boolean zeroVelEventQ(Event e) {
		  if(e.getID()==5) 
		  {
			  // its a NoteOn
		         if(((NoteOn)e).getVelocity() ==0) return true;
	  	  }
	 	 // most commonly:
	  	return false;
		
	}
	
}
