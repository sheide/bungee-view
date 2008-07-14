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
import edu.cmu.cs.bungee.client.query.Query.ItemList;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyContainer;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;
import edu.umd.cs.piccolox.event.PStyledTextEventHandler;
import edu.umd.cs.piccolox.nodes.PStyledText;
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
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

final class QueryViz extends LazyContainer implements MouseDoc {

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

//	private APText shortSearchWarning;

	QueryViz(Summary _summary) {
		summary = _summary;
		setPickable(false);
	}

	boolean isInitted = false;

	private Boundary boundary;

	private APText clusterLabel;

	private APText itemListLabel;

	/**
	 * This gets called after validate is first called
	 */
	void init() {
		// Util.print("QueryViz.init");
//		shortSearchWarning = oneLineLabel();
//		shortSearchWarning.setPaint(Bungee.helpColor);
//		shortSearchWarning.setPickable(false);

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

		itemListLabel = oneLineLabel();
		itemListLabel.setTextPaint(Bungee.summaryFG);
		itemListLabel.setPickable(false);
		itemListLabel.setVisible(false);
		itemListLabel.setText("Informedia Queries");
		addChild(itemListLabel);

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
		boundary.margin = -Rank.LABEL_RIGHT_MARGIN / 2;
		addChild(boundary);
		boundary.validate();

		isInitted = true;
		positionLabels();
	}

	double getBottomMargin() {
		int nSearches = query().nSearches();
		// 1 is for label, and 0.5 is for a margin below
		double margin = lineH() * (1.5 + nSearches);
		int nClusters = query().nClusters();
		if (nClusters > 0) {
			margin += lineH() * (1 + nClusters);
		}
		int nItemLists = query().nItemLists();
		if (nItemLists > 0) {
			margin += lineH() * (1 + nItemLists);
		}
		return margin;
	}

	void validate(double _w, double _h) {
		// Util.print("qv.validate " + _w);
		w = _w;
		h = _h;
		assert _w == Math.round(_w);
		if (boundary != null) {
			boundary.validate();
		}
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

	void positionLabels() {
		// h = _h;
		double y = h - getBottomMargin();
		if (isInitted) {
			// Util.print("positionSearchBox " + y);
			double x = Rank.LABEL_LEFT_MARGIN;
			searchLabel.setFont(font());
			searchLabel.setOffset(x, y);
			x += Math.ceil(searchLabel.getWidth());
			// searchBox.setHeight(lineH() - 2);
			searchBox.setOffset(w, y); // x + 10, y);
			searchBox.setWidth(summary.w - w); // 10 * lineH());
			if (searchBox.getParent() == null)
				addChild(searchBox); // If edited string is empty and you
			// click
			// outside, it is removed.
			// searchBox gets taller when you click on it.
			// Can't figure out how to make it get taller initially,
			// so just add a fudge factor.

			x = searchBox.getXOffset() + Math.ceil(searchBox.getWidth()) + 10;
//			shortSearchWarning.setOffset(x, searchBox.getYOffset());
			y += lineH() * (1 + query().nSearches());

			if (query().nClusters() > 0) {
				x = 2;
				clusterLabel.setOffset(x, y);
				clusterLabel.setFont(font());
				y += lineH() * (1 + query().nClusters());
			}

			if (query().nItemLists() > 0) {
				x = 2;
				itemListLabel.setOffset(x, y);
				itemListLabel.setFont(font());
			}
		}
	}

	public void updateBoundary(Boundary boundary1) {
		// System.out.println("Grid.updateBoundary " + boundary.centerX());
		assert boundary1 == boundary;
		validate(boundary1.center(), -1);
		summary.updateQueryBoundary();
	}

	public void enterBoundary(Boundary boundary1) {
		if (!art().getShowBoundaries()) {
			boundary1.exit();
		}
	}

	public double minWidth() {
		return lineH() * 3;
	}

	public double maxWidth() {
		return summary.w - summary.widgetWidth();
	}

	void doSearch() {
		String s = editor.getText().trim();
		art().printUserAction(Bungee.SEARCH, s, 0);
		// Util.print("doSearch '" + s + "'");
		String error = Query.isShortSearch(s);
		if (error != null) {
			art().setTip(error);
//			shortSearchWarning.setText(error);
//			addChild(shortSearchWarning);
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
//			shortSearchWarning.removeFromParent();
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
		};
		return tComp;
	}

	List spareDeleteButtons = new LinkedList();

	void synchronizeWithQuery() {
		if (isInitted) {
			// Util.print("synchronizeWithQuery " );
			positionLabels();
			ListIterator nodeIt = searches.listIterator();
			synchronizeTextSearches(nodeIt);
			synchronizeClusters(nodeIt);
			synchronizeItemLists(nodeIt);
		}
	}

	void synchronizeTextSearches(ListIterator nodeIt) {
		List desiredSearches = new LinkedList(query().getSearches());
		double y = h - getBottomMargin() + lineH();
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
	}

	void synchronizeClusters(ListIterator nodeIt) {
		int nClusters = query().nClusters();
		clusterLabel.setVisible(nClusters > 0);
		double y = clusterLabel.getYOffset() + lineH();
		List desiredClusters = new LinkedList(query().clusters());
		// Out with the old Clusters
		while (nodeIt.hasNext()) {
			DeleteSearchButton button = (DeleteSearchButton) nodeIt.next();
			if (button.treeObject instanceof ItemList)
				break;
			else
				y = updateButton(desiredClusters, nodeIt, button, y);
		}
		// nodeIt either points to the first ItemList FacetText or nothing
		if (nodeIt.hasNext())
			nodeIt.previous();
		// In with the new Clusters
		for (Iterator it = desiredClusters.iterator(); it.hasNext();) {
			Cluster cluster = (Cluster) it.next();
			y = addButton(nodeIt, cluster, y);
		}
	}

	void synchronizeItemLists(ListIterator nodeIt) {
		int nItemLists = query().nItemLists();
		itemListLabel.setVisible(nItemLists > 0);
		double y = itemListLabel.getYOffset() + lineH();
		// }
		List desiredItemLists = new LinkedList(query().itemLists());
		// Out with the old ItemLists
		while (nodeIt.hasNext()) {
			DeleteSearchButton button = (DeleteSearchButton) nodeIt.next();
			assert button.treeObject instanceof ItemList;
			y = updateButton(desiredItemLists, nodeIt, button, y);
		}
		assert !nodeIt.hasNext();
		// In with the new ItemLists
		for (Iterator it = desiredItemLists.iterator(); it.hasNext();) {
			ItemList itemList = (ItemList) it.next();
			y = addButton(nodeIt, itemList, y);
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
		DeleteSearchButton delete = (DeleteSearchButton) (spareDeleteButtons
				.size() > 0 ? spareDeleteButtons.remove(0)
				: new DeleteSearchButton());
		addChild(delete);
		delete.setFont(font());
		nodeIt.add(delete);
		delete.setOffset(5, y + 2);
		delete.treeObject = treeObject;

		FacetText searchText = FacetText.getFacetText(treeObject, art(), false,
				null);
		addChild(searchText);
		nodeIt.add(searchText);
		searchText.setOffset(Math.ceil(delete.getMaxX() + 5), y);
		return y + lineH();
	}

	void setSearchVisibility(boolean isVisible) {
		// Util.print("setSearchVisibility12 " + isVisible);
		isVisible = isVisible || query().nSearches() > 0;
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
		} else if (treeObject instanceof Cluster) {
			query().toggleCluster((Cluster) treeObject);
			result = true;
		} else if (treeObject instanceof ItemList) {
			query().toggleItemList((ItemList) treeObject);
			result = true;
		} else {
			assert false : treeObject;
		}
		if (result)
			art().updateAllData();
		return result;
	}

	void highlightCluster(Set clusters) {
		// Util.print("highlightCluster " + cluster.toString());
		if (clusterLabel.getVisible()) {
			for (Iterator nodeIt = searches.iterator(); nodeIt.hasNext();) {
				nodeIt.next();
				FacetText label = (FacetText) nodeIt.next();
				if (clusters.contains(label.cluster)) {
					label.setTextPaint(art().clusterTextColor(label.cluster));
				}
			}
		}
	}

//	public void setMouseDoc(PNode source, boolean state) {
//		art().setMouseDoc(source, state);
//	}

	public void setMouseDoc(String doc) {
		art().setMouseDoc(doc);
	}

	final class DeleteSearchButton extends BungeeTextButton {

		Object treeObject;

		DeleteSearchButton() {
			// super("X", art.font, 0, 0, art.lineH, art.lineH, null, 2.0f,
			// Bungee.summaryFG, Bungee.summaryBG);
			super("X", Bungee.summaryFG, Bungee.summaryBG, QueryViz.this.art(), "Remove this filter");
			setScale(0.6);
			 mouseDoc = "Remove this filter";
		}

		public void doPick() {
			removeSearch(treeObject);
		}

	}

	void setSelectedForEdit(Perspective facet) {
		// Util.print("QueryViz.setSelectedForEdit");
		try {
			Document doc = searchBox.getDocument();
			doc.remove(0, doc.getLength());
			doc.insertString(0, facet.toString(), null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		// searchBox.setVisible(true);
		searchBox.syncWithDocument();
		searchBox.validateFullPaint();
		searchBox.repaint();
	}

}