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

import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyContainer;
import edu.cmu.cs.bungee.piccoloUtils.gui.MouseDoc;

final class QueryViz extends LazyContainer implements MouseDoc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Summary summary;

	// /**
	// * A p-list of DeleteButtons and FacetTexts for both text searches and
	// * clusters
	// */
	// private final List searches = new Vector();

	// private APText shortSearchWarning;

	QueryViz(Summary _summary) {
		summary = _summary;
		setPickable(false);
	}

	// private boolean isInitted = false;

	private Boundary boundary;

	// private APText clusterLabel;
	//
	// private APText itemListLabel;

	/**
	 * This gets called after validate is first called
	 */
	void init() {
		// Util.print("QueryViz.init");
		// shortSearchWarning = oneLineLabel();
		// shortSearchWarning.setPaint(Bungee.helpColor);
		// shortSearchWarning.setPickable(false);

		// clusterLabel = oneLineLabel();
		// clusterLabel.setTextPaint(Bungee.summaryFG);
		// clusterLabel.setPickable(false);
		// clusterLabel.setVisible(false);
		// clusterLabel.setText("Clusters");
		// addChild(clusterLabel);

		// itemListLabel = oneLineLabel();
		// itemListLabel.setTextPaint(Bungee.summaryFG);
		// itemListLabel.setPickable(false);
		// itemListLabel.setVisible(false);
		// itemListLabel.setText("Informedia Queries");
		// addChild(itemListLabel);

		boundary = new Boundary(this, false);
		boundary.margin = -Rank.LABEL_RIGHT_MARGIN / 2;
		addChild(boundary);
		boundary.validate();

		// isInitted = true;
		// positionLabels();
	}

	double getBottomMargin() {
		// int nSearches = query().nSearches();
		// // 1 is for label, and 0.5 is for a margin below
		// double margin = nSearches == 0 ? 0 : lineH() * (0.5 + nSearches);
		// int nClusters = query().nClusters();
		// if (nClusters > 0) {
		// margin += lineH() * (1 + nClusters);
		// }
		// int nItemLists = query().nItemLists();
		// if (nItemLists > 0) {
		// margin += lineH() * (1 + nItemLists);
		// }
		// return margin;

		return 0;
	}

	void validate(double _w, double _h) {
		// Util.print("qv.validate " + _w);
		w = _w;
		h = _h;
		assert _w == Math.round(_w);
		if (boundary != null) {
			boundary.validate();
		}
		// synchronizeWithQuery();
	}

	private Bungee art() {
		return summary.art;
	}

	// private Query query() {
	// return summary.q;
	// }

	private double lineH() {
		return art().lineH + 1.0;
	}

	// private Font font() {
	// return art().font;
	// }
	//
	// private APText oneLineLabel() {
	// return art().oneLineLabel();
	// }

	// void positionLabels() {
	// // h = _h;
	// double y = h - getBottomMargin();
	// if (isInitted) {
	// // Util.print("positionSearchBox " + y);
	// double x = Rank.LABEL_LEFT_MARGIN;
	// y += lineH() * (1 + query().nSearches());
	//
	// if (query().nClusters() > 0) {
	// x = 2;
	// clusterLabel.setOffset(x, y);
	// clusterLabel.setFont(font());
	// y += lineH() * (1 + query().nClusters());
	// }
	//
	// if (query().nItemLists() > 0) {
	// x = 2;
	// itemListLabel.setOffset(x, y);
	// itemListLabel.setFont(font());
	// }
	//
	// // art().header.textSearch.positionLabels();
	// }
	// }

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
		return summary.w - summary.minTagWallWidth();
	}

	// List spareDeleteButtons = new LinkedList();
	//
	// void synchronizeWithQuery() {
	// if (isInitted) {
	// // Util.print("synchronizeWithQuery " );
	// positionLabels();
	// ListIterator nodeIt = searches.listIterator();
	// synchronizeTextSearches(nodeIt);
	// synchronizeClusters(nodeIt);
	// synchronizeItemLists(nodeIt);
	// }
	// }
	//
	// void synchronizeTextSearches(ListIterator nodeIt) {
	// List desiredSearches = new LinkedList(query().getSearches());
	// double y = h - getBottomMargin() /* + lineH() */;
	// // Out with the old Strings
	// while (nodeIt.hasNext()) {
	// DeleteSearchButton button = (DeleteSearchButton) nodeIt.next();
	// if (button.treeObject instanceof Cluster)
	// break;
	// else
	// y = updateButton(desiredSearches, nodeIt, button, y);
	// }
	// // nodeIt either points to the first cluster FacetText or nothing
	// if (nodeIt.hasNext())
	// nodeIt.previous();
	// // In with the new Strings
	// for (Iterator textIt = desiredSearches.iterator(); textIt.hasNext();) {
	// String desiredSearch = (String) textIt.next();
	// y = addButton(nodeIt, desiredSearch, y);
	// }
	// }
	//
	// void synchronizeClusters(ListIterator nodeIt) {
	// int nClusters = query().nClusters();
	// clusterLabel.setVisible(nClusters > 0);
	// double y = clusterLabel.getYOffset() + lineH();
	// List desiredClusters = new LinkedList(query().clusters());
	// // Out with the old Clusters
	// while (nodeIt.hasNext()) {
	// DeleteSearchButton button = (DeleteSearchButton) nodeIt.next();
	// if (button.treeObject instanceof ItemList)
	// break;
	// else
	// y = updateButton(desiredClusters, nodeIt, button, y);
	// }
	// // nodeIt either points to the first ItemList FacetText or nothing
	// if (nodeIt.hasNext())
	// nodeIt.previous();
	// // In with the new Clusters
	// for (Iterator it = desiredClusters.iterator(); it.hasNext();) {
	// Cluster cluster = (Cluster) it.next();
	// y = addButton(nodeIt, cluster, y);
	// }
	// }
	//
	// void synchronizeItemLists(ListIterator nodeIt) {
	// int nItemLists = query().nItemLists();
	// itemListLabel.setVisible(nItemLists > 0);
	// double y = itemListLabel.getYOffset() + lineH();
	// // }
	// List desiredItemLists = new LinkedList(query().itemLists());
	// // Out with the old ItemLists
	// while (nodeIt.hasNext()) {
	// DeleteSearchButton button = (DeleteSearchButton) nodeIt.next();
	// assert button.treeObject instanceof ItemList;
	// y = updateButton(desiredItemLists, nodeIt, button, y);
	// }
	// assert !nodeIt.hasNext();
	// // In with the new ItemLists
	// for (Iterator it = desiredItemLists.iterator(); it.hasNext();) {
	// ItemList itemList = (ItemList) it.next();
	// y = addButton(nodeIt, itemList, y);
	// }
	// }
	//
	// double updateButton(Collection desired, ListIterator nodeIt,
	// DeleteSearchButton button, double y) {
	// if (!desired.contains(button.treeObject)) {
	// removeChild(button);
	// spareDeleteButtons.add(button);
	// nodeIt.remove();
	// FacetText text = (FacetText) nodeIt.next();
	// removeChild(text);
	// nodeIt.remove();
	// } else {
	// desired.remove(button.treeObject);
	// button.setOffset(5, y + 2);
	// button.setFont(font());
	// FacetText text = (FacetText) nodeIt.next();
	// text.setOffset(Math.ceil(button.getMaxX() + 5), y);
	// text.setFont(font());
	// y += lineH();
	// }
	// return y;
	// }
	//
	// double addButton(ListIterator nodeIt, Object treeObject, double y) {
	// // Util.print("addButton " + s);
	// DeleteSearchButton delete = (DeleteSearchButton) (spareDeleteButtons
	// .size() > 0 ? spareDeleteButtons.remove(0)
	// : new DeleteSearchButton());
	// addChild(delete);
	// delete.setFont(font());
	// nodeIt.add(delete);
	// delete.setOffset(5, y + 2);
	// delete.treeObject = treeObject;
	//
	// FacetText searchText = FacetText.getFacetText(treeObject, art(), false,
	// null);
	// addChild(searchText);
	// nodeIt.add(searchText);
	// searchText.setOffset(Math.ceil(delete.getMaxX() + 5), y);
	// return y + lineH();
	// }
	//
	// boolean removeSearch(Object treeObject) {
	// boolean result = false;
	// summary.mayHideTransients();
	// if (treeObject instanceof String) {
	// String searchText = (String) treeObject;
	// art().printUserAction(Bungee.BUTTON, searchText, 0);
	// result = query().removeTextSearch(searchText);
	// } else if (treeObject instanceof Cluster) {
	// query().toggleCluster((Cluster) treeObject);
	// result = true;
	// } else if (treeObject instanceof ItemList) {
	// query().toggleItemList((ItemList) treeObject);
	// result = true;
	// } else {
	// assert false : treeObject;
	// }
	// if (result)
	// art().updateAllData();
	// return result;
	// }
	//
	// void highlightCluster(Set clusters) {
	// // Util.print("highlightCluster " + cluster.toString());
	// if (clusterLabel.getVisible()) {
	// for (Iterator nodeIt = searches.iterator(); nodeIt.hasNext();) {
	// nodeIt.next();
	// FacetText label = (FacetText) nodeIt.next();
	// if (clusters.contains(label.cluster)) {
	// label.setTextPaint(art().clusterTextColor(label.cluster));
	// }
	// }
	// }
	// }

	// public void setMouseDoc(PNode source, boolean state) {
	// art().setMouseDoc(source, state);
	// }

	public void setMouseDoc(String doc) {
		art().setMouseDoc(doc);
	}

	// final class DeleteSearchButton extends BungeeTextButton {
	//
	// Object treeObject;
	//
	// DeleteSearchButton() {
	// // super("X", art.font, 0, 0, art.lineH, art.lineH, null, 2.0f,
	// // Bungee.summaryFG, Bungee.summaryBG);
	// super("X", Bungee.summaryFG, Bungee.summaryBG, QueryViz.this.art(),
	// "Remove this filter");
	// setScale(0.6);
	// mouseDoc = "Remove this filter";
	// }
	//
	// public void doPick() {
	// removeSearch(treeObject);
	// }
	//
	// }

}