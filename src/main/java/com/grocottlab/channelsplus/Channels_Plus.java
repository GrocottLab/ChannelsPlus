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

import ij.*;
import ij.plugin.*;

import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/*
 * Need to seperate the gui/dialog, of which several instances may be created/detroyed, from the PropertyChangeListener (PCL).
 * This should avoid having multiple PropertyChangeListeners active at the same time, since there will be no need then to create more than one PCL.
 * May also need to implememnt the singleton pattern on the main plugin - done, with the exception of making constructor private
 */

/** Displays the "Channels_Plus" dialog. */
public class Channels_Plus implements PlugIn, PropertyChangeListener {
    
    //static Channels_Plus instance;
    static ArrayList<ChannelsPlusGUI> gui_list;
    
    KeyboardFocusManager focusManager;
    
    public static boolean flag = false;
    
    static ChannelsPlusGUI current_cpg; // Pointer to current ChannelsPlusGUI
    
    public Channels_Plus() {
        
        //if (instance == null) instance = this;
        // Initialise the gui_list
        if (Channels_Plus.gui_list == null)
            Channels_Plus.gui_list = new ArrayList<ChannelsPlusGUI>();
        if (current_cpg != null && WindowManager.getCurrentImage() != null) {
            current_cpg.setVisible(true);
            WindowManager.addWindow(current_cpg);
        }
        
        // Get a ChannelsPlusGUI for the current immage...
        ImagePlus ci = WindowManager.getCurrentImage();
        if (ci == null) {
                IJ.log("ci==null");
                return;
        }
        current_cpg = getChannelsPlusGUI( ci );
        WindowManager.addWindow(current_cpg);
        
        focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        focusManager.addPropertyChangeListener("focusedWindow", this);
    }
    
    @Override
    public void run(String arg) {
    }
    
    private ChannelsPlusGUI getChannelsPlusGUI(ImagePlus ci) {
        
        ChannelsPlusGUI cpg;
        // Get the CompositeImage's unique  ID...
        int imageID = ci.getID();
        // Iterate over gui_list...
        for (int i = 0; i < gui_list.size(); i++) {
            // Get the unique ID of this GUI's CompositeImage...
            cpg = Channels_Plus.gui_list.get(i);
            // Check for a match...
            if (cpg.getImageID() == imageID) {
                // We have a match - return the current GUI...
                return cpg;
            }
        }
        // The iterator finished without a match, so we need to create a GUI...
        cpg = new ChannelsPlusGUI(ci, this);
        // ...and add it to the list...
        Channels_Plus.gui_list.add(cpg);
        // ...and finally return it...
        return cpg;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        
        update();
    }
    
    private void update() {
        
        ImagePlus imp = WindowManager.getCurrentImage();
        // If current image is null, then return
        if (imp==null)
            return;
        
        // If current_cpg is null...
        if (current_cpg == null) {
            // ...get a current_gui...
            current_cpg = getChannelsPlusGUI( (CompositeImage) imp );
            // ...and show it...
            current_cpg.setVisible(true);
//            return;
        }
        
        // Get ID of current image...
        int imageID = imp.getID();
        // Check if it matches currently visible GUI...
        if ( imageID == current_cpg.getImageID() ) {
            return;
        } else {
            // Otherwise, hide currently visible gui...
            int x = current_cpg.getX();
            int y = current_cpg.getY();
            current_cpg.setAutoRequestFocus(false);
            current_cpg.setVisible(false);
            current_cpg.setAutoRequestFocus(true);
            WindowManager.removeWindow(current_cpg);
            // ...update current_gui...
            current_cpg = getChannelsPlusGUI(imp);
            // ...and show it...
            current_cpg.setLocation(x, y);
            WindowManager.addWindow(current_cpg);
            current_cpg.setAutoRequestFocus(false);
            current_cpg.setVisible(true);
            current_cpg.setAutoRequestFocus(true);
        }
    }
    
    // Implement ImageListener methods...
    // Note that the reguar Channels tool doesn't implement ImageListener
    // This functionality may be covered via the update() method instead
    public void imageOpened(ImagePlus imp) {
//    	IJ.log( "imageOpened() - " + getImage().getTitle() );
    }
    
    public void imageClosed(ImagePlus imp) {
//    	IJ.log( "imageClosed() - " + getImage().getTitle() );
    }

    public void imageUpdated(ImagePlus imp) {
//    	IJ.log( "imageUpdated() - " + getImage().getTitle() );
        //updateTracks();
        //update();
    }
}