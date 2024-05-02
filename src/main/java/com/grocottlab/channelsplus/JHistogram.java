/*
 * Copyright (C) 2024 Grocott Lab
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.grocottlab.channelsplus;

import javax.swing.JComponent;
import ij.CompositeImage;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author timothygrocott
 */
class JHistogram extends JComponent implements ImageListener {
    Graphics2D g2;
    //CompositeImage ci;
    //int channel;
    int channel;
    int channels;
    int[] hist;
    double[] dHist;
    double[] logHist;
    double dMax;
    double logMax;
    double maxBin;
    double rangeMin;
    double rangeMax;
    JSlider minSlider;
    JSlider midSlider;
    JSlider maxSlider;
    Color channelColor;
    Color logColor;
    Color linColor;
    ImagePlus ci;
    
    boolean violin = false;
    
    public JHistogram(ImagePlus ci, int channel) {
        super();
        this.ci = ci;
        this.channel = channel;
        channels = ci.getDimensions()[2];
        setPreferredSize( new Dimension(256+20, 60) );
        setMinimumSize( new Dimension(256+20, 60) );
        this.buildUI(ci);
        updateHist(ci, channel);
        ci.addImageListener(this);
    }

    private void buildUI(ImagePlus ci) {
        
        JHistogram thisHist = this;
        int currentChannel = ci.getC();
        ci.setC(this.channel);
        int rangeMin = (int)ci.getDisplayRangeMin();
        int rangeMax = (int)ci.getDisplayRangeMax();
        ci.setC(currentChannel);
        int midRange = (int)( (rangeMin + rangeMax)/2.0f );
        int diff     = (int)( (rangeMax - rangeMin)/1.0f );
    	
        // Define sliders first, so each of their ChangeHandlers can see them all..
        maxSlider = new JSlider(0, 255, rangeMax );
        maxSlider.setPaintTrack(false);
        maxSlider.setToolTipText("Maximum");
    
        midSlider = new JSlider(0, 256, midRange);
        midSlider.setPaintTrack(false);
        midSlider.setToolTipText("Brightness");
    
        minSlider = new JSlider(0, 255,  rangeMin );
        minSlider.setPaintTrack(false);
        minSlider.setToolTipText("Minimum");
    	
        // Now set up each of their ChangeHandlers...
        maxSlider.addChangeListener( new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                int value = maxSlider.getValue();
                ImagePlus ci = WindowManager.getCurrentImage();
                int currentChan = ci.getC();
                ci.setC(channel);
                double min = ci.getDisplayRangeMin();
                if (value<=min) {
                    maxSlider.setValue( (int)(min+1) );
                    return;
                }
                double max = value;
                ci.setDisplayRange(min, max);
                updateHist(ci, channel);
                ci.updateChannelAndDraw();
                ci.setC(currentChan);
                // Update mid slider without triggering a change event...
                ChangeListener[] midListeners = midSlider.getChangeListeners();
                midSlider.removeChangeListener(midListeners[0]);
                midSlider.setValue( (int)((min + max)/2.0f) );
                midSlider.addChangeListener(midListeners[0]);
            }
        });
        minSlider.addChangeListener( new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                int value = minSlider.getValue();
                ImagePlus ci = WindowManager.getCurrentImage();
                int currentChan = ci.getC();
                ci.setC(channel);
                double max = ci.getDisplayRangeMax();
                if (value>=max) {
                    minSlider.setValue( (int)(max-1) );
                    return;
                }
                double min = value;
                ci.setDisplayRange(min, max);
                updateHist(ci, channel);
                ci.updateChannelAndDraw();
                ci.setC(currentChan);
                // Update mid slider without triggering a change event...
                ChangeListener[] midListeners = midSlider.getChangeListeners();
                midSlider.removeChangeListener(midListeners[0]);
                midSlider.setValue( (int)((min + max)/2.0f) );
                midSlider.addChangeListener(midListeners[0]);
            }
        });
        midSlider.addChangeListener( new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                int value = midSlider.getValue();
                ImagePlus ci = WindowManager.getCurrentImage();
                int currentChan = ci.getC();
                ci.setC(channel);
                double max = ci.getDisplayRangeMax();
                double min = ci.getDisplayRangeMin();
                double diff = (max-min)/2.0f;
                if (value>=255-diff) {
                    midSlider.setValue( (int)(255-diff) );
                    return;
                } else if (value<=  0+diff) {
                    midSlider.setValue( (int)(  0+diff) );
                    return;
                }
                double mid = value;
                ci.setDisplayRange(mid-diff, mid+diff);
                updateHist(ci, channel);
                ci.updateChannelAndDraw();
                ci.setC(currentChan);
                // Update min/max sliders without triggering change events...
                ChangeListener[] minListeners = minSlider.getChangeListeners();
                minSlider.removeChangeListener(minListeners[0]);
                ChangeListener[] maxListeners = maxSlider.getChangeListeners();
                maxSlider.removeChangeListener(maxListeners[0]);
                minSlider.setValue( (int)(mid-diff) );
                maxSlider.setValue( (int)(mid+diff) );
                minSlider.addChangeListener(minListeners[0]);
                maxSlider.addChangeListener(maxListeners[0]);
            }
        });
        // Disable sliders by default - they will be enabled individually when a channel is soloed
        setSlidersEnabled(false);
    	// Add sliders...    		
        this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
        this.add(maxSlider);
        this.add(midSlider);
        this.add(minSlider);
    }
    
    public void setSlidersEnabled(boolean enabled) {
        
        minSlider.setEnabled(enabled);
        midSlider.setEnabled(enabled);
        maxSlider.setEnabled(enabled);
    }

    @Override
    protected void paintComponent(Graphics g) {

        g2 = (Graphics2D) g.create();
        g2.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        int x = 10;
        int y = 10;
        int w = getWidth()-20;
        int h = getHeight()-20;
        // Paint the background...
        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRect(x, y, w, h);
        
        // Draw the log & linear histograms...
        if (violin) {
            for(int i = 0; i < 256; i++) {
                g2.setColor(logColor);
                g2.fillRect(x+i, y+(h/2)-(int)(logHist[i]/2), 1, (int)(logHist[i]) );
                g2.setColor(linColor);
                g2.fillRect(x+i, y+(h/2)-(int)(dHist[i]/2), 1, (int)(dHist[i]) );
            }
        } else {
            for(int i = 0; i < 256; i++) {
                g2.setColor(logColor);
                g2.fillRect(x+i, y+(h)-(int)logHist[i], 1, (int)logHist[i] );
                g2.setColor(linColor);
                g2.fillRect(x+i, y+h-(int)dHist[i], 1, (int)dHist[i] );
            }
        }
        
        // Draw display range...
        g2.setColor( new Color(0.0f,0.0f,0.0f,0.125f) );
        g2.fillRect(x+(int)rangeMax, y, w-(int)rangeMax, h);
        g2.fillRect(x, y, (int)rangeMin, h);
        //Color col = javax.swing.UIManager.getDefaults().getColor("List.selectionBackground");
        //g2.setColor( col );
        g2.drawLine(x+(int)rangeMin, h+y, x+(int)rangeMax, y);
        
        g2.dispose();
        super.paintComponent(g);
   }

    public void updateHist(ImagePlus ci, int channel) {
        int currentChannel = ci.getC();
        if (currentChannel != channel)
            ci.setC(channel);
        double rangeMin = ci.getDisplayRangeMin();
        double rangeMax = ci.getDisplayRangeMax();
        ImageProcessor ip = ci.getProcessor();
        if (channels>1) {
            channelColor = ((CompositeImage)ci).getChannelColor();
        } else {
            channelColor = Color.BLACK;
        }
        hist = ip.getHistogram();
        if (currentChannel != channel)
            ci.setC(currentChannel);
        double rangeMid = (rangeMax+rangeMin)/2.0f;

        logColor = new Color(channelColor.getRed()/255.0f, channelColor.getGreen()/255.0f, channelColor.getBlue()/255.0f, 0.25f);
        linColor = new Color(channelColor.getRed()/255.0f, channelColor.getGreen()/255.0f, channelColor.getBlue()/255.0f, 1.0f);

        // Update min/max sliders without triggering change events...
        ChangeListener[] minListeners = minSlider.getChangeListeners();
        minSlider.removeChangeListener(minListeners[0]);
        ChangeListener[] midListeners = midSlider.getChangeListeners();
        midSlider.removeChangeListener(midListeners[0]);
        ChangeListener[] maxListeners = maxSlider.getChangeListeners();
        maxSlider.removeChangeListener(maxListeners[0]);
        minSlider.setValue( (int)(rangeMin) );
        midSlider.setValue( (int)(rangeMid) );
        maxSlider.setValue( (int)(rangeMax) );
        minSlider.addChangeListener(minListeners[0]);
        midSlider.addChangeListener(midListeners[0]);
        maxSlider.addChangeListener(maxListeners[0]);
        
        dHist = new double[hist.length];
        logHist = new double[hist.length];
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        dMax = 0.0;
        logMax = 0.0;
        for(int i = 0; i < logHist.length; i++) {
            dHist[i] = hist[i];
            if(dMax < dHist[i]) dMax = dHist[i];
            logHist[i] = Math.log( hist[i] );
            if(logMax < logHist[i]) logMax = logHist[i];
        }
        for(int i = 0; i < logHist.length; i++) {
            dHist[i] = 40.0*(dHist[i]/dMax);
            logHist[i] = 40.0*(logHist[i])/logMax;
        }
        repaint();
    }

    @Override
    public void imageOpened(ImagePlus ip) {
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void imageClosed(ImagePlus ip) {
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void imageUpdated(ImagePlus ip) {
        
        if(ip.getID() == ci.getID() && ip.getC() == channel) {
            ip.removeImageListener(this);
            updateHist(ip, channel );
            ip.addImageListener(this);
        }
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}