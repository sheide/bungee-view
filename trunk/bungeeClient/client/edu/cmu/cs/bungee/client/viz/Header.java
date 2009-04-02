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

import java.awt.Component;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.AbstractMenuItem;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.Menu;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.umd.cs.piccolo.PNode;

final class Header extends LazyPNode implements MouseDoc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Horizontal space between buttons
	 */
	private final static int BUTTON_MARGIN = 8;

	ClearButton clear;

	private BookmarkButton bookmark;

	private APText clearMessage;

	private RestrictButton restrict;

	private Menu databaseMenu, helpMenu, modeMenu;

	/**
	 * This is used instead of a menu when there is only one database
	 */
	private APText databaseLabel;
	private APText label, countLabel;

	Bungee art;

	private Boundary boundary;

	private ColorKey colorKey;

	private SummaryText summaryText;

	public TextSearch textSearch;

	Header(Bungee _art, String[][] allDBdescriptions, String selectedDBname) {
		art = _art;
		setPaint(Bungee.headerBG);

		label = art.oneLineLabel();
		label.setScale(2);
		label.setTextPaint(Bungee.headerFG);
		label.setPickable(false);
		label.setOffset(5.0, 0.0);
		label.setText("Bungee View");
		addChild(label);

		if (true || art.basicJNLPservice != null) {
			// String mouseDoc = "Show this help in your web browser";
			//
			// showHelp = new Runnable() {
			//
			// public void run() {
			try {
				// String which = helpMenu.getChoice().substring(0, 3);
				// String where = art.codeBase() + which + ".html";
				// if (which.equals("Abo")) {
				// if (helpMenu.getChoice()
				// .equals("About Bungee View"))
				// where += "?"
				// + URLEncoder.encode(art.getBugInfo(),
				// "UTF-8");
				// else
				// where = "About this collection";
				// } else if (which.equals("Wha")) {
				// where = art.codeBase() + "index.html#mining";
				// }
				// art.showDocument(where);
				// } catch (UnsupportedEncodingException e) {
				// e.printStackTrace();
				// }
				// helpMenu.setText("Help");
				// }
				// };

				helpMenu = new Menu(Bungee.headerBG, Bungee.headerFG, art.font);
				helpMenu.mouseDoc = "Show Help Topics";
				helpMenu.setJustification(Component.RIGHT_ALIGNMENT);
				helpMenu.addButton(new HelpMenuItem("Abo.html?"
						+ URLEncoder.encode(art.getBugInfo(), "UTF-8"),
						"About Bungee View"));
				helpMenu.addButton(new HelpMenuItem("<about collection>",
						"About This Collection"));
				helpMenu.addButton(new HelpMenuItem("index.html#mining",
						"What do the colored bars mean?"));
				helpMenu
						.addButton(new HelpMenuItem("Sea.html", "Search Syntax"));
				helpMenu.addButton(new HelpMenuItem("Tip.html",
						"Tips and Tricks"));
				if (art.query.isEditable()) {
					helpMenu.addButton(new HelpMenuItem("Edi.html",
							"Editing the database"));
				}
				helpMenu.setText("Help");
				addChild(helpMenu);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			// Runnable showMode = new Runnable() {
			//
			// public void run() {
			// String which = modeMenu.getChoice();
			// if (which.equals("Beginner Mode")) {
			// art.beginnerMode();
			// } else if (which.equals("Expert Mode")) {
			// art.expertMode();
			// } else if (which.equals("Custom")) {
			// PreferencesDialog.createAndShowGUI(art);
			// }
			// }
			// };

			modeMenu = new Menu(Bungee.headerBG, Bungee.headerFG, art.font);
			modeMenu.mouseDoc = "Set Preferences";
			modeMenu.addButton(new BeginnerModeCommand());
			modeMenu.addButton(new ExpertModeCommand());
			modeMenu.addButton(new CustomModeCommand());
			if (art.isExpertMode())
				modeMenu.setText("Expert Mode");
			addChild(modeMenu);

			clearMessage = art.oneLineLabel();
			// clearMessage.setPaint(Bungee.summaryFG);
			clearMessage.setTextPaint(Bungee.helpColor);
			clearMessage.setPickable(false);
			clearMessage
					.setText(" (To clear a single filter, click on the tag again.) ");

			clear = new ClearButton();
			// clear.addInputEventListener(new ClearQueryHandler());
			clear.setVisible(false);
			addChild(clear);

			if (true || art.jnlpClipboardService != null) {
				bookmark = new BookmarkButton();
				bookmark.setVisible(false);
				addChild(bookmark);
			}

			restrict = new RestrictButton();
			restrict.setVisible(false);
			addChild(restrict);

			textSearch = new TextSearch(art);
			addChild(textSearch);
		}

		// Add this after help, so it won't be occluded
		String selectedDBdescription = allDBdescriptions[0][1];
		if (allDBdescriptions.length > 1) {
			// String mouseDoc = "Select this database";
			//
			// Runnable setDatabase = new Runnable() {
			//
			// public void run() {
			// art.setDatabase((String) databaseMenu.getData());
			// }
			// };

			databaseMenu = new Menu(Bungee.headerBG, Bungee.headerFG, art.font);
			databaseMenu.setJustification(Component.LEFT_ALIGNMENT);
			databaseMenu.mouseDoc = "Choose among Collections";
			for (int i = 0; i < allDBdescriptions.length; i++) {
				String dbName = allDBdescriptions[i][0];
				String dbDesc = allDBdescriptions[i][1];
				databaseMenu.addButton(new SetDatabaseCommand(dbDesc, dbName));
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

		countLabel = art.oneLineLabel();
		countLabel.setTextPaint(Bungee.headerFG);
		countLabel.setPickable(false);
		addChild(countLabel);

		summaryText = new SummaryText(art);
		summaryText.setColor(Bungee.headerBG);
		addChild(summaryText);

		boundary = new Boundary(this, true);
		boundary.mouseDoc = "Start dragging boundary to change font size";
		addChild(boundary);

		setPickable(false);
	}

	double validate(double w) {
		double h = Math.ceil(4.5 * art.lineH);
		// Util.print("Header.validate " + w + "x" + h);
		setBounds(0, 0, w, h);
		label.setFont(art.font);
		countLabel.setFont(art.font);
		countLabel.setOffset(Math.round(label.getMaxX() * 1.1), Math
				.round((label.getMaxY() - countLabel.getHeight()) / 2.0));

		double x = label.getXOffset();

		validateColorKey();

		summaryText.setOffset(countLabel.getXOffset() + 10, countLabel
				.getMaxY());
		// Util.print("ttt " + summaryText.getXOffset() + " "
		// + colorKey.getFullBounds().getWidth() + " " + w);
		summaryText.validate(w - colorKey.getFullBounds().getWidth()
				- summaryText.getXOffset() - 20);

		double toolY = summaryText.getMaxY() + BUTTON_MARGIN;
		modeMenu.setFont(art.font);
		modeMenu.setOffset(x, label.getMaxY());
		x += modeMenu.getWidth() + BUTTON_MARGIN;
		boundary.validate();
		updateData();

		if (helpMenu != null) {
			helpMenu.setFont(art.font);
			helpMenu.setOffset(w - helpMenu.getGlobalBounds().getWidth() - 10,
					Math
							.round((label.getMaxY() - helpMenu.value
									.getHeight()) / 2.0));
		}

		double buttonY = toolY + (modeMenu.getHeight() - clear.getHeight())
				/ 2.0;
		x = summaryText.getXOffset();

		// Place this now so getTopMargin works
		clear.setFont(art.font);
		clear.setOffset(x, buttonY);
		clearMessage.setOffset(x, buttonY);
		x += clear.getWidth() + BUTTON_MARGIN;

		// double ellipsisX = w - ellipsis.minWidth();
		// ellipsis.setOffset(ellipsisX, summaryTextY - 2); // -2 corrects for
		// border, and makes
		// text line up

		restrict.setFont(art.font);
		restrict.setOffset(x, buttonY);
		x += restrict.getWidth() + BUTTON_MARGIN;
		if (bookmark != null) {
			bookmark.setFont(art.font);
			bookmark.setOffset(x, buttonY);
			x += bookmark.getWidth() + BUTTON_MARGIN;
		}

		x += 2 * BUTTON_MARGIN;
		textSearch.setWidth(colorKey.getXOffset() - x - 3 * BUTTON_MARGIN);
		// Util.print("iii " + clear.getHeight() + " "
		// + textSearch.getFullBounds().getHeight());
		textSearch.setOffset(x, toolY);
		textSearch.positionLabels();
		validateColorKey();

		return h;
	}

	private void placeDatabaseWidget() {
		double x = countLabel.getMaxX() + 10;
		double y = countLabel.getYOffset();
		if (databaseLabel != null)
			databaseLabel.setOffset(x, y);
		// Util.print("ppp " + label.getGlobalBounds() + " " +
		// label.getHeight());
		if (databaseMenu != null) {
			databaseMenu.setFont(art.font);
			databaseMenu.setOffset(x, y);
		}
	}

	void validateColorKey() {
		if (colorKey != null)
			colorKey.removeFromParent();
		colorKey = new ColorKey(art);
		// Util.print("validateColorKey "+colorKey.getFullBounds()+" "+textSearch
		// .getFullBounds());
		colorKey.setOffset(getWidth() - colorKey.getFullBounds().getWidth()
				- 10, textSearch.getFullBounds().getMaxY()
				- colorKey.getFullBounds().getHeight());
		colorKey.setVisible(art.query.isRestricted());
		colorKey.setPickable(false);
		addChild(colorKey);
	}

	public void updateBoundary(Boundary boundary1) {
		// System.out.println("Header.updateBoundary " + (art.fontLineH() /
		// art.textH * boundary.center()
		// / getHeight()));
		assert boundary1 == boundary;
		if (art.getShowBoundaries()) {
			int fontsize = (int) Math.round(art.getTextSize()
					* boundary1.center() / (2.5 * art.lineH /* fontLineH() */));
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

	double widgetWidth() {
		return clear.minWidth() + restrict.minWidth() + bookmark.minWidth() + 2
				* BUTTON_MARGIN;
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

	void updateData() {
		if (colorKey != null)
			colorKey.setVisible(art.query.isRestricted());

		summaryText.setDescription();
		countLabel.setText(Util.addCommas(art.query.getTotalCount()) + " "
				+ art.query.getGenericObjectLabel(true) + " from");
		placeDatabaseWidget();

		// setEllipsisVisibility();

		boolean buttonsVisible = art.query.isRestricted();
		clear.setNumFilters();
		clear.setVisible(buttonsVisible || art.query.isRestrictedData());
		if (bookmark != null) {
			bookmark.setVisible(buttonsVisible);
		}
		if (restrict != null) {
			restrict.setVisible(buttonsVisible);
		}

		if (bookmark != null)
			bookmark.showDisabledState();
		if (restrict != null)
			restrict.showDisabledState();
	}

	private class HelpMenuItem extends AbstractMenuItem {
		String url;

		HelpMenuItem(String url, String label) {
			super(label, "Show this help in your web browser");
			this.url = url;
		}

		public String doCommand() {
			String where = url;
			if (where == "<about collection>")
				where = art.aboutCollection();
			art.showDocument(where);
			return null;
		}

	}

	private class BeginnerModeCommand extends AbstractMenuItem {
		BeginnerModeCommand() {
			super("Beginner Mode", "Disable advanced features");
		}

		public String doCommand() {
			art.beginnerMode();
			return "Beginner Mode";
		}

	}

	private class ExpertModeCommand extends AbstractMenuItem {
		ExpertModeCommand() {
			super("Expert Mode", "Enable advanced features");
		}

		public String doCommand() {
			art.expertMode();
			return "Expert Mode";
		}

	}

	private class CustomModeCommand extends AbstractMenuItem {
		CustomModeCommand() {
			super("Custom", "Choose features");
		}

		public String doCommand() {
			PreferencesDialog.createAndShowGUI(art);
			return "Custom";
		}

	}

	private class SetDatabaseCommand extends AbstractMenuItem {
		private String dbName;

		SetDatabaseCommand(String dbDesc, String dbName) {
			super(dbDesc, "Select this database");
			this.dbName = dbName;
		}

		public String doCommand() {
			art.setDatabase(dbName);
			return getLabel();
		}

	}

	void setDBdescription(String desc) {
		if (databaseMenu != null)
			databaseMenu.setText(desc);
		else
			databaseLabel.setText(desc);
		// if (databaseMenu != null)
		// databaseMenu.setWidth(art.getStringWidth(desc));
	}

	// public void setMouseDoc(PNode source, boolean state) {
	// art.setMouseDoc(source, state);
	// }

	public void setMouseDoc(String doc) {
		art.setMouseDoc(doc);
	}

	public void updateSelections(Set facets) {
		// Util.print("Header.updateSelections");
		summaryText.updateSelections(facets);
	}

	// public void setMouseDoc(Markup doc, boolean state) {
	// art.setMouseDoc(doc, state);
	// }

	void clickClear() {
		clear.doPick();
	}

	void clearQuery() {
		art.printUserAction(Bungee.BUTTON, "Clear", 0);
		boolean isMultipleFilters = art.query.nFilters(false, true, false,
				false) > 1;
		art.clearQuery();
		if (isMultipleFilters) {
			addChild(clearMessage);
		}
	}

	void doHideTransients() {
		if (clearMessage.getParent() != null)
			removeChild(clearMessage);
		// contractSummary();
		// colorKey.fit();
		textSearch.doHideTransients();
	}

	// void expandColorKey() {
	// colorKey.expand();
	// }

	class SummaryButton extends BungeeTextButton {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		SummaryButton(String text) {
			super(text, Bungee.headerBG, Util.brighten(Bungee.headerFG, 0.8f),
					art, null);
		}

	}

	final class ClearButton extends SummaryButton {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static final String label1 = " Clear  ";

		ClearButton() {
			// super(label1, art.font, 0, 0, art.getStringWidth(" Unrestrict "),
			// art.lineH /* / scale */+ 2, null, 1.8f, color,
			// Bungee.summaryBG);
			super(label1);
			((APText) child).setConstrainWidthToTextWidth(true);
			mouseDoc = "Remove all text and category filters";

			// nFiltersLabel = art.oneLineLabel();
			// nFiltersLabel.setTextPaint(color);
			// nFiltersLabel.setJustification(Component.CENTER_ALIGNMENT);
			// // nFiltersLabel.setScale(1.0 / scale);
			// nFiltersLabel.setText(label);
			// nFiltersLabel.setOffset(Math.round((child.getWidth() -
			// nFiltersLabel
			// .getWidth()) / 2.0), 0.0);
			// child.addChild(nFiltersLabel);

			// setScale(scale);
		}

		public void doPick() {
			if (getText().equals(label1))
				clearQuery();
			else
				art.setDatabase(art.dbName);
		}

		void setNumFilters() {
			if (!art.query.isRestricted() && art.query.isRestrictedData()) {
				mouseDoc = "Revert to exploring the entire database";
				setText(" Unrestrict  ");
			} else {
				mouseDoc = "Remove " + art.query.describeNfilters();
				setText(label1);
			}
		}
	}

	final class BookmarkButton extends SummaryButton {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static final String label1 = " Bookmark  ";

		BookmarkButton() {
			// super(label1, art.font, 0, 0, art.getStringWidth(label1),
			// art.lineH /* / scale */+ 2, null, 1.8f, color,
			// Bungee.summaryBG);
			super(label1);
			((APText) child).setConstrainWidthToTextWidth(true);
			mouseDoc = "Copy a URL for your current query to the system Clipboard";
			setDisabledMessage("There are no matches to bookmark");
			if (art.informedia != null) {
				setText("New Video Set");
				mouseDoc = "Save these matches as an Informedia video set";
				setDisabledMessage("There are no matches to save");
			}
		}

		public boolean isEnabled() {
			return art.query.getOnCount() > 0;
		}

		public void doPick() {
			if (art.informedia != null)
				art.saveVideoSet();
			else
				art.copyBookmark();
		}

		public void mayHideTransients(PNode node) {
			art.mayHideTransients();
		}

	}

	final class RestrictButton extends SummaryButton {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static final String label1 = " Restrict  ";

		RestrictButton() {
			// super(label1, art.font, 0, 0, art.getStringWidth(label1),
			// art.lineH /* / scale */+ 2, null, 1.8f, color,
			// Bungee.summaryBG);
			super(label1);
			((APText) child).setConstrainWidthToTextWidth(true);
			mouseDoc = "Explore within the current matches";
			setDisabledMessage("There are no matches to restrict to");
		}

		public boolean isEnabled() {
			return art.query.getOnCount() > 0;
		}

		public void doPick() {
			art.restrict();
		}

		public void mayHideTransients(PNode node) {
			art.mayHideTransients();
		}

	}
}