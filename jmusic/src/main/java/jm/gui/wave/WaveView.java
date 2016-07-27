/*

<This Java Class is part of the jMusic API>

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

package jm.gui.wave;

import jm.audio.io.SampleIn;
import jm.audio.Instrument;
import jm.audio.RTMixer;
import java.awt.*;
import java.awt.event.*;

/*
 * This class is a jMusic utility that displays an .au file
 * as an amplitude-time graph. It will support files of up to
 * eight channels.
 * A part of the jMusic audio wave file viewing package
 * @author Andrew Brown
 */
public class WaveView implements ActionListener, ComponentListener {
	private String lastFileName  = "Drunk.au";
        private String lastDirectory  = "";
        private SampleIn si;
        /* Dimensions of the frame */
        private int width = 800;
        /* Height on one wave panel */
        private int channelHeight = 200;
        /* Current display magnification of the waveform's amplitude */
	private int amplitude = 1;
        /* The number of cahnnels in the file */
        private int channels;
        /* an array that contains the read sample data */
        private float[] data;
        /* The screen display resolution */
	private int resolution = 256;
        /* the amount of data to read from the file */
        private int segmentSize;
        /* The current sample position to start reading from */
        private int startPos = 0;
	// menu items
	private MenuItem size1, size2, size4, size8, size16, size32,
		size64, size128, size256, size512, size1024, size2048,
		size4096, openFile, quit, changeColor,
		vSmall, small, medium, large, huge, times1, times2, times3, times4;
	
        private Frame f = new Frame();
	private WaveCanvas[] canvases = new WaveCanvas[8];
        private ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_NEVER);
        private WaveScrollPanel scrollPanel = new WaveScrollPanel();
        private boolean whiteColor = true;
        /** The RTMixer object for playback */
        private RTMixer mixer;
	
        /*
	 * A constructor that prompts the user for a file to be displayed.
	 */
	public WaveView() {
		super();
		openFile();
		drawWave(0,0);
	}

	/*
	 * A constructor that displays a specified audio file.
	 * @param fileName The name (and path) of a file to be displayed.
	 */
	public WaveView(String fileName) {
            this(fileName, 0, 0);
        }
        
        /*
	 * A constructor that displays a specified audio file.
	 * @param fileName The name (and path) of a file to be displayed.
         * @param int The horizontal location for the display.
         * @param int The vertical location for the display.
	 */
	public WaveView(String fileName, int xLoc, int yLoc) {
		super();
		this.lastFileName = fileName;
                si = new SampleIn(new DummyInst(), fileName);
		init();
		drawWave(xLoc, yLoc);
                f.setTitle("jMusic Wave Viewer: " + lastFileName);
                scrollPanel.setScrollbarAttributes(si.getWaveSize(), width, resolution);
	}
        
        /**
	* Dialog to import an audio file
	*/
	public void openFile() {
		FileDialog loadFile = new FileDialog(f, 
                    "Select a 16 bit audio file in .au format (no compression).", 
                    FileDialog.LOAD);
		loadFile.setDirectory( lastDirectory );
		loadFile.setFile(lastFileName );
		loadFile.show();
		String fileName = loadFile.getFile();
		if (fileName != null) {
			lastFileName = fileName;
			lastDirectory = loadFile.getDirectory();
			si = new SampleIn(new DummyInst(), lastDirectory + fileName);
			init();
			setupPanel();
                        if (channels <= 2)setHeight(200);
                        if (channels > 2) setHeight(100);
                        if (channels > 4) setHeight(50);
			lastFileName = fileName;
			f.setTitle("jMusic Wave Viewer: " + lastFileName);
                        updateScrollInfo();
                        if(!whiteColor) {
                            changeColor();
                            // maintain color setting
                            whiteColor = !whiteColor;
                        }
			f.pack();
                        sp.repaint();
			repaint();
		}
                
        }
        
         private void init()  {
		si.read();
		channels = si.getChannels();
		if (channels > 8) {
			System.out.println("Files with more than 8 channels are not supported :(");
			System.exit(1);
		}
                //segmentSize = samples; // the whole file
                segmentSize = width * resolution * channels;
		data = new float[segmentSize];
                 // Fill array from a sample start point
		si.getSamples(data, startPos);
		setupChannels();
                if (channels <= 2) setHeight(200);
                if (channels > 2) setHeight(100);
                if (channels > 4) setHeight(50);
        }

	private void drawWave(int xLoc, int yLoc) {
		f.setName("jMusic Wave Viewer: " + lastFileName);
                f.setLocation(xLoc, yLoc);
                f.setLayout(new BorderLayout());
                sp.setSize(new Dimension(width, (channelHeight+1) * channels));
		setupPanel();
		f.add(sp, "Center");
                // set up scroll bar panel
                scrollPanel.setViewer(this);
                updateScrollInfo();
                scrollPanel.setScrollbarAttributes(si.getWaveSize(), width, resolution);
                f.add(scrollPanel, "South");
                
		// menus
		MenuBar menus = new MenuBar();
		Menu fileMenu  = new Menu("Wave", true);
		Menu heightMenu  = new Menu("Height", true);
		Menu resolutionMenu  = new Menu("Resolution", true);
		Menu amplitudeMenu  = new Menu("Amplitude", true);

		size1 = new MenuItem("1:1");
		size1.addActionListener(this);
		resolutionMenu.add(size1);

		size2 = new MenuItem("1:2");
		size2.addActionListener(this);
		resolutionMenu.add(size2);

		size4 = new MenuItem("1:4");
		size4.addActionListener(this);
		resolutionMenu.add(size4);

		size8 = new MenuItem("1:8");
		size8.addActionListener(this);
		resolutionMenu.add(size8);

		size16 = new MenuItem("1:16");
		size16.addActionListener(this);
		resolutionMenu.add(size16);

		size32 = new MenuItem("1:32");
		size32.addActionListener(this);
		resolutionMenu.add(size32);
		
		size64 = new MenuItem("1:64");
		size64.addActionListener(this);
		resolutionMenu.add(size64);

		size128 = new MenuItem("1:128");
		size128.addActionListener(this);
		resolutionMenu.add(size128);

		size256 = new MenuItem("1:256");
		size256.addActionListener(this);
		resolutionMenu.add(size256);

		size512 = new MenuItem("1:512");
		size512.addActionListener(this);
		resolutionMenu.add(size512);

		size1024 = new MenuItem("1:1024");
		size1024.addActionListener(this);
		resolutionMenu.add(size1024);

		size2048 = new MenuItem("1:2048");
		size2048.addActionListener(this);
		resolutionMenu.add(size2048);
                /*
		size4096 = new MenuItem("1:4096");
		size4096.addActionListener(this);
		resolutionMenu.add(size4096);
                */
		openFile = new MenuItem("Open...", new MenuShortcut(111));
		openFile.addActionListener(this);
		fileMenu.add(openFile);
                
                changeColor = new MenuItem("Change Color");
		changeColor.addActionListener(this);
		fileMenu.add(changeColor);
		
		quit = new MenuItem("Quit", new MenuShortcut(113));
		quit.addActionListener(this);
		fileMenu.add(quit);

		vSmall = new MenuItem("X Small");
		vSmall.addActionListener(this);
		heightMenu.add(vSmall);

		small = new MenuItem("Small");
		small.addActionListener(this);
		heightMenu.add(small);

		medium = new MenuItem("Medium");
		medium.addActionListener(this);
		heightMenu.add(medium);

		large = new MenuItem("Large");
		large.addActionListener(this);
		heightMenu.add(large);

		huge = new MenuItem("X Large");
		huge.addActionListener(this);
		heightMenu.add(huge);

		times1 = new MenuItem("x1");
		times1.addActionListener(this);
		amplitudeMenu.add(times1);

		times2 = new MenuItem("x2");
		times2.addActionListener(this);
		amplitudeMenu.add(times2);

		times3 = new MenuItem("x3");
		times3.addActionListener(this);
		amplitudeMenu.add(times3);

		times4 = new MenuItem("x4");
		times4.addActionListener(this);
		amplitudeMenu.add(times4);

		menus.add(fileMenu);
		menus.add(heightMenu);
		menus.add(resolutionMenu);
		menus.add(amplitudeMenu);
		f.setMenuBar(menus);
		
		// display
                sp.setSize(new Dimension(width, (channelHeight+1) * channels));
		f.pack();
                width = f.getSize().width;
		f.setVisible(true);
                f.addComponentListener(this);
	}
        
        private void updateScrollInfo() {
                // update scrollpanel info
                scrollPanel.setFileName(this.lastFileName);
                scrollPanel.setBitSize(si.getBitResolution());
                scrollPanel.setSampleRate(si.getSampleRate());
                scrollPanel.setChannels(si.getChannels());
                scrollPanel.getWaveRuler().setMarkerWidth(si.getSampleRate() / resolution);
                scrollPanel.setScrollbarValue(startPos);
                scrollPanel.setScrollbarResolution(resolution);
        }

               
	private void setupChannels() {
            channels = si.getChannels();
            float[][] tracks = new float[channels][segmentSize/channels];
            int counter = 0;
            for (int i=0; i<segmentSize; i += channels) {
                for (int j=0; j<channels; j++) {
                        tracks[j][counter] = data[i+j];
                }
                counter++;
            }
            // set up canvases for each channel
            for (int i=0; i<channels; i++) {
                canvases[i] = new WaveCanvas();
                canvases[i].setSize(new Dimension(this.width, this.channelHeight + 1));
                canvases[i].setData(tracks[i]);
                canvases[i].setResolution(this.resolution);
                canvases[i].setHeight(this.channelHeight);
                canvases[i].setAmplitude(this.amplitude);
                canvases[i].setWaveSize(si.getWaveSize());
            }		
	}
        
        /*
        * Respond to a change is horizontal view location of the wave
        */
        public void setStartPos(int newOffset) {
            this.startPos = newOffset;
            reRead();
        }
        
        /*
        * Report the horizontal view location of the wave
        */
        public int getStartPos() {
            return this.startPos;
        }
        
        private void reRead() {
            si.read();
            segmentSize = width * resolution * channels;
            // stop unnessesarily excessive array sizes
            if (segmentSize > si.getWaveSize() * channels) segmentSize = si.getWaveSize() * channels;
            data = new float[segmentSize];
            // Fill array from a sample startPos point
            si.getSamples(data, startPos);
            updateChannelData();
        }
        
        private void updateChannelData() {
		float[][] tracks = new float[channels][segmentSize/channels];
		int counter = 0;
		for (int i=0; i<segmentSize; i += channels) {
			for (int j=0; j<channels; j++) {
				tracks[j][counter] = data[i+j];
			}
			counter++;
		}
		for (int i=0; i<channels; i++) {
			canvases[i].setData(tracks[i]);
			canvases[i].setResolution(this.resolution);
		}		
	}

	private void setupPanel() {
		Panel panel = new Panel();
		panel.setLayout(new GridLayout(channels, 1));
		panel.setSize(new Dimension(width, channelHeight * channels));
		for (int i=0; i<channels; i++) {
			panel.add(canvases[i]);
		}
		sp.add(panel);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == size1) setResolution(1);
		if(e.getSource() == size2) setResolution(2);
		if(e.getSource() == size4) setResolution(4);
		if(e.getSource() == size8) setResolution(8);
		if(e.getSource() == size16) setResolution(16);
		if(e.getSource() == size32) setResolution(32);
		if(e.getSource() == size64) setResolution(64);
		if(e.getSource() == size128) setResolution(128);
		if(e.getSource() == size256) setResolution(256);
		if(e.getSource() == size512) setResolution(512);
		if(e.getSource() == size1024) setResolution(1024);
		if(e.getSource() == size2048) setResolution(2048);
		if(e.getSource() == openFile) openFile();
		if(e.getSource() == quit) System.exit(0);
		if(e.getSource() == vSmall) setHeight(25);
		if(e.getSource() == small) setHeight(50);
		if(e.getSource() == medium) setHeight(100);
		if(e.getSource() == large) setHeight(200);
		if(e.getSource() == huge) setHeight(300);
		if(e.getSource() == times1) setAmplitude(1);
		if(e.getSource() == times2) setAmplitude(2);
		if(e.getSource() == times3) setAmplitude(3);
		if(e.getSource() == times4) setAmplitude(4);
                if(e.getSource() == changeColor) changeColor();
	}
        
        // Switch to a different display color combination
        private void changeColor() {
            for (int i=0; i<channels; i++) {
                canvases[i].toggleColor();
            }
            whiteColor = !whiteColor;
        }

	/**
	* Change the horizontal zoom size of the displayed waveform.
        * @param size The new resolution value.
	*/
	public void setResolution(int size) {
		if (size > 0 && size <= 2048) { // 8000
                    this.resolution = size;
                    scrollPanel.setResolution(size);
                    scrollPanel.setScrollbarResolution(size);
                    for (int i=0; i<channels; i++) {
                            canvases[i].setResolution(size);
                    }
                    reRead();
                }
	}
        
        /*
        * Pass back the current display resolution.
        * @return int The resolution value
        */
        public int getResolution() {
            return this.resolution;
        }
        
        /*
        * Pass on the sample rate of the currently displayed file.
        */
        public int getSampleRate() {
            return si.getSampleRate();
        }
        
         /*
        * Pass on the file name of the currently displayed file.
        */
        public String getFileName() {
            return lastDirectory + lastFileName;
        }
        
        /*
        * Pass the current width of this frame.
        */
        public int getWidth() {
            return this.width;
        }

	private void repaint() {
                sp.setSize(f.getSize().width, f.getSize().height);
		for (int i=0; i<channels; i++) {
                        canvases[i].setSize(f.getSize().width, canvases[i].getSize().height);
			canvases[i].repaint();
		}
	}
        
        // component listener methods
        
        public void componentResized(ComponentEvent e)  {
            if(f.getSize().width > width) {
                width = f.getSize().width;
                reRead();
            } else width = f.getSize().width;
            for (int i=0; i<channels; i++) {
                        canvases[i].setSize(width, canvases[i].getSize().height);
			canvases[i].setResized(true);
		}
        }
        
        public void componentHidden(ComponentEvent e)  {}
        public void componentMoved(ComponentEvent e)  {}
        public void componentShown(ComponentEvent e) {}

	/**
	* Change the virtical zoom size of the displayed waveform.
	 * @param val The new height value.
	 */
	public void setHeight(int val) {
		this.channelHeight = val;
                setupChannels();
		setupPanel();
		sp.setSize(new Dimension(width, (channelHeight+1)*channels));
                if(!whiteColor) {
                    changeColor();
                    // maintain color setting
                    whiteColor = !whiteColor;
                }
		f.pack();
                
                repaint();
                sp.repaint();
        }


	/**
	* Change the waveform amplitude magnification of the displayed waveform.
	 * @param size The new resolution value.
	 */
	public void setAmplitude(int size) {
		this.amplitude = size;
		for (int i=0; i<channels; i++) {
			canvases[i].setAmplitude(size);
		}
	}
        
        public void playFile() {
            if (channels > 2) {
                System.out.println("jMusic Wave View notification:" +
                    " Sorry, only mono and stereo files can be played at present.");
            } else {
                System.out.println("---- Playing audio file '" + getFileName() + "'... Sample rate = "
                +si.getSampleRate() + " Channels = " + si.getChannels() + " ----");
                jm.music.rt.RTLine[] lineArray = {new jm.util.AudioRTLine(getFileName())};	
                mixer = new RTMixer(lineArray, 4096, si.getSampleRate(), 
                    si.getChannels(), 0.01);	
                mixer.begin();
            }
        }
        
        /**
	* Pause playback of an audio file jMusic audio playback via javaSound.
        * This method requires the javax.sound packages in Java 1.3 or higher.
	*/ 
	public void pauseFile() {
            if (mixer != null) ;
            mixer.pause();
        }
        
        /**
	* Continue playback an audio file jMusic audio playback via javaSound.
        * This method requires the javax.sound packages in Java 1.3 or higher.
	* @param String The name of the file to be played.
	*/ 
	public void unPauseFile() {
            if (mixer != null) mixer.unPause();
        }

}