package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;

import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Query;

public class SearchText extends FacetText implements PickFacetTextNotifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	SearchText(String s, Bungee art) {
		super(s, art, -1, -1, false, false, false, 1, null);
		pickFacetTextNotifier = this;
		addInputEventListener(facetTextHandler);
	}

	@Override
	void setObject(Object treeObject, Object oldTreeObject) {
		if (!treeObject.equals(treeObject())) {
			decache(oldTreeObject);
			assert treeObject instanceof String;
			addInputEventListener(facetTextHandler);
		}
	}

	public boolean highlight(FacetText node, boolean state, int modifiers) {
		// Util.print("SearchText.hightlight " + this);
		Color color = state ? Color.white : Bungee.headerFG;
		setPermanentTextPaint(color);
		Markup mouseDoc = null;
		if (state) {
			mouseDoc = Query.emptyMarkup();
			mouseDoc.add("Remove text search on '" + this + "'");
		}
		art.setClickDesc(mouseDoc);
		return false;
	}

	public boolean pick(FacetText node, int modifiers) {

		boolean result = false;
		String searchText = (String) treeObject();
		art().printUserAction(Bungee.BUTTON, searchText, 0);
		result = art.query.removeTextSearch(searchText);
		if (result)
			art().updateAllData();
		return result;
	}

	@Override
	public String toString() {
		return (String) treeObject();
	}

}
