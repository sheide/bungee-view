package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;

class SummaryText extends TextNfacets implements PickFacetTextNotifier {

	// private APText ellipsis;

	SummaryText(Bungee art) {
		super(art, Bungee.headerFG, false);
		setWrapText(false);
		setWrapOnWordBoundaries(false);
		facetPermanentTextPaint = Bungee.headerFG;
		// setPaint(Bungee.summaryBG);
		addInputEventListener(new SummaryTextHover());
		// ellipsis = new EllipsisButton();
		// ellipsis.setVisible(false);
		// addChild(ellipsis);
	}

	void validate(double w) {
		// Util.print("SUmmaryText.validate " + w + " "+h);
		setBounds(0, 0, w, art.lineH);
		layoutBestFit();
	}

//	double layout(double w, double h) {
//		// Util.print("SUmmaryText.layout " + w + " "+h);
//		return super.layout(w, h);
//	}

	void setDescription() {
		Markup description = art.query.descriptionVerbPhrase();
		// if (getText() == null
		// || !getText().equals(description)) {
		// Util.print("Summary.setDescription");
		setContent(hackSearches(description.uncolor()));
		layoutBestFit();
		// setTextPaint(q.isRestricted() ? summaryTextColor : FG);
		// Util.print("SummaryText.setDescription " + getGlobalFullBounds());
	}

	private Markup hackSearches(Markup markup) {
		query();
		Markup result = Query.emptyMarkup();
		for (Iterator it = markup.iterator(); it.hasNext();) {
			Object object = it.next();
			result.add(object);
			if ("whose description mentions '".equals(object)) {
				String search = (String) it.next();
				SearchText st = new SearchText(search, art);
				result.add(st);
			}
		}
		return result;
	}

	// boolean toggleSummary() {
	// return getHeight() > art.lineH ? contractSummary()
	// : expandSummary();
	// }

	boolean expandSummary() {
		art.printUserAction(Bungee.BUTTON, "Ellipsis", 0);
		if (getHeight() <= art.lineH && isIncomplete()) {
			// Util.print("expandSummary");
			setWrapText(true);
			setWrapOnWordBoundaries(true);
			setTrim(-1, (int) (art.lineH / 2));
			layout(getWidth(), 99999 - getYOffset());
			moveToFront();
			// ellipsis.setState(false);
			// ellipsis.mouseDoc = "Truncate the summary";
			return true;
		}
		return false;
	}

	boolean contractSummary() {
		if (getHeight() > art.lineH) {
			// Util.print("contractSummary");
			setTrim(-1, -1);
			setWrapText(false);
			setWrapOnWordBoundaries(false);
			setHeight(art.lineH);
			layout();
			// ellipsis.setState(true);
			// ellipsis.mouseDoc = "Show the rest of the summary";
			// setEllipsisVisibility();
			return true;
		}
		return false;
	}

	/*
	 * Just do highlighting, in white
	 * 
	 * @see
	 * edu.cmu.cs.bungee.client.viz.TextNfacets#updateSelections(java.util.Set)
	 */
	void updateSelections(Set facets) {
		// Util.print("SummaryText.updateSelections " +
		// facets+" "+art.highlightedFacets);
		for (int i = 0; i < getChildrenCount(); i++) {
			if (getChild(i) instanceof FacetText) {
				FacetText child = (FacetText) getChild(i);
				Perspective childFacet = child.getFacet();
				if (childFacet != null && facets.contains(childFacet)) {
					// child.highlightFacet();
					Color color = art.highlightedFacets.contains(childFacet) ? Color.white
							: Bungee.headerFG;
					// Util.print("... " + childFacet+" "+color);
					child.setPermanentTextPaint(color);
				}
			}
		}
	}

	final class SummaryTextHover extends MyInputEventHandler {

		// private Summary summary;

		SummaryTextHover() {
			super(TextNfacets.class);
			// summary = _summary;
		}

		// LazyPNode getSource(LazyPNode node) {
		// if (node == null)
		// return null;
		// LazyPNode parent = node.getParent();
		// if (parent instanceof Summary)
		// // Could be ellipsis, summaryText, or one of its children.
		// return node;
		// else
		// return getSource(parent);
		// }

		// protected boolean click(PNode node) {
		// return expandSummary();
		// // ((Summary) parent).setMouseDoc(node, false);
		// }

		public boolean enter(PNode node) {
			// Util.print("SummaryTextHover.enter " + node);
			// setSummaryTextDoc(true);
			return expandSummary();
		}

		public boolean exit(PNode node, PInputEvent e) {
			Point2D point = e.getPositionRelativeTo(SummaryText.this);
			return !getBounds().contains(point) && contractSummary();
			// setSummaryTextDoc(false);
			// maybeHideTransients(e);
		}

		// protected void mayHideTransients(PNode node) {
		// Summary.this.mayHideTransients();
		// }
	}

	// final class EllipsisButton extends SummaryButton {
	//
	// private static final String label1 = "...";
	//
	// EllipsisButton() {
	// // super("...", art.font, 0, 0, art.getStringWidth("...") + 2,
	// // art.lineH + 2, null, 1.5f, color, Bungee.summaryBG);
	// super(label1);
	// mouseDoc = "Show the rest of the summary";
	// // ((PText) child).setTextPaint(color);
	// }
	//
	// public void doPick() {
	// toggleSummary();
	// }
	//
	// public void exit() {
	// Summary summary = (Summary) getParent();
	// summary.contractSummary();
	// }
	//
	// public void mayHideTransients(PNode node) {
	// // ((Summary) getParent()).mayHideTransients();
	// }
	//
	// }
	//
	// void clickEllipsis() {
	// ellipsis.doPick();
	// }
	//
	// void maybeHideTransients(PInputEvent e) {
	// // Util.print(summaryText);
	// Point2D p = e.getPositionRelativeTo(summaryText);
	// if (p.getX() < 0 || p.getY() < 0 || p.getX() >= summaryText.getWidth()
	// || p.getY() >= summaryText.getHeight() - 10)
	// contractSummary();
	// }
	//
	// void setSummaryTextDoc(boolean state) {
	// ellipsis.setMouseDoc(state);
	// // setMouseDoc(summaryText, state);
	// }

	/*
	 * Make facets highlight AND expandSummary.
	 * 
	 * @see edu.cmu.cs.bungee.client.viz.TextNfacets#trim()
	 */
	protected void trim() {
//		Util.print("SummaryText.trim "+content);
		for (Iterator it = getChildrenIterator(); it.hasNext();) {
//			Object o = it.next();
//			if (o instanceof FacetText) {
				FacetText text = (FacetText) it.next();
				if (text.treeObject() instanceof ItemPredicate) {
					text.pickFacetTextNotifier = this;
//				}
			}
		}
		super.trim();
	}

	public boolean highlight(FacetText node, boolean state, int modifiers) {
//		Util.print("SummaryText.highlight");
		if (state)
			expandSummary();
		else
			contractSummary();
		if (node.facet != null) {
			art.highlightFacet(state ? node.facet : null, modifiers);
			art.setClickDesc(state ? node.facet.facetDoc(modifiers) : null);
		} else if (node.cluster != null)
			art.highlightCluster(state ? node.cluster : null);
		else
			return false;
		return true;
	}

	public boolean pick(FacetText node, int modifiers) {
		if (node.facet != null && node.facet.getParent() != null) {
			node.printUserAction(modifiers);
			art.toggleFacet(node.facet, modifiers);
		} else if (node.cluster != null) {
			art.toggleCluster(node.cluster);
		} else
			return false;
		return true;
	}
}
