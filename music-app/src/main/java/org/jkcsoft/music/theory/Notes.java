package org.jkcsoft.music.theory;

import jm.JMC;
import jm.constants.Frequencies;
import jm.midi.MidiSynth;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;
import org.jkcsoft.music.render.AudioPlayer;
import org.jkcsoft.music.synthesis.SinWaveForm;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.jkcsoft.music.util.AudioUtil.print;

/**
 * Created by jcoles on 7/22/16.
 * Copyright 2016 Jim Coles (jameskcoles@gmail.com).
 */
public class Notes {

    private static final Comparator<AbstractNote> noteFreqComp = (o1, o2) -> o1.getFrequency() > o2.getFrequency() ? 1 : -1;
    private static final AbsoluteFreqNote refNote = new AbsoluteFreqNote(110.0d);
    private static List<AbsoluteFreqNote> constMultNotes;

    public static void main(String[] args) {
        noteDumper();
        print("============================================================");
//        playSomethingMidi();
//        playRawAudio();
        playChord();
        print("normal exit");
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
        print("=== Rational Notes =======================================");

        List<RationalNote> relativeNotes = getManyRationalNotes(notesPerScale, refNote);

        print("ref note: " + refNote);
        for(AbstractNote note : relativeNotes) {
            print("Note: " + note);
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
        print("==== Unique Rational Notes ========================================================");

        List<RationalNote> relativeNotes = getUniqueRationalNotes();

        print("ref note: " + refNote);
        int num = 1;
        for(RationalNote note : relativeNotes) {
            String pad = new String(new char[ ( note.getRefNumPeaks() - 1 ) * 2]).replace('\0', ' ');
            print("Note ("+ num++ +"): " + new String(pad) + note);
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
                print("stopping note search");
                break;
            }
        }

        relativeNotes.sort(noteFreqComp);
        return relativeNotes;
    }

    private static void printConstMultipleNotes(AbsoluteFreqNote refNote) {
        print("======== Const Multiple Notes ====================================================");
        int numSteps = 12;
        print("Const Mult: " + computeMultiple(numSteps));

        int num = 0;
        constMultNotes = getConstMultNotes(refNote, numSteps);
        for (AbsoluteFreqNote note : constMultNotes) {
            print("Note ("+ num++ +"): " + note);
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
            print("Midi Device: " + info);
        }

        MidiSynth midi = new MidiSynth();
        Score score = new Score();
        print("playing");
        Note refNote = new Note(JMC.C4, JMC.SEMI_QUAVER);

        Part part = score.createPart();
        part.setInstrument(JMC.SINE_WAVE);
        Phrase phrase = part.createPhrase();

        // play harmonic notes ...
        List<? extends AbstractNote> urNotes = getUniqueRationalNotes();
        for(AbstractNote myNote : urNotes) {
            phrase.add(refNote);
            phrase.add(new Note(myNote.getFrequency(), JMC.QUAVER));
        }
        play(midi, score);

        try {
            print("sleeping for 30 seconds");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // play const mult notes ...
        score = new Score();
        part = score.createPart();
        part.setInstrument(JMC.SINE_WAVE);
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
        print("played");
    }

    private static AudioPlayer audioPlayer = new AudioPlayer();

    public static void playRawAudio() {
        try {
            audioPlayer.initLine(Formats.BEST_MUSIC_FORMAT);
            for (int i = 1; i <= 10; i++) {
                long t1 = System.currentTimeMillis();
                byte[] buffer = snippetSinWave(100.d * i, Formats.BEST_MUSIC_FORMAT, 0.2f, .5);
                print("compute time millis ["+(System.currentTimeMillis() - t1)+"]");
                print("writing buffer ["+i+"]");
                audioPlayer.playRawAudio(buffer);
            }
            audioPlayer.closeLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void playChord() {
        try {
            AudioFormat audioFormat = Formats.BEST_MUSIC_FORMAT;
            double duration = 2.0;
            float scaleVolume = 0.2f;

            audioPlayer.initLine(audioFormat);

            float[] buffer1 = snippetSinWaveReal(Frequencies.FRQ[JMC.C4], audioFormat.getSampleRate(), scaleVolume, duration);
            float[] buffer2 = snippetSinWaveReal(Frequencies.FRQ[JMC.E4], audioFormat.getSampleRate(), scaleVolume, duration);
            float[] buffer3 = snippetSinWaveReal(Frequencies.FRQ[JMC.G4], audioFormat.getSampleRate(), scaleVolume, duration);

            float[] merged = new float[Math.max(buffer1.length, buffer2.length)];

            for(int idx = 0; idx < buffer1.length; idx++) {
                merged[idx] = buffer1[idx] + buffer2[idx] + buffer3[idx];
            }

            byte[] buffer = formatToBytes(merged, audioFormat.getChannels(), audioFormat.getSampleSizeInBits() / 8);
            audioPlayer.playRawAudio(buffer);

            audioPlayer.closeLine();
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

        float[] allSnippetSamplesOneChannel = snippetSinWaveReal(freq, format.getSampleRate(), scaleVolume, duration);
        byte[] snippetAsBytes =
                formatToBytes(allSnippetSamplesOneChannel, format.getChannels(), format.getSampleSizeInBits() / 8);
        return snippetAsBytes;
    }

    private static byte[] formatToBytes(float[] allSnippetSamplesOneChannel, int numChannels, int bytesPerSamplePerChannel) {
        byte[] waveAllBytes = new byte[allSnippetSamplesOneChannel.length * numChannels * bytesPerSamplePerChannel];
        ByteBuffer bbSample = ByteBuffer.allocate(bytesPerSamplePerChannel);
        for (int idxSample = 0; idxSample < allSnippetSamplesOneChannel.length; idxSample++) {
            float floatVal = allSnippetSamplesOneChannel[idxSample];
            short shortValue = (short) (Short.MAX_VALUE * floatVal);
            bbSample.clear();
            bbSample.putShort(shortValue);
            int destPosCh1 = idxSample * numChannels * bytesPerSamplePerChannel;
            System.arraycopy(bbSample.array(), 0, waveAllBytes, destPosCh1, bytesPerSamplePerChannel);
            if (numChannels == 2) {
                // copy 2nd channel
                System.arraycopy(bbSample.array(), 0, waveAllBytes, destPosCh1 + bytesPerSamplePerChannel, bytesPerSamplePerChannel);
            }
        }
        return waveAllBytes;
    }

    /**
     * Keep model in the real-valued world.
     */
    public static float[] snippetSinWaveReal(double freq, float sampleRate, float scaleVolume, double duration) {
        int totalNumOfSamples = (int) (sampleRate * duration);
        float[] waveAllSamples = new float[totalNumOfSamples];
        int totalNumOfCycles = (int) (freq * duration);
        int samplesPerCycle = (int) (sampleRate / freq);
        // TODO genericize
        SinWaveForm sinWaveForm = new SinWaveForm();
        float[] oneCycleSamplesNorm = sinWaveForm.genForm(samplesPerCycle);
        //
        for (int idxSample = 0; idxSample < oneCycleSamplesNorm.length; idxSample++) {
            waveAllSamples[idxSample] = oneCycleSamplesNorm[idxSample] * scaleVolume;
        }
        for (int idxCycle = 1; idxCycle < totalNumOfCycles; idxCycle++) {
            System.arraycopy(waveAllSamples, 0, waveAllSamples, idxCycle * samplesPerCycle, samplesPerCycle);
        }
        return waveAllSamples;
    }

}
