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
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.cmu.cs.bungee.piccoloUtils.gui.TextButton;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolox.event.PStyledTextEventHandler;
import edu.umd.cs.piccolox.nodes.PStyledText;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;



public class QueryViz extends LazyPNode implements MouseDoc {

	private static final long serialVersionUID = -4215862457900923376L;

	private double h;

	private final Summary summary;

	private final List searches = new Vector();

	private PStyledText searchBox;

	JTextComponent editor;

	private PStyledTextEventHandler textHandler;

	private APText searchLabel;

	private APText shortSearchWarning;

	public QueryViz(Summary _summary) {
		summary = _summary;
		setPickable(false);
	}

	boolean isInitted = false;

	private Boundary boundary;

	private APText clusterLabel;

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
		editor.setFont(art().font);
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
		searchBox.setHeight(lineH());
		addChild(searchBox);

		InputMap inputMap = editor.getInputMap();
		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(key, new EnterAction(this));

		boundary = new Boundary(this, false);
		addChild(boundary);

		isInitted = true;
		positionSearchBox(h);
	}

	double getBottomMargin() {
		int nSearches = query().getSearches().size();
		double margin = 6 + lineH() * (1 + nSearches);
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
		positionSearchBox(_h);
	}

	Bungee art() {
		return summary.art;
	}

	Query query() {
		return summary.q;
	}
	
	double lineH() {
		return art().lineH;
	}
	
	APText oneLineLabel() {
		return art().oneLineLabel();
	}

	void positionSearchBox(double _h) {
		h = _h;
		if (isInitted) {
			// setBounds(0, 0, w, h);
			// setPaint(queryBG);
			// label.setOffset(w / 2.0 - label.getScale() * label.getWidth() /
			// 2.0,
			// 0);
			// textLabel.setOffset(2, y);
			// y += textLabel.getScale() * textLabel.getHeight() * 1.2;
			double y = h - getBottomMargin();
			double x = 2;
			searchLabel.setFont(art().font);
			searchLabel.setOffset(x, y);
			x += Math.ceil(searchLabel.getWidth());
			searchBox.setHeight(lineH());
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
				y += lineH() * (1 + query().getSearches().size());
				clusterLabel.setOffset(x, y);
			}
		}
	}

	public void updateBoundary(Boundary boundary1) {
		// System.out.println("Grid.updateBoundary " + boundary.centerX());
		assert boundary1 == boundary;
		validate(boundary1.center(), h);
		summary.updateQueryBoundary();
	}

	public double minWidth() {
		return 50;
	}

	public double maxWidth() {
		return getWidth() + summary.getWidth() - summary.minWidth(null);
	}

	public void doSearch() {
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
		}
	}

	void doHideTransients() {
		if (isInitted) {
			shortSearchWarning.removeFromParent();
			textHandler.stopEditing();
			art().grabFocus();
		}
	}

	static class EnterAction extends AbstractAction {

		private static final long serialVersionUID = -3636184693260706104L;

		QueryViz qv;

		public EnterAction(QueryViz _q) {
			qv = _q;
		}

		public void actionPerformed(ActionEvent e) {
			qv.doSearch();
		}
	}

	// copied from PStyledTextEventHandler
	private JTextComponent createEditor() {
		JTextPane tComp = new JTextPane() {

			private static final long serialVersionUID = -1081364272578141120L;

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

	public void synchronizeWithQuery() {
		if (isInitted) {
			// summary.updateQueryH(0);
			// Util.print("ClearQuery.setVisible " + q.isRestricted());
			Set desiredSearches = query().getSearches();
			// Color color = (desiredSearches.size() > 0 ?
			// Util.brighten(Art.summaryFG)
			// : Art.summaryFG);
			// searchLabel.setTextPaint(color);

			double y = h - getBottomMargin() + searchLabel.getHeight() + 3;

			Iterator textIt = desiredSearches.iterator();
			ListIterator nodeIt = searches.listIterator();
			while (textIt.hasNext()) {
				String desiredSearch = (String) textIt.next();
				addButton(nodeIt, desiredSearch, y, null);
				y += lineH() + 1.0;
			}
			int nClusters = query().nClusters();
			clusterLabel.setVisible(nClusters > 0);
			if (nClusters > 0) {
				positionSearchBox(h);
				y += lineH() + 1.0;
				for (Iterator it = query().clusters().iterator(); it.hasNext();) {
					Cluster cluster = (Cluster) it.next();
					String s = cluster.toString();
					addButton(nodeIt, s, y, cluster);
					y += lineH() + 1.0;
				}
			}
			while (nodeIt.hasNext()) {
				PNode node = (PNode) nodeIt.next();
				if (node.getParent() == null)
					break;
				// removeChild(node);
				node.setVisible(false);
				node.setPickable(false);
				// searches.remove(node);
			}
		}
	}

	void addButton(ListIterator nodeIt, String s, double y, Cluster cluster) {
		// Util.print("addButton " + s);
		DeleteSearchButton delete;
		FacetText searchText;
		if (nodeIt.hasNext()) {
			delete = (DeleteSearchButton) nodeIt.next();
			searchText = (FacetText) nodeIt.next();
			delete.setVisible(true);
			searchText.setVisible(true);
			searchText.setPickable(true);
		} else {
			// nodeIt = null;
			delete = new DeleteSearchButton(art());
			addChild(delete);
			// delete.addInputEventListener(deleteSearchHandler);
			nodeIt.add(delete);

			Object treeObject = s;
			if (cluster != null)
				treeObject = cluster;
			searchText = FacetText.getFacetText(treeObject, art());
			addChild(searchText);
			// searchText.pickFacetTextNotifier = pickFacetTextNotifier;

			// searchText = oneLineLabel();
			// searchText.setTextPaint(Art.greens[2]);
			// searchText.setPickable(false);
			nodeIt.add(searchText);
		}
		delete.setOffset(5, y + 2);
		delete.setSearchText(s);
		delete.cluster = cluster;
		searchText.setOffset(Math.ceil(delete.getXOffset() + delete.getWidth()
				* delete.getScale() + 5), y);
		searchText.setText(s);
		// if (delete.getParent() == null) {
		// addChild(delete);
		// addChild(searchText);
		// }
	}

	public void setSearchVisibility(boolean isVisible) {
		searchLabel.setVisible(isVisible);
		searchBox.setVisible(isVisible);
		if (searchBox.getParent() == null)
			addChild(searchBox);
	}

	public void removeSearch(String searchText) {
		summary.mayHideTransients();
		art().printUserAction(Bungee.BUTTON, searchText, 0);
		if (!query().removeTextSearch(searchText)) {
			Cluster cluster = query().findCluster(searchText);
			assert cluster != null : searchText;
			query().toggleCluster(cluster);
		}
		art().updateAllData();
	}

	void highlightCluster(Cluster cluster) {
		// Util.print("highlightCluster " + cluster.toString());
		if (clusterLabel.getVisible()) {
			for (ListIterator nodeIt = searches.listIterator(); nodeIt
					.hasNext();) {
				DeleteSearchButton button = (DeleteSearchButton) nodeIt.next();
				APText label = (APText) nodeIt.next();
				if (cluster.equals(button.cluster)) {
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

	public void setMouseDoc(String doc, boolean state) {
		art().setMouseDoc(doc, state);
	}

	public void setMouseDoc(Markup doc, boolean state) {
		art().setMouseDoc(doc, state);
	}

	// class ClearQueryHandler extends PBasicInputEventHandler {
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

	static class DeleteSearchButton extends TextButton {

		private static final long serialVersionUID = 7249386072107937171L;

		private String searchText;

		Cluster cluster;

		DeleteSearchButton(Bungee art) {
			super("X", art.font, 0, 0, art.lineH, art.lineH, null, 2.0f,
					Bungee.summaryFG, Bungee.summaryBG);
			setScale(0.6);
		}

		void setSearchText(String text) {
			searchText = text;
			mouseDoc = "Remove search '" + searchText + "'";
		}

		public void doPick() {
			((QueryViz) getParent()).removeSearch(searchText);
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

	public void setSelectedForEdit(Perspective facet) {
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

	// class DeleteSearch extends APText {
	// String searchText;
	//
	// DeleteSearch(String s) {
	// super("x");
	// setUnderline(true);
	// searchText = s;
	// }
	// }
	//
	// class DeleteSearchHandler extends PBasicInputEventHandler {
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