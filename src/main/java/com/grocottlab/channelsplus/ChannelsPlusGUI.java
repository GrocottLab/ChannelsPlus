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

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.plugin.CompositeConverter;
import ij.plugin.LutLoader;
import ij.plugin.frame.PlugInDialog;
import ij.plugin.frame.Recorder;
import ij.process.ImageConverter;
import ij.process.LUT;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

/**
 *
 * @author timothygrocott
 */
class ChannelsPlusGUI extends PlugInDialog implements WindowListener {
    
    //CompositeImage ci;
    ImagePlus ci;
    int image_id;
    private Point location;
    int channels;
    String info;
    JLabel[] label;
    JTextField[] scribble;
    JToggleButton[] soloButton, muteButton;
    JLutButton[] lutButton;
    JHistogram[] histogram;
    // Names of built-in luts...
    String[] lutListBuiltIn = {	"Fire",
                                "Grays",
                                "Ice",
                                "Spectrum",
                                "3-3-2 RGB",
                                "Red",
                                "Green",
                                "Blue",
                                "Cyan",
                                "Magenta",
                                "Yellow",
                                "Red/Green"};
    
    /** GUI constructor... **/
    public ChannelsPlusGUI(ImagePlus ci, Channels_Plus parent) {
    
        super("Channels Plus");
        
        this.ci = ci;
        image_id = ci.getID();
        // Get image dimensions
        int[] dims = ci.getDimensions();
        channels = dims[2];
        setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
        
        JPanel guiPanel;
        if (ci.getType() == ImagePlus.COLOR_RGB) {
            guiPanel = getRgbGUI();
        } else {
            guiPanel = getCompositeGUI();
        }
        this.add(guiPanel);
        this.setResizable(false);
        this.pack();
        if (location == null) {
            GUI.centerOnImageJScreen(this);
            location = getLocation();
        } else {
            setLocation(location);
        }
        //this.setAlwaysOnTop(true);
	ci.updateAndDraw();
        setAutoRequestFocus(false);
        setVisible(true);
        setAutoRequestFocus(true);
    }
    
    private JPanel getRgbGUI() {
        
        JLabel message = new JLabel("RGB Color images not supported.");
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        message.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JButton button = new JButton("Make composite");
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                
                CompositeConverter.makeComposite​(ci).show();
            }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS) );
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(message);
        panel.add(button);
        return panel;
    }
    
    private  JPanel getCompositeGUI () {
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS) );
        
        JPanel[] track = new JPanel[channels];
	
        // Get image info and parse info for channel names
        info = ci.getInfoProperty();
        String[] names = new String[channels];
        for(int c = 0; c < channels; c++) {
            String searchTerm = "Information|Image|Channel|Name #" + (c+1) + " = ";
            try {
                int index1 = info.indexOf(searchTerm);
                int index2 = info.indexOf("=", index1);
                int index3 = info.indexOf("\n", index2);
                names[c] = info.substring(index2+2, index3);
            } catch (NullPointerException e) {
                names[c] = "Channel " + (c+1);
            }
        }	

        // Get luts for CompositeImage
        LUT[] lut = ci.getLuts();
	
        // Define arrays for chanel components
        label = new JLabel[channels];
        scribble = new JTextField[channels];
        soloButton = new JToggleButton[channels];
        muteButton = new JToggleButton[channels];
        lutButton  = new JLutButton[channels];
        histogram  = new JHistogram[channels];
        
        // For each channel...
        for (int c = 0; c < channels; c++) {
            
            // Make a new track for this channe...
            track[c] = new JPanel(new FlowLayout(FlowLayout.LEFT) );
    
            // Add a channel label...
            label[c] = new JLabel(" " + (c+1) + " ");
            label[c].setFont(new Font("Arial", Font.BOLD, 32));
            label[c].setForeground(Color.GRAY);
            track[c].add( label[c] );
	
            // Add a scribble strip for short temporary notes...
            scribble[c] = new JTextField( names[c] );
            scribble[c].setPreferredSize( new Dimension(100, 20) );
            track[c].add( scribble[c] );
	
            // Create solo button...
            soloButton[c] = new JToggleButton("S");
            soloButton[c].setToolTipText("Solo channel");
            final int chan = c;
            final int chanCount = channels;
            soloButton[c].addItemListener(
            new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    int state = itemEvent.getStateChange();
                    if (state == ItemEvent.SELECTED) {
      
                        // deselect the other toogles in this group and all histogram sliders...
                        for (int c = 0; c < channels; c++) {
                            // Remove Itemlisteners from mute/solo buttons...
                            ItemListener[] soloListeners = soloButton[c].getItemListeners();
                            soloButton[c].removeItemListener(soloListeners[0]);
                            if (c != chan) {
                                soloButton[c].setSelected(false);
                                histogram[c].setSlidersEnabled(false);
                            } else {
                                soloButton[c].setSelected(true);
                                histogram[c].setSlidersEnabled(true);
                            }
                            // ...restore ItemListeners
                            soloButton[c].addItemListener(soloListeners[0]);
                        }
                        ci.setDisplayMode(IJ.COLOR);
                        ci.setC(chan+1);
                    } else if (state == ItemEvent.DESELECTED) {
                        // switch back to composite display mode
                        ci.setDisplayMode(IJ.COMPOSITE);
                        // disable histogram sliders for this channel
                        histogram[chan].setSlidersEnabled(false);
                        // restore channel mute settings based on mutebutton states
                        boolean[] active = new boolean[chanCount];
                        for (int c = 0; c < channels; c++) {
                            active[c] = true;
                            if(muteButton[c].isSelected()) active[c] = false;
                        }
                        String muteStr = "";
                        for (int c = 0; c < chanCount; c++) {
                            if (active[c]) {
                                muteStr = muteStr + "1";
                            } else {
                                muteStr = muteStr + "0";
                            }
                        }
                        ci.setActiveChannels(muteStr);
                    }
                }
            });
            track[c].add(soloButton[c]);
            // Create a mute button...
            muteButton[c] = new JToggleButton("M");
            muteButton[c].setToolTipText("Mute channel");
            // Get status of each channel from CompositeImage...
            boolean[] active;
            if (channels>1) {
                active = ((CompositeImage)ci).getActiveChannels();
            } else {
                active = new boolean[1];
                active[0] = true;
            }
            // Set current mute buttons selection to match channel status...
            muteButton[c].setSelected( !active[c] );
            // Add ItemListener to this mute button...
            muteButton[c].addItemListener(
                new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent itemEvent) {
                        int state = itemEvent.getStateChange();
                        //CompositeImage ci = parent.getImage();
                        //boolean[] active = ci.getActiveChannels();
                        // Get status of each channel from CompositeImage...
//                        boolean[] active;
//                        if (channels>1) {
//                            active = ((CompositeImage)ci).getActiveChannels();
//                        } else {
//                            active = new boolean[1];
//                            active[0] = true;
//                        }
                        if (state == ItemEvent.SELECTED) {
                            // Mute/hide the selected channel
                            active[chan] = false;
                        } else if (state == ItemEvent.DESELECTED) {
                            // Unmute/show the selected channel
                            active[chan] = true;
                        }
                        String muteStr = "";
                        for (int c = 0; c < chanCount; c++) {
                            if (active[c]) {
                                muteStr = muteStr + "1";
                            } else {
                                muteStr = muteStr + "0";
                            }
                        }
                        ci.setActiveChannels(muteStr);
                    }
                }
            );
            track[c].add(muteButton[c]);
            
            // Create a pop-up menu listing luts
            JPopupMenu lutPopup = new JPopupMenu("LUTs");
            JMenuItem[] lutItem = new JMenuItem[lutListBuiltIn.length];
            for (int i = 0; i < lutListBuiltIn.length; i++) {
                lutItem[i] = new JMenuItem( lutListBuiltIn[i] );
                lutItem[i].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        //IJ.log("actionPerformed() - lutItem");
                        String lutStr = ae.getActionCommand();
                        java.awt.image.IndexColorModel icm = LutLoader.getLut(lutStr);
                        LUT newLut = new LUT(icm, 0, 255);
                        //CompositeImage ci = (CompositeImage) imp;
                        //ImagePlus ci = WindowManager.getCurrentImage();
                        if (channels>1) {
                            ((CompositeImage)ci).setChannelLut(newLut, chan+1);
                        } else {
                            ci.setLut(newLut);
                        }
                        updateTracks();
                        ci.updateImage();
                    }
                });
                lutPopup.add(lutItem[i]);
            }
            // Create an LUT button...
            if(ci.getType() == ImagePlus.COLOR_RGB) {
                
            } else {
                lutButton[c]  = new JLutButton( lut[c] ); // ArrayIndexOutOfBounds when image is RGB. Other color modes seem to work ok, including 8-bit color, 16-bit, 32-bit.
                lutButton[c].setToolTipText("LUT");
                lutButton[c].addActionListener( new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        //IJ.log("actionPerformed() - lut button");
                        JLutButton b = (JLutButton) ae.getSource();
                        lutPopup.show(b, 0, 0);
                    }
                });
                track[c].add(lutButton[c]);
            }
            // Create channel histogram...
            histogram[c] = new JHistogram(ci, c+1);
            
            track[c].add(histogram[c]);
	
            // Add this track...
            panel.add(track[c]);
            if(c < channels-1) panel.add(new JSeparator() );
        }
        return panel;
    }
    
    public int getImageID() {
        return image_id;
    }
    
    public void updateTracks() {
        channels = ci.getDimensions()[2];
        // Get channel Luts from current image...
        LUT[] lut = ci.getLuts();
        // Get status of each channel from current image...
//        boolean[] active = ci.getActiveChannels();
        // Get status of each channel from CompositeImage...
        boolean[] active;
        if (channels>1) {
            active = ((CompositeImage)ci).getActiveChannels();
        } else {
            active = new boolean[1];
            active[0] = true;
        }
        // Get current channel
        int currentChannel = ci.getC() - 1;
        for (int c = 0; c < channels; c++) {
            // Remove Itemlisteners from mute/solo buttons...
            ItemListener[] muteListeners = muteButton[c].getItemListeners();
            muteButton[c].removeItemListener(muteListeners[0]);
            ItemListener[] soloListeners = soloButton[c].getItemListeners();
            soloButton[c].removeItemListener(soloListeners[0]);
            // Set mute button without triggering ItemListeners
            muteButton[c].setSelected( !active[c] );
            // Now we can set solo buttons without triggering ItemListeners...
            // Deselect current solo button, in case it was previously selected
            soloButton[c].setSelected(false);
            // Similarly, disable histogram sliders
            histogram[c].setSlidersEnabled(false);
            // If the current channel is actve...
            if (channels>1)
            if ( ((CompositeImage)ci).getMode() == CompositeImage.COLOR && c == currentChannel) {
                // ...select it's solo button
                soloButton[c].setSelected(true);
                // ...and enable it's histogram sliders
                histogram[c].setSlidersEnabled(true);
            }
            // ...finally restore ItemListeners    		
            muteButton[c].addItemListener(muteListeners[0]);
            soloButton[c].addItemListener(soloListeners[0]);
            // Update Lut button with channel Lut from current image
            lutButton[c].updateLut( lut[c] );
            // Update histogram with channel histogram from current image
            histogram[c].updateHist(ci, c+1);
        }
    }

    @Override
    public void close() {
	
        WindowManager.removeWindow(this);
        location = getLocation();
        setVisible(false);
        //dispose();
    }
    
    @Override
    public void windowClosing(WindowEvent e) {
        if (e.getSource()==this) {
            close();
            if (Recorder.record)
                Recorder.record("run", "Close");
            }
    	}
    @Override
    public void windowDeactivated(WindowEvent e) {}
    @Override
    public void windowActivated(WindowEvent e) {}
    @Override
    public void windowDeiconified(WindowEvent e) {}
    @Override
    public void windowIconified(WindowEvent e) {}
    @Override
    public void windowClosed(WindowEvent e) {
        WindowManager.removeWindow(this);
    }
    @Override
    public void windowOpened(WindowEvent e) {}
}
