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
import java.awt.*;
import java.awt.event.*;

/**
* This class displays floating point data passed to it, typically 
* read from the audio file or generated by s synthesis process.
* It is mainly used in conjunction with the WaveView class.
* @author Andrew Brown
*/

public class WaveCanvas extends Canvas {
	//private SampleIn si;
	private float[] data;
	private int segmentSize = 0;
	private int resolution = 125;
	private int height = 200;
	private int amplitude = 1;
	// for double buffering
	public Image image = null;
	protected Graphics g;
	private boolean resized = false;
        private Color waveColor = Color.darkGray;
        private Color backgroundColor = Color.white;
        private int sampleStart = 0;
        private int waveSize;

	public WaveCanvas() {
		super();
	}

	/*
	 * Fill the data array with the sample values to be displayed
	 */
	public void setData(float[] data) {
		this.data = data;
		this.segmentSize = data.length;
	}

	/**
	* Change the horizontal zoom size of the displayed waveform.
	 * @param size The new resolution value.
	*/
	public void setResolution(int size) {
		if(size > 0) this.resolution = size;
		repaint();
	}

	/**
	* Change the vertical zoom size of the displayed waveform.
	 * @param val The new height value.
	 */
	public void setHeight(int val) {
		if(val > 0) this.height = val;
		this.setSize(new Dimension(800, height + 1));
		resized = true;
		repaint();
	}


	/**
	* Change the waveform amplitude magnification of the displayed waveform.
	 * @param size The new resolution value.
	 */
	public void setAmplitude(int size) {
		if(size > 0) this.amplitude = size;
		repaint();
	}
        
        /*
        * Specify the number of samples in one channel of the file.
        */
        public void setWaveSize(int size) {
            this.waveSize = size;
        }
        
        /*
        * Notify instance if it needs to draw at a new size.
        * @param boolean True to redraw.
        */
        public void setResized(boolean val) {
            this.resized = val;
            repaint();
        }
        
        public void toggleColor() {
            if(waveColor == Color.darkGray) {
                waveColor = Color.green;
                backgroundColor = Color.darkGray;
            } else {
                waveColor = Color.darkGray;
                backgroundColor = Color.white;
            }
            //this.setBackground(backgroundColor);
            repaint();
        }

	public void paint(Graphics graphics) {
		// set cursor
		this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		// set up for double buffering
		if(image == null || resized) {
			image = this.createImage(this.getSize().width, this.getSize().height);
			g = image.getGraphics();
			resized = false;
		}   
                clearImage(g);             
		int h2 = height/2 - 1;
		float max = 0.0f;
		float min = 0.0f;
		float drawMax, drawMin, currData;
		int position = 0;
		
		// mid line
		g.setColor(Color.black);
		g.drawLine(0, h2, this.getSize().width, h2);
		// draw wave
                
                int pixCount = Math.min(data.length-resolution, this.getSize().width * resolution);
		if (resolution == 1) {
                    g.setColor(waveColor);
                    for (int i = sampleStart; i < sampleStart + pixCount; i += resolution) {
                        currData = data[i];
                        g.drawLine(position,(int)(h2 - currData * h2 * amplitude),
                                position,(int)(h2 - currData * h2 * amplitude));
                        position++;
                    }
		} else {			
                    for (int i = sampleStart; i < sampleStart + pixCount; i += resolution) {
                        if(i<waveSize) {
                            currData = data[i];
                            // waveform
                            g.setColor(waveColor);
                            // max and min
                            max = 0.0f;
                            min = 0.0f;
                            for(int j=0; j< resolution; j++) {
                                if (data[i+j] > max) max = data[i+j];
                                if (data[i+j] < min) min = data[i+j];
                            }
                            // highest and lowest curve values
                            if (resolution > 8) {
                                    drawMax = Math.max(currData, data[i+resolution]);
                                    drawMin = Math.min(currData, data[i+resolution]);
                                    if (max > 0.0f) g.drawLine(position, 
                                        (int)(h2 - drawMax * h2 * amplitude),
                                        position, (int)(h2 - max * h2 * amplitude));
                                    if (min < 0.0f) g.drawLine(position, 
                                            (int)(h2 - drawMin * h2 * amplitude),
                                            position, (int)(h2 - min * h2 * amplitude));
                            }
                            // draw wave
                            g.drawLine(position++,(int)(h2 - currData * h2 * amplitude),
                                    position, (int)(h2 - data[i+resolution] * h2 * amplitude));
                        }
                    }
		}
		// base line
		g.setColor(Color.lightGray);
		//g.drawLine(0, 0, this.getSize().width, 0);
		g.drawLine(0, height, this.getSize().width, height);
		
		/* Draw completed buffer to g */
		graphics.drawImage(image, 0, 0, null);
		clearImage(g);
		// reset cursor
		this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
        
        private void clearImage(Graphics g) {
            // clear image
            g.setColor(backgroundColor);
            g.fillRect(0,0, getSize().width, getSize().height);
            g.setColor(waveColor);
        }
}