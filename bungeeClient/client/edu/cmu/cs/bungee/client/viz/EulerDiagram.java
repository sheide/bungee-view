package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.tetrad.Distribution;
import edu.cmu.cs.bungee.client.query.tetrad.GraphicalModel;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;

class EulerDiagram extends LazyPPath {

	private static final double MARGIN = 6.0;
	private static final double ATOMIC_BLOCK_SIZE = 200.0;
	static final Paint textColor = Color.white;
	static final Paint bgColor = Color.black;
	static final Color[] primaryColors = { Color.blue, Color.orange, Color.cyan };

	Font font;
	public Paint borderColor = Color.orange;
	final List<ItemPredicate> nonPrimary;
	final List<ItemPredicate> primary;
	final Distribution distribution;
	private String label;

	EulerDiagram(Distribution observed, List<ItemPredicate> primary, List<ItemPredicate> usedFacets,
			Font font, String label) {
		if (!observed.getFacets().equals(usedFacets)) {
			observed = observed.getMarginalDistribution(usedFacets);
		}
		// Util.print("EulerDiagram  " + primary + " " + observed);
		this.label = label;
		this.primary = primary;
		this.distribution = observed;
		nonPrimary = new ArrayList<ItemPredicate>(observed.getFacets());
		assert nonPrimary.containsAll(primary) : observed + " " + primary;
		nonPrimary.removeAll(primary);
		if (font == null)
			font = PText.DEFAULT_FONT;
		this.font = font;
		draw();
		setWidth(0);
	}

	public void redraw() {
		draw();
		// printMe("Final");
	}

	public double setLabel() {
		APText l0 = APText.oneLineLabel(font);
		addChild(l0);
		l0.setTextPaint(Color.white);
		l0.setText(label);
		l0.setOffset(MARGIN, MARGIN);
		double bottom = l0.getIntMaxY();

		if (primary.size() > 0) {
			APText l1 = APText.oneLineLabel(font);
			addChild(l1);
			l1.setTextPaint(primaryColors[0]);
			l1.setText(primary.get(0).getName());
			l1.setOffset(MARGIN, MARGIN + l0.getIntMaxY());
			bottom = l1.getIntMaxY();

			if (primary.size() > 1) {
				APText l2 = APText.oneLineLabel(font);
				addChild(l2);
				l2.setTextPaint(primaryColors[1]);
				l2.setText(primary.get(1).getName());
				l2.setOffset(MARGIN, MARGIN + l1.getIntMaxY());
				bottom = l2.getIntMaxY();
			}
		}
		return MARGIN + bottom;
	}

	private void draw() {
		removeAllChildren();
		// System.out.println("draw");
		setPaint(bgColor);
		setStrokePaint(borderColor);
		double y = setLabel();

		EulerBlock block = new EulerBlock(new LinkedList<Boolean>());

		addChild(block);
		block.setYoffset(y);
		// block.setOffset(MARGIN, MARGIN + title.getIntMaxY());
		// setWidth(Math.max(title.getWidth(), block.getWidth()));
		setBounds(0, 0, block.getIntMaxX(), block.getIntMaxY());
		// Util.print("CC " + getWidth() + block);
		// edu.cmu.cs.bungee.piccoloUtils.gui.Util.printDescendents(this);
	}

	@Override
	public boolean setWidth(double w) {
		double Y_MARGIN = 8;
		double outlineW = MARGIN;
		reset();
		PBounds pb = getFullBounds();
		double xMargin = Math.max(Y_MARGIN, (w - pb.getWidth()) / 2) - outlineW
				/ 2;
		double yMargin = Y_MARGIN;
		float x0 = (float) (pb.getX() - xMargin);
		float y0 = (float) (pb.getY() - yMargin);
		float x1 = (float) (x0 + pb.getWidth() + 2 * xMargin);
		float y1 = (float) (y0 + pb.getHeight() + 2 * yMargin);
		float[] xs = { x0, x1, x1, x0, x0 };
		float[] ys = { y0, y0, y1, y1, y0 };
		// setBounds(x0, y0, x1, y1);
		setStroke(LazyPPath.getStrokeInstance((int) outlineW));
		setPathToPolyline(xs, ys);
		return true;
	}

	double r(Distribution dist) {
		double[] d = dist.getDistribution();
		assert d.length == 4;
		double p0x = d[0] + d[1];
		double p1x = d[2] + d[3];
		double px0 = d[0] + d[2];
		double px1 = d[1] + d[3];
		double result = Math.sqrt(1 - (d[0] * d[1] / p0x + d[2] * d[3] / p1x)
				/ (px0 * px1));
		if (Double.isNaN(result))
			result = 0;

		// int[] cnts = dist.getCounts();
		// ChiSq2x2 chisq = ChiSq2x2.getInstance(null, dist.getTotalCount(),
		// cnts[3] + cnts[1], cnts[3] + cnts[2], cnts[3], null);
		// Util.print("r " + d[3] + " - " + (d[3] + d[1]) * (d[3] + d[2]) +
		// " = "
		// + (d[3] - (d[3] + d[1]) * (d[3] + d[2])) + " " + dist + " "
		// + Util.valueOfDeep(d) + " " + chisq.correlation() + "\n"
		// + chisq.printTable());

		if (d[3] < (d[3] + d[1]) * (d[3] + d[2]))
			result = -result;
		return result;
	}

	private class EulerBlock extends LazyPNode {

		public EulerBlock getInstance(List<Boolean> nonPrimaryStates) {
			// Util.print("EB " + nonPrimary + " " + nonPrimaryStates + " "
			// + primary);
			EulerBlock atomic = new EulerBlock(nonPrimaryStates);
			EulerBlock result = atomic;
			if (nonPrimaryStates.size() >= nonPrimary.size()) {
				result = new EulerBlock();
				result.addChild(atomic);
			}
			return result;
		}

		EulerBlock(List<Boolean> nonPrimaryStates) {
			// Util.print("EB " + nonPrimary + " " + nonPrimaryStates + " "
			// + primary);
			if (nonPrimaryStates.size() < nonPrimary.size()) {
				compositeEulerBlock(nonPrimaryStates);
			} else {
				setBounds(0, 0, ATOMIC_BLOCK_SIZE, ATOMIC_BLOCK_SIZE);
				EulerBlock prim = new EulerBlock();
				prim.primaryEulerBlock(nonPrimaryStates);
				addChild(prim);
			}
		}

		private EulerBlock() {
			setBounds(0, 0, ATOMIC_BLOCK_SIZE, ATOMIC_BLOCK_SIZE);
		}

		private void primaryEulerBlock(List<Boolean> nonPrimaryStates) {
			assert primary.size() >= 2 && primary.size() <= 3 : "Need to write more code for primary.size = "
					+ primary.size() + ": " + primary;
			List<ItemPredicate> falseFacets = new LinkedList<ItemPredicate>();
			List<ItemPredicate> trueFacets = new LinkedList<ItemPredicate>();
			for (int i = 0; i < nonPrimaryStates.size(); i++) {
				ItemPredicate nonPrim = nonPrimary.get(i);
				Boolean state = nonPrimaryStates.get(i);
				if (state != null) {
					if (state.booleanValue())
						trueFacets.add(nonPrim);
					else
						falseFacets.add(nonPrim);
				}
			}
			Distribution conditionalDistribution = distribution
					.getConditionalDistribution(falseFacets, trueFacets);
			if (conditionalDistribution != null) {
				setPaint(textColor);
				setBounds(0, 0, 1, 1);
				List<ItemPredicate> firstTwo = primary.subList(0, 2);
				Distribution marginal = new Distribution(firstTwo,
						conditionalDistribution.getMarginalCounts(firstTwo));
				int[] counts = marginal.getCounts();
				double total = marginal.getTotalCount();
				setScale(ATOMIC_BLOCK_SIZE
						* Math.pow(total / distribution.getTotalCount(), 0.5));
				double pct12 = counts[3] / total;
				double pct1 = pct12 + counts[1] / total;
				double pct2 = pct12 + counts[2] / total;
				double h1 = pct1;
				double w1 = 1;
				boolean positivelylCorrelated = pct1 * pct2 <= pct12;
				double w2 = positivelylCorrelated ? pct12 / pct1
						: (pct2 - pct12) / (1 - pct1);
				if (Double.isNaN(w2) && pct1 * pct2 == pct12)
					w2 = (pct2 - pct12) / (1 - pct1);
				double h2 = pct2 == 0 ? 0 : pct2 / w2;
				assert h2 <= 1 && w2 < 1.00000000001 : positivelylCorrelated
						+ " " + pct12 + " " + pct1 + " " + pct2 + " "
						+ Util.valueOfDeep(counts) + " " + h2 + " " + w2;
				addRect(0, 1 - h1, w1, h1, 0);
				addRect(0, positivelylCorrelated ? 1 - h2 : 0, w2, h2, 1);

				// Util.print(" EB " + positivelylCorrelated + " " + h1 + "x" +
				// w1
				// + ", " + h2 + "x" + w2 + " " + pct1);
				// + Util.valueOfDeep(counts) + " " + positivelylCorrelated
				// + " " + h1 + " " + h2 + " " + w2);
				assert Util.approxEquals(h1 * w1, pct1);
				assert Util.approxEquals(h2 * w2, pct2) : (h2 * w2) + " "
						+ pct2;
				{
					// Rectangle2D intersection = r1.getBounds()
					// .createIntersection(r2.getBounds());
					// assert Util.approxEquals(intersection.getWidth()
					// * intersection.getHeight(), pct12) : intersection
					// .getWidth()
					// + " "
					// + intersection.getHeight()
					// + " "
					// + (intersection.getWidth() * intersection
					// .getHeight()) + " " + pct12;
				}
				APText l1 = APText.oneLineLabel(font);
				addChild(l1);
				l1.setTextPaint(Color.red);
				l1.setScale(1.0 / getScale());
				l1.setText(GraphicalModel.formatWeight(r(marginal)));
				// Util.print(" EB " + l1.getText());

				if (primary.size() == 3) {
					int[] cnts = conditionalDistribution
							.getMarginalCounts(primary);

					Util.print(Util.valueOfDeep(counts) + "\n"
							+ Util.valueOfDeep(cnts));
					Util.print(p(cnts, 1) + " " + p(cnts, 4) + " " + p(cnts, 3)
							+ " " + p(cnts, 6));

					double x3 = w2 * p(cnts, 1);
					addRect(x3, 0, w2 - x3, 1 - h1, 2);

					double x4 = (1 - w2) * p(cnts, 4);
					addRect(w2, 0, x4, 1 - h1, 2);

					double x5 = w2 * p(cnts, 3);
					addRect(x5, 1 - h1, w2 - x5, h1, 2);

					double x6 = (1 - w2) * p(cnts, 6);
					addRect(w2, 1 - h1, x6, h1, 2);
				}
			}
		}

		private double p(int[] counts, int index) {
			double num = counts[index];
			if (num == 0)
				return num;
			int index2 = index > 3 ? index - 4 : index + 4;
			return num / (num + counts[index2]);
		}

		private LazyPNode addRect(double x, double y, double w, double h,
				int color) {
			double minSize = 2.0 / getScale();
			w = Math.max(minSize, w);
			h = Math.max(minSize, h);
			x = Math.min(x, getWidth() - w);
			y = Math.min(y, getHeight() - h);
			LazyPNode r3 = new LazyPNode();
			r3.setBounds(x, y, w, h);
			r3.setPaint(primaryColors[color]);
			r3.setTransparency(0.5f);
			addChild(r3);
			// Util.print("ar " + r3.getGlobalBounds());
			return r3;
		}

		private void compositeEulerBlock(List<Boolean> nonPrimaryStates) {
			nonPrimaryStates.add(Boolean.FALSE);
			EulerBlock left = new EulerBlock(nonPrimaryStates);
			nonPrimaryStates.remove(nonPrimaryStates.size() - 1);
			nonPrimaryStates.add(Boolean.TRUE);
			EulerBlock right = new EulerBlock(nonPrimaryStates);
			nonPrimaryStates.remove(nonPrimaryStates.size() - 1);
			nonPrimaryStates.add(null);
			EulerBlock either = new EulerBlock(nonPrimaryStates);
			nonPrimaryStates.remove(nonPrimaryStates.size() - 1);
			addChild(left);
			addChild(right);
			addChild(either);

			String name = nonPrimary.get(nonPrimaryStates
					.size()).getName();

			APText l1 = APText.oneLineLabel(font);
			addChild(l1);
			l1.setTextPaint(textColor);
			l1.setText("~ " + name);

			APText l2 = APText.oneLineLabel(font);
			addChild(l2);
			l2.setTextPaint(textColor);
			l2.setText(name);

			APText l3 = APText.oneLineLabel(font);
			addChild(l3);
			l3.setTextPaint(textColor);
			l3.setText("Either");

			if (nonPrimaryStates.size() % 2 == 0) {
				l1.setOffset(0, (left.getIntMaxY() - l1.getHeight()) / 2);
				left.setOffset(MARGIN + l1.getIntMaxX(), 0);
				right.setOffset(left.getXOffset(), left.getIntMaxY() + MARGIN);
				l2.setOffset(l1.getXOffset(), right.getYOffset()
						+ (right.getHeight() * right.getScale() - l2
								.getHeight()) / 2);
				either.setOffset(right.getXOffset(), right.getIntMaxY()
						+ MARGIN);
				l3.setOffset(l2.getXOffset(), either.getYOffset()
						+ (either.getHeight() * either.getScale() - l3
								.getHeight()) / 2);
			} else {
				l1.setOffset(0, 0);
				left.setOffset(0, l1.getIntMaxY() + MARGIN);
				right.setOffset(left.getIntMaxX() + MARGIN, left.getYOffset());
				l2.setOffset(right.getXOffset(), l1.getYOffset());
				either.setOffset(right.getIntMaxX() + MARGIN, right
						.getYOffset());
				l3.setOffset(either.getXOffset(), l2.getYOffset());
				l1.setConstrainWidthToTextWidth(false);
				l2.setConstrainWidthToTextWidth(false);
				l3.setConstrainWidthToTextWidth(false);
				l1.setWidth(left.getWidth());
				l2.setWidth(right.getWidth());
				l3.setWidth(either.getWidth());
			}
			setBoundsFromFullBounds();
		}
	}

}
