package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.javaExtensions.StringAlign;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Arrow;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.cmu.tetrad.data.ColtDataSet;
import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.data.RectangularDataSet;
import edu.cmu.tetrad.data.Variable;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.IndTestChiSquare;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.LogisticRegression;
import edu.cmu.tetrad.search.LogisticRegressionResult;
import edu.cmu.tetrad.search.PcSearch;
import edu.cmu.tetrad.search.RegressionResult;
import edu.cmu.tetrad.util.PermutationGenerator;
import edu.cmu.tetrad.util.TetradLogger;
import edu.cmu.tetradapp.model.DataWrapper;
import edu.cmu.tetradapp.model.RegressionParams;
import edu.cmu.tetradapp.model.RegressionRunner;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * @author mad
 * 
 */
class Tetrad extends LazyPPath {
	private static final int GRAPH_EDGE_LENGTH = 100;
	private static final double ALPHA = 0.001;
	private final Perspective popupFacet;
	private final Bungee art;
	private final Graph graph;

	int[] counts;
	/**
	 * The variable names in the discrete data sets for which conditional
	 * independence judgements are desired. Maps from String to Perspective
	 * 
	 * @serial
	 */
	private final Map variables = new LinkedHashMap();
	private final ColtDataSet data;
	private APText title;

	Tetrad(Perspective facet, Bungee _art) {
		TetradLogger.getInstance().setLogging(true);
		TetradLogger.getInstance().addOutputStream(System.out);
		TetradLogger.getInstance().setForceLog(true);
		popupFacet = facet;
		art = _art;
		// setPaint(Color.white);
		variables.put(tetradName(popupFacet), popupFacet);
		for (Iterator it = query().allRestrictions().iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			variables.put(tetradName(p), p);
		}
		getCounts();
		data = getData(Collections.EMPTY_LIST);
		graph = getGraph();
		draw();
	}

	Bungee art() {
		return art;
	}

	Query query() {
		return popupFacet.query();
	}

	Perspective getUniverse() {
		Perspective result = popupFacet.getParent();
		if (result == null)
			result = popupFacet;
		return result;
	}

	List facetsOfInterest() {
		return new ArrayList(variables.values());
	}

	void draw() {
		Util.print(graph);
		setPaint(Color.black);
		int ARROW_SIZE = 8;
		Map nodeMap = new Hashtable();
		// Node popupNode = graph.getNode(tetradName(popupFacet));
		List adjacent = graph.getEdges();
		for (Iterator it = graph.getNodes().iterator(); it.hasNext();) {
			Node node = (Node) it.next();
			getPNode(node, nodeMap);
		}
		for (Iterator it = adjacent.iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			Node node1 = edge.getNode1();
			Node node2 = edge.getNode2();
			if (edge.pointsTowards(node1)) {
				Node temp = node1;
				node1 = node2;
				node2 = temp;
			}
			boolean pointsToNode1 = edge.getProximalEndpoint(node1) == Endpoint.ARROW;
			boolean pointsToNode2 = edge.getProximalEndpoint(node2) == Endpoint.ARROW;
			APText pnode1 = getPNode(node1, nodeMap);
			APText pnode2 = getPNode(node2, nodeMap);
			Perspective cause = lookupFacet(node1);
			Perspective caused = lookupFacet(node2);
			// double weight = getCorrelation(p1, p2);
			String label = formatWeight(getBetaWeight(cause, caused));
			if (pointsToNode1 == pointsToNode2) {
				label += "/" + formatWeight(getBetaWeight(caused, cause));
			}

			Line2D centerToCenter = new Line2D.Double(pnode1.getCenterX(),
					pnode1.getCenterY(), pnode2.getCenterX(), pnode2
							.getCenterY());
			Point2D point1 = getIntersection(centerToCenter, pnode1);
			Point2D point2 = getIntersection(centerToCenter, pnode2);

			Arrow arrow = new Arrow(Color.white, ARROW_SIZE, 1);
			arrow.addLabel(label, art().font);
			arrow.setVisible(Arrow.LEFT_TAIL, false);
			arrow.setVisible(Arrow.LEFT_HEAD, pointsToNode1);
			arrow.setVisible(Arrow.RIGHT_HEAD, pointsToNode2);
			arrow.setEndpoints(point1.getX(), point1.getY(), point2.getX(),
					point2.getY());
			addChild(arrow);
		}
		title = art().oneLineLabel();
		title.setTextPaint(Color.white);
		title.setText(nodeMap.size() > 0 ? "Influence Diagram"
				: "No dependencies");
		addChild(title);
		setWidth(0);
	}

	public boolean setWidth(double w) {
		double Y_MARGIN = 8;
		double outlineW = PopupSummary.MARGIN;
		reset();
		PBounds pb = getFullBounds();
		// double x = -bounds.getX();
		// double y = -bounds.getY();
		// for (Iterator it = getChildrenIterator(); it.hasNext();) {
		// PNode child = (PNode) it.next();
		// child.translate(x,y);
		// }
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
		setStrokePaint(Bungee.goldBorderColor);
		setPathToPolyline(xs, ys);
		title.setOffset(Math.round(x0 + 2 * outlineW), Math.round(y0 + 2
				* outlineW));
		return true;
	}

	String formatWeight(double weight) {
		return Integer.toString((int) Math.rint(100 * weight));
	}

	// String printLine(Line2D line) {
	// return "<Line " + line.getP1() + "-" + line.getP2() + ">";
	// }

	private Point2D getIntersection(Line2D segment, APText pnode1) {
		double x1 = pnode1.getXOffset();
		double y1 = pnode1.getYOffset();
		double x2 = x1 + pnode1.getWidth();
		double y2 = y1 + pnode1.getHeight();
		Point2D result = getIntersection(segment, new Line2D.Double(x1, y1, x2,
				y1));
		if (result == null)
			result = getIntersection(segment, new Line2D.Double(x2, y1, x2, y2));
		if (result == null)
			result = getIntersection(segment, new Line2D.Double(x1, y2, x2, y2));
		if (result == null)
			result = getIntersection(segment, new Line2D.Double(x1, y1, x1, y2));
		assert result != null : segment + " " + x1 + " " + y1 + " " + x2 + " "
				+ y2;
		return result;
	}

	private int[] getCounts() {
		if (counts == null) {
			counts = new int[1 << nFacets()];
			Perspective parent = getUniverse();
			ResultSet rs = art().query
					.onCountMatrix(facetsOfInterest(), parent);
			int i = 0;
			try {
				while (rs.next()) {
					counts[i++] = rs.getInt(1);
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			Util.print("facetsOfInterest: " + facetsOfInterest());
			Util.print("counts: " + Util.valueOfDeep(counts));
			Util.print("nRows: " + parent.getTotalCount() + " for " + parent);

			// The low order bit represents the first facet (popupFacet)
			assert getCountInUniverse(0) == popupFacet.getTotalCount() : popupFacet
					+ " "
					+ popupFacet.getTotalCount()
					+ " "
					+ getCountInUniverse(0);
		}
		return counts;
	}

	private int getCountInUniverse(int varIndex) {
		assert varIndex >= 0 && varIndex < nFacets();
		int total = 0;
		for (int j = 0; j < counts.length; j++) {
			if (isBit(j, varIndex))
				total += counts[j];
		}
		return total;
	}

	// private ChiSq2x2 getChiSq2x2(Perspective p1, Perspective p2) {
	// getCounts();
	// Perspective[] row = { p1 };
	// Perspective[] col = { p2 };
	// Perspective[] both = { p1, p2 };
	// return ChiSq2x2.getInstance(null, getUniverse().getTotalCount(),
	// marginalCount(row), marginalCount(col), marginalCount(both));
	// }
	//
	// private int marginalCount(Perspective[] facets) {
	// int pattern = 0;
	// for (int i = 0; i < facets.length; i++) {
	// pattern |= 1 << (facetsOfInterest().tailSet(facets[i]).size() - 1);
	// }
	// int count = 0;
	// for (int i = 0; i < counts.length; i++) {
	// if ((i & pattern) == pattern)
	// count += counts[i];
	// }
	// return count;
	// }

	Perspective lookupFacet(Node node) {
		return (Perspective) variables.get(node.getName());
	}

	private String tetradName(Perspective p) {
		return p.getName().replace(' ', '_');
	}

	private ColtDataSet getData(Collection facetsToStandardize) {
		List categories = new ArrayList(2);
		categories.add("0");
		categories.add("1");
		int nCols = nFacets();
		List vars = new ArrayList(nCols);
		double[] means = new double[nCols];
		double[] stdDevs = new double[nCols];
		double universe = getUniverse().getTotalCount();
		int index = 0;
		for (Iterator it = facetsOfInterest().iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			if (facetsToStandardize.contains(facet)) {
				double count = getCountInUniverse(index);
				means[index] = count / universe;
				stdDevs[index] = Math.sqrt(2 * count * (universe - count)
						/ (universe * (universe - 1)));
				Util.print("gg " + index + " " + count + " " + universe + " "
						+ means[index] + " " + stdDevs[index]);
			} else {
				stdDevs[index] = -1;
			}
			index++;
		}
		for (Iterator it = facetsOfInterest().iterator(); it.hasNext();) {
			Perspective facet = (Perspective) it.next();
			String name = tetradName(facet);
			Variable variable;
			if (facetsToStandardize.contains(facet)) {
				variable = new ContinuousVariable(name);
			} else {
				variable = new DiscreteVariable(name, categories);
				((DiscreteVariable) variable)
						.setAccommodateNewCategories(false);
			}
			vars.add(variable);
		}
		ColtDataSet data1 = new ColtDataSet(getUniverse().getTotalCount(), vars);
		int row = 0;
		for (int condition = 0; condition < counts.length; condition++) {
//			double[] sum = new double[nCols];
			for (int item = 0; item < counts[condition]; item++) {
				for (int col = 0; col < nCols; col++) {
					int colValue = getBit(condition, col);
					if (stdDevs[col] >= 0) {
						double standardized = (colValue - means[col])
								/ stdDevs[col];
						data1.setDouble(row, col, standardized);
					} else {
						// multiplier may work for chi square independence test
						data1.setInt(row, col, colValue);
					}
//					sum[col] += data1.getDouble(row, col);
				}
				row++;
			}
//			Util.print("sums: " + condition + " " + counts[condition] + " "
//					+ Util.valueOfDeep(sum));
		}
//		Util.print(data1);
		return data1;

	}

	private double getBetaWeight(Perspective cause, Perspective caused) {
		MyRegressionResult regression = getRegression(caused, false);
		int index = Util.member(regression.params.regressorNames,
				tetradName(cause));
		double result = regression.coefs[index + 1];
		// Util.print(" beta " + tetradName(cause) + " => " + tetradName(caused)
		// + ": " + result);
		return result;
	}

	private MyRegressionResult getRegression(Perspective p, boolean isLogistic) {
		MyRegressionParams params = getRegressionParams(p);
		double[] coefs;
		if (isLogistic) {
			LogisticRegressionResult regression = getLogisticRegression(p,
					params);
			coefs = regression.getCoefs();
		} else {
			RegressionResult regression = getLinearRegression(p, params);
			coefs = regression.getCoef();
			// Util.printDeep(coefs);
			// for (int i = 1; i < coefs.length; i++) {
			// Perspective cause = (Perspective) variables
			// .get(params.regressorNames[i - 1]);
			// ChiSq2x2 chisq = getChiSq2x2(cause, p);
			// coefs[i] *= Math.sqrt(chisq.sampleVariance(ChiSq2x2.ROW)
			// / chisq.sampleVariance(ChiSq2x2.COL));
			// }
			// Util.printDeep(coefs);
		}
		return new MyRegressionResult(params, coefs);
	}

	private class MyRegressionResult {
		MyRegressionParams params;
		double[] coefs;

		MyRegressionResult(MyRegressionParams _params, double[] _coefs) {
			params = _params;
			coefs = _coefs;
		}
	}

	private List getCauses(Perspective p) {
		List result = new LinkedList();
		List adjacent = graph.getEdges();
		for (Iterator it = adjacent.iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			Node distalNode = null;
			if (edge.getNode1().getName().equals(tetradName(p)))
				distalNode = edge.getNode2();
			else if (edge.getNode2().getName().equals(tetradName(p)))
				distalNode = edge.getNode1();
			if (distalNode != null && !edge.pointsTowards(distalNode))
				result.add(lookupFacet(distalNode));
		}
		return result;
	}

	private class MyRegressionParams {
		double[][] regressors;
		String[] regressorNames;
		double[] target;

		MyRegressionParams(double[][] _regressors, String[] _regressorNames,
				double[] _target) {
			regressors = _regressors;
			regressorNames = _regressorNames;
			target = _target;
		}
	}

	private MyRegressionParams getRegressionParams(Perspective p) {
		List varsToKeep = new LinkedList();
		for (Iterator it = getCauses(p).iterator(); it.hasNext();) {
			Perspective cause = (Perspective) it.next();
			varsToKeep.add(tetradName(cause));
		}
		assert varsToKeep.size() > 0 : p;
		RectangularDataSet regressorsDataSet = new ColtDataSet(data);
		for (Iterator it = data.getVariables().iterator(); it.hasNext();) {
			Variable variable = (Variable) it.next();
			if (!varsToKeep.contains(variable.getName()))
				regressorsDataSet.removeColumn(variable);
		}

		Object[] namesObj = (regressorsDataSet.getVariableNames()).toArray();
		String[] names = new String[namesObj.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = (String) namesObj[i];
		}

		// Get the list of regressors selected by the user
		String[] regressorNames = names; // All names except the target

		int ncases = regressorsDataSet.getNumRows();
		int nvars = regressorsDataSet.getNumColumns();

		double[][] regressors = new double[nvars][ncases];

		for (int i = 0; i < nvars; i++) {
			for (int j = 0; j < ncases; j++) {
				regressors[i][j] = regressorsDataSet.getDouble(j, i);
			}
		}

		// target is the array storing the values of the target variable
		double[] target = new double[ncases];
		int targetIndex = data.getVariables().indexOf(
				data.getVariable(tetradName(p)));
		// Util.print(data.getVariableNames());
		// Util.print(p + " targetIndex = " + targetIndex);
		// int n = 0;
		for (int j = 0; j < ncases; j++) {
			target[j] = data.getDouble(j, targetIndex);
			// if (target[j] == 1)
			// n++;
		}
		// Util.print("nOn = " + n);
		return new MyRegressionParams(regressors, regressorNames, target);
	}

	private RegressionResult getLinearRegression(Perspective p,
			MyRegressionParams params) {
		ColtDataSet data2 = getData(facetsOfInterest());
		List regressors = new ArrayList(params.regressorNames.length);
		for (int i = 0; i < params.regressorNames.length; i++) {
			regressors.add(data2.getVariable(params.regressorNames[i]));
		}

		DataWrapper dataWrapper = new DataWrapper(data2);

		RegressionParams params1 = new RegressionParams();
		params1.setRegressorNames(params.regressorNames);
		params1.setTargetName(tetradName(p));
		params1.setAlpha(ALPHA);

		RegressionRunner runner = new RegressionRunner(dataWrapper, params1);
		runner.execute();
		RegressionResult regressionResult = runner.getResult();

		// Util.print(regressionResult);
		return regressionResult;
	}

	private LogisticRegressionResult getLogisticRegression(Perspective p,
			MyRegressionParams params) {
		LogisticRegression regression = new LogisticRegression();
		regression.setRegressors(params.regressors);
		regression.setVariableNames(params.regressorNames);
		regression.setAlpha(ALPHA);

		String report = regression.regress(params.target, tetradName(p));
		Util.print(report);
		LogisticRegressionResult regressionResult = regression.getResult();
		return regressionResult;
	}

	private Graph getGraph() {
		IndependenceTest independenceTest = new IndTestChiSquare(data, ALPHA);
		Knowledge knowledge = new Knowledge();
		for (Iterator it = facetsOfInterest().iterator(); it.hasNext();) {
			Perspective caused = (Perspective) it.next();
			int tier = caused.isCausable() ? 1 : 0;
			knowledge.addToTier(tier, tetradName(caused));
			if (tier == 1) {
				for (Iterator it2 = facetsOfInterest().iterator(); it2
						.hasNext();) {
					Perspective cause = (Perspective) it2.next();
					if (cause != caused && noCause(cause, caused))
						knowledge.setEdgeForbidden(tetradName(cause),
								tetradName(caused), true);
				}
			}
		}
		knowledge.setTierForbiddenWithin(0, true);
		Util.print("knowledge " + knowledge);
		PcSearch pcSearch = new PcSearch(independenceTest, knowledge);
		// pcSearch.setDepth(5);
		Graph graph1 = pcSearch.search();
		// Util.print("graph: " + graph1);

		// new LayeredDrawing(graph).doLayout();

		// GraphUtils.arrangeInCircle(graph, 0, 300, 200);

		// boolean randomlyInitialized = true;
		// double naturalEdgeLength = 90;
		// double springConstant = 1;
		// double stopEnergy = 0.01;
		// GraphUtils.kamadaKawaiLayout(graph, randomlyInitialized,
		// naturalEdgeLength, springConstant, stopEnergy);

		tryLayoutPermutations(graph1);
		return graph1;
	}

	/**
	 * 
	 * There are 2**(nFacets - 2) subsets of the other variables, and for every
	 * value assignment to every such subsets, we want to compute the odds ratio
	 * for caused given cause versus caused given not cause.
	 * 
	 * The set bits in the binary representation of conditioningVars determines
	 * the subset.
	 * 
	 * The subset is make explicit in substateIndexes (and its complement in
	 * marginIndexes).
	 * 
	 * substate ranges over the possible value assignments for each subset.
	 * 
	 * The odds ratio is computed from the marginal counts for each of the four
	 * assignments to cause and caused, superimposed on the substate.
	 * 
	 * @param cause
	 *            conditioning variable for odds ratio test
	 * @param caused
	 *            the variable to compute the odds ratio for
	 * @return whether Pr(p2|p) is nearly equal to Pr(p2) for every state of
	 *         every other variable.
	 */
	private boolean noCause(Perspective cause, Perspective caused) {
		Perspective[] facets = (Perspective[]) facetsOfInterest().toArray(
				new Perspective[nFacets()]);
		int causeIndex = Util.member(facets, cause);
		int causedIndex = Util.member(facets, caused);
		boolean causes = false;
		double bestOddsRatio = 1;

		int nConditioningVars = nFacets() - 2;
		int bestState = 0;
		int nSubsets = 1 << nConditioningVars;
		for (int conditioningVars = 0; conditioningVars < nSubsets && !causes; conditioningVars++) {
			int[] marginIndexes = new int[0];
			int[] substateIndexes = new int[0];
			for (int var = 0; var < nConditioningVars; var++) {
				int varIndex = var;
				if (varIndex >= Math.min(causeIndex, causedIndex))
					varIndex++;
				if (varIndex >= Math.max(causeIndex, causedIndex))
					varIndex++;
				if (isBit(conditioningVars, var)) {
					marginIndexes = Util.push(marginIndexes, varIndex);
				} else {
					substateIndexes = Util.push(substateIndexes, varIndex);
				}
			}
//			Util.print("cc " + causeIndex + " " + causedIndex + " "
//					+ conditioningVars + " "
//					+ Util.valueOfDeep(substateIndexes));
			int nSubstateVars = nConditioningVars - marginIndexes.length;
			int nSubstates = 1 << nSubstateVars;
			for (int substate = 0; substate < nSubstates && !causes; substate++) {
				int state = setSubstate(0, substate, substateIndexes);
				double a = marginCounts(setBit(setBit(state, causeIndex, true),
						causedIndex, true), marginIndexes);
				double b = marginCounts(setBit(state, causeIndex, true),
						marginIndexes);
				double c = marginCounts(setBit(state, causedIndex, true),
						marginIndexes);
				double d = marginCounts(state, marginIndexes);
				if (a + b > 10 && c + d > 10) {
					double oddsRatio = a * d / b / c;
					double absOdds = oddsRatio < 1 ? 1 / oddsRatio : oddsRatio;
					Util.print("odds (" + state + ") "
							+ Util.valueOfDeep(marginIndexes) + " " + oddsRatio
							+ " " + a + " " + b + " " + c + " " + d);
					assert oddsRatio != 1;
					if (absOdds > bestOddsRatio) {
						bestOddsRatio = absOdds;
						bestState = state;
						if (absOdds > 2)
							causes = true;
					}
				}
			}
		}
		Util.print("Can Cause " + cause + " => " + caused + " "
				+ +bestOddsRatio + " " + bestState);
		return !causes;
	}

	int setSubstate(int state, int substate, int[] indexes) {
		for (int subvar = 0; subvar < indexes.length; subvar++) {
			state = setBit(state, indexes[subvar], isBit(substate, subvar));
		}
		// Util.print("setState " + substate + " " + Util.valueOfDeep(indexes)
		// + " " + state);
		return state;
	}

	// static double oddsRatio(double p, double q) {
	// return p * (1 - q) / q / (1 - p);
	// }

	private int nFacets() {
		return variables.size();
	}

	int marginCounts(int state, int[] marginIndexes) {
		int sum = 0;
		for (int i = 0; i < 1 << marginIndexes.length; i++) {
			for (int j = 0; j < marginIndexes.length; j++) {
				state = setBit(state, marginIndexes[j], isBit(i, j));
				// Util.print("hh "+i+" "+j+" "+state+" "+isBit(i, j));
			}
			sum += counts[state];
		}
		// Util.print("marginCounts " + state + " "
		// + Util.valueOfDeep(marginIndexes) + " " + sum);
		return sum;
	}

	int getBit(int i, int bit) {
		return (i >> bit) & 1;
	}

	boolean isBit(int i, int bit) {
		return getBit(i, bit) == 1;
	}

	int setBit(int i, int bit, boolean state) {
		int mask = 1 << bit;
		if (state) {
			i |= mask;
		} else {
			i &= ~mask;
		}
		return i;
	}

	int state(int index0, int val0, int index1, int val1, int substate) {
		int result = 0;
		int substateBit = 0;
		for (int i = 0; i < nFacets(); i++) {
			int val = 0;
			if (i == index0) {
				val = val0;
			} else if (i == index1) {
				val = val1;
			} else {
				val = getBit(substate, substateBit);
				substateBit++;
			}
			result = setBit(result, i, val == 1);
		}
		return result;
	}

	private APText getPNode(Node node, Map nodeMap) {
		APText pNode = (APText) nodeMap.get(node);
		if (pNode == null) {
			pNode = art().oneLineLabel();
			Perspective facet = lookupFacet(node);
			pNode.setText(facet.getName());
			pNode.setOffset(
					Math.rint(node.getCenterX() - pNode.getWidth() / 2), Math
							.rint(node.getCenterY() - pNode.getHeight() / 2));
			// pNode.setPaint(Color.white);
			// pNode.setScale(0.9);
			pNode.setTextPaint(art().facetTextColor(facet));
			addChild(pNode);
			nodeMap.put(node, pNode);
		}
		return pNode;
	}

	/**
	 * @param graph1
	 * @param causeIndex
	 *            index of proposed left node
	 * @param causedIndex
	 *            index of proposed right node
	 * @return whether this left/right layout is better than the reverse
	 */
	private boolean moreCaused(Graph graph1, int causeIndex, int causedIndex) {
		int causeScore = netCauses(graph1, causeIndex);
		int causedScore = netCauses(graph1, causedIndex);
		boolean result = causeScore == causedScore ? causeIndex < causedIndex
				: causeScore < causedScore;
		// Util.print("moreCaused " + graph1.getNodes().get(causeIndex) + " => "
		// + graph1.getNodes().get(causedIndex) + " = " + result);
		return result;
	}

	private int netCauses(Graph graph1, int nodeIndex) {
		int score = 0;
		Node node = (Node) graph1.getNodes().get(nodeIndex);
		for (Iterator it = graph1.getEdges(node).iterator(); it.hasNext();) {
			Edge edge = (Edge) it.next();
			if (edge.pointsTowards(node))
				score++;
			Node distal = edge.getDistalNode(node);
			if (edge.pointsTowards(distal))
				score--;
		}
		// Util.print("netCauses " + node + " " + score);
		return score;
	}

	void tryLayoutPermutations(Graph graph1) {
		int[] bestPerm = new int[nFacets()];
		int bestEdgeCrossings = Integer.MAX_VALUE;
		for (PermutationGenerator it = new PermutationGenerator(nFacets());;) {
			int[] perm = it.next();
			if (perm == null)
				break;
			if (perm[0] == 0
					&& moreCaused(graph1, perm[perm.length - 1], perm[1])) {
				// Canonicalize by starting at 0 and preferring arrows that
				// point to the right.
				arrangeInCircle(graph1, 0, GRAPH_EDGE_LENGTH,
						GRAPH_EDGE_LENGTH, perm);
				int edgeCrossings = edgeCrossings(graph1);
				if (edgeCrossings < bestEdgeCrossings) {
					bestEdgeCrossings = edgeCrossings;
					System.arraycopy(perm, 0, bestPerm, 0, perm.length);
				}
			}
		}
		arrangeInCircle(graph1, 0, GRAPH_EDGE_LENGTH, GRAPH_EDGE_LENGTH,
				bestPerm);
	}

	/**
	 * Arranges the nodes in the graph clockwise in a circle, starting at 12
	 * o'clock.
	 * 
	 * @param graph1
	 * 
	 * @param centerx
	 * @param centery
	 * @param radius
	 *            The radius of the circle in pixels; a good default is 150.
	 */
	void arrangeInCircle(Graph graph1, int centerx, int centery, int radius,
			int[] perm) {
		List nodes = graph1.getNodes();

		double rad = 6.28 / nodes.size();
		double phi = .75 * 6.28; // start from 12 o'clock.

		for (int i = 0; i < perm.length; i++) {
			Node node = (Node) nodes.get(perm[i]);
			int centerX = centerx + (int) (radius * Math.cos(phi));
			int centerY = centery + (int) (radius * Math.sin(phi));

			node.setCenterX(centerX);
			node.setCenterY(centerY);

			phi += rad;
		}
	}

	private int edgeCrossings(Graph graph1) {
		int nCrosses = 0;
		List edges = graph1.getEdges();
		int index = 0;
		for (Iterator it1 = edges.iterator(); it1.hasNext();) {
			index++;
			Edge edge1 = (Edge) it1.next();
			for (Iterator it2 = edges.subList(index, edges.size()).iterator(); it2
					.hasNext();) {
				Edge edge2 = (Edge) it2.next();
				if (getIntersection(edge1, edge2) != null)
					nCrosses++;
			}
		}
		return nCrosses;
	}

	/**
	 * @param edge1
	 * @param edge2
	 * @return whether the edges cross
	 * 
	 *         see <a
	 *         href="http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/"
	 *         >Intersection point of two lines< /a>
	 */
	private Point2D getIntersection(Edge edge1, Edge edge2) {
		Node node11 = edge1.getNode1();
		Node node12 = edge1.getNode2();
		int x11 = node11.getCenterX();
		int y11 = node11.getCenterY();
		int x12 = node12.getCenterX();
		int y12 = node12.getCenterY();
		Node node21 = edge2.getNode1();
		Node node22 = edge2.getNode2();
		int x21 = node21.getCenterX();
		int y21 = node21.getCenterY();
		int x22 = node22.getCenterX();
		int y22 = node22.getCenterY();
		return getIntersection(new Line2D.Double(x11, y11, x12, y12),
				new Line2D.Double(x21, y21, x22, y22));
	}

	private Point2D getIntersection(Line2D edge1, Line2D edge2) {
		// Find intersection of the lines determined by each edge's line segment
		double x11 = edge1.getX1();
		double y11 = edge1.getY1();
		double x12 = edge1.getX2();
		double y12 = edge1.getY2();
		double x21 = edge2.getX1();
		double y21 = edge2.getY1();
		double x22 = edge2.getX2();
		double y22 = edge2.getY2();

		double denom = (y22 - y21) * (x12 - x11) - (x22 - x21) * (y12 - y11);
		double numeratorA = (x22 - x21) * (y11 - y21) - (y22 - y21)
				* (x11 - x21);
		double numeratorB = (x12 - x11) * (y11 - y21) - (y12 - y11)
				* (x11 - x21);
		Point2D.Double result = null;
		if (denom == 0) {
			if (numeratorA == 0) {
				// lines are coincident
				assert numeratorB == 0;
				if (x21 == x22) {
					if (between(y11, y21, y12)) {
						result = new Point2D.Double(x21, y21);
					} else if (between(y11, y22, y12)) {
						result = new Point2D.Double(x22, y22);
					}
				} else {
					if (between(x11, x21, x12)) {
						result = new Point2D.Double(x21, y21);
					} else if (between(x11, x22, x12)) {
						result = new Point2D.Double(x22, y22);
					}
				}
			} else {
				// lines are parallel. result remains null.
			}
		} else if (between(numeratorA, 0, denom)
				&& between(numeratorB, 0, denom)) {
			double ua = numeratorA / denom;
			result = new Point2D.Double(x11 + ua * (x12 - x11), y11 + ua
					* (y12 - y11));
		}
		return result;
	}

	private boolean between(double x, double x1, double x2) {
		return x > Math.min(x1, x2) && x < Math.max(x1, x2);
	}

}

class PerspectiveParentPrinter {
	private final Perspective facet;

	private static final StringAlign align = new StringAlign(13,
			StringAlign.JUST_LEFT);

	PerspectiveParentPrinter(Perspective p) {
		facet = p;
	}

	public String toString() {
		String result = align.format(facet);
		if (facet.getParent() != null) {
			result += " => " + facet.getParent();
		}
		return result;
	}

}
