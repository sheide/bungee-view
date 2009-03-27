package edu.cmu.cs.bungee.piccoloUtils.gui;

public interface MenuItem {
	
	String getLabel();
	String getMouseDoc();
	
	// Return the desired Menu.value
	String doCommand();

}
