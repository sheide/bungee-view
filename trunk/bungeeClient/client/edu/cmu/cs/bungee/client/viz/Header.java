/* 

 Created on Mar 4, 2005

 Bungee View lets you search, browse, and data-mine an image collection.  
 Copyright (C) 2006  Mark Derthick

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.  See gpl.html.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 You may also contact the author at 
 mad@cs.cmu.edu, 
 or at
 Mark Derthick
 Carnegie-Mellon University
 Human-Computer Interaction Institute
 Pittsburgh, PA 15213

 */

package edu.cmu.cs.bungee.client.viz;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.Menu;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.umd.cs.piccolo.PNode;


public class Header extends LazyPNode implements MouseDoc {

	private static final long serialVersionUID = 6662532437514803590L;

	 Menu databaseMenu;

	 APText databaseLabel;

	 Menu helpMenu;

	 Bungee art;

	private APText label;

	private Boundary boundary;

	public void validate(double w, double h) {
		setBounds(0, 0, w, h);
		setPaint(Bungee.headerBG);
		if (databaseLabel != null)
			databaseLabel.setOffset(Math
					.round((w - databaseLabel.getGlobalBounds().getWidth()) / 2.0), Math
					.round((h - databaseLabel.getGlobalBounds().getHeight()) / 2.0));
		if (databaseMenu != null) {
			databaseMenu.setFont(art.font);
			databaseMenu.setOffset(Math
					.round((w - databaseMenu.getGlobalBounds().getWidth()) / 2.0), Math
					.round((h - databaseMenu.value.getGlobalBounds().getHeight()) / 2.0));
		}
		if (helpMenu != null) {
			helpMenu.setFont(art.font);
			helpMenu.setOffset(w - helpMenu.getGlobalBounds().getWidth() - 10, Math
					.round((h - helpMenu.getGlobalBounds().getHeight()) / 2.0));
		}
		label.setScale(h * 0.9 / label.getHeight());
		boundary.validate();
	}

	public void updateBoundary(Boundary boundary1) {
//		 System.out.println("Header.updateBoundary " + (art.fontLineH() / art.textH * boundary.center()
//					/ getHeight()));
		assert boundary1 == boundary1;
		int fontsize = (int) Math.round(art.textH * boundary1.center() / (2.5 * art.fontLineH()));
		if (fontsize != art.textH) {
			art.setTextSize(fontsize);
			art.revalidate();
		}
	}

	public double minHeight(Boundary ignore) {
		assert ignore == boundary;
		int minTextH = 8;
		return Math.ceil(minTextH * getHeight() / art.textH);
	}

	public double maxHeight(Boundary ignore) {
		assert ignore == boundary;
		double contentW = 2 * label.getGlobalBounds().getWidth();
		if (databaseMenu != null)
			contentW += databaseMenu.getGlobalBounds().getWidth();
		else if (databaseLabel!= null)
			contentW += databaseLabel.getGlobalBounds().getWidth();		
		return Math.ceil(getHeight() * getWidth() / contentW);
	}

	public Header(Bungee _art, String[][] allDBdescriptions, String selectedDBname) {
		art = _art;

		if (true || art.basicJNLPservice != null) {
			String mouseDoc = "Show this help in your web browser";


			Runnable showHelp = new Runnable() {

				public void run() {
					try {
						String which = helpMenu.getChoice().substring(0, 3);
						String where = art.codeBase() + which + ".html";
						if (which.equals("Abo")) {
							if (helpMenu.getChoice().equals("About Bungee View"))
								where += "?"
										+ URLEncoder.encode(art.getBugInfo(), "UTF-8");
							else
								where = "About this collection";
						} else if (which.equals("Wha")) {
							where = art.codeBase() + "index.html#mining";
						}
						art.showDocument(where);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					helpMenu.value.setText("Help");
				}
			};
			
			helpMenu = new Menu(Bungee.headerBG, Bungee.headerFG, showHelp, art.font);
			helpMenu.addButton("About Bungee View", mouseDoc);
			helpMenu.addButton("About This Collection", mouseDoc);
			helpMenu.addButton("What do the colored bars mean?", mouseDoc);
			helpMenu.addButton("Search Syntax", mouseDoc);
			helpMenu.addButton("Tips and Tricks", mouseDoc);
			helpMenu.value.setText("Help");
			addChild(helpMenu);
		}

		// Add this after help, so it won't be occluded
		String selectedDBdescription = allDBdescriptions[0][1];
		if (allDBdescriptions.length > 1) {
			String mouseDoc = "Select this database";


			Runnable setDatabase = new Runnable() {

				public void run() {
					art.setDatabase((String) databaseMenu.getData());
				}
			};
			
			databaseMenu = new Menu(Bungee.headerBG, Bungee.headerFG, setDatabase,
					art.font);
			for (int i = 0; i < allDBdescriptions.length; i++) {
				String dbName = allDBdescriptions[i][0];
				String dbDesc = allDBdescriptions[i][1];
				databaseMenu.addButton(dbDesc, mouseDoc, dbName);
				if (dbName.equalsIgnoreCase(selectedDBname))
					selectedDBdescription = dbDesc;
			}
			databaseMenu.value.setText(selectedDBdescription);
			addChild(databaseMenu);
		} else {
			databaseLabel = art.oneLineLabel();
			databaseLabel.setTextPaint(Bungee.headerFG);
			databaseLabel.setPickable(false);
			databaseLabel.setText(selectedDBdescription);
			addChild(databaseLabel);
		}

		label = art.oneLineLabel();
		label.setTextPaint(Bungee.headerFG);
		label.setPickable(false);
		label.setOffset(5.0, 0.0);
		label.setText("Bungee View");
		addChild(label);

		boundary = new Boundary(this, true);
		addChild(boundary);

		setPickable(false);
	}
	
	public void setDBdescription(String desc, boolean isRestriction) {
		APText dbLabel = databaseMenu != null ? databaseMenu.value : databaseLabel;
		if (isRestriction)
			desc = dbLabel.getText() + " / " + desc;
		dbLabel.setText(desc);
		if (databaseMenu != null)
			databaseMenu.setWidth(art.getStringWidth(desc));
	}

	public void setMouseDoc(PNode source, boolean state) {
		art.setMouseDoc(source, state);
	}

	public void setMouseDoc(String doc, boolean state) {
		art.setMouseDoc(doc, state);
	}

	public void setMouseDoc(Markup doc, boolean state) {
		art.setMouseDoc(doc, state);
	}
}