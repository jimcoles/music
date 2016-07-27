package org.jkcsoft.music.theory;

import jm.JMC;
import jm.midi.MidiSynth;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;
import org.jkcsoft.music.synthesis.SinWaveForm;
import org.jkcsoft.music.util.AudioUtil;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jcoles on 7/22/16.
 * Copyright 2016 Jim Coles (jameskcoles@gmail.com).
 */
public class Notes {

    private static final Comparator<AbstractNote> noteFreqComp = (o1, o2) -> o1.getFrequency() > o2.getFrequency() ? 1 : -1;
    private static final AbsoluteFreqNote refNote = new AbsoluteFreqNote(110.0d);

    public static void main(String[] args) {
        noteDumper();
        AudioUtil.print("============================================================");
//        playSomethingMidi();
        playRawAudio();
        AudioUtil.print("normal exit");
    }

    private static void noteDumper() {
    /*
     note peaks per ref

     3 / 2 =>
     4 / 2 ref (octave above ref)

     4 / 3
     5 / 3

     5 / 4
     6 / 4
     7 / 4

     6 / 5
     7 / 5
     8 / 5
     9 / 5



     not interested in fraction that is > 2 because that is an octave

     */
        int notesPerScale = 12;

        AbsoluteFreqNote refNote = new AbsoluteFreqNote(1.0d);

        printRationalNotes(notesPerScale, refNote);

        printULRNotes(notesPerScale, refNote);

        printConstMultipleNotes(refNote);

    }

    /**
    - start with smallest numerator (2)
    - increment numerator by 1
    - determine any new (unique) rational numbers < 2 wrt numerator
    - continue until total number of rational numbers equals some limit
     */
    private static void printRationalNotes(int notesPerScale, AbsoluteFreqNote refNote)
    {
        AudioUtil.print("=== Rational Notes =======================================");

        List<RationalNote> relativeNotes = getManyRationalNotes(notesPerScale, refNote);

        AudioUtil.print("ref note: " + refNote);
        for(AbstractNote note : relativeNotes) {
            AudioUtil.print("Note: " + note);
        }
    }

    private static List<RationalNote> getManyRationalNotes(int notesPerScale, AbsoluteFreqNote refNote) {
        List<RationalNote> relativeNotes = new LinkedList<>();
        for(int numRefNotePeaks = 1; numRefNotePeaks <= notesPerScale; numRefNotePeaks++) {
            int beginNotePeaks = numRefNotePeaks + 1;
            int endNodePeaks = numRefNotePeaks * 2 - 1;
            for(int numNotePeaks = beginNotePeaks; numNotePeaks <= endNodePeaks; numNotePeaks++) {
                relativeNotes.add(new RationalNote(refNote, numNotePeaks, numRefNotePeaks));
            }
        }

        relativeNotes.sort(noteFreqComp);
        return relativeNotes;
    }

    private static void printULRNotes(int notesPerScale, AbsoluteFreqNote refNote) {
        AudioUtil.print("==== Unique Rational Notes ========================================================");

        List<RationalNote> relativeNotes = getUniqueRationalNotes();

        AudioUtil.print("ref note: " + refNote);
        int num = 1;
        for(RationalNote note : relativeNotes) {
            String pad = new String(new char[ ( note.getRefNumPeaks() - 1 ) * 2]).replace('\0', ' ');
            AudioUtil.print("Note ("+ num++ +"): " + new String(pad) + note);
        }
    }

    private static List<RationalNote> getUniqueRationalNotes()
    {
        int notesPerScale = 12;
        List<RationalNote> relativeNotes = new LinkedList<>();

        int maxNotesPerScale = 12;

        for(int numRefNotePeaks = 1; numRefNotePeaks <= notesPerScale; numRefNotePeaks++) {
            int beginNotePeaks = numRefNotePeaks + 1;
            int endNodePeaks = numRefNotePeaks * 2;
            for(int numNotePeaks = beginNotePeaks; numNotePeaks <= endNodePeaks; numNotePeaks++) {
                RationalNote newPossibleNote = new RationalNote(refNote, numNotePeaks, numRefNotePeaks);
                if (!relativeNotes.contains(newPossibleNote)) {
                    relativeNotes.add(newPossibleNote);
                }
            }
            if (relativeNotes.size() >= maxNotesPerScale) {
                AudioUtil.print("stopping note search");
                break;
            }
        }

        relativeNotes.sort(noteFreqComp);
        return relativeNotes;
    }

    private static void printConstMultipleNotes(AbsoluteFreqNote refNote) {
        AudioUtil.print("======== Const Multiple Notes ====================================================");
        int numSteps = 12;
        AudioUtil.print("Const Mult: " + computeMultiple(numSteps));

        int num = 0;
        List<AbsoluteFreqNote> constMultNotes = getConstMultNotes(refNote, numSteps);
        for (AbsoluteFreqNote note : constMultNotes) {
            AudioUtil.print("Note ("+ num++ +"): " + note);
        }

    }

    private static List<AbsoluteFreqNote> getConstMultNotes() {
        return getConstMultNotes(refNote, 12);
    }

    private static List<AbsoluteFreqNote> getConstMultNotes(AbsoluteFreqNote refNote, int numSteps) {
        double multiple = computeMultiple(numSteps);
        List<AbsoluteFreqNote> notes = new LinkedList<>();
        for(int num = 0; num <= numSteps; num++) {
            AbsoluteFreqNote note = new AbsoluteFreqNote(refNote.getFrequency() * Math.pow(multiple, num));
            notes.add(note);
        }
        return notes;
    }

    private static double computeMultiple(int numSteps) {
        return Math.pow(2, (1.0d/numSteps));
    }

    private static void playSomethingMidi() {
        MidiDevice.Info[] midiDeviceInfos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : midiDeviceInfos) {
            AudioUtil.print("Midi Device: " + info);
        }

        MidiSynth midi = new MidiSynth();
        Score score = new Score();
        AudioUtil.print("playing");
        Note refNote = new Note(110.0d, JMC.SEMI_QUAVER);

        Part part = score.createPart();
        part.setInstrument(JMC.PIANO);
        Phrase phrase = part.createPhrase();

        // play harmonic notes ...
        List<? extends AbstractNote> urNotes = getUniqueRationalNotes();
        for(AbstractNote myNote : urNotes) {
            phrase.add(refNote);
            phrase.add(new Note(myNote.getFrequency(), JMC.QUAVER));
        }
        play(midi, score);

        try {
            AudioUtil.print("sleeping for 30 seconds");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // play const mult notes ...
        score = new Score();
        part = score.createPart();
        part.setInstrument(JMC.PIANO);
        phrase = part.createPhrase();
        List<? extends AbstractNote> cmNotes = getConstMultNotes();
        for(AbstractNote myNote : cmNotes) {
            phrase.add(refNote);
            phrase.add(new Note(myNote.getFrequency(), JMC.QUAVER));
        }
        play(midi, score);
    }

    private static void play(MidiSynth midi, Score score) {
        try {
            midi.play(score);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        AudioUtil.print("played");
    }

    public static void playRawAudio() {
        Mixer.Info[] mixersInfos = AudioUtil.dumpMixerInfo();

        Mixer.Info mixerInfo = mixersInfos[0];

        try {
//            AudioFileFormat mp3Format = AudioSystem.getAudioFileFormat(new File("/Users/jcoles/Documents/My Music/ACDC - Dirty Deeds Done Dirt Cheap.mp3"));
//            print("mp3 format: " + mp3Format);

            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            //12 = {AudioFormat@731} "PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, little-endian"
            //13 = {AudioFormat@732} "PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, big-endian"
            AudioFormat bestMusicFormat =
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, true);
//            TargetDataLine targetDataLine = AudioSystem.getTargetDataLine(bestMusicFormat);
//            targetDataLine.open(bestMusicFormat);
            SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(bestMusicFormat);
            sourceDataLine.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    AudioUtil.print("line event: " + event);
                }
            });
            sourceDataLine.open(bestMusicFormat);
            sourceDataLine.start();
            int oneSecBufferSize = (int) bestMusicFormat.getSampleRate()
                    * bestMusicFormat.getFrameSize()
                    * bestMusicFormat.getChannels();

            for (int i = 1; i <= 1; i++) {
                long t1 = System.currentTimeMillis();
                byte[] buffer = snippetSinWave(100.d * i, bestMusicFormat, 0.2f, .5);
                AudioUtil.print("compute time millis ["+(System.currentTimeMillis() - t1)+"]");
                AudioUtil.print("writing buffer ["+i+"]");
                sourceDataLine.write(buffer, 0, buffer.length);
                Thread.sleep(2000);
            }
            Thread.sleep(5000);
            sourceDataLine.flush();
            sourceDataLine.stop();
            sourceDataLine.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Insert sin wave form at freq cycles/second as byte array.
     * Assumptions:
     * - populates 'duration' sec buffer
     * - sample rate (samples / sec) is much higher than freq (cycles / sec)
     *   e.q., min sample rate 8000.0; max freq about 1000
     *
     *
     * @param duration
     * @param freq Cycles/Second
     * @param
     * @return
     */
    public static byte[] snippetSinWave(double freq, AudioFormat format, float scaleVolume, double duration) {
        if (format.getSampleSizeInBits() != 16)
            throw new IllegalArgumentException("currently only 16 bit (short int) support");

        int bytesPerSamplePerChannel = format.getSampleSizeInBits() / 8;
        int bytesPerFrame = bytesPerSamplePerChannel * format.getChannels();
        ByteBuffer bbSample = ByteBuffer.allocate(bytesPerSamplePerChannel);
        int totalNumOfSamplesPerChannel = (int) (format.getSampleRate() * duration);
        int totalNumOfCycles = (int) (freq * duration);
        int numBytesTotal = totalNumOfSamplesPerChannel * format.getFrameSize();
        byte[] waveAllBytes = new byte[numBytesTotal];
        int samplesPerCycle = (int) (format.getSampleRate() / freq);
        int totalBytesPerCycle = samplesPerCycle * bytesPerSamplePerChannel * format.getChannels();
        // TODO genericize
        SinWaveForm sinWaveForm = new SinWaveForm();
        float[] oneCycleSamples = sinWaveForm.genForm(samplesPerCycle);
        //
        for (int idxSample = 0; idxSample < oneCycleSamples.length; idxSample++) {
            float floatVal = oneCycleSamples[idxSample];
            short shortValue = (short) (Short.MAX_VALUE * floatVal * scaleVolume);
            bbSample.clear();
            bbSample.putShort(shortValue);
            int destPosCh1 = idxSample * bytesPerFrame;
            System.arraycopy(bbSample.array(), 0, waveAllBytes, destPosCh1, bytesPerSamplePerChannel);
            if (format.getChannels() == 2) {
                // copy 2nd channel
                System.arraycopy(bbSample.array(), 0, waveAllBytes, destPosCh1 + bytesPerSamplePerChannel, bytesPerSamplePerChannel);
            }
        }
        for (int idxCycle = 1; idxCycle < totalNumOfCycles; idxCycle++) {
            System.arraycopy(waveAllBytes, 0, waveAllBytes, idxCycle * totalBytesPerCycle, totalBytesPerCycle);
        }
        return waveAllBytes;
    }


}
