package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.query.tetrad.Explanation;
import edu.cmu.cs.bungee.client.query.tetrad.NonAlchemyModel;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.graph.Graph.GraphWeigher;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Graph;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * see <a href="C:\Projects\ArtMuseum\DesignSketches\popupColors.xcf">color
 * key</a>
 */
final class PopupSummary extends LazyPNode implements PerspectiveObserver {

	private static final long TRANSLATE_TO_CORNER_DELAY = 10000;

	static final double MARGIN = 6.0;

	private static final double BIG_TEXT_SCALE = 1.4;

	private static final float FADED_TRANSPARENCY = 0.3F;
	private static final float SPACEBAR_TRANSPARENCY = 0.6f;

	private static final double LABEL_ANIMATION_SCALE = 3;

	private final static int TRANSLATE_TO_CORNER_STEP = 400;

	private final static int TRANSLATION_OPACITY_DELAY = TRANSLATE_TO_CORNER_STEP / 3;

	private void actions() {
		if (getRoot() != null) {
			// After an unrestrict, we may be left hanging

			massActions();

			totalCountActions();
			totalDescActions();
			nameActions();
			namePrefixActions();

			facetPercentActions();
			facetCountActions();
			facetDescActions();
			facetNparentFadeActions();
			parentInitActions();
			ratioDescActions();

			sigActions();

			spacebarDescActions();

			totalHeaderActions();
			totalBGActions();
			barBGActions();
			summaryBGActions();

			totalLinesActions();
			ratioLinesActions();
			// parentLinesActions();

			globalActions();
		}
	}

	private static final class Step implements Comparable {
		private static int ordinalCounter = 0;

		private final static List elements = new ArrayList();

		private final int ordinal = ordinalCounter++;

		final String name;

		private Step(String _name) {
			name = _name;
			elements.add(this);
		}

		Step next() {
			if (elements.size() == ordinal + 1)
				return null;
			return (Step) elements.get(ordinal + 1);
		}

		Step previous() {
			if (0 == ordinal)
				return null;
			return (Step) elements.get(ordinal - 1);
		}

		public int compareTo(Object arg0) {
			return ordinal - ((Step) arg0).ordinal;
		}

		public String toString() {
			return name;
		}

		final static Step NO_POPUP = new Step("NO_POPUP");

		final static Step NO_FRAME = new Step("NO_FRAME");

		final static Step START_FRAME = new Step("START_FRAME");

		final static Step NO_HELP = new Step("NO_HELP");

		final static Step GOLD_OVERLAY = new Step("GOLD_OVERLAY");

		final static Step SCALE_FRONT = new Step("SCALE_FRONT");

		// final static Step TRANSLATE_POPUP = new Step("TRANSLATE_POPUP");

		final static Step FADE_IN_TOTAL = new Step("FADE_FOR_TOTAL");

		final static Step HELP_TOTAL = new Step("HELP_TOTAL");

		final static Step FADE_OUT_TOTAL = new Step("FADE_TOTAL");

		final static Step FADE_IN_RATIO = new Step("HELP_RATIO");

		final static Step HELP_RATIO = new Step("HIGHLIGHT_RATIO");

		// final static Step FADE_OUT_RATIO = new Step("FADE_RATIO");

		// final static Step FADE_IN_PARENT = new Step("HELP_PARENT");

		// final static Step HELP_PARENT = new Step("HIGHLIGHT_PARENT");

		// final static Step FADE_OUT_PARENT = new Step("HELP_PARENT");

		final static Step UNFADE = new Step("UNFADE");

		final static Step HELP_SIG = new Step("HELP_SIG");
	}

	private int animationSpeed;

	private Step showHelp = Step.NO_POPUP;

	private final Bungee art;

	private Perspective facet;

	private Cluster cluster;

	private Perspective conditionalMedian;

	private Perspective unconditionalMedian;

	private final TextNfacets namePrefix;

	private final TextNfacets name;

	/**
	 * This is the origin for the popup layout
	 */
	private final APText totalCountDescNum;

	private final TextNfacets totalDesc;

	private final APText facetPercent;

	private final APText facetCount;

	private final TextNfacets facetDesc;

	private final APText siblingPercent;

	private final APText parentDesc;

	private final APText ratioNumber;

	private final APText ratioDesc;

	private final APText significanceDesc;

	private final APText significanceTypeDesc;

	private final APText spacebarDesc;

	private final LazyPPath totalLines1;

	private final LazyPPath ratioLines;

	private final LazyPPath totalLines2;

	// private final LazyPPath sigLines;
	//
	// private final LazyPPath heavyLines;

	private final LazyPNode totalHeader = new LazyPNode();

	private final LazyPNode totalBG = new LazyPNode();

	private final LazyPNode significanceHeader = new LazyPNode();

	private final LazyPNode barBG = new LazyPNode();

	private final LazyPNode summaryBG = new LazyPNode();

	private PNode anchor;

	private PNode tempRatioLabel;

	Graph graph;

	private Explanation explanation;

	private static final float TRANSPARENT = 0;

	private static final float OPAQUE = 1;

	// private long scaleBarsDuration() {
	// return animationSpeed * 1000;
	// }
	//
	// private long translateToBarDuration() {
	// return animationSpeed * 1000;
	// }

	private long fadeDuration() {
		return animationSpeed * 1000;
	}

	private long scaleDuration() {
		return animationSpeed * 500;
	}

	/**
	 * Non-text colors
	 */

	/**
	 * Gold background that appears as a border in the no-help case, and a PNode
	 * text paint in help mode.
	 */
	private static final Color BGcolor = Bungee.goldBorderColor;

	private static final Color no_helpHeaderBGColor = Color.BLACK;

	private static final Color no_helpBGcolor = Color.getHSBColor(0f, 0f, 0.1f);

	private static final Color significanceLineColor = Color.getHSBColor(0.9f,
			1f, 1f);

	private static final Color significanceBGcolor = significanceLineColor
			.darker().darker().darker().darker();
	private static final Color significanceHeaderBGColor = significanceBGcolor
			.darker();
	// Color.getHSBColor(0.9f, 0.07f, 0.2f);

	/**
	 * Text colors
	 */

	private static final Color countTextColor = Color.white;

	private static final Color unimportantTextColor = countTextColor.darker(); // .
	// darker
	// (
	// )
	// ;

	private static final Color totalLinesColor = unimportantTextColor;

	/**
	 * Color of the message "Press the spacebar for more information"
	 */
	private static final Color spaceBarDescTextColor = Bungee.helpColor; // unimportantTextColor
	// ;

	private static final Color facetPercentColor = Color.getHSBColor(0.15f,
			0.4f, 0.9f);

	// private static final Color parentPercentTextColor = facetPercentColor
	// .darker();

	private static final Color significanceDescTextColor = unimportantTextColor;

	private APText newAPText() {
		APText result = new APText(art.font);
		// initialize so globalBoundsCheck will succeed.
		result.setText("<uninitialized>");
		return result;
	}

	PopupSummary(Bungee _art) {
		art = _art;

		totalLines1 = new LazyPPath();
		totalLines1.setStrokePaint(totalLinesColor);

		ratioLines = new LazyPPath();
		ratioLines.setStrokePaint(facetPercentColor);

		totalLines2 = new LazyPPath();
		totalLines2.setStrokePaint(totalLinesColor);

		// sigLines = new LazyPPath();
		// sigLines.setStrokePaint(significanceLineColor);
		//
		// heavyLines = new LazyPPath();
		// heavyLines.setStrokePaint(significanceLineColor);

		name = new TextNfacets(art, Color.darkGray, false);
		name.setScale(BIG_TEXT_SCALE);
		name.setWrapText(true);
		name.setWrapOnWordBoundaries(true);
		name.setRedrawer(this);

		namePrefix = new TextNfacets(art, unimportantTextColor.darker(), false);
		namePrefix.setWrapText(true);
		namePrefix.setWrapOnWordBoundaries(true);
		namePrefix.setTrim(-1, 0);
		namePrefix.setRedrawer(this);

		totalCountDescNum = newAPText();
		totalCountDescNum.setTextPaint(countTextColor);

		totalDesc = new TextNfacets(art, unimportantTextColor, false);
		totalDesc.setWrapText(true);
		totalDesc.setWrapOnWordBoundaries(true);
		totalDesc.setTrim(-1, 0);
		totalDesc.setRedrawer(this);

		facetPercent = newAPText();
		facetPercent.setTextPaint(facetPercentColor);

		facetCount = newAPText();
		facetCount.setTextPaint(countTextColor);

		facetDesc = new TextNfacets(art, unimportantTextColor, false);
		facetDesc.setWrapText(true);
		facetDesc.setWrapOnWordBoundaries(true);
		facetDesc.setTrim(-1, 0);
		facetDesc.setRedrawer(this);

		siblingPercent = newAPText();
		siblingPercent.setTextPaint(facetPercentColor);

		parentDesc = newAPText();
		parentDesc.setTextPaint(unimportantTextColor);
		parentDesc.setConstrainWidthToTextWidth(false);

		ratioNumber = newAPText();
		ratioNumber.setTextPaint(facetPercentColor);

		ratioDesc = newAPText();
		ratioDesc.setTextPaint(unimportantTextColor);
		ratioDesc.setConstrainWidthToTextWidth(false);

		significanceDesc = newAPText();
		significanceDesc.setTextPaint(significanceDescTextColor);
		significanceDesc.setConstrainWidthToTextWidth(false);
		significanceDesc.setPaint(significanceBGcolor);

		significanceTypeDesc = newAPText();
		significanceTypeDesc.setScale(BIG_TEXT_SCALE);
		significanceTypeDesc.setConstrainWidthToTextWidth(false);

		spacebarDesc = newAPText();
		spacebarDesc.setTextPaint(spaceBarDescTextColor);
		// spacebarDesc.setScale(0.75);

		totalHeader.setPaint(no_helpHeaderBGColor);
		significanceHeader.setPaint(significanceHeaderBGColor);
		barBG.setPaint(BGcolor);
		summaryBG.setPaint(BGcolor);
		summaryBG.setPickable(false);
		totalBG.setPaint(no_helpBGcolor);

		addPermanentChildren();

		setPickable(false);
		setChildrenPickable(false);
		setVisible(false);
	}

	private void addPermanentChildren() {
		addChild(barBG);
		addChild(totalBG); // (transparency varies)
		addChild(totalHeader);
		addChild(name); // E.g. "20th Century"
		addChild(namePrefix);
		addChild(totalCountDescNum); // E.g. "1,001" or "55 / 99 = 66% (p =
		// 0.03)"
		addChild(totalDesc);

		addChild(facetPercent); // E.g. "25%"
		addChild(facetCount); // E.g. "499"
		addChild(facetDesc); // " of them satisfy all 1
		// filters, compared with"
		addChild(siblingPercent); // E.g. "25%"
		addChild(parentDesc); // E.g. "13% for all Dates" or
		// "5% of all works"
		addChild(ratioNumber); // E.g. "1 / 23"
		addChild(ratioDesc); // E.g. "times as likely to satisfy filters, as
		// shown by the bar height"

		addChild(significanceHeader);
		addChild(significanceDesc); // E.g. ""
		addChild(significanceTypeDesc); // E.g. ""
		// addChild(sigLines);
		// addChild(heavyLines);

		addChild(spacebarDesc);
		addChild(totalLines1);
		addChild(ratioLines);
		addChild(totalLines2);
	}

	private void fade(PNode[] nodes, float transparency) {
		fade(nodes, 0, 1000 * animationSpeed, transparency);
	}

	private void fade(PNode[] nodes, long delay, long duration,
			float transparency) {
		for (int i = 0; i < nodes.length; i++) {

			if (transparency != nodes[i].getTransparency()) {
				if (transparency > 0 && nodes[i].getTransparency() == 0) {
					// Gross hack to tell fitToNodes that this node should be
					// considered
					nodes[i].setTransparency(Float.MIN_VALUE);
				}

				addAnimationJob(edu.cmu.cs.bungee.piccoloUtils.gui.Util
						.animateToTransparency(nodes[i], transparency, delay,
								duration - delay));
			}
		}
	}

	private void setWidth(PNode[] nodes, double w) {
		for (int i = 0; i < nodes.length; i++) {
			nodes[i].setWidth((int) (w / nodes[i].getScale()));
		}
	}

	private void setFont(PNode[] nodes) {
		for (int i = 0; i < nodes.length; i++) {
			((APText) nodes[i]).setFont(art.font);
		}
	}

	private void visibility(PNode[] nodes, boolean isVisible) {
		fade(nodes, 0, 0, isVisible ? OPAQUE : TRANSPARENT);
	}

	private void fitToNodes(PNode[] nodes, PNode[] virtualChildren,
			double margin) {
		for (int i = 0; i < nodes.length; i++) {
			setBoundsFromNodes(nodes[i], virtualChildren, margin);
		}
	}

	private void layout(TextNfacets node, int xMargin, int yMargin) {
		node.setTrim(xMargin, yMargin);
		node.layout(node.getWidth(), 9999);
	}

	private void delay(long duration) {
		if (duration != 0)
			addAnimationJob(new PActivity(duration));
	}

	private void waitForUserInput() {
		delay(-1);
	}

	private static PNode[] nodes(PNode node) {
		PNode[] result = { node };
		return result;
	}

	private static PNode[] nodes(PNode node1, PNode node2) {
		PNode[] result = { node1, node2 };
		return result;
	}

	private static PNode[] nodes(PNode node1, PNode node2, PNode node3) {
		PNode[] result = { node1, node2, node3 };
		return result;
	}

	private static PNode[] nodes(PNode node1, PNode node2, PNode node3,
			PNode node4) {
		PNode[] result = { node1, node2, node3, node4 };
		return result;
	}

	private static PNode[] nodes(PNode node1, PNode node2, PNode node3,
			PNode node4, PNode node5) {
		PNode[] result = { node1, node2, node3, node4, node5 };
		return result;
	}

	// Compartmentalize these that are 'obvious'
	// Many popup PNodes have empty text or contents when they shouldn't be
	// visible,
	// instead of making them transparent.
	private void massActions() {
		if (showHelp == Step.NO_FRAME) {
			setFont(nodes(totalCountDescNum, facetPercent, facetCount,
					ratioDesc));
			setFont(nodes(siblingPercent, parentDesc, significanceDesc,
					significanceTypeDesc, spacebarDesc));

			// Turn most everything off initially
			visibility(nodes(namePrefix, significanceTypeDesc,
					significanceDesc, significanceHeader, ratioDesc), false);
			visibility(nodes(// sigLines, heavyLines,
					spacebarDesc, totalLines1), false);
			visibility(nodes(ratioLines, totalLines2, summaryBG, ratioNumber),
					false);

			boolean isVis = !isAnchorable();
			visibility(
					nodes(barBG, totalHeader, totalCountDescNum, facetCount),
					isVis);
			visibility(nodes(facetPercent, facetDesc, siblingPercent,
					parentDesc), isVis);

			// Turn 2 things on
			visibility(nodes(name, totalDesc), true);

			// These widths won't change during popup
			setWidth(nodes(name), maxW());

		} else if (showHelp == Step.START_FRAME) {

			setWidth(nodes(name, namePrefix, parentDesc, ratioDesc), maxW());

		} else if (showHelp == Step.HELP_SIG) {

			setWidth(nodes(significanceTypeDesc, significanceDesc), maxW());

		}
	}

	private void globalActions() {
		if (showHelp == Step.NO_FRAME) {
			if (isAnchorable()) {
				align(this, 0, anchor, 0);
				delay(TRANSLATE_TO_CORNER_DELAY);
			}
		} else if (showHelp == Step.START_FRAME) {
			setTetrad();
			summary().moveToFront();
			// setBoundsFromNode(this, barBG, 0);
			animateToAlignment(this, 2, art.header, 2, -barBG.getX()
					- barBG.getMaxX(), -barBG.getY(),
					isAnchorable() ? TRANSLATE_TO_CORNER_STEP : 0);

		} else if (showHelp == Step.NO_HELP) {
			waitForUserInput();
		} else if (showHelp == Step.SCALE_FRONT) {
			rank().connect();
			delay(Bungee.rankAnimationMS);
			// double newFrontH = summary().selectedFrontH()
			// + summary().selectedLabelH();
			// double foldH = summary().selectedFoldH();
			// double marginH = summary().marginH();
			// summary().computeRankComponentHeights(
			// new DesiredSize(foldH, foldH, 1),
			// new DesiredSize(newFrontH, newFrontH, 1),
			// new DesiredSize(0, 0, 1),
			// new DesiredSize(marginH, marginH, 1));
			// summary().flagConnectedRank(rank());
			// summary().layoutChildrenWhenNeeded(scaleBarsDuration());
			// addAnimationJob(summary().rankAnimator);
			// } else if (showHelp == Step.TRANSLATE_POPUP) {
			//
			// // animateToAlignment(this, ...) depends on anchor layout
			// // Should only be a problem when animationSpeed == 0, but
			// // shouldn't count on timings to work out.
			// pv().layoutChildren();
			//
			// animateToAlignment(this, 16, anchor, 2, 10 * MARGIN, art.lineH /
			// 2,
			// translateToBarDuration());
			// } else if (showHelp == Step.FADE_IN_TOTAL) {
			// animateBarTransparencies(FADED_TRANSPARENCY, 0, fadeDuration());
		} else if (showHelp == Step.FADE_IN_RATIO) {
			fade(nodes(anchor), OPAQUE);
		} else if (showHelp == Step.UNFADE) {
			// animateBarTransparencies(OPAQUE, 0, fadeDuration());
		} else if (showHelp == Step.HELP_SIG) {
			waitForUserInput();
		}
	}

	boolean updateTetrad() {
		boolean result = graph != null;
		if (result)
			setTetrad();
		return result;
	}

	boolean setTetrad() {
		boolean result = facet != null && facet.query().isRestricted();
		if (result) {
			if (graph != null) {
				// If we generate a new Tetrad, the facetsOfInterest might be
				// different, and we don't want the graph to change on redraws
				removeChild(graph);
				// relabel(tetradGraph);
			} else {
				// tetradGraph = Tetrad.getTetradGraph(facet, this, this);
				// tetradGraph = Alchemy.getAlchemyGraph(facet, this, this,
				// art.dbName);
				explanation = NonAlchemyModel.getExplanation(facet);
				// drawTetradGraph(tetradGraph, "tetrad");
			}
			// if (explanation == null) {
			// return false;
			// }
			graph = getGraph();
			addChild(graph);
			graph.setWidth(barBG.getWidth());
			align(graph, 2, barBG, 18);
			graph.translate(-graph.getX(), -graph.getY() - MARGIN);
		}
		return result;
		// return false;
	}

	// private void relabel(
	// edu.cmu.cs.bungee.javaExtensions.graph.Graph tetradGraph2) {
	// for (Iterator it = tetradGraph2.getNodes().iterator(); it.hasNext();) {
	// Node node = (Node) it.next();
	// Perspective p = (Perspective) node.object;
	// node.setLabel(p.getName(this));
	// }
	// }

	private Graph getGraph() {
		edu.cmu.cs.bungee.javaExtensions.graph.Graph tetradGraph;
		if (explanation == null) {
			tetradGraph = new edu.cmu.cs.bungee.javaExtensions.graph.Graph(
					(GraphWeigher) null);
		} else {
			tetradGraph = explanation.buildGraph(this, facet);
		}
		Graph graph1 = new Graph(tetradGraph, art.font);
		graph1.setStrokePaint(BGcolor);
		graph1
				.setLabel(explanation == null ? "You must select and/or hover over at least two tags to get a graph"
						: graph1.getNumEdges() > 0 ? "Influence Diagram"
								: "No dependencies");
		colorFacetGraph(graph1);
		return graph1;
	}

	// public void drawTetradGraph(
	// edu.cmu.cs.bungee.javaExtensions.graph.Graph graph1, String status) {
	// Graph pnode = new Graph(graph1, art.font);
	// pnode.printMe(status);
	// }

	void colorFacetGraph(Graph graph1) {
		for (Iterator it = graph1.getNodeObjects().iterator(); it.hasNext();) {
			Object object = it.next();
			if (object instanceof Perspective) {
				APText node = graph1.getNode(object);
				node.setTextPaint(art.facetTextColor((Perspective) object));
			}
		}
	}

	private void totalCountActions() {
		if (showHelp == Step.START_FRAME) {
			totalCountDescNum.setText(totalCountString());
			align(totalCountDescNum, 16, this, 0);

			// Have to interleave facet- and total- alignment
			facetPercent.setText(percentOn(null).toString());
			align(facetPercent, 0, totalCountDescNum, 0);
			align(totalCountDescNum, 0, facetPercent, 2, MARGIN, 0);

			fade(nodes(totalCountDescNum), TRANSLATION_OPACITY_DELAY,
					TRANSLATE_TO_CORNER_STEP, OPAQUE);
		} else if (showHelp == Step.FADE_OUT_TOTAL) {
			fade(nodes(totalCountDescNum), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(totalCountDescNum), OPAQUE);
		}
	}

	private void totalDescActions() {
		if (showHelp == Step.NO_FRAME) {
			// need to set text first, or global bounds will be empty
			setWidth(nodes(totalDesc), maxW());
			totalDesc.setContent(noFrameDesc());
			layout(totalDesc, 0, 0);
			align(totalDesc, 16, this, 0);
		} else if (showHelp == Step.START_FRAME && totalCount() >= 0) {
			// totalCount = -1 for deeply nested facets when isDataRestricted

			// Our width is smaller than the others
			setWidth(nodes(totalDesc), maxW() - totalCountDescNum.getMaxX());

			totalDesc.setContent(totalCountDesc(false));
			layout(totalDesc, -1, 0);

			// We use spaces in the text for separation rather than margins
			align(totalDesc, 0, totalCountDescNum, 2);
		} else if (showHelp == Step.HELP_TOTAL) {
			totalDesc.setContent(totalCountDesc(true));
			layout(totalDesc, -1, 0);
			delay(scaleDuration() * 4);
		} else if (showHelp == Step.FADE_OUT_TOTAL) {
			fade(nodes(totalDesc), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(totalDesc), OPAQUE);
		}
	}

	private void nameActions() {
		if (showHelp == Step.NO_FRAME) {
			name.setContent(nameContents());
			layout(name, 0, 0);
			align(name, 16, totalDesc, 0, 0, -MARGIN);
			visibility(nodes(name), !isFacetType());
		} else if (showHelp == Step.START_FRAME) {
			fade(nodes(name), TRANSLATION_OPACITY_DELAY,
					TRANSLATE_TO_CORNER_STEP, OPAQUE);
			layout(name, -1, 0);
		} else if (showHelp == Step.FADE_IN_TOTAL) {
			fade(nodes(name), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(name), OPAQUE);
		}
	}

	private void namePrefixActions() {
		if (showHelp == Step.START_FRAME) {
			namePrefix.setContent(namePrefixContents());
			layout(namePrefix, -1, 0);
			align(namePrefix, 16, name, 0);
			fade(nodes(namePrefix), TRANSLATION_OPACITY_DELAY,
					TRANSLATE_TO_CORNER_STEP, OPAQUE);
		} else if (showHelp == Step.FADE_IN_TOTAL) {
			fade(nodes(namePrefix), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(namePrefix), OPAQUE);
		}

	}

	private void spacebarDescActions() {
		float transparency = canHelp() ? SPACEBAR_TRANSPARENCY : TRANSPARENT;
		if (showHelp == Step.START_FRAME) {

			spacebarDesc.setText("Press the spacebar for more explanation");
			align(spacebarDesc, 2, isShowFacetInfo() ? (PNode) parentDesc
					: totalDesc, 18, 0, MARGIN);
			fade(nodes(spacebarDesc), TRANSLATION_OPACITY_DELAY,
					TRANSLATE_TO_CORNER_STEP, transparency);
		} else if (showHelp == Step.GOLD_OVERLAY) {
			spacebarDesc.setText("Press the spacebar to skip this animation");
		} else if (showHelp == Step.FADE_IN_TOTAL) {
			fade(nodes(spacebarDesc), canHelp() ? FADED_TRANSPARENCY
					: TRANSPARENT);
		} else if (showHelp == Step.FADE_IN_RATIO) {
			align(spacebarDesc, 2, isShowFacetInfo() ? (PNode) ratioDesc
					: totalDesc, 18, 0, MARGIN);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(spacebarDesc), transparency);
		} else if (showHelp == Step.HELP_SIG) {
			spacebarDesc
					.setText("Press the spacebar again for a slower animation");
			align(spacebarDesc, 2, significanceDesc, 18, 0, MARGIN);

			// Have to interleave here
			setBoundsFromNodes(significanceHeader, nodes(spacebarDesc,
					significanceTypeDesc), MARGIN);
		}
	}

	private void totalBGActions() {
		if (showHelp == Step.NO_FRAME) {
			totalBG.setTransparency(unconditionalMedian != null ? 1 : 0.5f);
			fitToNodes(nodes(totalBG), nodes(name, totalDesc), 0);
		} else if (showHelp == Step.START_FRAME) {
			totalBG.setTransparency(isAnchorable() ? TRANSPARENT : OPAQUE);
			fade(nodes(totalBG), TRANSLATION_OPACITY_DELAY,
					TRANSLATE_TO_CORNER_STEP, OPAQUE);
			fitToNodes(nodes(totalBG), nodes(totalHeader, totalDesc,
					parentDesc, spacebarDesc, facetDesc), 0);
		} else if (showHelp == Step.FADE_IN_RATIO) {
			fitToNodes(nodes(totalBG), nodes(totalHeader, totalDesc,
					parentDesc, spacebarDesc, facetDesc), 0);
		}
	}

	private void barBGActions() {
		if (showHelp == Step.START_FRAME) {
			fade(nodes(barBG), TRANSLATION_OPACITY_DELAY,
					TRANSLATE_TO_CORNER_STEP, OPAQUE);

			fitToNodes(nodes(barBG), nodes(totalBG), MARGIN);
		} else if (showHelp == Step.FADE_IN_TOTAL) {
			fade(nodes(barBG), TRANSPARENT);
		} else if (showHelp == Step.HELP_SIG) {
			fitToNodes(nodes(barBG), nodes(totalBG, spacebarDesc), MARGIN);

		}

	}

	private void summaryBGActions() {
		if (showHelp == Step.GOLD_OVERLAY) {
			assert facet != null;
			assert isShowFacetInfo() : facet + " " + query().isRestricted()
					+ " " + query().isQueryValid() + " " + facet.getOnCount();

			if (summaryBG.getParent() == null) {
				// art.summary == null when we're initted, so check here
				art.summary.addChild(summaryBG);
			}

			summaryBG.moveToFront();
			rank().moveToFront();
			moveToFront();

			fitToNodes(nodes(summaryBG), nodes(art.header, art.mouseDoc),
					MARGIN);

			fade(nodes(summaryBG), 0.9f);
		}
	}

	private void totalHeaderActions() {
		if (showHelp == Step.START_FRAME) {
			fade(nodes(totalHeader), OPAQUE);
			fitToNodes(nodes(totalHeader), nodes(namePrefix, name), MARGIN);
		}
	}

	private void facetNparentFadeActions() {
		if (showHelp == Step.START_FRAME) {
			fade(nodes(facetPercent, facetCount, facetDesc, siblingPercent,
					parentDesc), TRANSLATION_OPACITY_DELAY,
					TRANSLATE_TO_CORNER_STEP, OPAQUE);
		} else if (showHelp == Step.FADE_IN_TOTAL) {
			fade(nodes(facetPercent, facetCount, facetDesc, siblingPercent,
					parentDesc), FADED_TRANSPARENCY);
		} else if (showHelp == Step.FADE_IN_RATIO) {
			fade(nodes(siblingPercent, facetPercent /*
													 * , facetCountDescNum,
													 * facetCountDescText
													 */), OPAQUE);
			// } else if (showHelp == Step.FADE_OUT_RATIO) {
			// fade(nodes(/* facetCountDescPercent, */ facetCountDescNum,
			// facetCountDescText), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(facetPercent, facetCount, facetDesc, siblingPercent,
					parentDesc), OPAQUE);
		}
	}

	private void facetPercentActions() {
		// if (showHelp == Step.START_FRAME)
		align(facetPercent, 4, totalDesc, 20, 0, MARGIN);
	}

	private void facetCountActions() {
		// if (showHelp == Step.START_FRAME) {
		facetCount.setText(onCountString());

		// right align
		align(facetCount, 34, totalCountDescNum, 34);

		// top align
		align(facetCount, 4, facetPercent, 4);
		// }
	}

	private void facetDescActions() {
		// if (showHelp == Step.START_FRAME) {
		// Our width is smaller than the others
		setWidth(nodes(facetDesc), maxW() - facetCount.getMaxX());

		facetDesc.setContent(facetCountDesc());
		layout(facetDesc, -1, 0);

		// We use spaces in the text for separation rather than margins
		align(facetDesc, 0, facetCount, 2);
		// }
	}

	private void parentInitActions() {
		if (showHelp == Step.START_FRAME) {
			// Text never changes, plus we're using attributes to have the
			// percentage in a different color
			setParentDesc("");
		}

		// left align
		align(siblingPercent, 32, facetPercent, 32);

		// align below
		align(siblingPercent, 4, facetDesc, 20, 0, MARGIN);

		align(parentDesc, 0, siblingPercent, 2, MARGIN, 0);
		double width = maxW() - siblingPercent.getMaxX() - MARGIN;
		assert width >= 0 : art.getW() + " " + art.summary.w + " " + maxW()
				+ " " + siblingPercent.getMaxX() + " " + MARGIN + " " + facet;
		setWidth(nodes(parentDesc), width);
	}

	private void ratioDescActions() {
		if (showHelp == Step.FADE_IN_TOTAL) {
			fade(nodes(ratioNumber, ratioDesc), TRANSPARENT);
		} else if (showHelp == Step.FADE_IN_RATIO) {
			setRatioDesc();
			ratioDesc.setWidth(maxW() - ratioNumber.getWidth() - MARGIN);

			// left align
			align(ratioNumber, 32, siblingPercent, 32);

			// align below
			align(ratioNumber, 4, parentDesc, 20, 0, MARGIN);

			align(ratioDesc, 0, ratioNumber, 2, MARGIN, 0);

			fade(nodes(ratioNumber, ratioDesc), 0, 2 * fadeDuration(), OPAQUE);

			setParentDesc("\u00F7 ");
			align(parentDesc, 0, siblingPercent, 2, MARGIN, 0);
		}

	}

	private void sigActions() {
		if (showHelp == Step.HELP_SIG) {
			visibility(nodes(significanceDesc, significanceTypeDesc,
					significanceHeader), true);
			// fade(nodes(sigLines), 0, 2 * fadeDuration(), 0.3f);
			// fade(nodes(heavyLines), 0, 2 * fadeDuration(), OPAQUE);
			// sigLines.setStroke(LazyPPath.getStrokeInstance(2));
			// heavyLines.setStroke(LazyPPath.getStrokeInstance(3));

			significanceTypeDesc.setTextPaint(art.facetTextColor(facet));
			significanceTypeDesc.setText(sigSummary());
			significanceTypeDesc.setWidth((int) (maxW() / significanceTypeDesc
					.getScale()));
			align(significanceTypeDesc, 0, totalBG, 16, MARGIN, 0);

			significanceDesc.setWidth(maxW());
			significanceDesc.setText(sigExplanation());
			align(significanceDesc, 0, significanceTypeDesc, 16, 0, 0);

			setBoundsFromNodes(significanceHeader, nodes(significanceDesc,
					significanceTypeDesc), MARGIN);

			// summary().colorKey.moveToFront();

			// Rectangle2D barBounds = ((Bar) anchor).visibleBounds();
			// double top = barBounds.getY();
			// double bottom = barBounds.getMaxY();
			// boolean isUpward = top < 0.5;
			// PBounds sigBounds = new PBounds(barBounds.getCenterX() - MARGIN,
			// top, 2 * MARGIN, bottom - top);
			// Rectangle2D globalDeltaBounds = anchor.getParent().localToGlobal(
			// sigBounds);
			// heavyLines.setBounds(heavyLines.getParent().globalToLocal(
			// globalDeltaBounds));
			// iBeam(heavyLines);
			// stretchLine(sigLines, heavyLines, 9, significanceDesc, 8,
			// 4 * MARGIN);
			// int point = isUpward ? 17 : 1;
			// scaleAboutPoint(heavyLines, point, 0.1, 0);
			// scaleAboutPoint(heavyLines, point, 1, 2 * fadeDuration());

			// summary().expandColorKey();
			// summary().colorKey.moveToFront();
		}
	}

	private void totalLinesActions() {
		if (showHelp == Step.FADE_IN_TOTAL) {
			totalLines1.setTransparency(TRANSPARENT);
			fade(nodes(totalLines1), OPAQUE);
			totalLines1.setStroke(LazyPPath.getStrokeInstance(1));
			totalLines2.setTransparency(TRANSPARENT);
			fade(nodes(totalLines2), OPAQUE);
			totalLines2.setStroke(LazyPPath.getStrokeInstance(1));

			Rectangle2D localBarBounds = anchor.getBounds();
			// Rectangle2D localBarBounds = ((Bar) anchor).visibleBounds();
			// localBarBounds.setRect(0, localBarBounds.y, localBarBounds.x
			// + localBarBounds.width, localBarBounds.height);
			Rectangle2D barBounds = anchor.localToGlobal(localBarBounds);
			stretchLine(totalLines1, barBounds, 1, totalCountDescNum
					.getGlobalBounds(), 8, 0);
			stretchLine(totalLines2, barBounds, 0, barBounds, 2, 0);

		} else if (showHelp == Step.HELP_TOTAL) {
			// totalLines1.setStroke(LazyPPath.getStrokeInstance(3));
			totalLines2.setStroke(LazyPPath.getStrokeInstance(3));

			// PNode label = pv().percentLabels[2];
			// scaleAboutPoint(label, 9, LABEL_ANIMATION_SCALE,
			// scaleDuration());
			// scaleAboutPoint(totalCountDescNum, 9, LABEL_ANIMATION_SCALE,
			// scaleDuration());
			// delay(scaleDuration() * 2);
		} else if (showHelp == Step.FADE_OUT_TOTAL) {
			fade(nodes(totalLines1), 0.5f);
			fade(nodes(totalLines2), 0.5f);

			// PNode label = pv().percentLabels[2];
			// scaleAboutPoint(label, 9, PerspectiveViz.PERCENT_LABEL_SCALE,
			// scaleDuration());
			// scaleAboutPoint(totalCountDescNum, 9, 1, scaleDuration());
			// } else if (showHelp == Step.FADE_IN_RATIO) {
			// totalLines.setStroke(LazyPPath.getStrokeInstance(1));
		}
	}

	private void ratioLinesActions() {
		if (showHelp == Step.FADE_IN_RATIO) {
			ratioLines.setTransparency(TRANSPARENT);
			fade(nodes(ratioLines), OPAQUE);
			ratioLines.setStroke(LazyPPath.getStrokeInstance(1));

			double ratio = facet.percentageRatio();
			tempRatioLabel = pv()
					.tempRatioLabel(Rank.constrainOddsRatio(ratio));
			tempRatioLabel.moveToFront();

			// Rectangle2D localBarBounds = ((Bar) anchor).visibleBounds();
			// Rectangle2D barBounds = anchor.localToGlobal(localBarBounds);
			// int barPoint = ratio > 1 ? 0 : 16;
			PNode label = pv().percentLabels[1];
			stretchLine(ratioLines, ratioNumber, 8, label, 10, -8 * MARGIN);

		} else if (showHelp == Step.HELP_RATIO) {
			ratioLines.setStroke(LazyPPath.getStrokeInstance(3));

			scaleAboutPoint(tempRatioLabel, 9, LABEL_ANIMATION_SCALE,
					scaleDuration());

			delay(scaleDuration() * 5);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(ratioLines), 0.5f);

			scaleAboutPoint(tempRatioLabel, 9, 1, scaleDuration());

			// } else if (showHelp == Step.FADE_IN_PARENT) {
			// facetLines.setStroke(LazyPPath.getStrokeInstance(1));
		}
	}

	// private void parentLinesActions() {
	// if (showHelp == Step.FADE_IN_PARENT) {
	// totalLines2.setTransparency(TRANSPARENT);
	// fade(nodes(totalLines2), OPAQUE);
	// totalLines2.setStroke(LazyPPath.getStrokeInstance(1));
	//
	// PNode label = pv().percentLabels[1];
	// stretchLine(totalLines2, parentPercentDesc, 8, label, 10, -4
	// * MARGIN);
	// } else if (showHelp == Step.HELP_PARENT) {
	// totalLines2.setStroke(LazyPPath.getStrokeInstance(3));
	//
	// PNode label = pv().percentLabels[1];
	// label.moveToFront();
	// scaleAboutPoint(label, 9, LABEL_ANIMATION_SCALE, scaleDuration());
	// delay(scaleDuration() * 2);
	// } else if (showHelp == Step.FADE_OUT_PARENT) {
	// fade(nodes(totalLines2), 0.5f);
	//
	// PNode label = pv().percentLabels[1];
	// scaleAboutPoint(label, 9, PerspectiveViz.PERCENT_LABEL_SCALE,
	// scaleDuration());
	// } else if (showHelp == Step.UNFADE) {
	// // parentLines.setStroke(LazyPPath.getStrokeInstance(1));
	// }
	// }

	void performNextStep() {
		if (!animationFinished() && !finishingAnimation && art.isReady) {
			finishAnimation();
			showHelp = showHelp.next();
			actions();
			long delay = maxFinishTime() - System.currentTimeMillis();
			// Util.print(showHelp + " " + delay);
			schedulePerformStep(delay);
		}
	}

	/*
	 * Anything that might refer to a Perspective should use this as the
	 * redrawer
	 * 
	 * @see edu.cmu.cs.bungee.client.query.PerspectiveObserver#redraw()
	 */
	public void redraw() {
		Step currentStep = showHelp;
		int currentAnimationSpeed = animationSpeed;
		animationSpeed = 0;
		for (showHelp = Step.NO_FRAME; showHelp != null
				&& showHelp.compareTo(currentStep) <= 0; showHelp = showHelp
				.next()) {
			actions();
		}
		animationSpeed = currentAnimationSpeed;
		showHelp = currentStep;
	}

	private String onCountString() {
		if (isShowFacetInfo()) {
			int onCount = facet != null ? facet.getOnCount() : cluster
					.getOnCount();
			return Util.addCommas(onCount);
		} else {
			return " ";
		}
	}

	private String totalCountString() {
		if (totalCount() >= 0)
			return Util.addCommas(totalCount());
		else
			return " ";
	}

	private void pValueString(StringBuffer buf) {
		double pValue = facet != null ? facet.pValue() : cluster.pValue();
		// Util.print(facet + " " + cluster + " " + pValue);
		if (pValue >= 0 && art.getShowPvalues()) {
			// If cluster is created by replayOps, pValue = -1
			buf.append(" (");
			MouseDocLine.formatPvalue(pValue, buf);
			buf.append(")");
		}
	}

	private static void ensureArea(PNode node) {
		if (node.getWidth() <= 0)
			node.setWidth(1);
		if (node.getHeight() <= 0)
			node.setHeight(1);
	}

	// translate attachment to align with base. Margins are added to base.
	private void animateToAlignment(PNode attachment, int attachmentPoint,
			PNode base, int basePoint, double baseXoffset, double baseYoffset,
			long duration) {
		assert isPoint(attachmentPoint);
		assert isPoint(basePoint);
		PBounds baseBounds = ensureGlobalBounds(base);
		PBounds attachmentBounds = ensureGlobalBounds(attachment);
		double attachmentX0 = (int) attachmentBounds.getX();
		double attachmentY0 = (int) attachmentBounds.getY();

		if (isChangeX(attachmentPoint)) {
			double baseX = pointX(baseBounds, basePoint) + baseXoffset;
			double attachmentX = pointX(attachmentBounds, attachmentPoint);
			double xOffset = attachmentX - attachmentX0;
			assert xOffset >= 0 : attachmentPoint + " " + attachmentX0 + " "
					+ attachmentX + " " + attachment.getWidth();
			attachmentX0 = baseX - xOffset;
		} else {
			assert baseXoffset == 0 : "You probably didn't mean to use an x offset but not change x";
		}

		if (isChangeY(attachmentPoint)) {
			double baseY = pointY(baseBounds, basePoint) + baseYoffset;
			double attachmentY = pointY(attachmentBounds, attachmentPoint);
			double yOffset = attachmentY - attachmentY0;
			assert yOffset >= 0 : attachmentY + " " + yOffset;
			attachmentY0 = baseY - yOffset;
			// Util.print(baseY + " " + attachmentY + " " + yOffset);
		} else {
			assert baseYoffset == 0 : "You probably didn't mean to use an y offset but not change y";
		}
		Point2D goal = attachment.getParent().globalToLocal(
				new Point2D.Double(attachmentX0, attachmentY0));
		PActivity a = attachment.animateToPositionScaleRotation(goal.getX(),
				goal.getY(), attachment.getScale(), 0, duration);
		addAnimationJob(a);
	}

	private static PBounds ensureGlobalBounds(PNode node) {
		if (node.getRoot() == null)
			edu.cmu.cs.bungee.piccoloUtils.gui.Util.printAncestors(node);
		assert node.getRoot() != null;
		// bounds will be empty if width or height is zero, and we don't want
		// that to prevent using the offset for alignment.
		ensureArea(node);
		PBounds result = node.getGlobalBounds();
		assert !result.isEmpty() : node;
		return result;
	}

	private void align(PNode attachment, int attachmentPoint, PNode base,
			int basePoint) {
		align(attachment, attachmentPoint, base, basePoint, 0, 0);
	}

	private void align(PNode attachment, int attachmentPoint, PNode base,
			int basePoint, double baseXoffset, double baseYoffset) {
		animateToAlignment(attachment, attachmentPoint, base, basePoint,
				baseXoffset, baseYoffset, 0);
	}

	private static void stretchLine(LazyPPath line, PNode base1,
			int base1Point, PNode base2, int base2Point,
			double base1VerticalOffset) {
		Rectangle2D base1Bounds = ensureGlobalBounds(base1);
		Rectangle2D base2Bounds = ensureGlobalBounds(base2);
		stretchLine(line, base1Bounds, base1Point, base2Bounds, base2Point,
				base1VerticalOffset);
	}

	/**
	 * @param line
	 *            PPath to update: draw horzontal-vertical-horizontal line
	 *            segments between the bases
	 * @param base1Bounds
	 *            define start
	 * @param base1Point
	 *            define start
	 * @param base2Bounds
	 *            define end
	 * @param base2Point
	 *            define end
	 * @param base1VerticalOffset
	 *            distance to the vertical line segment
	 */
	private static void stretchLine(LazyPPath line, Rectangle2D base1Bounds,
			int base1Point, Rectangle2D base2Bounds, int base2Point,
			double base1VerticalOffset) {
		// Util.print(base1Bounds);

		double x1 = pointX(base1Bounds, base1Point);
		double y1 = pointY(base1Bounds, base1Point);
		double x2 = pointX(base2Bounds, base2Point);
		double y2 = pointY(base2Bounds, base2Point);

		PNode parent = line.getParent();
		Point2D point1 = parent.globalToLocal(new Point2D.Double(x1, y1));
		Point2D point2 = parent.globalToLocal(new Point2D.Double(x1
				+ base1VerticalOffset, y1));
		Point2D point3 = parent.globalToLocal(new Point2D.Double(x1
				+ base1VerticalOffset, y2));
		Point2D point4 = parent.globalToLocal(new Point2D.Double(x2, y2));

		line.reset();
		line.moveTo((float) point1.getX(), (float) point1.getY());
		if (y1 != y2) {
			line.lineTo((float) point2.getX(), (float) point2.getY());
			line.lineTo((float) point3.getX(), (float) point3.getY());
		}
		line.lineTo((float) point4.getX(), (float) point4.getY());
	}

	private void scaleAboutPoint(PNode node, int point, double scale,
			long duration) {

		// scale to an absolute factor
		scale = scale / node.getScale();

		PBounds bounds = node.getBounds();
		double x = pointX(bounds, point);
		double y = pointY(bounds, point);
		PAffineTransform xform = node.getTransform();
		xform.translate(x, y);
		xform.scale(scale, scale);
		xform.translate(-x, -y);
		addAnimationJob(node.animateToTransform(xform, duration));
	}

	private static boolean isPoint(int direction) {
		return oppositeDirection(direction) >= 0;
	}

	private static boolean isChangeX(int direction) {
		return (direction & 4) == 0;
	}

	private static boolean isChangeY(int direction) {
		return (direction & 32) == 0;
	}

	// 00 01 02 04
	// 08 09 10 12
	// 16 17 18 20
	// 32 33 34 36
	private static int oppositeDirection(int direction) {
		switch (direction) {
		case 0:
		case 1:
		case 2:
		case 8:
		case 9:
		case 10:
		case 16:
		case 17:
		case 18:
			direction = 18 - direction;
			break;
		case 32:
		case 34:
			direction = 66 - direction;
			break;
		case 4:
		case 20:
			direction = 24 - direction;
			break;
		default:
			assert false : direction;
			direction = -1;
		}
		return direction;
	}

	/**
	 * @param b
	 * @param point
	 *            encodes an x coordinate on a rectangle, a y coordinate, or
	 *            both. the low three bits encode x, and the next three encode
	 *            y. 0 = left/top, 1 = middle, 2 = right/bottom, 4 = n/a
	 * @return the x-coordinate of the point
	 */
	private static double pointX(Rectangle2D b, int point) {
		double result = Double.NaN;
		point = point & 7;
		if (point < 4) {
			result = b.getX();
			if (point == 2)
				result += b.getWidth();
			else if (point == 1)
				result += b.getWidth() / 2;
		}
		return (int) result;
	}

	private static double pointY(Rectangle2D b, int point) {
		// Util.print("pointY " + point + " " + (point >> 3));
		double result = Double.NaN;
		point = (point >> 3) & 7;
		if (point < 4) {
			result = b.getY();
			if (point == 2) {
				result += b.getHeight();
			} else if (point == 1) {
				result += b.getHeight() / 2;
			}
		}
		return (int) result;
	}

	private static void setBoundsFromNodes(PNode node, PNode[] virtualChildren,
			double margin) {
		animateBoundsFromNodes(node, virtualChildren, margin, 0);
	}

	private static void animateBoundsFromNodes(PNode node,
			PNode[] virtualChildren, double margin, long duration) {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < virtualChildren.length; i++) {
			PNode child = virtualChildren[i];
			if (child.getRoot() != null
					&& child.getVisible()
					&& child.getTransparency() > 0
					&& (!(child instanceof TextNfacets) || !((TextNfacets) child)
							.isEmpty())
					&& (!(child instanceof APText) || ((APText) child)
							.getText().trim().length() != 0)) {
				// Util.print(node + "\n" + child + "\n");
				PBounds childBounds = child.getGlobalBounds();
				double x0 = childBounds.getX();
				double x1 = x0 + childBounds.getWidth();
				double y0 = childBounds.getY();
				double y1 = y0 + childBounds.getHeight();
				minX = Math.min(minX, x0);
				minY = Math.min(minY, y0);
				maxX = Math.max(maxX, x1);
				maxY = Math.max(maxY, y1);
			}
		}
		double scale = node.getGlobalScale();
		PBounds newBounds = new PBounds((int) (minX - margin),
				(int) (minY - margin), (maxX - minX + 2 * margin) / scale,
				(maxY - minY + 2 * margin) / scale);
		Rectangle2D local = node.globalToLocal(newBounds);
		node.animateToBounds(local.getX(), local.getY(), local.getWidth(),
				local.getHeight(), duration);
	}

	// private static void iBeam(LazyPPath node) {
	// Rectangle2D bounds = node.getBoundsReference();
	// float midX = (int) node.getBoundsReference().getCenterX();
	// float top = (int) bounds.getY();
	// float bottom = (int) bounds.getMaxY();
	// node.reset();
	// node.moveTo(midX - 5, top);
	// node.lineTo(midX + 5, top);
	// node.moveTo(midX, top);
	// node.lineTo(midX, bottom);
	// node.moveTo(midX - 5, bottom);
	// node.lineTo(midX + 5, bottom);
	// }

	private Markup namePrefixContents() {
		Markup v;
		if (facet != null)
			v = getPrefix(facet.facetDescription());
		else {
			v = Query.emptyMarkup();
		}
		return v;
	}

	private Markup nameContents() {
		Markup v;
		if (facet != null)
			v = getPostfix(facet.facetDescription());
		else {
			v = Query.emptyMarkup();
			v.add(cluster);
		}
		return v;
	}

	private int onCount() {
		if (facet != null)
			return facet.getOnCount();
		else
			return cluster.getOnCount();
	}

	private int totalCount() {
		if (facet != null)
			return facet.getTotalCount();
		else
			return cluster.getTotalCount();
	}

	private StringBuffer percentOn(StringBuffer buf) {
		if (isShowFacetInfo()) {
			buf = Util.formatPercent(facetPercent(), buf);
		} else if (buf == null) {
			buf = new StringBuffer(" ");
		}
		return buf;
	}

	private double facetPercent() {
		return onCount() / (double) totalCount();
	}

	private int parentOnCount() {
		if (facet == null || facet.getParent() == null)
			return query().getOnCount();
		else
			return facet.getParent().getOnCount();
	}

	private int parentTotalCount() {
		if (facet == null || facet.getParent() == null)
			return query().getTotalCount();
		else
			return facet.getParent().getTotalCount();
	}

	private int siblingTotalCount() {
		return parentTotalCount() - totalCount();
	}

	private int siblingOnCount() {
		return parentOnCount() - onCount();
	}

	private double siblingPercentOn() {
		return siblingOnCount() / (double) siblingTotalCount();
	}

	private StringBuffer siblingPercentOn(StringBuffer buf) {
		if (isShowFacetInfo()) {
			buf = Util.formatPercent(siblingPercentOn(), buf);
		} else if (buf == null) {
			buf = new StringBuffer(" ");
		}
		return buf;
	}

	private Markup totalCountDesc(boolean isHelp) {
		Markup medianDesc = medianContent(false);
		medianDesc.add(0, collectionDescription(isHelp));
		// Util.print(TextNfacets.toText(medianDesc));
		return medianDesc;
	}

	private Markup facetCountDesc() {
		if (isShowFacetInfo()) {
			Markup medianDesc = medianContent(true);
			StringBuffer buf = new StringBuffer();
			buf.append(" of them satisfy all ").append(
					query().nFilters(true, true, true, true))
					.append(" filters");
			if (conditionalMedian != null) {
				// pValueString(buf);
			} else {
				if (siblingTotalCount() > 0)
					buf.append(", compared with");
			}
			medianDesc.add(0, buf.toString());
			return medianDesc;
		} else {
			return Query.emptyMarkup();
		}
	}

	private Markup medianContent(boolean isConditional) {
		Markup content = Query.emptyMarkup();
		if (unconditionalMedian != null) {
			content.add(Markup.NEWLINE_TAG);
			int significant = isConditional ? facet.medianTestSignificant() : 0;
			content.add(Bungee.significanceColor(significant, 0));
			content.add("median: ");
			// content.add(Markup.DEFAULT_COLOR_TAG);
			content
					.add(isConditional ? conditionalMedian
							: unconditionalMedian);
			if (isConditional) {
				content.add(medianPvalueString());
			}
		}
		return content;
	}

	private String medianPvalueString() {
		StringBuffer buf = new StringBuffer();
		double pValue = facet.medianTest();
		buf.append(" (");
		MouseDocLine.formatPvalue(pValue, buf);
		buf.append(")");
		return buf.toString();
	}

	private String parentDescString() {
		Markup markup;
		if (facet != null)
			markup = facet.parentDescription();
		else {
			markup = query().parentDescription();
		}
		return query().markupToText(markup, this);
	}

	private void setParentDesc(String prefix) {
		if (isShowFacetInfo() && unconditionalMedian == null) {
			if (siblingTotalCount() > 0) {
				StringBuffer buf = new StringBuffer();
				buf.append(prefix);
				siblingPercent.setText(siblingPercentOn(buf).toString());

				parentDesc
						.setWidth(maxW() - siblingPercent.getWidth() - MARGIN);
				buf = new StringBuffer();
				buf.append("for the other ").append(
						Util.addCommas(siblingTotalCount())).append(" ")
						.append(parentDescString());
				pValueString(buf);
				parentDesc.setText(buf.toString());

				siblingPercent.clearAttributes();
				if (prefix.length() > 0) {
					// hack to show a division result
					siblingPercent.addAttribute(TextAttribute.UNDERLINE,
							TextAttribute.UNDERLINE_ON);
				}
			} else {
				siblingPercent.setText("");
				parentDesc.setText("");
			}
		} else {
			// Use many spaces to avoid out-of-range error for color attribute
			siblingPercent.setText("");
			parentDesc.setText("");
		}
	}

	private void setRatioDesc() {
		double ratio = facet.percentageRatio();
		String ratioLabel = PerspectiveViz.formatOddsRatio(ratio);
		// Util.print(ratio + " " + ratioLabel);
		String desc = "as likely to satisfy the filters, as shown by the bar height. "
				+ "(Height scale is non-linear; move the mouse over the scale to see.)";
		double limit = Math.exp(Rank.LOG_ODDS_RANGE);
		switch (ratioLabel.charAt(0)) {
		case '*':
			ratioLabel = ratioLabel.substring(2);
			if (ratio > limit)
				ratioLabel = "> " + ((int) limit);
			desc = "times " + desc;
			break;
		case '/':
			ratioLabel = "1 " + ratioLabel;
			if (ratio < 1.0 / limit)
				ratioLabel = "< 1 / " + ((int) limit);
			break;
		case '=':
			desc = "about as likely to satisfy the filters";
			break;
		default:
			assert false;
		}
		ratioNumber.setText("  " + ratioLabel);
		ratioDesc.setText(desc);
	}

	private void schedulePerformStep(long delay) {
		if (delay > 0) {

			PActivity ta = new PActivity(delay) {

				protected void activityFinished() {
					super.activityFinished();
					performNextStep();
				}
			};
			addActivity(ta);
			addAnimationJob(ta);
		} else {
			performNextStep();
		}
	}

	// private void animateBarTransparencies(final float newTransparency,
	// long delay, long duration) {
	// final Rank rank = rank();
	// if (rank != null) {
	// if (duration == 0 && delay == 0) {
	// rank.setBarTransparencies(newTransparency);
	// // anchor.setTransparency(1);
	// } else {
	// // final PNode _node = anchor;
	// // final Rank _rank = rank;
	// final float oldTransparency = anchor.getTransparency();
	//
	// PActivity ta = new PInterpolatingActivity(duration,
	// PUtil.DEFAULT_ACTIVITY_STEP_RATE, delay
	// + System.currentTimeMillis(), 1,
	// PInterpolatingActivity.SOURCE_TO_DESTINATION) {
	//
	// public void setRelativeTargetValue(float zeroToOne) {
	// rank.setBarTransparencies((float) lerp(zeroToOne,
	// oldTransparency, newTransparency));
	// // _node.setTransparency(1);
	// }
	// };
	// addActivity(ta);
	// addAnimationJob(ta);
	// }
	// }
	// }

	private PerspectiveViz pv() {
		if (anchor == null)
			return null;
		return (PerspectiveViz) anchor.getParent().getParent();
	}

	private Summary summary() {
		return art.summary;
	}

	private Rank rank() {
		PerspectiveViz pv = pv();
		if (pv == null)
			return null;
		return pv.rank;
	}

	private Object medianName(boolean isConditional) {
		Perspective median = isConditional ? conditionalMedian
				: unconditionalMedian;
		if (median != null)
			return median;
		else
			// if no items are selected, conditional median will be null
			return "<undefined>";
	}

	private Markup noFrameDesc() {
		Markup result = Query.emptyMarkup();
		result.add(Color.white);
		if (unconditionalMedian != null) {
			result.add("median ");
			result.add(medianName(false));
			result.add(" \u2192 ");
			result.add(medianName(true));
			// result.add(medianPvalueString());
		} else {
			StringBuffer buf = new StringBuffer();
			if (isShowFacetInfo()) {
				buf.append(onCountString());
				buf.append(" / ").append(totalCountString());
				buf.append(" = ");
				percentOn(buf);
				// pValueString(buf);
			} else {
				buf.append(totalCountString());
			}
			result.add(buf.toString());
		}
		return result;
	}

	private String collectionDescription(boolean isHelp) {
		StringBuffer buf = new StringBuffer();
		if (query().isRestrictedData())
			buf.append(" in restricted set");
		else
			buf.append(" in collection");
		if (isHelp) {
			buf.append(", as shown by the bar's width");
			// buf.append(query().markupToText(facet.facetDescription(), this));
		}
		return buf.toString();
	}

	private int maxW() {
		return (int) (art.getW() - art.summary.w - 33 * MARGIN);
	}

	private Markup getPrefix(Markup facetDescList) {
		Markup result = Query.emptyMarkup();
		for (Iterator it = facetDescList.iterator(); it.hasNext();) {
			Object o = it.next();
			if (o instanceof Perspective) {
				break;
			} else {
				result.add(o);
			}
		}
		return result;
	}

	/**
	 * This loses prefix Markup like COLOR and PLURAL (which is a good thing)
	 * 
	 * @param facetDescList
	 * @return Markup with prefixes removed
	 */
	private Markup getPostfix(Markup facetDescList) {
		Markup result = Query.emptyMarkup();
		boolean startRecording = false;
		for (Iterator it = facetDescList.iterator(); it.hasNext();) {
			Object o = it.next();
			if (o instanceof Perspective) {
				startRecording = true;
			}
			if (startRecording) {
				result.add(o);
			}
		}
		return result;
	}

	// private double getRelativePercentOnDifference() {
	// Rectangle2D barBounds = ((Bar) anchor).visibleBounds();
	// double delta = barBounds.getHeight() - anchor.getHeight() / 2;
	// return delta;
	// }

	private String sigSummary() {
		String relationTypeString = null;
		Color[] colorFamily = art.chiColorFamily(facet);
		if (colorFamily == Markup.UNASSOCIATED_COLORS)
			relationTypeString = "...is not correlated with filters:";
		else if (colorFamily == Markup.NEGATIVE_ASSOCIATION_COLORS)
			relationTypeString = "...is inversely correlated with filters:";
		else if (colorFamily == Markup.POSITIVE_ASSOCIATION_COLORS)
			relationTypeString = "...is correlated with filters:";
		else
			assert false;
		return relationTypeString;
	}

	// private static double PRACTICAL_SIGNIFICANCE_THRESDHOLD = 0.17;

	private String sigExplanation() {
		// String desc = query().markupToText(facet.facetDescription(), this);
		// double delta = getRelativePercentOnDifference();
		StringBuffer sigStringBuf = new StringBuffer();
		Object chiColorFamily = art.chiColorFamily(facet);
		if (chiColorFamily == Markup.UNASSOCIATED_COLORS) {
			sigStringBuf
					.append("The difference is percentage is not statistically significant, "
							+ "so this tag is colored gray");
		} else {
			sigStringBuf
					.append(
							"The difference is percentage is statistically significant, so this tag is colored ")
					.append(
							chiColorFamily == Markup.NEGATIVE_ASSOCIATION_COLORS ? "brown"
									: "green");
		}
		sigStringBuf.append(" (see Color Key).");
		pValueString(sigStringBuf);
		return sigStringBuf.toString();
	}

	private boolean animationFinished() {
		return showHelp.next() == null;
	}

	private List animationJobs = new Vector();

	private void addAnimationJob(PActivity job) {
		if (job != null) {
			// will be null if duration == 0
			if (!animationFinished()) {
				// Util.print("adding job " + showHelp + " " + job);
				animationJobs.add(job);
			}
		}
	}

	private long maxFinishTime() {
		long result = 0;
		for (Iterator it = animationJobs.iterator(); it.hasNext();) {
			PActivity job = (PActivity) it.next();
			long finish = job.getStopTime();
			result = Math.max(result, finish);
		}
		return result;
	}

	/**
	 * Ignore recursive calls (via terminate)
	 */
	private boolean finishingAnimation = false;

	private void finishAnimation() {
		if (!finishingAnimation) {
			finishingAnimation = true;
			try {
				// Util.print("finishing anim");
				for (Iterator it = animationJobs.iterator(); it.hasNext();) {
					PActivity job = (PActivity) it.next();
					it.remove(); // remove first to avoid infinite
					// recursion
					// Util.print("finishing " + job.isStepping() + " " +
					// job);
					// if (job.isStepping()) {
					job.terminate(PActivity.TERMINATE_AND_FINISH);
					// }
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				finishingAnimation = false;
			}
		}
	}

	private boolean isHelping() {
		return showHelp.compareTo(Step.NO_HELP) > 0;
	}

	private boolean isShowFacetInfo() {
		return query().isRestricted() && query().isQueryValid()
		// Deeply nested facets will have onCount < 0
				&& (facet == null || facet.getOnCount() >= 0);
	}

	private boolean isFacetType() {
		return facet != null && facet.getParent() == null
				&& unconditionalMedian == null;
	}

	private boolean canHelp() {
		return facet != null && isShowFacetInfo() && !isFacetType()
				&& unconditionalMedian == null
				&& facet.getParent().percentOn() < 1;
	}

	private boolean isAnchorable() {
		// If bars are redrawn, anchor might have been dropped
		return anchor != null && anchor.getRoot() != null;
	}

	// Summary.showPopup is always called before this, so we know everthing is
	// reset
	void setFacet(Perspective _facet, boolean _showMedian, PNode _anchor) {
		assert _facet != null;
		// assert _anchor != null;
		anchor = _anchor;
		facet = _facet;
		if (_showMedian) {
			conditionalMedian = facet.getMedianPerspective(true);
			unconditionalMedian = facet.getMedianPerspective(false);
		}
		performNextStep();
		setVisible(true);
	}

	void setCluster(Cluster _cluster) {
		// Util.print("setCluster " + _cluster);
		assert _cluster != null;
		cluster = _cluster;
		performNextStep();
		setVisible(true);
	}

	boolean showMoreHelp() {
		if (showHelp == Step.NO_FRAME) {
			performNextStep();
		} else if (showHelp == Step.NO_HELP) {
			if (canHelp()) {
				animationSpeed = 1;
				performNextStep();
			}
		} else if (showHelp == Step.HELP_SIG) {
			showHelp = Step.FADE_IN_TOTAL.previous(); // TRANSLATE_POPUP;
			animationSpeed = 2;
			visibility(nodes(// sigLines, heavyLines,
					totalLines1), false);
			visibility(nodes(ratioLines, totalLines2), false);
			visibility(nodes(significanceDesc, significanceTypeDesc,
					significanceHeader), false);
			performNextStep();
		} else {
			animationSpeed = 0;
			performNextStep();
		}
		return false;
	}

	void exit() {
		if (showHelp != Step.NO_POPUP) {
			boolean isHelp = isHelping();
			showHelp = Step.NO_POPUP;
			// performance++;
			animationSpeed = 0;
			finishAnimation();
			summaryBG.removeFromParent();
			if (graph != null) {
				graph.removeFromParent();
				graph = null;
			}
			if (isHelp) {
				art.summary.computeRankComponentHeights(0);
				// animateBarTransparencies(1, 0, 0);
				totalCountDescNum.setScale(1);
				facetCount.setScale(1);
				// summary().doHideTransients();
				if (pv() != null)
					pv().loseHotZone();
				siblingPercent.clearAttributes();
			}
			anchor = null;
			facet = null;
			cluster = null;
			conditionalMedian = null;
			unconditionalMedian = null;
			setVisible(false);
		}
	}

	Query query() {
		return art.query;
	}

	// public void setVisible(boolean state) {
	// Util.print("Popup.setVisible " + state + " " +
	// facetDesc.getTransparency());
	// edu.cmu.cs.bungee.piccoloUtils.gui.Util.printDescendents(this,20,false);
	// super.setVisible(state);
	// }
}
