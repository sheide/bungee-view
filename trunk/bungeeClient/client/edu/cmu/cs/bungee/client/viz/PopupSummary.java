package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.PerspectiveObserver;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.viz.Summary.DesiredSize;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.activities.PTransformActivity;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PUtil;


final class PopupSummary extends LazyPNode implements PerspectiveObserver {

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
			if (elements.size() == ordinal)
				return null;
			return (Step) elements.get(ordinal + 1);
		}

		Step previous() {
			if (elements.size() == ordinal)
				return null;
			return (Step) elements.get(ordinal + 1);
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

		final static Step FADE_OTHER_BARS = new Step("FADE_OTHER_BARS");

		final static Step TRANSLATE_POPUP = new Step("TRANSLATE_POPUP");

		final static Step FADE_FOR_TOTAL = new Step("FADE_FOR_TOTAL");

		final static Step HELP_TOTAL = new Step("HELP_TOTAL");

		final static Step FADE_TOTAL = new Step("FADE_TOTAL");

		final static Step HELP_FACET = new Step("HELP_FACET");

		final static Step HIGHLIGHT_FACET = new Step("HIGHLIGHT_FACET");

		final static Step FADE_FACET = new Step("FADE_FACET");

		final static Step HELP_PARENT = new Step("HELP_PARENT");

		final static Step HIGHLIGHT_PARENT = new Step("HIGHLIGHT_PARENT");

		final static Step HELP_SIG = new Step("HELP_SIG");

		final static Step UNFADE = new Step("UNFADE");
	}

	// private static class Action {
	// final Step step;
	//
	// final PNode[] nodes;
	//
	// final long duration;
	//
	// static final boolean globalBoundsCheck(PNode[] nodes) {
	// for (int i = 0; i < nodes.length; i++) {
	// ensureGlobalBounds(nodes[i]);
	// }
	// return true;
	// }
	//
	// // static Action getInstance(PNode _node, Step _step, long _duration) {
	// // PNode[] nodes = { _node };
	// // Action result = new Action(nodes, _step, _duration);
	// // return result;
	// // }
	// //
	// // static Action getInstance(PNode[] _nodes, Step _step, long _duration)
	// // {
	// // Action result = new Action(_nodes, _step, _duration);
	// // return result;
	// // }
	//
	// private Action(PNode[] _nodes, Step _step, long _duration) {
	// step = _step;
	// nodes = _nodes;
	// duration = _duration;
	// assert step != null;
	// // assert nodes != null;
	// // assert duration >= 0;
	// }
	//
	// PActivity[] perform() {
	// PActivity[] result = { new PActivity(duration) };
	// return result;
	// }
	//
	// public String toString() {
	// return getClass() + " " + step + " " + duration + " "
	// + PrintArray.printArrayString(nodes);
	// }
	// }
	//
	// private static class FadeAction extends Action {
	// final float transparency;
	//
	// private FadeAction(PNode[] _nodes, Step _step, long _duration,
	// float _transparency) {
	// super(_nodes, _step, _duration);
	// transparency = _transparency;
	// assert transparency >= 0 && transparency <= 1;
	// }
	//
	// PActivity[] perform() {
	// PActivity[] result = new PActivity[nodes.length];
	// for (int i = 0; i < nodes.length; i++) {
	// if (transparency > 0 && nodes[i].getTransparency() == 0) {
	// // Gross hack to tell fitToNodes that this node should be
	// // considered
	// nodes[i].setTransparency(Float.MIN_VALUE);
	// }
	// result[i] = nodes[i].animateToTransparency(transparency,
	// duration);
	// }
	// return result;
	// }
	// }
	//
	// private static class SetWidthAction extends Action {
	// final double width;
	//
	// private SetWidthAction(PNode[] _nodes, Step _step, double w) {
	// super(_nodes, _step, 0);
	// width = w;
	// assert w >= 0 : w;
	// }
	//
	// PActivity[] perform() {
	// for (int i = 0; i < nodes.length; i++) {
	// nodes[i].setWidth((int) (width / nodes[i].getScale()));
	// }
	// return new PActivity[0];
	// }
	// }
	//
	// private static class SetTrimAction extends Action {
	// final int trimX;
	//
	// final int trimY;
	//
	// private SetTrimAction(PNode[] _nodes, Step _step, int x, int y) {
	// super(_nodes, _step, 0);
	// trimX = x;
	// trimY = y;
	// }
	//
	// PActivity[] perform() {
	// for (int i = 0; i < nodes.length; i++) {
	// TextNfacets node = (TextNfacets) nodes[i];
	// node.setTrim(trimX, trimY);
	// node.layout(node.getWidth(), 9999);
	// }
	// return new PActivity[0];
	// }
	// }
	//
	// private static class SetTextAction extends Action {
	// private final String text;
	//
	// private SetTextAction(PNode[] _nodes, Step _step, String s) {
	// super(_nodes, _step, 0);
	// text = s;
	// assert s != null;
	// }
	//
	// PActivity[] perform() {
	// for (int i = 0; i < nodes.length; i++) {
	// ((APText) nodes[i]).setText(text);
	// }
	// return new PActivity[0];
	// }
	// }
	//
	// private static class SetContentAction extends Action {
	// private final Markup content;
	//
	// private SetContentAction(PNode[] _nodes, Step _step, Markup _content) {
	// super(_nodes, _step, 0);
	// // content = Collections.unmodifiableList(new Vector(_content));
	// content = _content.copy();
	// }
	//
	// PActivity[] perform() {
	// for (int i = 0; i < nodes.length; i++) {
	// ((TextNfacets) nodes[i]).setContent(content);
	// }
	// return new PActivity[0];
	// }
	// }
	//
	// private static class FitToNodesAction extends Action {
	// final double margin;
	//
	// private final PNode[] virtualChildren;
	//
	// private FitToNodesAction(PNode[] _nodes, PNode[] _virtualChildren,
	// Step _step, double _margin) {
	// super(_nodes, _step, 0);
	// margin = _margin;
	// virtualChildren = _virtualChildren;
	// assert globalBoundsCheck(_nodes);
	// assert globalBoundsCheck(_virtualChildren);
	// }
	//
	// PActivity[] perform() {
	// for (int i = 0; i < nodes.length; i++) {
	// setBoundsFromNodes(nodes[i], virtualChildren, margin);
	// }
	// return new PActivity[0];
	// }
	// }
	//
	// private static class AlignAction extends Action {
	// final int point;
	//
	// final PNode base;
	//
	// final int basePoint;
	//
	// final double baseXoffset;
	//
	// final double baseYoffset;
	//
	// AlignAction(PNode[] _nodes, int _point, PNode _base, int _basePoint,
	// double _baseXoffset, double _baseYoffset, Step _step) {
	// super(_nodes, _step, 0);
	// point = _point;
	// base = _base;
	// basePoint = _basePoint;
	// baseXoffset = _baseXoffset;
	// baseYoffset = _baseYoffset;
	// assert isPoint(_point);
	// assert isPoint(_basePoint);
	// assert globalBoundsCheck(_nodes);
	// }
	//
	// PActivity[] perform() {
	// for (int i = 0; i < nodes.length; i++) {
	// align(base, basePoint, nodes[i], point, baseXoffset,
	// baseYoffset);
	// }
	// return new PActivity[0];
	// }
	// }
	//
	// private class ScaleAction extends Action {
	// final double scale;
	//
	// final int point;
	//
	// ScaleAction(Step _step, PNode[] _nodes, long _duration, double _scale,
	// int _point) {
	// super(_step, _nodes, _duration);
	// scale = _scale;
	// point = _point;
	// assert _scale > 0;
	// assert isPoint(point);
	// }
	//
	// void perform() {
	// for (int i = 0; i < nodes.length; i++) {
	// PNode node = nodes[i];
	// double newScale = scale / node.getScale();
	// PBounds bounds = node.getBounds();
	// double x = pointX(bounds, point);
	// double y = pointY(bounds, point);
	// PAffineTransform xform = node.getTransform();
	// xform.translate(x, y);
	// xform.scale(newScale, newScale);
	// xform.translate(-x, -y);
	// addAnimationJob(node.animateToTransform(xform, duration));
	// }
	// }
	// }

	private int animationSpeed;

	private Step showHelp = Step.NO_POPUP;

	private final Bungee art;

	private Perspective facet;

	private Cluster cluster;

	private Perspective conditionalMedian;

	private Perspective unconditionalMedian;

	private int performance = 1;

	private Rank rank;

	private final TextNfacets namePrefix;

	private final TextNfacets name;

	/**
	 * This is the origin for the popup layout
	 */
	private final APText totalCountDescNum;

	private final TextNfacets totalCountDescText;

	private final APText facetCountDescPercent;

	private final APText facetCountDescNum;

	private final TextNfacets facetCountDescText;

	private final APText parentPercentDesc;

	private final APText significanceDesc;

	private final APText significanceTypeDesc;

	private final APText hundredPercentAxisLabel;

	private final APText spacebarDesc;

	private final LazyPPath totalLines;

	private final LazyPPath facetLines;

	private final LazyPPath parentLines;

	private final LazyPPath sigLines;

	private final LazyPPath heavyLines;

	private final LazyPNode totalHeader = new LazyPNode();

	private final LazyPNode totalBG = new LazyPNode();

	private final LazyPNode significanceHeader = new LazyPNode();

	private final LazyPNode sigBG = new LazyPNode();

	private final LazyPNode barBG = new LazyPNode();

	private final LazyPNode summaryBG = new LazyPNode();

	private PNode anchor;

	// private final LazyPNode facetDescs = new LazyPNode();

	private final LazyPNode sigWidgets = new LazyPNode();

	// private static final double borderW = 2.0;

	private static final double MARGIN = 5.0;

	private static final double BIG_TEXT_SCALE = 1.4;

	private static final float FADED_TRANSPARENCY = 0.3F;

	private static final float TRANSPARENT = 0;

	private static final float OPAQUE = 1;

	private static final double LABEL_ANIMATION_SCALE = BIG_TEXT_SCALE;

	private final static int BALLOON_STEP = 200;

	private final static int TRANSLATE_TO_CORNER_STEP = 400;

	private final static int TRANSLATION_OPACITY_DELAY = TRANSLATE_TO_CORNER_STEP / 3;

	private long scaleBarsDuration() {
		return animationSpeed * 800;
	}

	private long scaleBarsStep() {
		return animationSpeed * 1000;
	}

	private long translateToBarDuration() {
		return animationSpeed * 1000;
	}

	private long translateToBarStep() {
		return animationSpeed * 2000;
	}

	private long fadeDuration() {
		return animationSpeed * 1000;
	}

	private long fadeStep() {
		return animationSpeed * 1000;
	}

	private long scaleDuration() {
		return animationSpeed * 500;
	}

	private long scaleStep() {
		return animationSpeed * 2000;
	}

	/**
	 * See color key at C:\Projects\ArtMuseum\DesignSketches\popupColors.xcf
	 */

	/**
	 * Non-text colors
	 */

	/**
	 * Gold background that appears as a border in the no-help case, and a PNode
	 * text paint in help mode.
	 */
	private static final Color BGcolor = Bungee.goldBorderColor;

	private static final Color no_helpHeaderBGColor = Color.BLACK; // Color.getHSBColor(0.15f,

	// 0.15f, 0.3f);

	private static final Color no_helpBGcolor = Color.getHSBColor(0f, 0f, 0.1f); // totalHeaderColor.brighter();

	// /**
	// * Backgound for "x% of all works ...". Shown in help mode only
	// */
	// private static final Color parentDescBGcolor = Color.darkGray;

	private static final Color significanceHeaderBGColor = Color.getHSBColor(
			0.9f, 0.07f, 0.3f);

	private static final Color significanceBGcolor = significanceHeaderBGColor
			.brighter();

	private static final Color significanceLineColor = Color.getHSBColor(0.75f,
			0.8f, 1f);

	/**
	 * Text colors
	 */

	private static final Color countTextColor = Color.white;

	private static final Color unimportantTextColor = countTextColor.darker(); // .darker();

	private static final Color unimportantAxisTextColor = unimportantTextColor;

	/**
	 * Color of the message "Press the spacebar for more information"
	 */
	private static final Color spaceBarDescTextColor = unimportantTextColor; // Color.getHSBColor(0f,

	// 0f,
	// 0.3f);

	private static final Color facetPercentTextColor = Color.getHSBColor(0.15f,
			0.4f, 0.9f);

	private static final Color parentPercentTextColor = facetPercentTextColor
			.darker(); // PerspectiveViz.pvBG;

	private static final Color significanceDescTextColor = significanceBGcolor
			.brighter();

	private APText newAPText() {
		APText result = new APText(art.font);
		// initialize so globalBoundsCheck will succeed.
		result.setText("<uninitialized>");
		return result;
	}

	PopupSummary(Bungee _art) {
		art = _art;
		Stroke heavyStroke = LazyPPath.getStrokeInstance(3);

		totalLines = new LazyPPath();
		totalLines.setStrokePaint(unimportantAxisTextColor);
		totalLines.setStroke(heavyStroke);

		facetLines = new LazyPPath();
		facetLines.setStrokePaint(facetPercentTextColor);
		facetLines.setStroke(heavyStroke);

		parentLines = new LazyPPath();
		parentLines.setStrokePaint(parentPercentTextColor);
		parentLines.setStroke(heavyStroke);

		sigLines = new LazyPPath();
		sigLines.setStrokePaint(significanceLineColor);
		sigLines.setStroke(LazyPPath.getStrokeInstance(1));

		heavyLines = new LazyPPath();
		heavyLines.setStrokePaint(significanceLineColor);
		heavyLines.setStroke(heavyStroke);

		name = new TextNfacets(art, Color.darkGray, false);
		name.setScale(BIG_TEXT_SCALE);
		name.setWrapText(true);
		name.setWrapOnWordBoundaries(true);
		name.setRedrawer(this);
		namePrefix = new TextNfacets(art, unimportantTextColor.darker(), false);
		namePrefix.setWrapText(true);
		namePrefix.setWrapOnWordBoundaries(true);
		namePrefix.setRedrawer(this);
		namePrefix.setTrim(-1, 0);
		totalHeader.setPaint(no_helpHeaderBGColor);
		significanceHeader.setPaint(significanceHeaderBGColor);
		barBG.setPaint(BGcolor);
		summaryBG.setPaint(BGcolor);
		summaryBG.setPickable(false);
		// parentHeader.setPaint(null); // parentDescBGcolor);
		sigBG.setPaint(significanceBGcolor);
		totalBG.setPaint(no_helpBGcolor);

		totalCountDescNum = newAPText();
		totalCountDescNum.setTextPaint(countTextColor);

		totalCountDescText = new TextNfacets(art, unimportantTextColor, false);
		totalCountDescText.setWrapText(true);
		totalCountDescText.setWrapOnWordBoundaries(true);
		totalCountDescText.setRedrawer(this);
		totalCountDescText.setTrim(-1, 0);

		facetCountDescPercent = newAPText();
		facetCountDescPercent.setTextPaint(facetPercentTextColor);

		facetCountDescNum = newAPText();
		facetCountDescNum.setTextPaint(countTextColor);

		facetCountDescText = new TextNfacets(art, unimportantTextColor, false);
		facetCountDescText.setWrapText(true);
		facetCountDescText.setWrapOnWordBoundaries(true);
		facetCountDescText.setRedrawer(this);
		facetCountDescText.setTrim(-1, 0);

		parentPercentDesc = newAPText();
		parentPercentDesc.setTextPaint(unimportantTextColor);
		significanceDesc = newAPText();
		significanceDesc.setTextPaint(significanceDescTextColor);
		significanceTypeDesc = newAPText();
		significanceTypeDesc.setScale(BIG_TEXT_SCALE);
		significanceTypeDesc.setConstrainWidthToTextWidth(false);

		parentPercentDesc.setConstrainWidthToTextWidth(false);
		significanceDesc.setConstrainWidthToTextWidth(false);

		hundredPercentAxisLabel = newAPText();
		hundredPercentAxisLabel.setText("100%");
		hundredPercentAxisLabel.setPaint(BGcolor);
		hundredPercentAxisLabel.setTextPaint(unimportantAxisTextColor);

		// parentPercentDesc.setPaint(parentDescBGcolor);
		significanceDesc.setPaint(significanceBGcolor);

		spacebarDesc = newAPText();
		spacebarDesc.setTextPaint(spaceBarDescTextColor);
		spacebarDesc.setScale(0.75);

		addPermanentChildren();

		setPickable(false);
		setChildrenPickable(false);
		setVisible(false);

		// setPaint(Color.yellow);
	}

	private void addPermanentChildren() {
		addChild(barBG);
		addChild(totalBG); // (transparency varies)
		addChild(totalHeader);
		addChild(name); // E.g. "20th Century"
		addChild(namePrefix);
		addChild(totalCountDescNum); // E.g. "1,001" or "55 / 99 = 66% (p =
		// 0.03)"
		addChild(totalCountDescText);

		// 1b: isShowFacetInfo(). Clusters are only ever in this state.
		addChild(facetCountDescPercent); // E.g. "25%"
		addChild(facetCountDescNum); // E.g. "499"
		addChild(facetCountDescText); // " of them satisfy all 1
		// filters, compared with"
		addChild(parentPercentDesc); // E.g. "13% for all Dates" or
		// "5% of all works"

		addChild(parentPercentDesc);

		// 6: HELP_SIG
		sigWidgets.addChild(sigBG);
		sigWidgets.addChild(significanceHeader);
		sigWidgets.addChild(significanceDesc); // E.g. ""
		sigWidgets.addChild(significanceTypeDesc); // E.g. ""
		sigWidgets.addChild(sigLines);
		sigWidgets.addChild(heavyLines);

		addChild(spacebarDesc);
		addChild(totalLines);
		addChild(facetLines);
		addChild(parentLines);
		addChild(sigWidgets);
	}

	private void ensureChild(PNode child, boolean state) {
		if ((child.getParent() != null) && !state) {
			child.removeFromParent();
		} else if ((child.getParent() == null) && state) {
			addChild(child);
		}
	}

	void fade(PNode[] nodes, float transparency) {
		fade(nodes, 1000, transparency);
	}

	void fade(PNode[] nodes, long duration, float transparency) {
		for (int i = 0; i < nodes.length; i++) {

			if (transparency > 0 && nodes[i].getTransparency() == 0) {
				// Gross hack to tell fitToNodes that this node should be
				// considered
				nodes[i].setTransparency(Float.MIN_VALUE);
			}

			addAnimationJob(nodes[i].animateToTransparency(transparency,
					duration));
		}
	}

	void setWidth(PNode[] nodes, double w) {
		for (int i = 0; i < nodes.length; i++) {
			nodes[i].setWidth((int) (w / nodes[i].getScale()));
		}
	}

	void visibility(PNode[] nodes, boolean isVisible) {
		fade(nodes, 0, isVisible ? OPAQUE : TRANSPARENT);
	}

	void fitToNodes(PNode[] nodes, PNode[] virtualChildren, double margin) {
		for (int i = 0; i < nodes.length; i++) {
			setBoundsFromNodes(nodes[i], virtualChildren, margin);
		}
	}

	void layout(TextNfacets node, int x, int y) {
		node.setTrim(x, y);
		node.layout(node.getWidth(), 9999);
	}

	void delay(long duration) {
		addAnimationJob(new PActivity(duration));
	}

	static PNode[] nodes(PNode node) {
		PNode[] result = { node };
		return result;
	}

	static PNode[] nodes(PNode node1, PNode node2) {
		PNode[] result = { node1, node2 };
		return result;
	}

	static PNode[] nodes(PNode node1, PNode node2, PNode node3) {
		PNode[] result = { node1, node2, node3 };
		return result;
	}

	static PNode[] nodes(PNode node1, PNode node2, PNode node3, PNode node4) {
		PNode[] result = { node1, node2, node3, node4 };
		return result;
	}

	// Compartmentalize these that are 'obvious'
	void massActions() {
		if (showHelp == Step.NO_FRAME) {
			// Turn most everything off initially
			visibility(nodes(namePrefix, parentPercentDesc,
					significanceTypeDesc, significanceDesc), false);
			visibility(nodes(barBG, totalHeader, totalCountDescText,
					facetCountDescNum), false);
			visibility(nodes(facetCountDescPercent, facetCountDescText, sigBG,
					significanceHeader), false);
			visibility(nodes(sigLines, heavyLines, spacebarDesc, totalLines),
					false);
			visibility(nodes(facetLines, parentLines, summaryBG), false);

			// Turn 2 things on
			visibility(nodes(name, totalCountDescNum), true);

			// These widths won't change during popup
			setWidth(nodes(name), maxW());

		} else if (showHelp == Step.START_FRAME) {

			setWidth(nodes(namePrefix, parentPercentDesc), maxW());

		} else if (showHelp == Step.HELP_SIG) {

			setWidth(nodes(significanceTypeDesc, significanceDesc), maxW());

		}
	}

	void globalActions() {
		if (showHelp == Step.NO_FRAME) {
			align(this, 0, anchor, 0);
			delay(2000);
		} else if (showHelp == Step.START_FRAME) {

			// setBoundsFromNode(this, barBG, 0);
			addAnimationJob(animateToAlignment(this, 2, art.header, 2, -barBG
					.getX()
					- barBG.getMaxX(), -barBG.getY(), facet == null ? 0
					: TRANSLATE_TO_CORNER_STEP));

		} else if (showHelp == Step.NO_HELP) {
			delay(-1);
		} else if (showHelp == Step.SCALE_FRONT) {
			double newFrontH = rank.foldH + rank.frontH;
			summary().computeRankComponentHeights(
					new DesiredSize(0, Double.POSITIVE_INFINITY, 1),
					new DesiredSize(newFrontH, newFrontH, 3),
					new DesiredSize(0, 0, 9),
					new DesiredSize(1, Double.MAX_VALUE, 0.1));
			summary().reconnectToRank(rank, scaleBarsDuration());
		} else if (showHelp == Step.HELP_SIG) {
			delay(-1);
		}
	}

	void totalCountDescNumActions() {
		if (showHelp == Step.NO_FRAME) {
			// need to set text first, or global bounds will be empty
			totalCountDescNum.setText(noFrameDesc());
			align(totalCountDescNum, 16, this, 0);

		} else if (showHelp == Step.START_FRAME) {
			totalCountDescNum.setText(totalCountString());

			// Have to interleave facet- and total- alignment
			facetCountDescPercent.setText(percentOn(null).toString());
			align(facetCountDescPercent, 0, totalCountDescNum, 0);
			align(totalCountDescNum, 0, facetCountDescPercent, 2, MARGIN, 0);
		} else if (showHelp == Step.HELP_FACET) {
			fade(nodes(totalCountDescNum), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(totalCountDescNum), OPAQUE);
		}
	}

	void totalCountDescTextActions() {
		if (showHelp == Step.START_FRAME) {
			// Our width is smaller than the others
			setWidth(nodes(totalCountDescText), maxW()
					- totalCountDescNum.getMaxX());

			totalCountDescText.setContent(totalCountDesc());
			layout(totalCountDescText, -1, 0);

			// We use spaces in the text for separation rather than margins
			align(totalCountDescText, 0, totalCountDescNum, 2);

			fade(nodes(totalCountDescText), TRANSLATE_TO_CORNER_STEP, OPAQUE);
		} else if (showHelp == Step.HELP_FACET) {
			fade(nodes(totalCountDescText), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(totalCountDescText), OPAQUE);
		}
	}

	void nameActions() {
		if (showHelp == Step.NO_FRAME) {
			name.setContent(nameContents());
			layout(name, 0, 0);
			align(name, 16, totalCountDescNum, 0, 0, -MARGIN);

		} else if (showHelp == Step.START_FRAME) {
			layout(name, -1, 0);

		} else if (showHelp == Step.FADE_FOR_TOTAL) {
			fade(nodes(name), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(name), OPAQUE);
		}
	}

	void namePrefixActions() {
		if (showHelp == Step.START_FRAME) {
			namePrefix.setContent(namePrefixContents());
			layout(namePrefix, -1, 0);
			align(namePrefix, 16, name, 0);

			fade(nodes(namePrefix), TRANSLATE_TO_CORNER_STEP, OPAQUE);

		} else if (showHelp == Step.FADE_FOR_TOTAL) {
			fade(nodes(namePrefix), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(namePrefix), OPAQUE);
		}

	}

	void spacebarDescActions() {
		final float SPACEBAR_TRANSPARENCY = 0.6f;
		if (showHelp == Step.START_FRAME) {

			spacebarDesc.setText("Press the spacebar to skip this animation");
			align(spacebarDesc, 2,
					isShowFacetInfo() ? (PNode) parentPercentDesc
							: totalCountDescText, 18, 0, MARGIN);
			fade(nodes(spacebarDesc), TRANSLATE_TO_CORNER_STEP,
					canHelp() ? SPACEBAR_TRANSPARENCY : TRANSPARENT);

		} else if (showHelp == Step.NO_HELP) {
			spacebarDesc.setText("Press the spacebar for more explanation");
		} else if (showHelp == Step.GOLD_OVERLAY) {
			spacebarDesc.setText("Press the spacebar to skip this animation");
		} else if (showHelp == Step.FADE_FOR_TOTAL) {
			fade(nodes(spacebarDesc), canHelp() ? FADED_TRANSPARENCY
					: TRANSPARENT);
		} else if (showHelp == Step.HELP_SIG) {
			spacebarDesc
					.setText("Press the spacebar again for a slower animation\n"
							+ "To skip the animation next time, press twice.");
			align(spacebarDesc, 2, significanceDesc, 18, 0, MARGIN);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(spacebarDesc), canHelp() ? SPACEBAR_TRANSPARENCY
					: TRANSPARENT);
		}
	}

	void totalBGActions() {
		if (showHelp == Step.NO_FRAME) {
			fade(nodes(totalBG), 0, 0.5f);
			fitToNodes(nodes(totalBG), nodes(name, totalCountDescNum), 0);
		} else if (showHelp == Step.START_FRAME) {
			fade(nodes(totalBG), OPAQUE);

			fitToNodes(nodes(totalBG), nodes(totalHeader, totalCountDescText,
					parentPercentDesc, spacebarDesc), 0);
		}
	}

	void barBGActions() {
		if (showHelp == Step.START_FRAME) {
			fade(nodes(barBG), TRANSLATE_TO_CORNER_STEP, OPAQUE);

			fitToNodes(nodes(barBG), nodes(totalBG), MARGIN);
		} else if (showHelp == Step.HELP_SIG) {
			fitToNodes(nodes(barBG), nodes(totalBG, significanceDesc), MARGIN);

		}

	}

	void summaryBGActions() {
		if (showHelp == Step.GOLD_OVERLAY) {
			assert facet != null;
			assert isShowFacetInfo();

			if (summaryBG.getParent() == null) {
				// art.summary == null when we're initted, so check here
				// summaryBG.setTransparency(0);
				art.summary.addChild(summaryBG);
			}

			summaryBG.moveToFront();
			moveToFront();
			rank.moveInBackOf(this);

			fitToNodes(nodes(summaryBG), nodes(art.header, art.mouseDoc),
					MARGIN);

			fade(nodes(summaryBG), 0.8f);
		}
	}

	void totalHeaderActions() {
		if (showHelp == Step.START_FRAME) {
			fade(nodes(totalHeader), OPAQUE);
			fitToNodes(nodes(totalHeader), nodes(namePrefix, name), MARGIN);
		}
	}

	void facetCountDescFadeActions() {
		if (showHelp == Step.START_FRAME) {
			fade(nodes(facetCountDescPercent, facetCountDescNum,
					facetCountDescText), TRANSLATE_TO_CORNER_STEP, OPAQUE);
		} else if (showHelp == Step.FADE_FOR_TOTAL) {
			fade(nodes(facetCountDescPercent, facetCountDescNum,
					facetCountDescText), FADED_TRANSPARENCY);
		} else if (showHelp == Step.HELP_FACET) {
			fade(nodes(facetCountDescPercent, facetCountDescNum,
					facetCountDescText), OPAQUE);
		} else if (showHelp == Step.FADE_FACET) {
			fade(nodes(facetCountDescPercent, facetCountDescNum,
					facetCountDescText), FADED_TRANSPARENCY);
		} else if (showHelp == Step.UNFADE) {
			fade(nodes(facetCountDescPercent, facetCountDescNum,
					facetCountDescText), OPAQUE);

		}

	}

	void facetCountDescPercentActions() {
		if (showHelp == Step.START_FRAME) {
			// This is done in totalCountDescNumActions, because that alignment
			// depends on it.
			// facetCountDescPercent.setText(percentOn(null).toString());

			align(facetCountDescPercent, 4, totalCountDescText, 20, 0, MARGIN);
		}
	}

	void facetCountDescNumActions() {
		if (showHelp == Step.START_FRAME) {
			facetCountDescNum.setText(onCountString());

			// right align
			align(facetCountDescNum, 34, totalCountDescNum, 34);

			// top align
			align(facetCountDescNum, 4, facetCountDescPercent, 4);
		}
	}

	void facetCountDescTextActions() {
		if (showHelp == Step.START_FRAME) {
			// Our width is small than the others
			setWidth(nodes(facetCountDescText), maxW()
					- facetCountDescNum.getMaxX());

			facetCountDescText.setContent(facetCountDesc());
			layout(facetCountDescText, -1, 0);

			// We use spaces in the text for separation rather than margins
			align(facetCountDescText, 0, facetCountDescNum, 2);

		}

	}

	void parentPercentDescActions() {
		if (showHelp == Step.START_FRAME) {
			// Text never changes, plus we're using attributes to have the
			// percentage in a different color
			setParentDesc();

			// left align
			align(parentPercentDesc, 32, facetCountDescPercent, 32);

			// align below
			align(parentPercentDesc, 4, facetCountDescText, 20, 0, MARGIN);

			fade(nodes(parentPercentDesc), TRANSLATE_TO_CORNER_STEP, OPAQUE);

		} else if (showHelp == Step.FADE_FOR_TOTAL) {
			fade(nodes(parentPercentDesc), FADED_TRANSPARENCY);
		} else if (showHelp == Step.HELP_PARENT) {
			fade(nodes(parentPercentDesc), OPAQUE);

		}

	}

	void sigBGActions() {

	}

	void significanceHeaderActions() {

	}

	void significanceDescActions() {

	}

	void significanceTypeDescActions() {

	}

	void sigLinesActions() {

	}

	void heavyLinesActions() {

	}

	void totalLinesActions() {

	}

	void facetLinesActions() {

	}

	void parentLinesActions() {

	}

	void actions() {
		massActions();

		totalCountDescNumActions();
		totalCountDescTextActions();
		nameActions();
		namePrefixActions();

		facetCountDescPercentActions();
		facetCountDescNumActions();
		facetCountDescTextActions();
		facetCountDescFadeActions();
		parentPercentDescActions();

		significanceDescActions();
		significanceTypeDescActions();

		spacebarDescActions();

		totalHeaderActions();
		totalBGActions();
		barBGActions();
		summaryBGActions();
		significanceHeaderActions();
		sigBGActions();

		sigLinesActions();
		heavyLinesActions();
		totalLinesActions();
		facetLinesActions();
		parentLinesActions();

		globalActions();
	}

	void performStep(int animatingPerformance) {
		if (!maybeExit(animatingPerformance) && !animationFinished()
				&& !finishingAnimation) {
			finishAnimation();
			showHelp = showHelp.next();
			actions();
			long delay = maxFinishTime() - System.currentTimeMillis();
//			Util.print(showHelp + " " + delay);
			performStep(animatingPerformance, delay);
		}
	}

	private void updateChildren() {
		//
		// // performStep will turn it off for facetTypes
		// ensureChild(name, true);
		//
		// showHelp = showHelp.next();
		// boolean state = showHelp.compareTo(Step.NO_FRAME) > 0;
		//
		// name.setTrim(state ? -1 : 5, 0);
		// ensureChild(barBG, state);
		// barBG.moveToBack();
		// ensureChild(totalHeader, state);
		// totalHeader.moveInFrontOf(totalBG);
		// // totalBG.setTransparency(state ? 1.0f : 0.5f);
		// ensureChild(totalCountDescText, state);
		// ensureChild(namePrefix, state && facet != null);
		//
		// state = state && isShowFacetInfo();
		// ensureChild(facetDescs, state);
		// ensureChild(parentPercentDesc, state && conditionalMedian == null);
		//
		// state = state && anchor instanceof Bar;
		// ensureChild(spacebarDesc, state);
		//
		// state = state && showHelp.compareTo(Step.GOLD_OVERLAY) >= 0;
		// if (state) {
		// art.summary.addChild(summaryBG);
		// moveToFront();
		// rank.moveInBackOf(this);
		// }
		//
		// state = state && showHelp.compareTo(Step.HELP_TOTAL) >= 0;
		// ensureChild(totalLines, state);
		//
		// state = state && showHelp.compareTo(Step.HIGHLIGHT_FACET) >= 0;
		// ensureChild(facetLines, state);
		//
		// state = state && showHelp.compareTo(Step.HIGHLIGHT_PARENT) >= 0;
		// ensureChild(parentLines, state);
		//
		// state = state && showHelp.compareTo(Step.HELP_SIG) >= 0;
		// ensureChild(sigWidgets, state);
		// // if (!isShowHelp() || animationFinished()) {
		// // facetLines.moveInBackOf(sigWidgets);
		// // facetLines.setTransparency(1);
		// // // facetLines.setVisible(true);
		// // facetPercentAxisLabel.setVisible(true);
		// // }
	}

	private PNode[] totalHeaderVirtualChildren() {
		PNode[] result = { name, namePrefix };
		return result;
	}

	private PNode[] totalBGVirtualChildren() {
		PNode[] result = { name, namePrefix, totalCountDescNum,
				facetCountDescText, parentPercentDesc, spacebarDesc };
		return result;
	}

	private boolean isHelping() {
		return showHelp.compareTo(Step.NO_HELP) > 0;
	}

	private boolean isShowFacetInfo() {
		return art.query.isRestricted()
				&& (facet == null || facet.getOnCount() >= 0);
	}

	private boolean isFacetType() {
		return facet != null && facet.getParent() == null;
	}

	private boolean canHelp() {
		return isShowFacetInfo() && !isFacetType();
	}

	boolean showMoreHelp() {
		if (showHelp.compareTo(Step.NO_FRAME) == 0) {
			finishAnimation();
			update();
		} else if (showHelp.compareTo(Step.NO_HELP) == 0) {
			if (canHelp()) {
				animationSpeed = 1;
				update();
			}
		} else if (showHelp.compareTo(Step.HELP_SIG) == 0) {
			showHelp = Step.NO_HELP;
			animationSpeed = 3;
			// ensureChild(totalLines, true);
			// totalLines.moveToFront();
			update();
		} else {
			showHelp = Step.HELP_SIG.previous();
			finishAnimation();
			updateChildren();
		}
		return false;
	}

	void setFacet(Perspective _facet, Rank _rank, boolean _showMedian,
			PNode _anchor) {
		assert _facet != null;
		assert _anchor != null;
		anchor = _anchor;
		facet = _facet;
		rank = _rank;
		conditionalMedian = _showMedian ? facet.getMedianPerspective(true)
				: null;
		unconditionalMedian = _showMedian ? facet.getMedianPerspective(false)
				: null;
		cluster = null;
		update();
	}

	void setCluster(Cluster _cluster) {
		anchor = null;
		facet = null;
		// rank = null;
		conditionalMedian = null;
		unconditionalMedian = null;
		cluster = _cluster;
		update();
	}

	private void update() {
		performStep(performance);
	}

	public void redraw() {
		// Util.print("popup redraw " + facet);
		update();
	}

	private String onCountString() {
		int onCount = facet != null ? facet.getOnCount() : cluster.getOnCount();
		return Util.addCommas(onCount);
	}

	private String totalCountString() {
		int totalCount = facet != null ? facet.totalCount : cluster
				.getTotalCount();
		return Util.addCommas(totalCount);
	}

	private void pValueString(StringBuffer buf) {
		double pValue = facet != null ? facet.pValue() : cluster.pValue();
		buf.append(" (");
		MouseDocLine.formatPvalue(pValue, buf);
		buf.append(")");
	}

	private static void ensureArea(PNode node) {
		if (node.getWidth() <= 0)
			node.setWidth(1);
		if (node.getHeight() <= 0)
			node.setHeight(1);
	}

	// translate attachment to align with base. Margins are added to base.
	private static PTransformActivity animateToAlignment(PNode attachment,
			int attachmentPoint, PNode base, int basePoint, double baseXoffset,
			double baseYoffset, long duration) {
		assert isPoint(attachmentPoint);
		assert isPoint(basePoint);
		PBounds baseBounds = ensureGlobalBounds(base);
		PBounds attachmentBounds = ensureGlobalBounds(attachment);
		double attachmentX0 = (int) attachmentBounds.getX();
		double attachmentY0 = (int) attachmentBounds.getY();
		// Util.print("align " + base);
		// Util.print(baseBounds);
		// Util.print(attachmentBounds);
		// Util.print("");

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
		return attachment.animateToPositionScaleRotation(goal.getX(), goal
				.getY(), attachment.getScale(), 0, duration);

		// attachment.setGlobalTranslation(new Point2D.Double(attachmentX0,
		// attachmentY0));
		// assert !attachment.getGlobalBounds().isEmpty();
	}

	private static PBounds ensureGlobalBounds(PNode node) {
		assert node.getRoot() != null;
		// bounds will be empty if width or height is zero, and we don't want
		// that to prevent using the offset for alignment.
		ensureArea(node);
		PBounds result = node.getGlobalBounds();
		assert !result.isEmpty() : node;
		return result;
	}

	private static void align(PNode attachment, int attachmentPoint,
			PNode base, int basePoint) {
		align(attachment, attachmentPoint, base, basePoint, 0, 0);
	}

	private static void align(PNode attachment, int attachmentPoint,
			PNode base, int basePoint, double baseXoffset, double baseYoffset) {
		animateToAlignment(attachment, attachmentPoint, base, basePoint,
				baseXoffset, baseYoffset, 0);
	}

	// adjust width or length (and possible translate) to align with base
	// attachment point should be a corner. The opposite corner won't move.
	// if attachment point isn't a corner, that dimension won't be changed.
	// thus, attachment point 4 is always a no-op.
	private static void stretch(PNode base, int basePoint, PNode attachment,
			int attachmentPoint, double baseXoffset, double baseYoffset) {
		PBounds baseBounds = ensureGlobalBounds(base);
		PBounds attachmentBounds = ensureGlobalBounds(attachment);
		if (isChangeX(attachmentPoint)) {
			double x0 = pointX(baseBounds, basePoint);
			double x1 = pointX(attachmentBounds,
					oppositeDirection(attachmentPoint));
			attachment.setWidth(Math.abs(x1 - x0));
		}
		if (isChangeY(attachmentPoint)) {
			double y0 = pointY(baseBounds, basePoint);
			double y1 = pointY(attachmentBounds,
					oppositeDirection(attachmentPoint));
			attachment.setHeight(Math.abs(y1 - y0));
		}
		align(attachment, attachmentPoint, base, basePoint, baseXoffset,
				baseYoffset);
	}

	private static void stretchLine(LazyPPath line, PNode base1,
			int base1Point, PNode base2, int base2Point,
			double base1VerticalOffset) {
		Rectangle2D base1Bounds = ensureGlobalBounds(base1);
		Rectangle2D base2Bounds = ensureGlobalBounds(base2);
		stretchLine(line, base1Bounds, base1Point, base2Bounds, base2Point,
				base1VerticalOffset);
	}

	// draw horzontal-vertical-horizontal line segments between the bases
	// base1verticalOffset is distance to the vertical line segment
	private static void stretchLine(LazyPPath line, Rectangle2D base1Bounds,
			int base1Point, Rectangle2D base2Bounds, int base2Point,
			double base1VerticalOffset) {
		// Util.print(base1Bounds);

		double x1 = pointX(base1Bounds, base1Point);
		double y1 = pointY(base1Bounds, base1Point);
		double x2 = pointX(base2Bounds, base2Point);
		double y2 = pointY(base2Bounds, base2Point);

		// Util.print(x1 + " " + x2 + " " + y1 + " " + y2);

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

	private static PTransformActivity scaleAboutPoint(PNode node, int point,
			double scale, long duration) {
		scale = scale / node.getScale();
		PBounds bounds = node.getBounds();
		double x = pointX(bounds, point);
		double y = pointY(bounds, point);
		PAffineTransform xform = node.getTransform();
		xform.translate(x, y);
		xform.scale(scale, scale);
		xform.translate(-x, -y);
		return node.animateToTransform(xform, duration);
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
	 * @return
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

	// private void setBoundsFromChildren(PNode node, double margin) {
	// setBoundsFromNodes(node, (PNode[]) node.getChildrenReference().toArray(
	// new PNode[0]), margin);
	// }

	private static void setBoundsFromNode(PNode node, PNode virtualChild,
			double margin) {
		PNode[] temp = { virtualChild };
		setBoundsFromNodes(node, temp, margin);
	}

	private static void setBoundsFromNodes(PNode node, PNode[] virtualChildren,
			double margin) {
		animateBoundsFromNodes(node, virtualChildren, margin, 0);
	}

	private static void animateBoundsFromNodes(PNode node,
			PNode[] virtualChildren, double margin, long duration) {
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;
		for (int i = 0; i < virtualChildren.length; i++) {
			PNode child = virtualChildren[i];
			if (child.getRoot() != null && child.getVisible()
					&& child.getTransparency() > 0) {
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
		// node.setGlobalTranslation(new Point2D.Double((int) (minX - margin),
		// (int) (minY - margin)));
		// node.setWidth((maxX - minX + 2 * margin) / scale);
		// node.setHeight((maxY - minY + 2 * margin) / scale);
		Rectangle2D local = node.globalToLocal(newBounds);
		node.animateToBounds(local.getX(), local.getY(), local.getWidth(),
				local.getHeight(), duration);
	}

	private static void iBeam(LazyPPath node) {
		Rectangle2D bounds = node.getBoundsReference();
		float midX = (int) node.getBoundsReference().getCenterX();
		float top = (int) bounds.getY();
		float bottom = (int) bounds.getMaxY();
		node.reset();
		node.moveTo(midX - 5, top);
		node.lineTo(midX + 5, top);
		node.moveTo(midX, top);
		node.lineTo(midX, bottom);
		node.moveTo(midX - 5, bottom);
		node.lineTo(midX + 5, bottom);
	}

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

	private void setName() {
		name.setContent(nameContents());
		name.layout(nameW(), 9999);
	}

	double nameW() {
		return (int) (maxW() / name.getScale());
	}

	private int onCount() {
		if (facet != null)
			return facet.getOnCount();
		else
			return cluster.getOnCount();
	}

	private int totalCount() {
		if (facet != null)
			return facet.totalCount;
		else
			return cluster.getTotalCount();
	}

	private StringBuffer percentOn(StringBuffer buf) {
		return ResultsGrid
				.formatPercent(onCount() / (double) totalCount(), buf);
	}

	private int parentOnCount() {
		if (facet == null || facet.getParent() == null)
			return art.query.onCount;
		else
			return facet.getParent().getOnCount();
	}

	private int parentTotalCount() {
		if (facet == null || facet.getParent() == null)
			return art.query.totalCount;
		else
			return facet.getParent().totalCount;
	}

	private StringBuffer parentPercentOn(StringBuffer buf) {
		return ResultsGrid.formatPercent(parentOnCount()
				/ (double) parentTotalCount(), buf);
	}

	private Markup totalCountDesc() {
		Markup medianDesc = medianContent(unconditionalMedian);
		medianDesc.add(0, collectionDescription());
		// Util.print(TextNfacets.toText(medianDesc));
		return medianDesc;
	}

	private Markup facetCountDesc() {
		Markup medianDesc = medianContent(conditionalMedian);
		StringBuffer buf = new StringBuffer();
		buf.append(" of them satisfy all ").append(
				art.query.nFilters(true, true)).append(" filters");
		if (conditionalMedian != null) {
			pValueString(buf);
		} else {
			buf.append(", compared with");
		}
		medianDesc.add(0, buf.toString());
		return medianDesc;
	}

	Markup medianContent(Perspective median) {
		Markup content = Query.emptyMarkup();
		if (median != null) {
			content.add(Markup.NEWLINE_TAG);
			content.add(Color.white);
			content.add("median: ");
			content.add(Markup.DEFAULT_COLOR_TAG);
			content.add(median);
		}
		return content;
	}

	// desc should be aligned before calling this, so width calculation is right
	// private void addMedianInfo(Perspective median, TextNfacets desc,
	// Markup prefix) {
	// Markup content = prefix;
	// if (content == null)
	// content = Query.emptyMarkup();
	// // result.add(collectionDescription());
	// if (median != null) {
	// content.add(Markup.NEWLINE_TAG);
	// content.add(Color.white);
	// content.add("median: ");
	// content.add(Markup.DEFAULT_COLOR_TAG);
	// content.add(median);
	// if (desc == facetCountDescText) {
	// StringBuffer buf = new StringBuffer(" (");
	// MouseDocLine.formatPvalue(facet.medianTest(), buf);
	// buf.append(")");
	// content.add(buf.toString());
	// }
	// }
	// desc.setContent(content);
	// double descW = maxW() - desc.getXOffset();
	// desc.layout(descW, 9999);
	// desc.setWidth(descW);
	// if (median != null && median.getOnCount() == 0) {
	// brightenFacet(desc, median);
	// }
	// }

	private String parentDescString() {
		Perspective parent = facet != null ? facet.getParent() : null;
		return parent == null ? "" : TextNfacets.toText(parent
				.facetDescription(), this);
	}

	private void setParentDesc() {
		StringBuffer buf = new StringBuffer();
		parentPercentOn(buf);
		int parentPercentStringLength = buf.length();
		buf.append(" for all").append(parentDescString());
		pValueString(buf);
		parentPercentDesc.setWidth(maxW());
		parentPercentDesc.setText(buf.toString());

		parentPercentDesc.clearAttributes();
		parentPercentDesc.addAttribute(TextAttribute.FOREGROUND,
				parentPercentTextColor, 0, parentPercentStringLength);
	}

	private void performStep(final int animatingPerformance, long delay) {
		if (delay > 0) {

			PActivity ta = new PActivity(delay) {

				protected void activityFinished() {
					super.activityFinished();
					performStep(animatingPerformance);
				}
			};
			addActivity(ta);
			addAnimationJob(ta);
		} else {
			performStep(animatingPerformance);
		}
	}

	private void animateToOpaque(PNode node) {
		node.setTransparency(0);
		addAnimationJob(edu.cmu.cs.bungee.piccoloUtils.gui.Util.animateToTransparency(node, 1,
				TRANSLATION_OPACITY_DELAY, TRANSLATE_TO_CORNER_STEP));
	}

	private PInterpolatingActivity animateBarTransparencies(final float newTransparency,
			long delay, long duration) {
		PInterpolatingActivity ta = null;
		if (rank != null) {
			if (duration == 0 && delay == 0) {
				rank.setBarTransparencies(newTransparency);
				// anchor.setTransparency(1);
			} else {
				final PNode _node = anchor;
				final Rank _rank = rank;

				ta = new PInterpolatingActivity(duration,
						PUtil.DEFAULT_ACTIVITY_STEP_RATE, delay
								+ System.currentTimeMillis(), 1,
						PInterpolatingActivity.SOURCE_TO_DESTINATION) {
					private final float oldTransparency = _node.getTransparency();

					public void setRelativeTargetValue(float zeroToOne) {
						_rank.setBarTransparencies(oldTransparency
								+ (zeroToOne * (newTransparency - oldTransparency)));
						// _node.setTransparency(1);
					}
				};
				addActivity(ta);
			}
		}
		return ta;
	}

	private PerspectiveViz pv() {
		return (PerspectiveViz) anchor.getParent().getParent();
	}

	// private SqueezablePNode front() {
	// return (SqueezablePNode) anchor.getParent();
	// }

	private Summary summary() {
		return art.summary;
	}

	private String noFrameDesc() {
		StringBuffer buf = new StringBuffer();
		buf.append(onCountString());
		if (isShowFacetInfo()) {
			buf.append(" / ").append(totalCountString());
			buf.append(" = ");
			percentOn(buf);
			pValueString(buf);
		}
		return buf.toString();
	}

	void performStepx(int animatingPerformance) {
		if (!maybeExit(animatingPerformance) && !animationFinished()) {
			// boolean state = computeStepState();
			// ensureChildren(stepChildren[showHelp], true);
			updateChildren();
			// Util.print("popup.performStep " + showHelp + " " + facet);
			// switch (animatingPerformance) {
			// case NO_FRAME:
			{
				totalBG.setTransparency(0.5f);
				StringBuffer buf = new StringBuffer();
				buf.append(onCountString());
				if (isShowFacetInfo()) {
					buf.append(" / ").append(totalCountString());
					buf.append(" = ");
					percentOn(buf);
					pValueString(buf);
				}
				totalCountDescNum.setText(buf.toString());
				if (anchor != null) {
					align(anchor, 0, this, 16);
					align(anchor, 0, totalCountDescNum, 16);
				}
				setName();
				if (isFacetType() && anchor instanceof TextNfacets)
					ensureChild(name, false);
				else {
					align(totalCountDescNum, 0, name, 16);
				}
				setBoundsFromNodes(totalBG, totalBGVirtualChildren(), 2);
				performStep(animatingPerformance, facet == null ? 0
						: BALLOON_STEP);
				// break;
				// }
				// case NO_HELP: {
				name.setWidth(nameW());
				totalCountDescNum.setText(totalCountString());
				align(totalCountDescNum, 0, name, 16, 0, -MARGIN);
				if (isShowFacetInfo()) {
					facetCountDescPercent.setText(percentOn(null).toString());
					// left align facetCountDescPercent below old
					// totalCountDescNum
					align(totalCountDescNum, 16, facetCountDescPercent, 0);

					// now move totalCountDescNum to the right
					align(facetCountDescPercent, 2, totalCountDescNum, 16,
							MARGIN, 0);
					// animateToOpaque(facetDescs);
				}
				align(totalCountDescNum, 2, totalCountDescText, 0, MARGIN, 0);
				List totalCountDescContent = new Vector();
				totalCountDescContent.add(collectionDescription());
				// addMedianInfo(unconditionalMedian, totalCountDescText,
				// totalCountDescContent);

				animateToOpaque(totalCountDescText);
				animateToOpaque(barBG);
				animateToOpaque(totalHeader);
				addAnimationJob(edu.cmu.cs.bungee.piccoloUtils.gui.Util.animateToTransparency(totalBG, 1,
						TRANSLATION_OPACITY_DELAY, TRANSLATE_TO_CORNER_STEP));

				if (isShowFacetInfo()) {
					facetCountDescNum.setText(Util.addCommas(onCount()));
					// right align facetCountDescNum below totalCountDescNum
					align(totalCountDescNum, 18, facetCountDescNum, 2);
					align(totalCountDescText, 20, facetCountDescNum, 4, 0,
							MARGIN);
					align(totalCountDescText, 20, facetCountDescPercent, 4, 0,
							MARGIN);

					// align facetCountDescText to the right of
					// facetCountDescNum
					align(facetCountDescNum, 2, facetCountDescText, 0, MARGIN,
							0);
					// StringBuffer buf = new StringBuffer();
					buf.append(" of them satisfy all ").append(
							art.query.nFilters(true, true)).append(" filters");
					if (conditionalMedian != null) {
						pValueString(buf);
					} else {
						buf.append(", compared with");
					}
					List facetCountDescContent = new Vector();
					facetCountDescContent.add(buf.toString());
					// addMedianInfo(conditionalMedian, facetCountDescText,
					// facetCountDescContent);
					if (conditionalMedian == null) {
						setParentDesc();
						align(facetCountDescText, 20, parentPercentDesc, 4, 0,
								MARGIN);
						align(facetCountDescPercent, 32, parentPercentDesc, 32);
						animateToOpaque(parentPercentDesc);
					}

					if (spacebarDesc.getRoot() != null) {
						spacebarDesc
								.setText("Press the spacebar for more explanation");
						PNode base = conditionalMedian == null ? (PNode) parentPercentDesc
								: facetCountDescText;
						align(base, 18, spacebarDesc, 2, 0, MARGIN);
						animateToOpaque(spacebarDesc);
					}
				}

				if (facet != null) {
					namePrefix.setContent(getPrefix(facet.facetDescription()));
					namePrefix.layout(maxW(), 9999);
					namePrefix.setWidth(maxW());
					align(name, 0, namePrefix, 16);
					animateToOpaque(namePrefix);
				}
				setBoundsFromNodes(totalHeader, totalHeaderVirtualChildren(), 2);
				animateBoundsFromNodes(totalBG, totalBGVirtualChildren(), 2,
						TRANSLATE_TO_CORNER_STEP);
				setBoundsFromNodes(barBG, totalBGVirtualChildren(), MARGIN);
				setBoundsFromNode(this, barBG, 0);

				addAnimationJob(animateToAlignment(art.header, 2, this, 2,
						-getX(), -getY(), facet == null ? 0
								: TRANSLATE_TO_CORNER_STEP));

				// We don't call performStep again. That only happens if user
				// presses spacebar

				// break;
				// }
				// case GOLD_OVERLAY: {
				assert facet != null;
				assert isShowFacetInfo();
				spacebarDesc
						.setText("Press the spacebar to skip this animation");
				PNode[] limits = { art.header, art.mouseDoc };
				setBoundsFromNodes(summaryBG, limits, 0);
				summaryBG.setTransparency(0);
				addAnimationJob(summaryBG.animateToTransparency(0.8f,
						fadeDuration()));
				performStep(animatingPerformance, fadeStep());
				// break;
				// }
				// case SCALE_FRONT:

				rank.setHeight(rank.foldH + rank.frontH);
//				summary().selectedFrontH += summary().selectedLabelH;
//				summary().selectedLabelH = 0;
				summary().connectToRank(rank);
				summary().layoutChildrenWhenNeeded(scaleBarsDuration());
//				mungePV();
				performStep(animatingPerformance, scaleBarsStep());
				// break;
				// case TRANSLATE_POPUP:
				addAnimationJob(animateToAlignment(anchor, 2, this, 16,
						10 * MARGIN, art.lineH / 2, translateToBarDuration()));
				performStep(animatingPerformance, translateToBarStep());
				// break;
				// case FADE_OTHER_BARS:
				performStep(animatingPerformance, 0);
				// break;
				// case FADE_FOR_TOTAL:
				addAnimationJob(name.animateToTransparency(FADED_TRANSPARENCY,
						fadeDuration()));
				addAnimationJob(namePrefix.animateToTransparency(
						FADED_TRANSPARENCY, fadeDuration()));
				animateBarTransparencies(FADED_TRANSPARENCY, 0, fadeDuration());
				animateFacetWidgetTransparency(FADED_TRANSPARENCY);
				animateParentWidgetTransparency(FADED_TRANSPARENCY);
				addAnimationJob(spacebarDesc.animateToTransparency(
						FADED_TRANSPARENCY, fadeDuration()));
				performStep(animatingPerformance, fadeStep());
				// break;
				// case HELP_TOTAL: {
				align(parentPercentDesc, 18, spacebarDesc, 2, 0, MARGIN);

				PNode label = pv().percentLabels[2];
				stretchLine(totalLines, totalCountDescNum, 8, label, 10, 0);
				// totalLines.setTransparency(1);
				totalLines.setStroke(LazyPPath.getStrokeInstance(3));
				addAnimationJob(scaleAboutPoint(label, 9,
						LABEL_ANIMATION_SCALE, scaleDuration()));
				addAnimationJob(scaleAboutPoint(totalCountDescNum, 9,
						LABEL_ANIMATION_SCALE, scaleDuration()));

				performStep(animatingPerformance, scaleStep());
				// break;
				// }
				// case FADE_TOTAL: {
				// PNode label = pv().percentLabels[2];
				addAnimationJob(scaleAboutPoint(label, 9,
						PerspectiveViz.PERCENT_LABEL_SCALE, scaleDuration()));
				addAnimationJob(scaleAboutPoint(totalCountDescNum, 9, 1,
						scaleDuration()));
				totalLines.setStroke(LazyPPath.getStrokeInstance(1));
				// addAnimationJob(totalLines.animateToTransparency(0,
				// fadeDuration()));

				performStep(animatingPerformance, fadeStep());
				// break;
				// }
				// case HELP_FACET:
				animateTotalWidgetTransparency(FADED_TRANSPARENCY);
				// facetLines.setTransparency(1);
				performStep(animatingPerformance, fadeStep());
				// break;
				// case HIGHLIGHT_FACET: {
				animateFacetWidgetTransparency(1);
				PBounds localBarBounds = ((Bar) anchor).visibleBounds();
				localBarBounds.setRect(0, localBarBounds.y, localBarBounds.x
						+ localBarBounds.width, localBarBounds.height);
				Rectangle2D barBounds = anchor.localToGlobal(localBarBounds);
				facetLines.setStroke(LazyPPath.getStrokeInstance(3));
				stretchLine(facetLines,
						facetCountDescPercent.getGlobalBounds(), 8, barBounds,
						0, -2 * MARGIN);
				// scaleAboutPoint(facetCountDescPercent, 9,
				// LABEL_ANIMATION_SCALE, scaleDuration());

				performStep(animatingPerformance, scaleStep());
				// break;
				// }
				// case FADE_FACET:
				animateFacetWidgetTransparency(FADED_TRANSPARENCY);
				// addAnimationJob(scaleAboutPoint(facetCountDescPercent, 9, 1,
				// scaleDuration()));
				facetLines.setStroke(LazyPPath.getStrokeInstance(1));
				// addAnimationJob(facetLines.animateToTransparency(0,
				// fadeDuration()));
				performStep(animatingPerformance, fadeStep());
				// break;
				// case HELP_PARENT:
				animateParentWidgetTransparency(1);
				addAnimationJob(pv().highlightParentRect(true, fadeDuration()));
				performStep(animatingPerformance, fadeStep());
				// break;
				// case HIGHLIGHT_PARENT: {
				// PNode label = pv().percentLabels[1];
				label.moveToFront();
				// parentLines.setTransparency(1);
				parentLines.setStroke(LazyPPath.getStrokeInstance(3));
				stretchLine(parentLines, parentPercentDesc, 8, label, 10, -4
						* MARGIN);
				addAnimationJob(scaleAboutPoint(label, 9,
						LABEL_ANIMATION_SCALE, scaleDuration()));
				// addAnimationJob(scaleAboutPoint(totalCountDescNum, 9,
				// labelAnimationScale,
				// 500));
				performStep(animatingPerformance, scaleStep());
				// break;
				// }
				// case UNFADE: {
				parentLines.setStroke(LazyPPath.getStrokeInstance(1));
				// addAnimationJob(parentLines.animateToTransparency(0,
				// fadeDuration()));
				// PNode label = pv().percentLabels[1];
				addAnimationJob(scaleAboutPoint(label, 9,
						PerspectiveViz.PERCENT_LABEL_SCALE, scaleDuration()));
				animateTotalWidgetTransparency(1);
				animateFacetWidgetTransparency(1);
				addAnimationJob(name.animateToTransparency(1, fadeDuration()));
				addAnimationJob(namePrefix.animateToTransparency(1,
						fadeDuration()));
				addAnimationJob(pv().highlightParentRect(false, fadeDuration()));
				addAnimationJob(spacebarDesc.animateToTransparency(1,
						fadeDuration()));
				performStep(animatingPerformance, 1000);
				// break;
				// }
				// case HELP_SIG:
				// parentHelpWidgets.moveInBackOf(facetHelpWidgets);
				updateSignificance();
				spacebarDesc
						.setText("Press the spacebar again for a slower animation\nTo skip the animation next time, press twice");
				align(significanceTypeDesc, 18, spacebarDesc, 2);
				// break;
				// default:
				// assert false : animatingPerformance + " " + performance;
			}
		}
	}

	void animateFacetWidgetTransparency(float transparency) {
		addAnimationJob(anchor.animateToTransparency(transparency,
				fadeDuration()));
		// addAnimationJob(facetDescs.animateToTransparency(transparency,
		// fadeDuration()));
		addAnimationJob(facetLines.animateToTransparency(transparency,
				fadeDuration()));
	}

	void animateParentWidgetTransparency(float transparency) {
		addAnimationJob(pv().parentRect.animateToTransparency(transparency,
				fadeDuration()));
		addAnimationJob(parentPercentDesc.animateToTransparency(transparency,
				fadeDuration()));
	}

	void animateTotalWidgetTransparency(float transparency) {
		addAnimationJob(totalCountDescNum.animateToTransparency(transparency,
				fadeDuration()));
		addAnimationJob(totalCountDescText.animateToTransparency(transparency,
				fadeDuration()));
	}

	private String collectionDescription() {
		if (art.query.isRestrictedData())
			return " in restricted set";
		else
			return " in collection";
	}

	private int maxW() {
		return (int) (art.getW() - art.summary.getWidth() - 33 * MARGIN);
	}

	private static void brightenFacet(TextNfacets text, ItemPredicate facet) {
		// gross hack because you can't read faded text in popups
		for (Iterator it = text.getChildrenIterator(); it.hasNext();) {
			Object childNode = it.next();
			if (childNode instanceof FacetText) {
				FacetText child = (FacetText) childNode;
				if (child.facet == facet) {
					child.setColor(1);
				}
			}
		}
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

	private void updateSignificance() {
		String relationTypeString = null;
		StringBuffer sigStringBuf = new StringBuffer();
		int chiColorFamily = art.chiColorFamily(facet);
		String facetDesc = TextNfacets.toText(facet.facetDescription(), this);
		PBounds barBounds = ((Bar) anchor).visibleBounds();
		double delta = anchor.getHeight() / 2 - barBounds.getHeight();
		switch (chiColorFamily) {
		case 0:
			relationTypeString = "...is not correlated with filters:";
			if (delta < -0.25 || delta > 0.25) {
				sigStringBuf.append("Even though the percentage of");
				sigStringBuf.append(facetDesc);
				sigStringBuf.append(" that satisfy the filters is much ");
				if (delta < -0.25)
					sigStringBuf.append("lower");
				else
					sigStringBuf.append("higher");
				sigStringBuf.append(" than that for other ");
				sigStringBuf.append(parentDescString());
				sigStringBuf
						.append(", the numbers are so small that the difference is not statistically significant.");
			} else {
				sigStringBuf.append(facetDesc);
				sigStringBuf
						.append(" are about as likely to satisfy the filters as other ");
				sigStringBuf.append(parentDescString());
			}
			break;
		default:
			relationTypeString = chiColorFamily == -1 ? "...is inversely correlated with filters:"
					: "...is correlated with filters:";
			if (delta > -0.25 && delta < 0.25) {
				sigStringBuf.append("Even though the percentage of");
				sigStringBuf.append(facetDesc);
				sigStringBuf.append(" that satisfy the filters is not much ");
				if (delta < -0.25)
					sigStringBuf.append("lower");
				else
					sigStringBuf.append("higher");
				sigStringBuf.append(" than that for other ");
				sigStringBuf.append(parentDescString());
				sigStringBuf
						.append(", the numbers are so large that the difference is statistically significant.");
			} else {
				sigStringBuf.append(facetDesc);
				sigStringBuf.append(" are significantly ");
				if (chiColorFamily == 1)
					sigStringBuf.append("more");
				else
					sigStringBuf.append("less");
				sigStringBuf
						.append(" likely to satisfy the filters than other ");
				sigStringBuf.append(parentDescString());
			}
		}
		pValueString(sigStringBuf);
		significanceTypeDesc.setTextPaint(art.facetTextColor(facet));
		significanceTypeDesc.setText(relationTypeString);
		significanceTypeDesc.setWidth((int) (maxW() / significanceTypeDesc
				.getScale()));
		align(totalBG, 16, significanceTypeDesc, 0, 0, 0);
		significanceDesc.setWidth(maxW());
		significanceDesc.setText(sigStringBuf.toString());
		align(significanceTypeDesc, 16, significanceDesc, 0, 0, 0);
		PNode[] sigChildren = { significanceTypeDesc, significanceDesc };
		setBoundsFromNodes(significanceHeader, sigChildren, MARGIN);
		// setBoundsFromNode(sigBG, significanceDesc, 0);

		PBounds sigBounds = new PBounds(barBounds.getMaxX() + MARGIN, Math.min(
				barBounds.y, 0.5), 2 * MARGIN, Math.abs(barBounds.y - 0.5));
		Rectangle2D globalDeltaBounds = anchor.getParent().localToGlobal(
				sigBounds);
		heavyLines.setBounds(heavyLines.getParent().globalToLocal(
				globalDeltaBounds));
		iBeam(heavyLines);
		stretchLine(sigLines, heavyLines, 10, significanceDesc, 8, MARGIN);
	}

	// void setAnchor(PNode _anchor) {
	// // anchor can be a bar, a pv (for median arrow), null (for cluster), or
	// // a facetLabel
	// anchor = _anchor;
	// }

	private boolean animationFinished() {
		return showHelp.compareTo(Step.HELP_SIG) == 0;
	}

	List animationJobs = new Vector();

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

	// I think this finishAnimation is getting called recursively (via
	// terminate),
	// which gives a concurrentModificationException, so prevent it with this
	// flag
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
			} finally {
				finishingAnimation = false;
			}
			// animationJobs.clear();
		}
	}

	private boolean maybeExit(int animatingPerformance) {
		boolean result = animatingPerformance != performance;
		if (result)
			exit();
		return result;
	}

	boolean exit() {
		facet = null;
		// oldUpdate = false;
		boolean isHelp = isHelping();
		showHelp = Step.NO_POPUP;
		performance++;
		animationSpeed = 0;
		finishAnimation();
		summaryBG.removeFromParent();
		if (isHelp) {
			art.summary.computeRankComponentHeights();
			animateBarTransparencies(1, 0, 0);
			totalCountDescNum.setScale(1);
			facetCountDescNum.setScale(1);
			namePrefix.setTransparency(1);
			name.setTransparency(1);
			spacebarDesc.setTransparency(1);
			animateFacetWidgetTransparency(1);
			animateParentWidgetTransparency(1);
			animateTotalWidgetTransparency(1);
//			mungePV();
		}
		if (getVisible()) {
			setVisible(false);
			return true;
		}
		return false;
	}

//	private void mungePV() {
//		if (anchor != null && anchor instanceof Bar) {
//			PerspectiveViz pv = pv();
//			rank.computeHeights();
//			pv.prevH = 9999;
//			pv.layoutChildren();
//			pv().layoutPercentLabels();
//		}
//	}
}
