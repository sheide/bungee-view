/* 


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

/**
 * ToDo:
 * 
 * Review uses of synchronized - are some of them no-ops because the code is
 * re-entrant rather than multi-threaded?
 * 
 * Rationalize startup: print start/end time for creating each frame, and its
 * "delayed init", and see whether we're really saving anything with the extra
 * complication. Also nest instantiatedPerspective inside Perspective.
 * 
 * For long alphabetized lists, put inital letter in scroll bar
 * 
 * Widget to limit cluster size (especially to 1)
 * 
 * Query for percentage on even for deeply nested facets.
 * 
 * Load facets with thumbs, and brush item <--> facet Brush grid scrollbar if
 * ordered by an attribute. scrollbar intervals correspond to category values.
 * Intervals can be computed from perspective cumOnCounts.
 * 
 * Search against facets, and display matches nested as usual, but without
 * adding ancestors to query. cf
 * http://www.cs.cmu.edu/~quixote/DynamicCategorySets.pdf Or list these nested
 * under the search term with checkboxes
 * 
 * do search like regular facets. Add bars for search terms to make them look
 * like other categories. Use numerical relvance.
 * 
 * Is itunes data good for bungee view? If so,put identical images together.
 * Draw grid over 1 or 2 copies to represent tracks on 1 album. AlsoARTstor
 * 
 * Call even item_url & getCountsIgnoringFacet in other threads, in case server
 * connection goes down.
 * 
 * add hard-to-see menu of categories to show
 * 
 * Editing option: editable description. drag to add properties (right click to
 * delete?). define new properties (right click?). (right?) drag to re-order
 * properties/categories.
 * 
 * Endeca automatically drills down when only one value has non-zero count
 * 
 * Tnf.layout - don't create new aptexts unless content changes. Keep two
 * layouts - wrapped & not? Tnf.Layout should check content & children's bounds
 * to see if it really has to do anything. Each tnf can cache its aptexts.
 * 
 * Shift-select should work across multiple selections.
 * 
 * JOHNS LIST:
 * 
 * Reduce false positives.
 * 
 * Task-specific interface customization. i.e. browsing interface could be
 * different from EDA interface.
 * 
 * Counts don't add up.
 * 
 * Store findings (patterns & individual documents). Highlight saved works in
 * new results.
 * 
 * Option to sort results.
 * 
 * LOW PRIORITY:
 * 
 * Help: keyboard events(shift, arrow), clippy.
 * 
 * History Recent/Saved Items galleries.
 * 
 * Support aggregation operator other than COUNT. For instance, have area depend
 * on sales volume rather than number of companies.
 */

/**
 * To run from command line: C:\Documents and
 * Settings\mad\Desktop\eclipse\workspace\art>java -cp ".;C:/Documents and
 * Settings/mad/Desktop/applet_test/piccoloNEW.jar;C:/Documents and
 * Settings/mad/Desktop/applet_test/piccolox.jar;C:/Documents and
 * Settings/mad/Desktop/mysql-connector-java-3.1.7/mysql-connector-java-3.1.7/mysql-connector-java-3.1.7-bin.jar"
 * -Xrunhprof:cpu=times,doe=n viz.Art
 * 
 * or -Xrunhprof:cpu=samples,depth=10,thread=y then
 * 
 * java -jar JPerfAnal.jar java.hprof.txt
 * 
 * in C:\eclipse\workspace\art
 * 
 * For HPJperf, use -Xrunhprof:cpu=samples,thread=y,depth=20,cutoff=0 and
 * possibly heap=all
 */

import javax.imageio.ImageIO;
import javax.jnlp.BasicService;
import javax.jnlp.ClipboardService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.ArrayList;

import javax.swing.Timer;



import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.PerspectiveObserver;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.javaExtensions.PrintArray;
import edu.cmu.cs.bungee.javaExtensions.QueueThread;
import edu.cmu.cs.bungee.javaExtensions.UpdateNoArgsThread;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.Boundary;
import edu.cmu.cs.bungee.piccoloUtils.gui.Button;
import edu.cmu.cs.bungee.piccoloUtils.gui.Menu;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PInputManager;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.PFrame;

public class Bungee extends PFrame {

	static final String version = "8/16/07";

	private static final boolean isPrintUserActions = true;

	/** ************* Start of tweakable parameters *************** */

	static final int ThumbQuality = 50;

	static public final int ImageQuality = 100;

	int textH = 14;

	// private int minW = 612;

	private int minH = 459;

	/**
	 * The margin between Summary/Results and Results/SelectedItem.
	 */
	private static final double regionMargins = 0.0;

	final static long rankAnimationMS = 400;

	final static long dataAnimationMS = 800;

	final static long rankAnimationStep = 50;

	final static long dataAnimationStep = 100;

	final static Color goldBorderColor = Color.getHSBColor(0.17f, 0.5f, 0.4f);

	// public static final float brightness = 0.15f; //0.08;
	//
	// public static final float saturation = 0.39f;

	// {{fade, normal, bright}, ...}
	// brights are web-safe. normals are close (88 instead of 99)
	// web-safe colors shown at http://www.visibone.com/colorlab/
	// static final Color[][] colors = {
	// { new Color(0x333D33), new Color(0x558855), new Color(0xBBEEBB) },
	// { new Color(0x3D3333), new Color(0x885555), new Color(0xEEBBBB) },
	// { new Color(0x333D3D), new Color(0x558888), new Color(0xBBEEEE) },
	// { new Color(0x3D333D), new Color(0x885588), new Color(0xEEBBEE) },
	// { new Color(0x33333D), new Color(0x555588), new Color(0xBBBBEE) },
	// { new Color(0x3D3D33), new Color(0x888855), new Color(0xEEEEBB) } };
	//
	// static final int nColors = colors.length;

	/**
	 * Used in percent labels and hotzone
	 */
	public final static Color PERCENT_LABEL_COLOR = Markup.whites[1];

	// /**
	// * Used in Bars
	// */
	// final static Color selectedFadedBarColor = whites[1];

	// /**
	// * Used in Bars, Labels, Facets, and Selected Item panes
	// */
	// final static Color selectedBarColor = whites[2];
	//
	// final static Color selectedLabelColor = selectedBarColor;
	//
	// /**
	// * Used in Bars, Labels, Facets, and Selected Item panes
	// */
	// final static Color highlightSelectedBarColor = whites[3];
	//
	// final static Color highlightSelectedLabelColor =
	// highlightSelectedBarColor;

	public static final Paint selectedItemOutlineColor = Color.yellow; // new

	// Color(0xFF9900);

	static final Color headerBG = new Color(0x333333);

	static final Color headerFG = new Color(0x669999);

	static final Color mouseDocFG = new Color(0x99CCCC);

	public static final Color summaryFG = new Color(0x666699);

	static final Color summaryBG = summaryFG.darker().darker().darker()
			.darker().darker();

	// Color.black; // anything but black makes the

	// light beams look funny.

	final static Color selectedItemFG = new Color(0x999966);

	final static Color selectedItemBG = selectedItemFG.darker().darker()
			.darker().darker().darker();

	final static Color gridFG = new Color(0x669966);

	final static Color gridBG = gridFG.darker().darker().darker().darker()
			.darker();

	final static Color textScrollBG = new Color(0x333300);

	final static Color textScrollFG = new Color(0x666633);

	final static Color facetTreeScrollBG = new Color(0x333333);

	final static Color facetTreeScrollFG = new Color(0x666666);

	final static Color gridScrollBG = new Color(0x003300);

	final static Color gridScrollFG = new Color(0x336633);

	static final Color checkColor = Color.getHSBColor(0.33f, 1.0f, 0.8f);

	static final Color checkFG = new Color(0x445544);

	static final Color checkBG = new Color(0x223322);

	static final Color xColor = Color.getHSBColor(0.0f, 1.0f, 0.8f);

	static final Color xFG = new Color(0x554444);

	static final Color xBG = new Color(0x332222);

	// Shows chars available in your Java environment:
	// http://www.pccl.demon.co.uk/java/unicode.html
	static final String childIndicatorSuffix = "\u2193";

	static final String checkBoxPrefix = "       "; // "\u2751 "; // "\u0000";

	// //'\u2193';

	// //'\u00BB'
	// '\u21B4'

	static final String parentIndicatorPrefix = "\u2192"; // '\u2023'

	// '\u21B3'

	// delete button is F078; checkbox is F0FE; delete is FoFD

	double parentIndicatorWidth;

	public boolean extraGreen = true;

	private boolean isPopups = true;

	/** ************* End of tweakable parameters *************** */

	private double digitWidth;

	private double commaWidth;

	private double spaceWidth;

	double childIndicatorWidth;

	double checkBoxWidth;

	private static final long serialVersionUID = 3907142159514321899L;

	int h;

	int w;

	public Font font;

	// public FontMetrics fontMetrics;

	public double lineH;

	double scrollBarWidth;

	/**
	 * Space between images and scroll bar
	 */
	double scrollMarginSize;

	double headerH;

	double mouseDocH;

	// public static final boolean isApplet = PApplet.class
	// .isAssignableFrom(Art.class);

	// ServletInterface db;

	public Query query;

	Header header;

	MouseDocLine mouseDoc;

	APText help;

	Summary summary;

	ResultsGrid grid;

	SelectedItem selectedItem;

	PerspectiveList perspectiveList;

	ClusterViz clusterViz;

	private Cursor waitCursor;

	private Hashtable facetNameWidths;

	private transient SoftReference truncatedStrings;

	String dbName;

	private DataUpdater updater;

	private transient FacetSelecter facetSelecter;

	public DocumentShower documentShower;

	ActionPrinter actionPrinter;

	Replayer replayer;

	/**
	 * This is the "argument" facet, in case the command needs more than the
	 * facet clicked on. Also used by the right-click addItemFacet gesture
	 */
	Perspective selectedForEdit;

	Menu editMenu;

	/**
	 * This is the facet we [middle] clicked on.
	 */
	Perspective editMenuPerspective;

	Item editMenuItem;

	private final KeyEventHandler keyHandler = new KeyEventHandler();

	/**
	 * Desired Thread priorities: DataUpdater art-1 ThumbLoader art-2
	 * ImageLoader art-2 ItemSetter art-2 GetPerspectiveNames art-3 Highlighter
	 * art-3
	 * 
	 * However art is at 4, and the only lower choice is 2. Therefore we settle
	 * for: DataUpdater art ThumbLoader art-2 ImageLoader art-2 ItemSetter art-2
	 * GetPerspectiveNames art-2 Highlighter art-2
	 * 
	 */

	/**
	 * Stick this on all the FacetNodes (or on an intermediate parent that's
	 * below any perspective selection handlers). If we just put it here, mouse
	 * clicks will go to select perspectives before they select facets. Then,
	 * the perspective will always be selected when the facet gets selected.
	 * Therefore, make sure the facet selection fires first.
	 */
	public static final FacetClickHandler facetClickHandler = new FacetClickHandler();

	boolean isReady = false;

	private APText tooSmall;

	BasicService basicJNLPservice;

	ClipboardService jnlpClipboardService;

	// AppletContext appletContext;

	// private StringBuffer userActions;

	private String argString;

	String server;

	private int maxCharW;

	static final double minThumbSize = 80;

	private final static String fontFamily = "SansSerif";

	private final static int fontStyle = Font.BOLD;

	public static void main(String[] args) {
		// Util.print("Starting Art");
		new Bungee(args);
	}

	public Bungee(String[] args) {
		// gross hack to pass arguments to initialize
		super(args.length > 0 ? args[0] : null, false, null);
	}

	public void initialize() {
		Runtime.runFinalizersOnExit(true);
		argString = getTitle();
		URLQuery argURLQuery = new URLQuery(argString);
		setTitle("Bungee View Image Collection Browser.  See the forest AND the trees.");
		// userActions = new StringBuffer();
		// userActions.append("Bungee View version ").append(version).append(
		// ", started ").append(new Date())
		// .append(
		// "\n\nType codes: 1=bar; 2=bar label; 3=category label; 4=thumbnail;
		// 5=selected item; "
		// + "6=selected item property; 8=search box; 9=keypress\n")
		// .append("\nTime\tType\tObject\tModifiers")
		//
		// Util.print(userActions.toString());

		// String[] dbs = argString.getArgument("dbs").split(";");
		// assert dbs.length > 0 : "No databases specified!";
		// databases = new String[dbs.length][];
		// for (int i = 0; i < dbs.length; i++) {
		// databases[i] = dbs[i].split(",");
		// assert databases[i].length == 3 : "Bad database spec: " + dbs[i];
		// for (int j = 0; j < databases[i].length; j++) {
		// try {
		// databases[i][j] = URLDecoder.decode(databases[i][j],
		// "UTF-8");
		// // Util.print(databases[i][j]);
		// } catch (UnsupportedEncodingException e1) {
		// e1.printStackTrace();
		// }
		// }
		// }
		String _dbName = argURLQuery.getArgument("db");
		// String fontFamilySpec = argString.getArgument("fontFamily");
		// if (fontFamilySpec.length() > 0)
		// fontFamily = fontFamilySpec;
		// String fontStyleSpec = argString.getArgument("fontStyle");
		// if (fontStyleSpec.length() > 0)
		// try {
		// fontStyle = Font.class.getField(fontStyleSpec).getInt(null);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		String fontSizeSpec = argURLQuery.getArgument("fontSize");
		if (fontSizeSpec.length() > 0)
			textH = Integer.parseInt(fontSizeSpec);
		setTextSize(textH);

		server = argURLQuery.getArgument("server");

		// try {
		// browser = JSObject.getWindow(this);
		// } catch (JSException e) {
		// Util.err("JSException trying to get browser: " + e);
		// }
		getCanvas().setPanEventHandler(null);
		getCanvas().setZoomEventHandler(null);
		// setFullScreenMode(true); // only works in applets
		grabFocus();

		PCamera cam = getCanvas().getCamera();
		w = (int) cam.getWidth();
		h = (int) cam.getHeight();
		cam.addPropertyChangeListener(PNode.PROPERTY_BOUNDS,
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						PBounds bounds = (PBounds) evt.getNewValue();
						setSize((int) bounds.getWidth(), (int) bounds
								.getHeight());
					}
				});

		basicJNLPservice = maybeGetBasicService();
		waitCursor = new Cursor(Cursor.WAIT_CURSOR);
		setDatabase(_dbName);

	}

	void setTextSize(int fontSize) {
		// Util.print("Art.setTextSize " + fontSize);
		facetNameWidths = null;
		FacetText.clearFacetTexts();
		textH = fontSize;
		font = new Font(fontFamily, fontStyle, textH);
		FontMetrics fontMetrics = getGraphics().getFontMetrics(font);
		lineH = Math.ceil(fontLineH());
		maxCharW = fontMetrics.getMaxAdvance();
		digitWidth = getStringWidth("0123456789") / 10;
		commaWidth = getStringWidth(",,,,,,,,,,") / 10;
		spaceWidth = getStringWidth("                    ") / 20;
		childIndicatorWidth = getStringWidth(childIndicatorSuffix);
		parentIndicatorWidth = getStringWidth(parentIndicatorPrefix);
		checkBoxWidth = getStringWidth(checkBoxPrefix);
		headerH = Math.ceil(2.5 * lineH);
		mouseDocH = lineH;
		scrollBarWidth = Math.round(0.67 * lineH);
		scrollMarginSize = Math.round(0.17 * lineH);

		// Util.print("childIndicatorWidth = " + childIndicatorWidth + ";
		// checkBoxWidth = " + checkBoxWidth);
	}

	public double fontLineH() {
		return font.getLineMetrics(
				"ABCabcdefghijklmnopqrstuvwxyz123~`':;>?/@#%^_-|{}",
				PPaintContext.RENDER_QUALITY_HIGH_FRC).getHeight();
	}

	public void setDatabase(String _dbName) {
		if (dbName == null || !dbName.equals(_dbName)
				|| query.isRestrictedData()) {
			// Util.print("setDatabase/handleCursor true");
			handleCursor(true);
			// if (db != null) {
			stop();
			// }
			dbName = _dbName;
			facetNameWidths = null;
			truncatedStrings = null;
			query = null;
			isReady = false;
			// dbName = databases[dbIndex][1];
			updater = new DataUpdater(this);
			updater.start();
			actionPrinter = null;

			PLayer layer = getCanvas().getLayer();
			layer.removeAllChildren();

			APText stop = oneLineLabel();
			stop.setText("Waiting for " + server + "   ...");
			stop.setScale(1.5);
			// TextButton stop = new StopButton("Stop waiting for " + server,
			// this);
			stop.setOffset(lineH, lineH);
			layer.addChild(stop);
		}
	}

	// Called in DataUpdater initialization after query is set
	void setDatabaseInternal() {
		PLayer layer = getCanvas().getLayer();
		layer.removeAllChildren();
		String errorMessage = query.errorMessage();
		if (errorMessage == null || errorMessage.equals("OK")) {
			// int dbIndex = dbIndex(_dbName);
			// String description = databases[dbIndex][2];

			// appletContext = maybeGetAppletContext();

			/**
			 * [[database, prettyName], ...]
			 */
			String[][] databases = query.getDatabases();
			header = new Header(this, databases, dbName);
			mouseDoc = new MouseDocLine(this);
			selectedItem = new SelectedItem(this);
			grid = new ResultsGrid(this);
			summary = new Summary(this);

			help = new APText(new Font(fontFamily, Font.ITALIC, textH));
			help.scale(2.0);
			help.setTextPaint(Color.white);
			help.setPickable(false);
			help
					.setText("Click on a category,\n and then click on its values\n to start diving");

			layer.addChild(grid);
			layer.addChild(selectedItem);
			layer.addChild(help);
			layer.addChild(summary);
			layer.addChild(mouseDoc);
			layer.addChild(header); // Last, to let menu overlap
			// other panes.
			summary.init();
			validate(w, h);
		} else {
			setNormalPaneVisibility(false);
			showError("Could not connect to " + server + " because\n"
					+ errorMessage);
		}
		// Util.print("...setInternal returning");
		// Util.print("setDatabaseInternal/handleCursor false");
		handleCursor(false);
	}

	// private int dbIndex(String name) {
	// for (int i = 0; i < databases.length; i++)
	// if (databases[i][1].equals(name) || databases[i][2].equals(name))
	// return i;
	// return 0; // default to first
	// }

	// Runnable queryIsReady = new Runnable() {
	//
	// public void run() {
	// summary.init();
	// query.setQueryValid();
	// ops =
	// "6870,1,20,16;13059,1,20,16;17555,4,31047,5;23113,6,6329,16;25977,7,grid,2;26178,7,grid,4;26298,7,grid,6;28841,1,6383,16;34600,1,6383,16"
	// .split(";");
	// opNum = 0;
	// opTimer.setRepeats(false);
	// replayOps();
	// // String _dbName = maybeGetDocumentBase().getQuery();
	// // if (_dbName != null) {
	// // int index = _dbName.indexOf('+');
	// // if (index >= 0) {
	// // replayOps(dbName.substring(index + 1));
	// // }
	// // }
	// }
	// };

	public void setSize(int width, int height) {
		// Util.print("Art.setSize " + width + " " + height);
		if (isReady)
			// This gets called before setup, when we're not ready.
			validate(width, height);
		else {
			// Need to remember these for when we are ready to validate
			w = width;
			h = height;
		}
	}

	public void revalidate() {
		isReady = false;
		validate(w, h);
	}

	int minW() {
		return (int) (selectedItem.labelWidth() + summary.minWidth() + grid
				.minWidth());
	}

	double scaleRatio(double power) {
		return Math.pow(w / (double) minW(), power);
	}

	void validate(int _w, int _h) {
		if (!isReady || _w != w || _h != h) {
//			 Util.print("Art.validate " + _w + " " + _h + " " + lineH);
			// super.setSize(_w, _h); // see if this is why Safari barfs
			isReady = true;
			w = _w;
			h = _h;
			printUserAction(SETSIZE, w, h);
			if (!setTooSmall()) {
				// Don't understand why I need to add these constants.
				// int kludgeW = w + 8;
				// int kludgeH = h + 34;
				// setBounds(0, 0, kludgeW, kludgeH);
				// getCanvas().setBounds(0, 0, kludgeW, kludgeH);
				header.validate(w, headerH);
				mouseDoc.validate(w, mouseDocH);

				double internalH = h - headerH - mouseDocH;

				double selectedItemW = (int) (selectedItem.labelWidth() * scaleRatio(0.5));
				double summaryW = (int) (summary.minWidth() * scaleRatio(1));
				double gridW = (w - summaryW - selectedItemW - regionMargins);
				// Util.print("gridW " + ((int) (w * 0.1)) + " "
				// + ((int) getStringWidth("(0.001% of 999,999 works)")));
				summary.validate(summaryW, internalH);
				summary.setOffset(0.0, headerH);
				grid.validate(gridW, internalH);
				grid.setOffset(summaryW, headerH);
				selectedItem.validate(selectedItemW, internalH);
				selectedItem.setOffset(summaryW + gridW, headerH);
				validatePerspectiveList();
				mouseDoc.setOffset(0.0, headerH + internalH);

				help.setOffset(summaryW + lineH, headerH + 4 * lineH);
				double helpRight = help.getXOffset() + help.getWidth();
				if (helpRight > w) {
					help.setConstrainWidthToTextWidth(false);
					help.setWidth(w - helpRight - 5);
				}
			}
		}
	}

	void validatePerspectiveList() {
		if (perspectiveList != null) {
			perspectiveList.validate();
			// perspectiveList.setOffset(-summary.queryW, lineH);
		}
		if (clusterViz != null) {
			clusterViz.validate(selectedItem.getWidth(), selectedItem
					.getHeight());
			clusterViz.setOffset(selectedItem.getOffset());
		}
	}

	void updateGridBoundary() {
		double offset = grid.getXOffset() + grid.getWidth();
		selectedItem.setOffset(offset, selectedItem.getYOffset());
		selectedItem.validate(w - offset, selectedItem.getHeight());
		validatePerspectiveList();
	}

	void updateSummaryBoundary() {
		double offset = summary.getXOffset() + summary.getWidth();
		grid.setOffset(offset, grid.getYOffset());
		grid.validate(w - offset - selectedItem.getWidth(), grid.getHeight());
	}

	private boolean setTooSmall() {
		if (tooSmall != null) {
			tooSmall.removeFromParent();
			tooSmall = null;
		}
		boolean isTooSmall = w < minW() || h < minH;
		setNormalPaneVisibility(!isTooSmall);
		if (isTooSmall) {
			showError("Bungee View requires at least " + minW() + " x " + minH
					+ " pixels," + "\n but your window is only " + w + " x "
					+ h + ".\n  1. Make sure your window is maximized, or"
					+ "\n  2. Increase your screen resolution.");
		}
		return isTooSmall;
	}

	void setNormalPaneVisibility(boolean isVisible) {
		if (summary != null)
			summary.setVisible(isVisible);
		if (grid != null)
			grid.setVisible(isVisible);
		if (selectedItem != null)
			selectedItem.setVisible(isVisible);
		if (mouseDoc != null)
			mouseDoc.setVisible(isVisible);
		if (header != null)
			header.setVisible(isVisible);
	}

	void showError(String msg) {
		if (tooSmall != null) {
			tooSmall.removeFromParent();
			tooSmall = null;
		}

		tooSmall = new APText(font);
		tooSmall.setScale(1.5);
		// label.setTextPaint(Color.white);
		tooSmall.setPickable(false);
		tooSmall.setOffset(50, 100);
		tooSmall.setConstrainWidthToTextWidth(false);
		tooSmall.setWidth(w - 2 * tooSmall.getXOffset());
		tooSmall.setText(msg);
		getCanvas().getLayer().addChild(tooSmall);
	}

	double getW() {
		return w;
	}

	double getH() {
		return h;
	}

	void grabFocus() {
		PInputManager pim = getCanvas().getRoot().getDefaultInputManager();
		// Util.print("grabFocus "+ pim.getKeyboardFocus());
		pim.setKeyboardFocus(keyHandler);

	}

	void setInitialState() {
		if (argString != null) {
			URLQuery argURLQuery = new URLQuery(argString);
			String q = argURLQuery.getArgument("Query");
			if (q.length() > 0) {
				// Util.print(q);
				String[] terms = Util.splitSemicolon(q);
				for (int i = 0; i < terms.length; i++) {
					// Util.print(terms[i]);
					String[] x = terms[i].split(":");
					assert x.length == 2;
					if (x[0].equals("TextSearch")) {
						summary.q.addTextSearch(x[1]);
					} else {
						Perspective facetType = query.findUsedPerspective(x[0],
								false);
						boolean required = (x[1].charAt(0) != '~');
						if (!required)
							x[1] = x[1].substring(1);
						String[] disjuncts = x[1].split("\\|");
						for (int j = 0; j < disjuncts.length; j++) {
							String[] ancestors = disjuncts[j].split(" -- ");
							Perspective parent = facetType;
							for (int k = 0; k < ancestors.length; k++) {
								query.prefetchData(parent);
								Perspective child = query.findPerspective(
										ancestors[k], parent);
								assert child != null : parent + " "
										+ ancestors[k];
								if (!child.isRestriction(required))
									query
											.toggleFacet(
													child,
													required ? InputEvent.CTRL_DOWN_MASK
															: ItemPredicate.EXCLUDE_ACTION);
								parent = child;
							}
						}
					}
				}
				updateAllData();
			}
			String facetName = argURLQuery.getArgument("SelectedFacet");
			if (facetName.length() > 0) {
				PerspectiveViz pv = summary.findPerspective(facetName);
				if (pv != null)
					summary.connectToPerspective(pv.p);
			}
			String itemURL = argURLQuery.getArgument("SelectedItem");
			if (itemURL.length() > 0) {
				grid.clickThumb(itemURL);
			}

			// ops =
			// "3234,1,20,16;6209,1,35,16;12067,1,806,16;16724,1,806,16;23533,1,41,16;29001,4,32827,5;32176,4,38298,11;36182,4,36341,15;40548,6,5221,16;45165,6,1,16;50322,4,36383,7"
			// .split(";");
			// String opsSpec = argString.getArgument("ops");
			// opsSpec =
			// "3234,1,20,16;6209,1,35,16;12067,1,806,16;16724,1,806,16;23533,1,41,16;29001,4,32827,5;32176,4,38298,11;36182,4,36341,15;40548,6,5221,16;45165,6,1,16;50322,4,36383,7;59535,6,3,16";
			// if (opsSpec.length() == 0) {
			String replay = argURLQuery.getArgument("Session");
			if (replay.length() > 0) {
				replayer = new Replayer(this, query.opsSpec(replay));
				replayer.start();
				replayer.update();
			}
			// }
			// if (opsSpec.length() > 0) {
			// ops = Util.splitSemicolon(opsSpec);
			// opTimer.setRepeats(false);
			// replayOps();
			// }
			argString = null;
		}
	}

	protected void finalize() throws Throwable {
		stop();
		if (documentShower != null) {
			documentShower.exit();
			// documentShower = null;
		}
		super.finalize();
	}

	public void stop() {
		// Util.print("Art.stop");
		if (updater != null) {
			updater.exit();
			updater = null;
		}
		if (facetSelecter != null) {
			facetSelecter.exit();
			facetSelecter = null;
		}
		if (replayer != null) {
			replayer.exit();
			replayer = null;
		}
		if (selectedItem != null) {
			selectedItem.stop();
			selectedItem = null;
		}
		// summary.highlighter.exit();
		// grid.thumbLoader.exit();
		if (grid != null) {
			grid.stop();
			grid = null;
		}
		// db = null;
		if (query != null) {
			query.exit();
			query = null;
		}
	}

	boolean toggleIsPopups() {
		return isPopups = !isPopups;
	}

	boolean getIsPopups() {
		return isPopups;
	}

	public void setSelectedItemID(Item item, PNode source) {
		// Util.print("art.setSelectedItemID " + item_id);
		printUserAction(Bungee.THUMBNAIL, item.getId(), 0);
		PBounds gStartRect = source.getGlobalBounds();
		Rectangle2D lStartRect = selectedItem.globalToLocal(gStartRect);
		double selectedY = lStartRect.getY();
		double selectedX = lStartRect.getX();
		double selectedW = lStartRect.getWidth();
		double selectedH = lStartRect.getHeight();
		// double gridSize = (true || perspectiveList != null
		// && perspectiveList.isHidden()) ? lStartRect.getWidth() : -1;
		selectedItem.animateOutline(item, selectedX, selectedY, selectedW,
				selectedH);
	}

	void showItemInNewWindow(Item item) {
		showDocument(item);
	}

	void showDocument(Object o) {
		if (documentShower == null) {
			documentShower = new DocumentShower(basicJNLPservice, this);
			documentShower.start();
		}
		documentShower.add(o);
	}

	public BasicService maybeGetBasicService() {
		BasicService s = null;
		try {
			s = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
			jnlpClipboardService = (ClipboardService) ServiceManager
					.lookup("javax.jnlp.ClipboardService");
		} catch (UnavailableServiceException e) {
			if (s == null)
			Util.err("jnlp.BasicService is not available");
			if (jnlpClipboardService == null)
				Util.err("jnlp.ClipboardService is not available");
		}
		return s;
	}

	void copyBookmark() {
		StringSelection ss = new StringSelection(bookmark());
		if (jnlpClipboardService != null)
			jnlpClipboardService.setContents(ss);
	}

	public String bookmark() {
		StringBuffer buf = new StringBuffer();
		buf.append("db=").append(URLEncoder.encode(dbName));
		if (selectedItem.currentItem != null) {
			buf.append("&SelectedItem=").append(
					URLEncoder.encode(query
							.getItemURL(selectedItem.currentItem)));
		}
		ItemPredicate cp = summary.connectedPerspective();
		if (cp != null) {
			buf.append("&SelectedFacet=").append(
					URLEncoder.encode(cp.getName()));
		}
		if (query.isRestricted()) {
			boolean emptyQuery = true;
			buf.append("&Query=");
			StringBuffer q = new StringBuffer();
			Set searches = query.getSearches();
			for (Iterator it = searches.iterator(); it.hasNext();) {
				String search = (String) it.next();
				if (!emptyQuery) {
					q.append(";");
				} else
					emptyQuery = false;
				q.append("TextSearch:").append(search);
			}

			for (Iterator it = query.perspectivesIterator(); it.hasNext();) {
				Perspective p = (Perspective) it.next();
				if (p.getParent() == null) {
					boolean[] reqtTypes = { true, false };
					for (int type = 0; type < 2; type++) {
						Perspective[] restrictions = p
								.getRestrictionFacetInfos(false,
										reqtTypes[type]);
						if (restrictions.length > 0) {
							String[] descs = new String[restrictions.length];
							for (int i = 0; i < restrictions.length; i++) {
								descs[i] = fullName(restrictions[i]);
							}
							descs[0] = p.getName() + (type == 0 ? ":" : ":~")
									+ descs[0];
							if (!emptyQuery) {
								q.append(";");
							} else
								emptyQuery = false;
							q.append(Util.join(descs, "|"));
						}
					}
				}
			}
			buf.append(URLEncoder.encode(q.toString()));
		}
		String result = codeBase() + "bungee.jsp?" + buf.toString();
		Util.print("bookmark " + result);
		return result;
	}

	String fullName(Perspective p) {
		if (p.getParent().getParent() == null)
			return p.getName();
		else
			return fullName(p.getParent()) + " -- " + p.getName();
	}

	public String codeBase() {
		String result;
		if (basicJNLPservice != null) {
			result = basicJNLPservice.getCodeBase().toString();
		} else
			result = "http://localhost/bungeeOLD/"
			// "http://cityscape.inf.cs.cmu.edu/bungee/"
			;
		return result;
	}

	public void showMedianArrowDesc(Perspective parent) {
		summary.showMedianArrowPopup(parent, parent != null);
	}

	void toggleFacet(Perspective facet, int modifiers) {
		// Util.print("Art.toggleFacet " + facet + " " + modifiers);
		// Date start = new Date();
		arrowFocus = facet;
		if (!facet.getParent().isRestricted()
				&& facet.getOnCount() == (Perspective
						.isExcludeAction(modifiers) ? query.onCount : 0))
			setTip("There would be no results if you added this filter.  (There are now "
					+ query.describeNfilters() + ")");
		else if (query.toggleFacet(facet, modifiers)) {
			updateAllData();
			// highlightFacet(facet, true, false, modifiers);
			Perspective toConnect = summary.connectedPerspective();
			// Util
			// .print("Art.selectFacet "
			// + facet
			// + " "
			// + (modifiers
			// & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK) |
			// FacetText.excludeAction)
			// + " " + toConnect);
			if (toConnect == null
					|| (modifiers & (InputEvent.CTRL_DOWN_MASK
							| InputEvent.SHIFT_DOWN_MASK | ItemPredicate.EXCLUDE_ACTION)) == 0)
				toConnect = facet;
			while (!toConnect.isUsed()) {
				// Util.print("default " + toConnect + " => " +
				// toConnect.parent);
				toConnect = toConnect.getParent();
			}
			summary.connectToPerspective(toConnect);
			// Date end = new Date();
			// long delay = end.getTime() - start.getTime();
			// Util.print("selectFacet time " + delay );
			highlightFacet(facet, true, modifiers);
		}
	}

	void toggleCluster(Cluster cluster) {
		int[] facet_ids = new int[cluster.nRestrictions()];
		for (int i = 0; i < facet_ids.length; i++) {
			facet_ids[i] = cluster.allRestrictions()[i].getID();
		}
		printUserAction(CLUSTER, Util.join(facet_ids, "/"), 0);
		query.toggleCluster(cluster);
		updateAllData();
	}

	Perspective highlightedFacet;

	Cluster highlightedCluster;

	public void highlightCluster(Cluster facet, boolean state) {
		Cluster prevFacet = highlightedCluster;
		highlightedCluster = state ? facet : null;
		if (prevFacet != null) {
			summary.highlightCluster(prevFacet);
		}
		summary.highlightCluster(facet);
		summary.showCluster(facet, state);
		prevModifiers = -9999;
		rehighlightFacet(0, true);
		if (clusterViz != null)
			clusterViz.showFacet(facet);
	}

	// private boolean highlightForSelectPerspectiveOnly;

	// private boolean highlightOnly;

	// modifiers: -1 means no action; -2 means open rank; otherwise control &
	// shift masks
	public void highlightFacet(Perspective facet, boolean state, int modifiers) {
		// Util.print("Art.highlightFacet " + facet + " " + state + " "
		// + modifiers );
		// Util.printStackTrace();
		Perspective prevFacet = highlightedFacet;
		highlightedFacet = state ? facet : null;
		// highlightForSelectPerspectiveOnly = onlySelectPerspective;
		// highlightOnly = modifiers < 0; // it's a no-op facet in
		// SelectedItem.

		if (prevFacet != null) {
			summary.highlightForFacet(prevFacet, false);
		}
		summary.highlightForFacet(facet, state);

		// Util.print("PI summary");
		// summary.repaintNow();
		// getCanvas().paintImmediately();

		showFacet(facet, state);
		prevModifiers = -9999;
		rehighlightFacet(modifiers, true);
		// Util.print("PI mouse");
		// mouseDoc.repaintNow();
		// getCanvas().paintImmediately();

		selectedItem.showFacet(facet);
		if (perspectiveList != null)
			perspectiveList.showFacet(facet);
		if (clusterViz != null)
			clusterViz.showFacet(facet);
	}

	private int prevModifiers = -9999;

	// update mouse doc when shift keys change or you rollover a checkbox
	void rehighlightFacet(int modifiers, boolean dontCombine) {
		if (!dontCombine) {
			// need to combine new modifiers from input event with old
			// modifiers,
			// which may contain additional bits (e.g. exclude)
			modifiers |= prevModifiers & ~(ItemPredicate.EXCLUDE_ACTION - 1);
		}
		if (prevModifiers != modifiers) {
			prevModifiers = modifiers;
			Markup desc = null;
			// Util.print("Art.rehighlightFacet " + modifiers + " " +
			// highlightedFacet + " " + highlightedCluster);
			if (highlightedFacet != null) {
				if (modifiers == -2) {
					desc = Query.emptyMarkup();
					desc.add("open category ");
					if (highlightedFacet.getParent() != null)
						desc.add(highlightedFacet.getParent());
					else
						// For the facet_type text labels
						desc.add(highlightedFacet);
				} else if (modifiers >= 0) {
					desc = highlightedFacet.facetDoc(modifiers);
				}
			} else if (highlightedCluster != null
					&& !query.usesCluster(highlightedCluster)) {
				desc = Query.emptyMarkup();
				desc.add("Add filter on ");
				desc.add(highlightedCluster);
			}
			setClickDesc(desc);
		}
	}

	// private JSObject browser;
	//
	// private static final String[] scrollArgs = { "0", "0" };

	public void mayHideTransients() {
		// Util.print("Art.mayHideTransients");
		setTip(null);
		// try {
		// if (browser != null)
		// browser.call("scrollTo", scrollArgs);
		// } catch (JSException e) {
		// Util.err("JSException trying to scroll browser: " + e);
		// }

		// may on the way up; do on the way down
		if (summary != null)
			summary.doHideTransients();
		if (selectedItem != null)
			selectedItem.doHideTransients();
		if (clusterViz != null)
			clusterViz.hide();
		if (arrowFocus != null) {
			arrowFocus = null;
			// if (perspectiveList != null)
			// perspectiveList.show(false);
		}
	}

	public void setTip(String s) {
		if (mouseDoc != null)
			mouseDoc.setTip(s);
	}

	public void setClickDesc(Markup s) {
		if (mouseDoc != null)
			mouseDoc.setClickDesc(s);
	}

	public void setClickDesc(String s) {
		if (mouseDoc != null)
			mouseDoc.setClickDesc(s);
	}

	public void setMouseDoc(PNode source, boolean state) {
		String desc = null;
		if (state) {
			if (source instanceof Button)
				desc = ((Button) source).mouseDoc;
			else if (source instanceof Boundary)
				desc = ((Boundary) source).mouseDoc;
		}
		setClickDesc(desc);
	}

	public void setMouseDoc(String doc, boolean state) {
		setClickDesc(state ? doc : null);
	}

	void setMouseDoc(Markup doc, boolean state) {
		setClickDesc(state ? doc : null);
	}

	APText oneLineLabel() {
		APText result = new APText(font);
		result.setWrapText(false);
		result.setConstrainHeightToTextHeight(false);
		result.setHeight(lineH);
		return result;
	}

	public Color clusterTextColor(Cluster cluster) {
		Color[] family;
		int fadeIndex = 1;
		if (cluster == highlightedCluster)
			fadeIndex++;
		if (query.usesCluster(cluster)) {
			fadeIndex++;
			family = Markup.greens;
		} else if (cluster != null
				&& cluster.pValue() < pValue()
						/ Math.pow(nBars(true), (cluster.size() - 1))) {
			family = Markup.blues;
		} else {
			family = Markup.whites;
		}
		// 1 <= fadeIndex <= 3
		return family[fadeIndex];
	}

	public Color facetTextColor(Perspective facet) {
		return facetTextColor(facet, 1);
	}

	public Color facetTextColor(Perspective facet, int onCount, Cluster cluster) {
		if (facet != null)
			return facetTextColor(facet, onCount);
		else
			return clusterTextColor(cluster);
	}

	public Color facetTextColor(Perspective facet, int onCount) {
		return facetTextColor(facet, onCount, -2);
	}

	public Color facetTextColor(Perspective facet, int onCount, int colorIndex) {
		// Util.print("Art.facetBarColor " + facet);
		// + " zero: "
		// + (facet.getOnCount() == 0) + "; highlighted: "
		// + (facet == highlightedFacet) + "; isRestriction: "
		// + facet.isRestriction());
		Color[] family;
		int fadeIndex = 1;
		if (facet == null)
			// for Clusters
			family = Markup.whites;
		else {
			if (facet == highlightedFacet)
				fadeIndex++;
			else if (onCount == 0)
				fadeIndex--;
			if (facet.isRestriction(true)) {
				fadeIndex++;
				family = Markup.greens;
			} else if (facet.isRestriction(false)) {
				fadeIndex++;
				family = Markup.reds;
			} else {
				if (colorIndex == -2)
					colorIndex = chiColorFamily(facet);
				switch (colorIndex) {
				case -1:
					family = Markup.oranges;
					break;
				case 1:
					family = Markup.blues;
					break;
				default:
					family = Markup.whites;
					break;
				}
			}
		}
		// 0 <= fadeIndex <= 3
		// default is 1; lose 1 for zero count; gain 1 for highlight, in filter
		return family[fadeIndex];
	}

	private double pValue = 0.0001;

	private int nBars = 0;

	private int nNonZeroBars = 0;

	double pValue() {
		return pValue;
	}

	/**
	 * Correct for multiple significance tests with the Bonferroni procedure
	 */
	void updatePvalueOLD() {
		pValue = 0.01 / summary.nBars();
		// Util.print("pValue => " + pValue);
	}

	/**
	 * Correct for multiple significance tests with the Benjamini and Yekutieli
	 * procedure, assuming positive correlation among tests. See
	 * http://en.wikipedia.org/wiki/False_discovery_rate
	 */
	void updatePvalue() {
		double[] pValues = summary.pValues();
		nBars = pValues.length;
		assert nBars == summary.nBars() : nBars + " " + summary.nBars();
		nNonZeroBars = 0;
		for (int i = nBars - 1; i >= 0; i--) {
			if (pValues[i] < 0 || pValues[i] >= 0.01) {
				pValues[i] = 1.0;
			} else {
				nNonZeroBars++;
			}
		}
		Arrays.sort(pValues);
		double x = 0.01 / nBars;
		pValue = x;
		for (int i = nNonZeroBars - 1; i > 1 && pValues[i] > pValue; i--) {
			// if(i % 1000 == 0)
			// Util.print(i + " " + pValues[i] + " " + (i*x) + " " + pValue);
			if (pValues[i] <= i * x) {
				pValue = pValues[i];
				// Util.print(i);
				break;
			}
		}
		// Util.print("pValue => " + pValue + " (old would be " + (0.01 / nBars)
		// + ") " + nNonZeroBars + " " + nBars);
	}

	int nBars(boolean nonZeroOnly) {
		return nonZeroOnly ? nNonZeroBars : nBars;
	}

	public int chiColorFamily(ItemPredicate facet) {
		return facet.chiColorFamily(pValue);
	}

	// public Color facetBarColor(Perspective facet, Perspective[] restrictions)
	// {
	// int fadeIndex = (facet == highlightedFacet) ? 1 : 0;
	// if (Util.isMember(restrictions, facet))
	// fadeIndex++;
	// return Art.whites[fadeIndex];
	// }
	//
	// public Color facetBarColor(boolean state, boolean isSelected) {
	// int fadeIndex = state ? 1 : 0;
	// if (isSelected)
	// fadeIndex++;
	// return Art.whites[fadeIndex];
	// }

	void clearQuery() {
		query.clear();
		updateAllData();
		// if (isMultipleFilters)
		// setTip("To clear a single filter, click on the value again.", false);
	}

	public void restrict() {
		printUserAction(Bungee.RESTRICT, 0, 0);
		Markup desc = query.description();
		desc.remove(0); // ...
		desc.remove(0); // for
		desc.remove(desc.size() - 1); // .
		header.setDBdescription(TextNfacets.toText(desc), true);
		query.restrict();
		updateAllData();
		summary.restrict();
	}

	boolean removeInitialHelp() {
		boolean result = help.getParent() != null;
		if (result) {
			getCanvas().getLayer().removeChild(help);
		}
		return result;
	}

	boolean isShowingInitialHelp() {
		return help.getParent() != null;
	}

	void updateAllData() {
		// Util.print("Art.updateAllData ");
		summary.synchronizeWithQuery();

		// Do this in redraw instead, because counts might be transiently zero
		// here.
		// selectedItem.synchronizeWithQuery(query);
		// if (perspectiveList != null)
		// perspectiveList.redraw();
		if (updater.update()) {
			// Calls q.updateAllData and then doRedraw.
			// Util.print("updateAllData/handleCursor true");
			handleCursor(true);
		} else {
			query.setQueryInvalid();
		}
	}

	int waiting;

	void handleCursor(boolean wait) {
		synchronized (waitCursor) {
			int oldWaiting = waiting;
			waiting += wait ? 1 : -1;
			// Util.print("handleCursor " + wait + " " + waiting);
			if (waiting > 0 != oldWaiting > 0) {
				if (wait)
					getCanvas().pushCursor(waitCursor);
				else
					getCanvas().popCursor();
				// Util.print("....handleCursor return " + waiting);
				// Util.printStackTrace();
			}
		}
	}

	private transient Runnable doRedraw;

	Runnable getRedrawer() {
		if (doRedraw == null)
			doRedraw = new Runnable() {

				public void run() {
					try {
						// Util.print("doRedraw");
						query.setQueryValid();
						updatePvalue();
						showFacet(highlightedFacet, true);
						summary.updateData();
						selectedItem.synchronizeWithQuery();
						if (perspectiveList != null)
							perspectiveList.redraw();
						if (clusterViz != null)
							clusterViz.redraw();
					} catch (Throwable e) {
						System.err
								.println("Aborting doRedraw - probably user has updated query since we were called.");
						e.printStackTrace();
					} finally {
						// Util.print("getRedrawer/handleCursor false");
						handleCursor(false);
					}
				}
			};
		return doRedraw;
	}

	// private IntHashtable onItems = new IntHashtable();
	//
	// private boolean isOn(int item) {
	// return onItems.get(item) != null;
	// }

	// private void updateOnItems(int onCount) {
	// onItems.clear();
	// grid.ensureRangeInternal(0, onCount);
	// // onItems = new int[onCount];
	// // System.arraycopy(grid.offsetItemTable, 0, onItems, 0, onCount);
	// // Arrays.sort(onItems);
	// for (int i = 0; i < onCount; i++)
	// onItems.put(grid.offsetItemTable[i], this);
	// // Util.print("Found " + onItems.size() + " on items");
	// }

	private transient Runnable onItemsUpdated;

	Runnable getOnItemsUpdated() {
		if (onItemsUpdated == null)
			onItemsUpdated = new Runnable() {

				public void run() {
					// Util.print("Art.onItemsUpdated");
					int onCount = query.onCount;
					grid.dataUpdated();

					// if (showImages) {
					// updateOnItems(onCount);
					// }

					selectedItem.hide(onCount == 0);
					if (onCount == 0 && !query.isShortSearch()) {
						setTip("No results. You need to remove some filters.  (There are now "
								+ query.describeNfilters() + ")");
					}
				}
			};
		return onItemsUpdated;
	}

	void waitForValidQuery() {
		query.waitForValidQuery();
	}

	// void waitForQuery() {
	// while (query == null) {
	// try {
	// wait();
	// } catch (InterruptedException e) {
	// }
	// }
	// waitForValidQuery();
	// }

	// synchronized void setQuery(Query q) {
	// synchronized (waitCursor) {
	// // Util.print("setQueryValid");
	// query = q;
	// notifyAll();
	// }
	// }

	public Rectangle getDefaultFrameBounds() {
		return new Rectangle(0, 0, 1000, 700); // 888, 688);
	}

	Perspective arrowFocus;

	public boolean handleArrow(int key, int modifiers) {
		// Util.print("Art.handleArrow " + arrowFocus);
		if (Util.isAltDown(modifiers)) {
			return editArrow(key);
		} else if (arrowFocus == null) {
			grid.handleArrow(key);
			return true;
		} else {
			ensurePerspectiveList(true);
			Perspective newFocus = perspectiveList.handleArrow(arrowFocus, key,
					modifiers);
			if (newFocus != null)
				arrowFocus = newFocus;
			return newFocus != null;
		}
	}

	void ensurePerspectiveList(boolean state) {
		if (perspectiveList == null) {
			if (state) {
				perspectiveList = new PerspectiveList(this);
				validatePerspectiveList();
			}
		} else {
			perspectiveList.show(state);
		}
	}

	public void artHideTransients() {
		mayHideTransients();
	}

	public String getBugInfo() {
		return "db=" + dbName + "&Session=" + query.getSession();
	}

	void printUserAction(int location, Perspective facet, int modifiers) {
		printUserAction(location, Integer.toString(facet.getID()), modifiers);
	}

	void printUserAction(int location, int item, int modifiers) {
		printUserAction(location, Integer.toString(item), modifiers);
	}

	void printUserAction(int location, String object, int modifiers) {
		if (isPrintUserActions) {
			if (actionPrinter == null) {
				actionPrinter = new ActionPrinter(query);
				actionPrinter.start();
			}
			assert location >= BAR && location <= CLUSTER : location;

			List v = new Vector();
			v.add(new Integer(location));
			v.add(object);
			v.add(new Integer(modifiers));
			actionPrinter.add(v);

			// userActions.append("\n").append(new Date().getTime() - startTime)
			// .append("\t").append(location).append("\t").append(object);
			// if (modifiers != 0)
			// userActions.append("\t").append(modifiers);
		}
	}

	double numWidth(int n) {
		assert n >= 0 : n;
		if (n < 10)
			return digitWidth;
		else if (n < 100)
			return 2 * digitWidth;
		else if (n < 1000)
			return 3 * digitWidth;
		else if (n < 10000)
			return 4 * digitWidth + commaWidth;
		else
			return 5 * digitWidth + commaWidth;
	}

	private static String[] strings = spaceStrings(100);

	static String[] spaceStrings(int n) {
		String[] result = new String[n];
		String s = "";
		for (int i = 0; i < n; i++) {
			result[i] = s;
			s += " ";
		}
		return result;
	}

	String stringForWidth(double width) {
		int n = (int) (width / spaceWidth + 0.5);
		if (strings.length <= n)
			strings = spaceStrings(n + 50);
		return strings[n];
	}

	String facetLabel(Perspective facet, double numY, double nameY,
			int onCount, boolean numFirst, boolean showChildIndicator,
			boolean showCheckBox, boolean justify, PerspectiveObserver redraw) {
		if (nameY < 0)
			nameY = 9999;
		StringBuffer name = new StringBuffer(100);
		if (showCheckBox && facet.getParent() != null) {
			name.append(checkBoxPrefix);
			nameY -= checkBoxWidth;
		}
		boolean hasChildren = showChildIndicator && facet.getParent() != null
				&& facet.isEffectiveChildren();
		if (hasChildren)
			nameY -= childIndicatorWidth;
		if (numFirst && numY > 0) {
			name.append(stringForWidth(numY - numWidth(onCount)));
			name.append(Util.addCommas(onCount));
			name.append("  ");
		}
		String namePart = truncateText(facet, nameY, redraw);
		name.append(namePart);
		// can't append before truncation or the suffix would be truncated.
		if (hasChildren)
			name.append(childIndicatorSuffix);
		if (justify)
			name
					.append(stringForWidth(nameY
							- truncatedTextWidth(facet, nameY)));
		if (!numFirst && numY > 0) {
			name.append(stringForWidth(numY - numWidth(onCount)));
			name.append(Util.addCommas(onCount));
		}
		// Util.print("facetLabel " + onCount + " '" + name.toString() + "'");
		return name.toString(); // Don't trim -- need initial spaces to position
		// past checkboxes
	}

	String facetLabel(Perspective v, double numY, double nameY,
			boolean numFirst, boolean showChildIndicator, boolean showCheckBox,
			boolean justify, PerspectiveObserver redraw) {
		int onCount = v.getOnCount();
		if (onCount < 0) {
			// If we've just selected the first facet, query will be restricted
			// but onCount won't be computed yet.
			onCount = v.totalCount;
		}
		return facetLabel(v, numY, nameY, onCount, numFirst,
				showChildIndicator, showCheckBox, justify, redraw);
	}

	public double getFacetStringWidth(Object facet, boolean showChildIndicator,
			boolean showCheckBox) {
		if (facetNameWidths == null)
			facetNameWidths = new Hashtable();
		Double cached = (Double) facetNameWidths.get(facet);
		Perspective p = null;
		if (facet instanceof Perspective)
			p = (Perspective) facet;
		double result;
		if (cached == null) {
			String name = p != null ? p.getNameIfPossible() : facet.toString();
			if (name != null) {
				result = getStringWidth(name);
				cached = new Double(result);
				facetNameWidths.put(facet, cached);
			} else
				result = 5;
		} else
			result = cached.doubleValue();
		if (p != null && showChildIndicator && p.isEffectiveChildren()
				&& p.getParent() != null)
			result += childIndicatorWidth;
		if (p != null && showCheckBox && p.getParent() != null)
			result += checkBoxWidth;
		return result;
	}

	public String truncateText(Perspective facet, double _w,
			PerspectiveObserver redraw) {
		// Util.print(w + " " + getStringWidth(s) + " " + s);
		if (_w < 1.0)
			return "";
		int iw = (int) _w;
		String name = facet.getName(redraw);
		int wBound = maxCharW * name.length();
		if (wBound <= iw /* || getFacetStringWidth(facet, false, false) < _w */)
			return name;
		Hashtable[] tables = truncatedStrings == null ? null
				: (Hashtable[]) truncatedStrings.get();
		if (tables == null || tables.length <= iw) {
			tables = new Hashtable[iw + 500];
			truncatedStrings = new SoftReference(tables);
		}
		Hashtable truncatedStringsTable = tables[iw];
		if (truncatedStringsTable == null) {
			truncatedStringsTable = new Hashtable();
			tables[iw] = truncatedStringsTable;
		}
		String trunc = (String) truncatedStringsTable.get(facet);
		if (trunc == null) {
			trunc = edu.cmu.cs.bungee.piccoloUtils.gui.Util.truncateText(name, iw, font);
			// assume that if getName returns '?' we returned above (dx >= 0)
			truncatedStringsTable.put(facet, trunc);
		}
		return trunc;
	}

	public double truncatedTextWidth(Perspective facet, double _w) {
		// Util.print(w + " " + getStringWidth(s) + " " + s);
		return Math.min(_w, getFacetStringWidth(facet, false, false));
	}

	public double getStringWidth(String s) {
		assert s != null;
		double result = Math.ceil(edu.cmu.cs.bungee.piccoloUtils.gui.Util.getStringWidth(s, font));
		// Util.print(((int) result) + " "
		// + ((int) getGraphics().getFontMetrics(font).stringWidth(s)));
		// These don't give the same result
		// assert Math.ceil(SwingUtilities.computeStringWidth(fontMetrics, s))
		// == result : SwingUtilities
		// .computeStringWidth(fontMetrics, s)
		// + " " + gui.Util.getStringWidth(s, font) + " '" + s + "'";
		return result;
	}

	// public double getStringHeight(String s, double width) {
	// return Math.ceil(gui.Util.getStringHeight(s, (float) width, font));
	// }

	// public static int nLines(String s) {
	// int n = 1;
	// int index = -1;
	// while ((index = s.indexOf('\n', index + 1)) >= 0)
	// n++;
	// return n;
	// }

	// private static Vector mutexes = new Vector();
	//	
	// public static boolean pre_sync(Object o, String desc) {
	// boolean result = mutexes.contains(o);
	// if (result)
	// System.err.println(desc + " waiting for " + o);
	// mutexes.add(o);
	// return result;
	// }
	//	
	// static public void post_sync(boolean isWaited, Object o, String desc) {
	// assert mutexes.contains(o);
	// mutexes.remove(o);
	// if (isWaited) {
	// System.err.println(desc + " finished waiting for " + o);
	// }
	// }

	private class KeyEventHandler extends MyInputEventHandler {

		public final int[] arrowKeys = { java.awt.event.KeyEvent.VK_KP_DOWN,
				java.awt.event.KeyEvent.VK_KP_UP,
				java.awt.event.KeyEvent.VK_KP_LEFT,
				java.awt.event.KeyEvent.VK_KP_RIGHT,
				java.awt.event.KeyEvent.VK_DOWN, java.awt.event.KeyEvent.VK_UP,
				java.awt.event.KeyEvent.VK_LEFT,
				java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.KeyEvent.VK_A,
				java.awt.event.KeyEvent.VK_END, java.awt.event.KeyEvent.VK_HOME };

		public final int[] shiftKeys = { java.awt.event.KeyEvent.VK_CONTROL,
				java.awt.event.KeyEvent.VK_SHIFT };

		KeyEventHandler() {
			super(Bungee.class);
		}

		public boolean keyPress(int key, PInputEvent e) {
			// Util.print("keyPress " + (key - '0'));
			if (Util.isMember(arrowKeys, key)) {
				return handleArrow(key, e.getModifiersEx());
			} else if (Util.isMember(shiftKeys, key)) {
				rehighlightFacet(e.getModifiersEx(), false);
			} else if (key == ' ') {
				showMoreHelp();
			} else if (key == 'G') {
				extraGreen = !extraGreen;
				Util.print("Show extra green: " + extraGreen);
			} else if (key == 'P') {
				showFacet(null, false);
				toggleIsPopups();
				Util.print("Show popups: " + getIsPopups());
			} else if (key == 'C') {
				showClusters();
			} else if (editMenu != null && key > '0'
					&& key - '0' <= editMenu.nChoices()) {
				editMenu.choose(key - '1');
			} else {
				artHideTransients();
				return false;
			}
			return true;
		}

		public boolean keyRelease(int key, PInputEvent e) {
			if (Util.isMember(shiftKeys, key)) {
				rehighlightFacet(e.getModifiersEx(), false);
				return true;
			}
			return false;
		}
	}

	void showClusters() {
		// Perspective[] associates = query.associates();
		// Util.print(associates.length + " Associates:");
		// PrintArray.printArray(associates);
		// Perspective[] previous = null;
		// for (int i = 0; i < associates.length; i++) {
		// if (!Util.isMember(previous, associates[i])) {
		// Cluster cluster = query.cluster(associates[i]);
		// if (cluster != null) {
		// Util.print("Cluster of " + cluster.size());
		// PrintArray.printArray(cluster);
		// previous = (Perspective[]) Util.append(previous,
		// cluster.facets, Perspective.class);
		// }
		// }
		// }
		printUserAction(KEYPRESS, "C", 0);
		if (query.isRestricted()) {
			if (clusterViz == null) {
				clusterViz = new ClusterViz(this);
			}
			if (clusterViz.isHidden())
				getCanvas().getLayer().addChild(clusterViz);
			validatePerspectiveList();
			clusterViz.showClusters();
		} else {
			setTip("Can't find clusters if there are no filters.");
		}
	}

	String aboutCollection() {
		return query.aboutCollection();
	}

	public void itemMiddleMenu(Item item, boolean isRight) {
		if (Query.isEditable) {
			if (isRight) {
				if (selectedForEdit != null) {
					Item oldItem = selectedItem.currentItem;
					selectedItem.currentItem = item;
					doEdit("Add to Selected Result: ");
					selectedItem.currentItem = oldItem;
				} else {
					Util
							.err("Can't add facet to item because selectedForEdit is null");
				}
			} else {
				if (editMenu != null)
					editMenu.removeFromParent();
				editMenu = new Menu(Color.black, Color.white, getEdit(), font);
				editMenuItem = item;
				addEditButton("Rotate 90 degrees clockwise: " + item);
				addEditButton("Rotate 180 degrees clockwise: " + item);
				addEditButton("Rotate 270 degrees clockwise: " + item);
				editMenu.value.setText("");
				getCanvas().getLayer().addChild(editMenu);
				editMenu.pick();
			}
		} else
			Util.err("Editing is disabled.");
	}

	public void facetMiddleMenu(Perspective facet) {
		if (Query.isEditable) {
			if (editMenu != null)
				editMenu.removeFromParent();
			editMenu = new Menu(Color.black, Color.white, getEdit(), font);
			editMenuPerspective = facet;
			addEditButton("Set edit selection to " + facet);
			addEditButton("Add to Selected Result: " + facet);
			addEditButton("Add to Result Set: " + facet);
			String name = summary.queryViz.editor.getText().trim();
			if (name.length() > 0) {
				addEditButton("Add child facet " + name + " to " + facet);
				addEditButton("Rename " + facet + " to " + name);
			}
			// addEditButton("Delete this facet");
			addEditButton("Remove from Selected Result: " + facet);
			addEditButton("Remove from Result Set: " + facet);
			// addEditButton("Remove from Result Set");
			if (selectedForEdit != null) {
				addEditButton("Reparent " + selectedForEdit + " to " + facet);
			}
			if (name.length() > 0)
				addEditButton("Add new facet type " + name);
			addEditButton("Writeback");
			editMenu.value.setText("");
			getCanvas().getLayer().addChild(editMenu);
			editMenu.pick();
		} else
			Util.err("Editing is disabled.");
	}

	void addEditButton(String s) {
		editMenu.addButton((editMenu.nChoices() + 1) + ". " + s, s, s);
	}

	private transient Runnable edit;

	Runnable getEdit() {
		if (edit == null)
			edit = new Runnable() {

				public void run() {
					String which = (String) editMenu.getData();
					doEdit(which);
					editMenu.removeFromParent();
					editMenu = null;
				}
			};
		return edit;
	}

	void doEdit(String which) {
		Util.print(editMenuPerspective + " " + which);
		ItemPredicate connected = summary.connectedPerspective();
		Collection updated = null;
		if (which.startsWith("Set edit selection to ")) {
			setSelectedForEdit(editMenuPerspective, 0);
		} else if (which.startsWith("Add to Selected Result: ")) {
			decacheItem(selectedItem.currentItem);
			updated = query.addItemFacet(editMenuPerspective,
					selectedItem.currentItem);
			updateSelectedItem();
		} else if (which.startsWith("Add to Result Set: ")) {
			decacheItems();
			updated = query.addItemsFacet(editMenuPerspective);
			updateSelectedItem();
		} else if (which.startsWith("Add child facet ")) {
			updated = query.addChildFacet(editMenuPerspective, which.substring(
					16).split(" to ")[0]);
		} else if (which.startsWith("Remove from Selected Result: ")) {
			decacheItem(selectedItem.currentItem);
			updated = query.removeItemFacet(editMenuPerspective,
					selectedItem.currentItem);
			updateSelectedItem();
		} else if (which.startsWith("Remove from Result Set: ")) {
			decacheItems();
			updated = query.removeItemsFacet(editMenuPerspective);
			updateSelectedItem();
		} else if (which.startsWith("Reparent ")) {
			updated = query.reparent(editMenuPerspective, selectedForEdit);
		} else if (which.startsWith("Rename ")) {
			String newName = which.split(" to ")[1];
			query.rename(editMenuPerspective, newName);
			editMenuPerspective.setName(newName);
		} else if (which.equals("Writeback")) {
			query.writeback();
			dispose();
		} else if (which.startsWith("Add new facet type ")) {
			updated = query.addChildFacet(null, which.substring(19));
		} else if (which.startsWith("Rotate ")) {
			decacheItem(selectedItem.currentItem);
			query.rotate(editMenuItem, which.substring(7, 10).trim());
			updateSelectedItem();
		} else {
			Util.err("Unknown choice: " + which);
		}
		if (updated != null && updated.size() > 0) {
			Util.print("Reverting PerspectiveViz's for "
					+ PrintArray.printArrayString(updated));
			Collection unchanged = new ArrayList(query.displayedPerspectives());
			unchanged.removeAll(updated);
			summary.synchronizePerspectives(unchanged);
		}
		updateAllData();
		if (connected != null && summary.connectedPerspective() != connected)
			summary.connectToPerspective(connected);
	}

	void setSelectedForEdit(Perspective facet, int modifiers) {
		if (Query.isEditable) {
			// Util.print("setSelectedForEdit " + facet + " " + modifiers + " "
			// + InputEvent.CTRL_DOWN_MASK + " "
			// + (modifiers & InputEvent.CTRL_DOWN_MASK) + " "
			// + Perspective.isControlDown(modifiers) + " "
			// + selectedItem.currentItem);
			selectedForEdit = facet;
			editMenuPerspective = facet;
			summary.setSelectedForEdit(facet);
			if (Util.isControlDown(modifiers) && selectedItem.currentItem != null) {
				doEdit("Add to Selected Result: ");
				if (selectedForEdit.nChildren() > 0) {
					toggleFacetLater();
				}
			}
		} else
			Util.err("Editing is disabled.");
	}

	/**
	 * Waits for valid query and then calls toggleFacet(selectedForEdit, 0)
	 */
	void toggleFacetLater() {
		if (facetSelecter == null) {
			facetSelecter = new FacetSelecter(this);
			facetSelecter.start();
		}
		facetSelecter.update();
	}

	private transient Runnable doSelectFacet;

	Runnable getDoSelectFacet() {
		if (doSelectFacet == null)
			doSelectFacet = new Runnable() {

				public void run() {
					toggleFacet(selectedForEdit, 0);
				}
			};
		return doSelectFacet;
	}

	boolean editArrow(int key) {
		if (Query.isEditable) {
			if (selectedForEdit != null) {
				Perspective updated = null;
				Perspective parent = selectedForEdit.getParent();
				switch (key) {
				case java.awt.event.KeyEvent.VK_KP_LEFT:
				case java.awt.event.KeyEvent.VK_LEFT:
					if (parent != null) {
						updated = parent;
						if (parent.isRestriction(selectedForEdit, true)) {
							toggleFacetLater();
						}
					}
					break;
				case java.awt.event.KeyEvent.VK_KP_UP:
				case java.awt.event.KeyEvent.VK_UP:
					int index = parent != null ? selectedForEdit.getIndex()
							: selectedForEdit.getID() - 1;
					if (index > 0) {
						updated = query
								.findPerspective(selectedForEdit.getID() - 1);
					}
					break;
				case java.awt.event.KeyEvent.VK_KP_RIGHT:
				case java.awt.event.KeyEvent.VK_RIGHT:
					if (selectedForEdit.isEffectiveChildren()) {
						if (!selectedForEdit.isPrefetched()) {
							query.prefetchData(selectedForEdit);
							// query.waitForPrefetch(selectedForEdit);
						}
						updated = selectedForEdit.getNthChild(0);
					}
					break;
				case java.awt.event.KeyEvent.VK_KP_DOWN:
				case java.awt.event.KeyEvent.VK_DOWN:
					index = parent != null ? selectedForEdit.getIndex()
							: selectedForEdit.getID() - 1;
					if (parent == null) {
						updated = query
								.findPerspectiveIfPossible(selectedForEdit
										.getID() + 1);
						if (updated.getParent() != null)
							updated = null;
					} else if (index < selectedForEdit.getParent().nChildren() - 1) {
						updated = query
								.findPerspective(selectedForEdit.getID() + 1);
					}
					break;
				}
				if (updated != null) {
					setSelectedForEdit(updated, 0);
					return true;
				}
			}
		} else
			Util.err("Editing is disabled.");
		return false;
	}
	private transient BufferedImage missingImage;

	BufferedImage getMissingImage() {
		if (missingImage == null) {
			String where = codeBase() + "missing.gif";
			// Util.print(where);
			try {
				missingImage = ImageIO.read(new URL(where));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return missingImage;
	}

	transient SoftReference itemImages;

	public synchronized ItemImage ensureItemImage(Item item, int rawW, int rawH,
			int quality, InputStream blobStream) {
		ItemImage result = lookupItemImage(item);
		if (blobStream == null) {
			if (result == null) {
				BufferedImage missing = getMissingImage();
				rawW = missing.getWidth();
				rawH = missing.getHeight();
				result = new ItemImage(this, item, rawW, rawH, 100, missing);
			}
		} else {
			Image bi = Util.readCompatibleImage(blobStream);
			if (result == null)
				result =  new ItemImage(this, item, rawW, rawH, quality, bi);
			else if (!result.bigEnough(bi, quality)) {
				assert quality >= result.quality
						|| bi.getWidth(null) > result.rawImage.getWidth(null)
						|| bi.getHeight(null) > result.rawImage.getHeight(null);
				result.setRawImage(bi, quality);
			}
		}
		return result;
	}

	Map getItemImagesTable() {
		return itemImages == null ? null : (Map) itemImages.get();
	}
	
	Item lookupItem(int itemID) {
		return Item.lookupItem(itemID);
	}
	
	Item ensureItem(int itemID) {
		return Item.ensureItem(itemID);
	}

	 ItemImage lookupItemImage(Item item) {
		// Util.print("lookupItem " + item + " " + itemImages.get(item));
		Map table = getItemImagesTable();
		return table == null ? null : (ItemImage) table.get(item);
	}

	void decacheItem(Item item) {
		Util.print("decacheItem " + item);
		selectedItem.removeFacetTree(item);
		Map table = getItemImagesTable();
		if (table != null)
			table.remove(item);
	}

	void decacheItems() {
		selectedItem.facetTrees = null;
	}

	void updateSelectedItem() {
		Item item = selectedItem.currentItem;
		selectedItem.currentItem = null;
		selectedItem.animateOutline(item, -1, -1, -1, -1);
	}

	void showFacet(Perspective facet, boolean state) {
		if (isPopups && !isShowingInitialHelp()) {
			summary.showFacet(facet, state);
			mouseDoc.showFacet(null, state);
		} else
			mouseDoc.showFacet(facet, state);
	}

	void showMoreHelp() {
		if (isPopups && summary.facetDesc.getVisible())
			summary.facetDesc.showMoreHelp();
		// else
		// summary.barHelp.phase2();
	}

	public void setItemDescription(Item currentItem, String description) {
		decacheItem(currentItem);
		query.setItemDescription(currentItem, description);
		updateSelectedItem();
	}

	// private Hashtable facetImages = new Hashtable();
	//
	// public int[] facetImages(Perspective facet) {
	// return facetImagesInternal(facet)[1];
	// }
	//
	// private int[][] facetImagesInternal(Perspective facet) {
	// int[][] result = (int[][]) facetImages.get(facet);
	// if (result == null) {
	// String onSQL = "SELECT record_num, NULL FROM item_facet_heap WHERE
	// facet_id = "
	// + facet.facet_id + " ORDER BY record_num";
	// int count = facet.totalCount;
	// result = new int[2][];
	// result[0] = new int[2];
	// result[0][1] = count;
	// result[1] = new int[count];
	// if (grid.offsetItemTable == null
	// || grid.offsetItemTable.length < count) {
	// grid.offsetItemTable = new int[count];
	// }
	// db.updateOnItems(onSQL, selectedItem.currentItem, true, count);
	// grid.ensureRangeInternal(0, count);
	// System.arraycopy(grid.offsetItemTable, 0, result[1], 0, count);
	// grid.offsetItemTableRangesIndex = 0;
	// Arrays.sort(result[1]);
	// facetImages.put(facet, result);
	// }
	// for (int i = result[0][0]; i < result[0][1]; i++) {
	// int item = result[1][i];
	// GridImage gi = (GridImage) grid.thumbs.get(item);
	// if (gi != null) {
	// if (true /* gi.rawImage == null */) {
	// badFacetImage(facet, i, item);
	// } else {
	// goodFacetImage(facet, i, item);
	// }
	// }
	// }
	// return result;
	// }
	//
	// public int nFacetImages(Perspective facet) {
	// facetImages(facet);
	// return ((int[][]) facetImages.get(facet))[0][1];
	// }
	//
	// void badFacetImage(Perspective facet, int i, int item) {
	// int[][] result = (int[][]) facetImages.get(facet);
	// assert result[1][i] == item;
	// swap(i, --result[0][1], result[1]);
	// }
	//
	// void goodFacetImage(Perspective facet, int i, int item) {
	// int[][] result = (int[][]) facetImages.get(facet);
	// assert result[1][i] == item;
	// swap(i, result[0][0]++, result[1]);
	// }
	//
	// void swap(int i, int j, int[] a) {
	// assert i < a.length : i + " " + PrintArray.printArrayString(a);
	// assert j < a.length : j + " " + PrintArray.printArrayString(a);
	// if (i != j) {
	// int temp = a[i];
	// a[i] = a[j];
	// a[j] = temp;
	// }
	// }
	//
	// public BufferedImage[] getThumbs(Perspective facet, boolean isOn, int n)
	// {
	// BufferedImage[] result = new BufferedImage[n];
	// int[][] fi = null; // = facetImagesInternal(facet);
	// // int[] fImages = facetImages(facet); // sorted by whether cached
	// int ord = 0;
	// for (int i = 0; i < fi[0][1] && ord < n; i++) {
	// int item = fi[1][i];
	// if (isOn(item) == isOn) {
	// GridImage gi = (GridImage) grid.thumbs.get(item);
	// if (gi == null) {
	// int[] thumbs = { item };
	// grid.loadThumbs(thumbs);
	// gi = (GridImage) grid.thumbs.get(item);
	// }
	// BufferedImage bi = gi.rawImage;
	// if (bi == null) {
	// badFacetImage(facet, i, item);
	// } else {
	// if (i > fi[0][0])
	// goodFacetImage(facet, i, item);
	// result[ord++] = bi;
	// }
	// }
	// }
	// return result;
	// }

	// public GridImage[] getAllThumbs(Perspective facet) {
	// GridImage[] result = new GridImage[facet.totalCount];
	// ResultSet rs = query.getThumbSizes(facet.facet_id);
	// int i = 0;
	// try {
	// while (rs.next()) {
	// int item = rs.getInt(1);
	// GridImage gi = (GridImage) grid.thumbs.get(item);
	// if (gi == null) {
	// int w = rs.getInt(2);
	// int h = rs.getInt(3);
	// gi = new GridImage(this, item, w, h);
	// assert gi.rawW > 0 : facet + " " + i + " " + item + " " + w;
	// }
	// result[i++] = gi;
	// }
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	// // Util.print("getAllThumbs " + facet + " " + i);
	// return result;
	// }

	static final int BAR = 1;

	static final int BAR_LABEL = 2;

	static final int RANK_LABEL = 3;

	static final int THUMBNAIL = 4;

	static final int IMAGE = 5;

	static final int FACET_TREE = 6;

	static final int SCROLL = 7;

	static final int KEYPRESS = 8;

	static final int BUTTON = 9;

	static final int SEARCH = 10;

	static final int SETSIZE = 11;

	static final int CHECKBOX = 12;

	static final int RESTRICT = 13;

	static final int CLUSTER = 14;

	void replayOp() {
		String[] args = replayer.getArgs();
		PrintArray.printArray(args);
		int modifiers = args.length > 3 ? Integer.parseInt(args[3]) : -1;
		switch (Integer.parseInt(args[1])) {
		case BAR:
		case BAR_LABEL:
			Util.print("clickBar " + replayPerspective(args[2]) + " "
					+ modifiers);
			summary.clickBar(replayPerspective(args[2]), modifiers);
			break;
		case RANK_LABEL:
			Util.print("clickRank " + replayPerspective(args[2]));
			summary.clickRank(replayPerspective(args[2]));
			break;
		case THUMBNAIL:
			Util.print("clickThumb " + Integer.parseInt(args[2]));
			grid.clickThumb(Item.ensureItem(Integer.parseInt(args[2])));
			break;
		case IMAGE:
			Util.print("clickImage " + Integer.parseInt(args[2]));
			selectedItem.clickImage(Item.ensureItem(Integer.parseInt(args[2])));
			break;
		case FACET_TREE:
			Util.print("clickText " + replayPerspective(args[2]) + " "
					+ modifiers);
			selectedItem.clickText(replayPerspective(args[2]), modifiers);
			break;
		// case SCROLL:
		// if (args[2].equals("grid")) {
		// Util.print("scroll " + Integer.parseInt(args[3]));
		// grid.clickThumb(Integer.parseInt(args[3]));
		// }
		// break;
		case KEYPRESS:
			Util.print("key " + args[2]);
			if (args[2].equals("grid")) {
				// grid.clickThumb(Integer.parseInt(args[3]));
			} else if (args[2].equals("PerspectiveList")) {
				summary.clickBar(replayPerspective(args[3]), 0);
			} else if (args[2].equals("C")) {
				showClusters();
			} else {
				assert false : args[2];
			}
			break;
		case BUTTON:
			Util.print("button " + args[2]);
			if (args[2].equals("Clear")) {
				summary.clickClear();
			} else if (args[2].equals("Ellipsis")) {
				summary.clickEllipsis();
			} else {
				summary.removeSearch(args[2]);
			}
			break;
		case SEARCH:
			Util.print("search " + args[2]);
			summary.q.addTextSearch(args[2]);
			updateAllData();
			break;
		case SETSIZE:
			Util.print("setSize " + args[2] + " " + modifiers);
			setSize(Integer.parseInt(args[2]), modifiers);
			break;
		case RESTRICT:
			Util.print("Restrict");
			restrict();
			break;
		case CLUSTER:
			Util.print("Cluster");
			String[] facet_ids = args[2].split("/");
			Perspective[] facets = new Perspective[facet_ids.length];
			for (int i = 0; i < facet_ids.length; i++) {
				facets[i] = query.findPerspective(Integer
						.parseInt(facet_ids[i]));
			}
			Cluster cluster = new Cluster(facets);
			toggleCluster(cluster);
			break;
		default:
			assert false : args[1];
		}
		replayer.update();
	}

	Perspective replayPerspective(String arg) {
		return query.findPerspective(Integer.parseInt(arg));
	}

}

class DataUpdater extends UpdateNoArgsThread {

	Bungee art;

	public synchronized void exit() {
		art = null;
		super.exit();
	}

	DataUpdater(Bungee _art) {
		super("DataUpdater", 0);
		art = _art;
		// Util.print("Enter DataUpdater");
		// setPriority(getPriority() + 1); // This doesn't do any good
		// - it's
		// MySQL's priority we want to lower
	}

	public void run() {
		// art.db = new ServletInterface(art.server, art.dbName);
		// if (art.db.isConnected()) {
		art.query = new Query(art.server, art.dbName);

		Runnable continueSetDatabase = new Runnable() {

			public void run() {
				art.setDatabaseInternal();
			}
		};

		javax.swing.SwingUtilities.invokeLater(continueSetDatabase);
		// javax.swing.SwingUtilities.invokeLater(art.queryIsReady);
		art.query.setQueryValid();
		super.run();
		// }
	}

	public void process() {
		boolean mustGetCounts = art.query.updateOnItems(art.grid.selectedItem,
				art.grid.maxThumbs(999999));
		if (isUpToDate()) {
			javax.swing.SwingUtilities.invokeLater(art.getOnItemsUpdated());
			art.query.updateData(!mustGetCounts);
			if (isUpToDate()) {
				javax.swing.SwingUtilities.invokeLater(art.getRedrawer());
			} else {
				assert art.waiting > 1;
				// Util.print("updater aborting/handleCursor false");
				art.handleCursor(false);
			}
		} else {
			assert art.waiting > 1;
			// Util.print("updater aborting/handleCursor false");
			art.handleCursor(false);
		}
	}
}

// class StopButton extends TextButton {
//
// private static final long serialVersionUID = 2894707079836918339L;
//
// Art art;
//
// StopButton(String msg, Art _art) {
// super(msg, _art.font, 0, 0, -1, -1, null, true);
// art = _art;
// mouseDoc = "Exit";
// }
//
// public void doPick() {
// Util.print("StopButton.doPick");
// removeFromParent();
// art.stop();
// art.setNormalPaneVisibility(false);
// art.showError("Could not connect to " + art.server);
// art.handleCursor(false);
// }
//
// public void mayHideTransients(PNode node) {
// }
//
// }

class DocumentShower extends QueueThread {

	BasicService basicJNLPservice;

	Bungee art;

	public DocumentShower(BasicService _basicJNLPservice, Bungee _art) {
		super("DocumentShower", 0);
		basicJNLPservice = _basicJNLPservice;
		art = _art;
	}

	public void process(Object URLstring) {
		if (URLstring instanceof Item) {
			Query q = art.query;
			if (q != null) {
				URLstring = q.getItemURL((Item) URLstring);
				if (URLstring == null)
					return;
			} else
				return;
		}
		if (URLstring.equals("About this collection"))
			URLstring = art.aboutCollection();

		String URLs = (String) URLstring;
		if (URLs.indexOf("library.pitt.edu") > 0) {
			// LoC & MedART can't handle additional query args
			String prefix = "?";
			if (URLs.indexOf('?') > 0)
				prefix = "&";
			try {
				URLs += prefix + "referer="
						+ URLEncoder.encode(art.bookmark(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		if (basicJNLPservice != null) {
			try {
				basicJNLPservice.showDocument(new URL(URLs));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Runtime.getRuntime().exec(
						"C:\\Program Files\\Mozilla Firefox\\firefox.exe \""
								+ URLs + "\"");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

class ActionPrinter extends QueueThread {

	Query db;

	public ActionPrinter(Query _db) {
		super("ActionPrinter", null, false, 0);
		db = _db;
	}

	public void process(Object args) {
		List v = (List) args;
		Integer location = (Integer) v.get(0);
		String object = (String) v.get(1);
		Integer modifiers = (Integer) v.get(2);
		db.printUserAction(location.intValue(), object, modifiers.intValue());
	}
}

class FacetSelecter extends UpdateNoArgsThread {

	Bungee art;

	public FacetSelecter(Bungee _art) {
		super("FacetSelecter", 0);
		art = _art;
	}

	public void process() {
		art.waitForValidQuery();
		javax.swing.SwingUtilities.invokeLater(art.getDoSelectFacet());
	}
}

class Replayer extends UpdateNoArgsThread {

	private String[] ops;

	private int opNum;

	private long opTime = 0;

	Bungee art;

	public Replayer(Bungee _art, String[] _ops) {
		super("Replayer", 0);
		ops = _ops;
		art = _art;
		opTimer.setRepeats(false);
	}

	public void run() {
		opNum = 0;
		opTime = now();
		super.run();
	}

	private Timer opTimer = new Timer(100, new ActionListener() {

		public void actionPerformed(ActionEvent ignore) {
			replayOps();
		}
	});

	public void process() {
		replayOps();
	}

	void replayOps() {
		// Util.print("replayOps " + opNum);
		// for (int i = opNum; i < ops.length; i++) {
		if (opNum < ops.length) {
			long now = now();
			long delay = opTime - now;
			// Util.print(opNum + " delay = " + delay);
			// if (startTime == 0)
			// // wait for delayedInit
			// delay = 1000;
			if (delay > 10) {
				opTimer.setInitialDelay((int) delay);
				opTimer.restart();
			} else {
				art.waitForValidQuery();
				if (art.selectedItem.setter.isIdle()) {
					opNum++;
					if (opNum < ops.length) {
						String[] args = Util.splitComma(ops[opNum]);
						String[] prevArgs = Util.splitComma(ops[opNum - 1]);
						delay = 1000 * (Integer.parseInt(args[0]) - Integer
								.parseInt(prevArgs[0]));
						opTime = now + delay;
						Util.print("replayOp " + (opNum - 1) + " delay = "
								+ delay);
					}

					Runnable doReplay = new Runnable() {

						public void run() {
							art.replayOp();
						}
					};

					javax.swing.SwingUtilities.invokeLater(doReplay);
				} else {
					opTimer.setInitialDelay(100);
					opTimer.restart();
				}
			}
		}
		// }
		// Util.print("replayOps done.");

		// summary.clickClear();
		// startTime = new Date().getTime();

		// art.setDatabase(null);
		// startTime = 0;
		//
		// opNum = 0;
		// replayOps();
	}

	/**
	 * @return time in milliseconds
	 */
	long now() {
		return new Date().getTime();
	}

	String[] getArgs() {
		return Util.splitComma(ops[opNum - 1]);
	}

}