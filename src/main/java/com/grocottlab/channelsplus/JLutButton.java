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

import ij.process.LUT;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;

/**
 *
 * @author timothygrocott
 */
class JLutButton extends JButton {
    
    LUT lut;
    Graphics2D g2;
    
    JLutButton(LUT lut) {
        super(" ");
        this.lut = lut;
        setSize(256, getHeight() );
        setContentAreaFilled(false);
        setFocusPainted(false); // used for demonstration
    }

    @Override
    protected void paintComponent(Graphics g) {

        // build Color array from LUT
        Color[] cols = new Color[256];
        for(int i = 0; i < 256; i++) {
            cols[i] = new Color(lut.getRed(i), lut.getGreen(i), lut.getBlue(i) );
        }
        g2 = (Graphics2D) g.create();
        
        // fill a series of rectangles, one per color
        int x = 0;
        int y = 0;
        int w = getWidth();
        int h = getHeight();
        int cinc = 1+(255/h);
        for(x = 0; x < h; x++) {
            int colIndex = x*cinc;
            if (colIndex > 255) colIndex = 255;
            g2.setColor( cols[colIndex] );
            g2.fillRect(x, y, 1, h);
        }
       g2.dispose();
       super.paintComponent(g);
    }
    
    public void updateLut(LUT lut) {
        this.lut = lut;
        repaint();
    }
}