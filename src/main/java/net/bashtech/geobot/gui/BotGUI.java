/*
 * Copyright 2012 Andrew Bashore
 * This file is part of GeoBot.
 * 
 * GeoBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * GeoBot is distributed in the hope that it will be useful
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GeoBot.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.bashtech.geobot.gui;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class BotGUI extends JFrame{

    private JTextArea statuspane = new JTextArea();
    private JScrollPane scrollpane = new JScrollPane(statuspane);
    
    
    public BotGUI(){
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("GeoBot");
        this.setSize(650, 410);
        
        statuspane.setEditable(false);
        statuspane.setAutoscrolls(true);
        scrollpane.setAutoscrolls(true);
        
        scrollpane.setSize(650, 200);
        
        this.add(scrollpane,BorderLayout.CENTER);
        
        setVisible(true);
	}
    
    public void log(String line){
    	statuspane.setText(statuspane.getText() + "\n" + line);
        statuspane.setCaretPosition(statuspane.getDocument().getLength());
    }


}
