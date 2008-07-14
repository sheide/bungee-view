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

import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.Menu;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.umd.cs.piccolo.PNode;

final class Header extends LazyPNode implements MouseDoc {

	Menu databaseMenu;

	APText databaseLabel;

	Menu helpMenu, modeMenu;

	Bungee art;

	private APText label;

	private Boundary boundary;

	void validate(double w, double h) {
		// Util.print("Header.validate " + w + "x" + h);
		setBounds(0, 0, w, h);
		if (databaseLabel != null)
			databaseLabel.setOffset(Math.round((w - databaseLabel
					.getGlobalBounds().getWidth()) / 2.0),
					Math
							.round((h - databaseLabel.getGlobalBounds()
									.getHeight()) / 2.0));
		if (databaseMenu != null) {
			databaseMenu.setFont(art.font);
			databaseMenu.setOffset(Math.round((w - databaseMenu
					.getGlobalBounds().getWidth()) / 2.0), Math
					.round((h - databaseMenu.value.getGlobalBounds()
							.getHeight()) / 2.0));
		}
		if (helpMenu != null) {
			helpMenu.setFont(art.font);
			helpMenu
					.setOffset(w - helpMenu.getGlobalBounds().getWidth() - 10,
							Math.round((h - helpMenu.getGlobalBounds()
									.getHeight()) / 2.0));
		}
		label.setScale((h - art.lineH) * 0.9 / label.getHeight());
		modeMenu.setFont(art.font);
		modeMenu.setOffset(label.getXOffset(), label.getMaxY());
		boundary.validate();
	}

	public void updateBoundary(Boundary boundary1) {
		// System.out.println("Header.updateBoundary " + (art.fontLineH() /
		// art.textH * boundary.center()
		// / getHeight()));
		assert boundary1 == boundary;
		if (art.getShowBoundaries()) {
			int fontsize = (int) Math
					.round(art.getTextSize() * boundary1.center()
							/ (2.5 * art.lineH /* fontLineH() */));
			// if (fontsize != art.textH) {
			art.setTextSize(fontsize);
			// art.revalidate();
		}
	}

	public void enterBoundary(Boundary boundary1) {
		if (!art.getShowBoundaries()) {
			boundary1.exit();
		}
	}

	public double minHeight() {
		// assert ignore == boundary;
		return 2 * art.lineH;
	}

	public double maxHeight() {
		// assert ignore == boundary;
		double contentW = 2 * label.getGlobalBounds().getWidth();
		if (databaseMenu != null)
			contentW += databaseMenu.getGlobalBounds().getWidth();
		else if (databaseLabel != null)
			contentW += databaseLabel.getGlobalBounds().getWidth();
		return Math.ceil(getHeight() * getWidth() / contentW);
	}

	Header(Bungee _art, String[][] allDBdescriptions, String selectedDBname) {
		art = _art;
		setPaint(Bungee.headerBG);

		if (true || art.basicJNLPservice != null) {
			String mouseDoc = "Show this help in your web browser";

			Runnable showHelp = new Runnable() {

				public void run() {
					try {
						String which = helpMenu.getChoice().substring(0, 3);
						String where = art.codeBase() + which + ".html";
						if (which.equals("Abo")) {
							if (helpMenu.getChoice()
									.equals("About Bungee View"))
								where += "?"
										+ URLEncoder.encode(art.getBugInfo(),
												"UTF-8");
							else
								where = "About this collection";
						} else if (which.equals("Wha")) {
							where = art.codeBase() + "index.html#mining";
						}
						art.showDocument(where);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					helpMenu.setText("Help");
				}
			};

			helpMenu = new Menu(Bungee.headerBG, Bungee.headerFG, showHelp,
					art.font);
			helpMenu.addButton("About Bungee View", mouseDoc);
			helpMenu.addButton("About This Collection", mouseDoc);
			helpMenu.addButton("What do the colored bars mean?", mouseDoc);
			helpMenu.addButton("Search Syntax", mouseDoc);
			helpMenu.addButton("Tips and Tricks", mouseDoc);
			helpMenu.setText("Help");
			addChild(helpMenu);

			Runnable showMode = new Runnable() {

				public void run() {
					String which = modeMenu.getChoice();
					if (which.equals("Beginner Mode")) {
						art.beginnerMode();
					} else if (which.equals("Expert Mode")) {
						art.expertMode();
					} else if (which.equals("Custom")) {
						PreferencesDialog.createAndShowGUI(art);
					}
				}
			};

			modeMenu = new Menu(Bungee.headerBG, Bungee.headerFG, showMode,
					art.font);

			modeMenu.addButton("Beginner Mode", "Disable advanced features");
			modeMenu.addButton("Expert Mode", "Enable advanced features");
			modeMenu.addButton("Custom", "Choose features");
			if (art.isExpertMode())
				modeMenu.setText("Expert Mode");
			addChild(modeMenu);
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

			databaseMenu = new Menu(Bungee.headerBG, Bungee.headerFG,
					setDatabase, art.font);
			for (int i = 0; i < allDBdescriptions.length; i++) {
				String dbName = allDBdescriptions[i][0];
				String dbDesc = allDBdescriptions[i][1];
				databaseMenu.addButton(dbDesc, mouseDoc, dbName);
				if (dbName.equalsIgnoreCase(selectedDBname))
					selectedDBdescription = dbDesc;
			}
			databaseMenu.setText(selectedDBdescription);
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

	void setDBdescription(String desc) {
		if (databaseMenu != null)
			databaseMenu.setText(desc);
		else
			databaseLabel.setText(desc);
		// if (databaseMenu != null)
		// databaseMenu.setWidth(art.getStringWidth(desc));
	}

	public void setMouseDoc(PNode source, boolean state) {
		art.setMouseDoc(source, state);
	}

	public void setMouseDoc(String doc) {
		art.setMouseDoc(doc);
	}

	// public void setMouseDoc(Markup doc, boolean state) {
	// art.setMouseDoc(doc, state);
	// }
}