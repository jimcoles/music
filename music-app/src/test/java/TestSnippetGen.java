import org.jkcsoft.music.theory.Formats;
import org.jkcsoft.music.theory.Notes;
import org.jkcsoft.music.util.AudioUtil;
import org.junit.Test;

import javax.sound.sampled.AudioFormat;
import java.util.Arrays;

/**
 * Created by jcoles on 7/26/16.
 * Copyright 2016 Jim Coles (jameskcoles@gmail.com).
 */
public class TestSnippetGen {

    @Test
    public void testSnippets() {
        AudioFormat format =
                new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 200.0f, 16, 2, 4, 100.0f, true);

//        byte[] bytes = Notes.snippetSinWave(2.0d, format, 0.1f, 1.0);
        byte[] bytes = Notes.snippetSinWave(100.d, Formats.BEST_MUSIC_FORMAT, 0.2f, .5);

        AudioUtil.print("bytes: " + Arrays.toString(bytes));
    }


}
