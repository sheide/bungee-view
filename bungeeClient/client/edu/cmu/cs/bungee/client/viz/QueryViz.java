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

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.cmu.cs.bungee.piccoloUtils.gui.TextButton;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolox.event.PStyledTextEventHandler;
import edu.umd.cs.piccolox.nodes.PStyledText;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

final class QueryViz extends LazyPNode implements MouseDoc {

	// private double h;

	private final Summary summary;

	/**
	 * A p-list of DeleteButtons and FacetTexts for both text searches and
	 * clusters
	 */
	private final List searches = new Vector();

	private PStyledText searchBox;

	JTextComponent editor;

	private PStyledTextEventHandler textHandler;

	private APText searchLabel;

	private APText shortSearchWarning;

	QueryViz(Summary _summary) {
		summary = _summary;
		setPickable(false);
	}

	boolean isInitted = false;

	private Boundary boundary;

	private APText clusterLabel;

	/**
	 * This gets called after validate is first called
	 */
	void init() {
		// Util.print("QueryViz.init");
		shortSearchWarning = oneLineLabel();
		// shortSearchWarning.setTextPaint(Art.unselectedLabelColor);
		shortSearchWarning.setPaint(Color.yellow);
		shortSearchWarning.setPickable(false);
		// shortSearchWarning.setVisible(false);
		// addChild(shortSearchWarning);

		searchLabel = oneLineLabel();
		searchLabel.setTextPaint(Bungee.summaryFG);
		searchLabel.setPickable(false);
		searchLabel.setVisible(false);
		searchLabel.setText("Text Search");
		addChild(searchLabel);

		clusterLabel = oneLineLabel();
		clusterLabel.setTextPaint(Bungee.summaryFG);
		clusterLabel.setPickable(false);
		clusterLabel.setVisible(false);
		clusterLabel.setText("Clusters");
		addChild(clusterLabel);

		editor = createEditor();
		editor.setFont(font());
		// editor.setText(" ");
		textHandler = new PStyledTextEventHandler(art().getCanvas(), editor);
		addInputEventListener(textHandler);

		searchBox = new PStyledText();
		searchBox.setPaint(Bungee.summaryFG);
		searchBox.setVisible(false);
		searchBox.setConstrainHeightToTextHeight(false);
		searchBox.setConstrainWidthToTextWidth(false);
		// searchBox = textHandler.createText();
		Document doc = editor.getUI().getEditorKit(editor)
				.createDefaultDocument();
		searchBox.setDocument(doc);
		searchBox.setHeight(lineH() - 2);
		addChild(searchBox);

		InputMap inputMap = editor.getInputMap();
		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(key, new KeypressEnterAction(this));

		boundary = new Boundary(this, false);
		addChild(boundary);

		isInitted = true;
		positionSearchBox();
	}

	double getBottomMargin() {
		int nSearches = query().nSearches();
		// 1 is for label, and 0.5 is for a margin below
		double margin = lineH() * (1.5 + nSearches);
		int nClusters = query().nClusters();
		if (nClusters > 0) {
			margin += lineH() * (1 + nClusters);
		}
		// if (nSearches > 0)
		// margin += 2;
		// Util.print("bottom " + margin);
		return margin;
	}

	void validate(double _w, double _h) {
		// Util.print("qv.validate " + _w);
		assert _w == Math.round(_w);
		setBounds(0, 0, _w, _h);
		if (boundary != null)
			boundary.validate();
		// positionSearchBox();
		synchronizeWithQuery();
	}

	Bungee art() {
		return summary.art;
	}

	Query query() {
		return summary.q;
	}

	double lineH() {
		return art().lineH + 1.0;
	}

	Font font() {
		return art().font;
	}

	APText oneLineLabel() {
		return art().oneLineLabel();
	}

	double positionSearchBox() {
		// h = _h;
		double y = getHeight() - getBottomMargin();
		if (isInitted) {
			// Util.print("positionSearchBox " + y);
			// setBounds(0, 0, w, h);
			// setPaint(queryBG);
			// label.setOffset(w / 2.0 - label.getScale() * label.getWidth() /
			// 2.0,
			// 0);
			// textLabel.setOffset(2, y);
			// y += textLabel.getScale() * textLabel.getHeight() * 1.2;
			double x = 2;
			searchLabel.setFont(font());
			searchLabel.setOffset(x, y);
			x += Math.ceil(searchLabel.getWidth());
//			searchBox.setHeight(lineH() - 2);
			searchBox.setOffset(x + 10, y);
			searchBox.setWidth(10 * lineH());
			if (searchBox.getParent() == null)
				addChild(searchBox); // If edited string is empty and you
			// click
			// outside, it is removed.
			// searchBox gets taller when you click on it.
			// Can't figure out how to make it get taller initially,
			// so just add a fudge factor.

			x = searchBox.getXOffset() + Math.ceil(searchBox.getWidth()) + 10;
			shortSearchWarning.setOffset(x, searchBox.getYOffset());
			// double availW = getParent().getWidth() - x - 5;
			// assert availW > 0;
			// shortSearchWarning.setScale(Math.min(1.0, availW
			// / shortSearchWarning.getWidth()));

			if (query().nClusters() > 0) {
				x = 2;
				y += lineH() * (1 + query().nSearches());
				clusterLabel.setOffset(x, y);
				clusterLabel.setFont(font());
				y += lineH();
			}
		}
		return y;
	}

	public void updateBoundary(Boundary boundary1) {
		// System.out.println("Grid.updateBoundary " + boundary.centerX());
		assert boundary1 == boundary;
		validate(boundary1.center(), -1);
		summary.updateQueryBoundary();
	}

	public double minWidth() {
		return lineH() * 3;
	}

	public double maxWidth() {
		return summary.getWidth() - summary.widgetWidth();
	}

	void doSearch() {
		String s = editor.getText().trim();
		art().printUserAction(Bungee.SEARCH, s, 0);
		// Util.print("doSearch '" + s + "'");
		String error = Query.isShortSearch(s);
		if (error != null) {
			shortSearchWarning.setText(error);
			addChild(shortSearchWarning);
			if (searchBox.getParent() == null)
				addChild(searchBox);
		} else {
			editor.setText(null);
			summary.mayHideTransients();
			query().addTextSearch(s);
			art().updateAllData();

			// This doesn't work - it is already grabbed from PInputManager's
			// point of view.
			// art().grabFocus();
		}
	}

	void doHideTransients() {
		if (isInitted) {
			shortSearchWarning.removeFromParent();
			textHandler.stopEditing();
			art().grabFocus();
		}
	}

	static final class KeypressEnterAction extends AbstractAction {

		QueryViz qv;

		/**
		 * What to do when Enter key is pressed
		 */
		KeypressEnterAction(QueryViz _q) {
			qv = _q;
		}

		public void actionPerformed(ActionEvent e) {
			qv.doSearch();
		}
	}

	// copied from PStyledTextEventHandler
	private JTextComponent createEditor() {
		JTextPane tComp = new JTextPane() {

			/**
			 * Set some rendering hints - if we don't then the rendering can be
			 * inconsistent. Also, Swing doesn't work correctly with fractional
			 * metrics.
			 */
			public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING,
						RenderingHints.VALUE_RENDER_QUALITY);
				g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
						RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

				super.paint(g);
			}

			/**
			 * If the standard scroll rect to visible is on, then you can get
			 * weird behaviors if the canvas is put in a scrollpane.
			 */
			// public void scrollRectToVisible() {
			// }
		};
		// tComp.setBackground(FG);
		// tComp.setBorder(new CompoundBorder(new LineBorder(Color.black),
		// new EmptyBorder(3, 3, 3, 3)));
		return tComp;
	}

	// public boolean isRestriction() {
	// return q.isRestricted() || editor.getText().length() != 0;
	// }

	List spareDeleteButtons = new LinkedList();

	void synchronizeWithQuery() {
		if (isInitted) {
			// summary.updateQueryH(0);
			// Util.print("synchronizeWithQuery " );
			List desiredSearches = new LinkedList(query().getSearches());
			double y = getHeight() - getBottomMargin() + lineH();
			ListIterator nodeIt = searches.listIterator();
			// Out with the old Strings
			while (nodeIt.hasNext()) {
				DeleteSearchButton button = (DeleteSearchButton) nodeIt.next();
				if (button.treeObject instanceof Cluster)
					break;
				else
					y = updateButton(desiredSearches, nodeIt, button, y);
			}
			// nodeIt either points to the first cluster FacetText or nothing
			if (nodeIt.hasNext())
				nodeIt.previous();
			// In with the new Strings
			for (Iterator textIt = desiredSearches.iterator(); textIt.hasNext();) {
				String desiredSearch = (String) textIt.next();
				y = addButton(nodeIt, desiredSearch, y);
			}
			int nClusters = query().nClusters();
			clusterLabel.setVisible(nClusters > 0);
			// if (nClusters > 0) {
			y = positionSearchBox();
			// }
			List desiredClusters = new LinkedList(query().clusters());
			// Out with the old Clusters
			while (nodeIt.hasNext()) {
				DeleteSearchButton button = (DeleteSearchButton) nodeIt.next();
				assert button.treeObject instanceof Cluster;
				y = updateButton(desiredClusters, nodeIt, button, y);
			}
			// In with the new Clusters
			for (Iterator it = desiredClusters.iterator(); it.hasNext();) {
				Cluster cluster = (Cluster) it.next();
				// String s = cluster.toString();
				y = addButton(nodeIt, cluster, y);
			}
		}
	}

	double updateButton(Collection desired, ListIterator nodeIt,
			DeleteSearchButton button, double y) {
		if (!desired.contains(button.treeObject)) {
			removeChild(button);
			spareDeleteButtons.add(button);
			nodeIt.remove();
			FacetText text = (FacetText) nodeIt.next();
			removeChild(text);
			nodeIt.remove();
		} else {
			desired.remove(button.treeObject);
			button.setOffset(5, y + 2);
			button.setFont(font());
			FacetText text = (FacetText) nodeIt.next();
			text.setOffset(Math.ceil(button.getMaxX() + 5), y);
			text.setFont(font());
			y += lineH();
		}
		return y;
	}

	double addButton(ListIterator nodeIt, Object treeObject, double y) {
		// Util.print("addButton " + s);
		// nodeIt = null;
		DeleteSearchButton delete = (DeleteSearchButton) (spareDeleteButtons
				.size() > 0 ? spareDeleteButtons.remove(0)
				: new DeleteSearchButton());
		addChild(delete);
		delete.setFont(font());
		// delete.addInputEventListener(deleteSearchHandler);
		nodeIt.add(delete);
		delete.setOffset(5, y + 2);
		delete.treeObject = treeObject;

		FacetText searchText = FacetText.getFacetText(treeObject, art(), false,
				null);
		addChild(searchText);
		// searchText.pickFacetTextNotifier = pickFacetTextNotifier;

		// searchText = oneLineLabel();
		// searchText.setTextPaint(Art.greens[2]);
		// searchText.setPickable(false);
		nodeIt.add(searchText);
		searchText.setOffset(Math.ceil(delete.getMaxX() + 5), y);
		return y + lineH();
	}

	void setSearchVisibility(boolean isVisible) {
//		Util.print("setSearchVisibility12 " + isVisible);
		searchLabel.setVisible(isVisible);
		searchBox.setVisible(isVisible);
		if (searchBox.getParent() == null)
			addChild(searchBox);
	}

	boolean removeSearch(Object treeObject) {
		boolean result = false;
		summary.mayHideTransients();
		if (treeObject instanceof String) {
			String searchText = (String) treeObject;
			art().printUserAction(Bungee.BUTTON, searchText, 0);
			result = query().removeTextSearch(searchText);
		} else {
			query().toggleCluster((Cluster) treeObject);
			result = true;
		}
		if (result)
			art().updateAllData();
		return result;
	}

	void highlightCluster(Cluster cluster) {
		// Util.print("highlightCluster " + cluster.toString());
		if (clusterLabel.getVisible()) {
			for (Iterator nodeIt = searches.iterator(); nodeIt.hasNext();) {
				nodeIt.next();
				FacetText label = (FacetText) nodeIt.next();
				if (cluster.equals(label.cluster)) {
					label.setTextPaint(art().clusterTextColor(cluster));
				}
			}
		}
	}

	// public void clearSearchDoc(String searchText, boolean state) {
	// if (state)
	// art().setClickDesc("Remove search '" + searchText + "'");
	// else
	// art().setClickDesc((String) null);
	// }

	public void setMouseDoc(PNode source, boolean state) {
		art().setMouseDoc(source, state);
	}

	public void setMouseDoc(String doc) {
		art().setMouseDoc(doc);
	}

	// public void setMouseDoc(Markup doc, boolean state) {
	// art().setMouseDoc(doc, state);
	// }

	// final class ClearQueryHandler extends PBasicInputEventHandler {
	// public void mousePressed(PInputEvent e) {
	// //Util.print("PerspectiveSelectionHandler.mousePressed");
	// PNode node = e.getPickedNode();
	// while (!(node instanceof Summary) && node != null)
	// node = node.getParent();
	// if (node instanceof Summary)
	// ((Summary) node).clearQuery();
	// else {
	// //Util.print(
	// // "PerspectiveSelectionHandler picked a PNode that's not a
	// // PerspectiveViz: "
	// // + e.getPickedNode());
	// super.mousePressed(e);
	// }
	// }
	//
	// public void mouseExited(PInputEvent e) {
	// //Util.print("PerspectiveSelectionHandler.mousePressed");
	// PNode node = e.getPickedNode();
	// while (!(node instanceof Summary) && node != null)
	// node = node.getParent();
	// if (node instanceof Summary)
	// ((Summary) node).clearQueryDoc(false);
	// else {
	// //Util.print(
	// // "PerspectiveSelectionHandler picked a PNode that's not a
	// // PerspectiveViz: "
	// // + e.getPickedNode());
	// super.mousePressed(e);
	// }
	// }
	//
	// public void mouseEntered(PInputEvent e) {
	// //Util.print("PerspectiveSelectionHandler.mousePressed");
	// PNode node = e.getPickedNode();
	// while (!(node instanceof Summary) && node != null)
	// node = node.getParent();
	// if (node instanceof Summary)
	// ((Summary) node).clearQueryDoc(true);
	// else {
	// //Util.print(
	// // "PerspectiveSelectionHandler picked a PNode that's not a
	// // PerspectiveViz: "
	// // + e.getPickedNode());
	// super.mousePressed(e);
	// }
	// }
	// }

	 final class DeleteSearchButton extends BungeeTextButton {

		// String searchText;
		//
		// Cluster cluster;

		Object treeObject;

		DeleteSearchButton() {
//			super("X", art.font, 0, 0, art.lineH, art.lineH, null, 2.0f,
//					Bungee.summaryFG, Bungee.summaryBG);
			super("X", Bungee.summaryFG, Bungee.summaryBG, QueryViz.this.art());
			setScale(0.6);
		}

		// void setSearchText(String text) {
		// searchText = text;
		// mouseDoc = "Remove search '" + searchText + "'";
		// }

		public void doPick() {
			removeSearch(treeObject);
		}

		// public void exit() {
		// QueryViz parent = ((QueryViz) getParent());
		// if (parent != null)
		// // When button is being deleted, it won't have a parent
		// parent.clearSearchDoc(searchText, false);
		// }
		//
		// public void enter() {
		// ((QueryViz) getParent()).clearSearchDoc(searchText, true);
		// }

	}

	void setSelectedForEdit(Perspective facet) {
		// Util.print("QueryViz.setSelectedForEdit");
		// searchBox.setEditing(true);
		try {
			Document doc = searchBox.getDocument();
			doc.remove(0, doc.getLength());
			doc.insertString(0, facet.toString(), null);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// searchBox.setVisible(true);
		searchBox.syncWithDocument();
		searchBox.validateFullPaint();
		searchBox.repaint();
		// editor.setEditable(true);
		// editor.setText(facet.toString());
		// editor.revalidate();
		// editor.repaint();
		// textHandler.stopEditing();
	}

	// final class DeleteSearch extends APText {
	// String searchText;
	//
	// DeleteSearch(String s) {
	// super("x");
	// setUnderline(true);
	// searchText = s;
	// }
	// }
	//
	// final class DeleteSearchHandler extends PBasicInputEventHandler {
	// public void mousePressed(PInputEvent e) {
	// DeleteSearch node = (DeleteSearch) e.getPickedNode();
	// ((QueryViz) node.getParent()).removeSearch(node.searchText);
	// }
	//
	// public void mouseExited(PInputEvent e) {
	// DeleteSearch node = (DeleteSearch) e.getPickedNode();
	// if (node.getParent() != null)
	// ((QueryViz) node.getParent())
	// .clearSearchDoc(node.searchText, false);
	// }
	//
	// public void mouseEntered(PInputEvent e) {
	// DeleteSearch node = (DeleteSearch) e.getPickedNode();
	// ((QueryViz) node.getParent()).clearSearchDoc(node.searchText, true);
	// }
	// }

}