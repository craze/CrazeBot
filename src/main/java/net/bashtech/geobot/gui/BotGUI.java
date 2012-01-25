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
