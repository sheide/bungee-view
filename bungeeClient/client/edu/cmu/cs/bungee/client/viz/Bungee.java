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
 * Partial unrestrict
 * 
 * Copy text in text boxes; highlight search terms.
 * 
 * Fancy thumb scrolling: when sorted, show facet labels to the right of the
 * scroll bar when scrolling. Label the first thumb with each facet value (on
 * top). havea a button to lock each column.
 * 
 * Tabs for history (query, selected images), top tags, favorites.
 * 
 * XML query rep [parent query, searches, required, excluded] & list rep.
 * Bookmark with the former; send the latter to the server, and construct the
 * query there. Markup should have been HTML.
 * 
 * perspective lists aren't highlighting
 * 
 * get rid of clusters in favor of top 10/bottom 10 lists
 * 
 * PerspectiveLists show tags that have zero totalCount in restricted set.
 * 
 * Light beams aren't right when you zoom
 * 
 * History mechanism
 * 
 * Query for percentage on even for deeply nested facets. Pass more arguments
 * analogous to relevantFacets, to get counts for nested facets of the selected
 * item along with all the other counts.
 * 
 * PerspectiveList headers should color facet name and use search color for
 * other text.
 * 
 * When you switch modes, bar label counts don't always line up. Maybe the
 * problem is when they've previsouly been displayed in selected item frame?
 * 
 * HP database: lose centuries and only group dates by decade. Days should be '1
 * June 1944' not '1'. Music database: many Labels' names start with blank
 * space; Artists not alphabetized right. HistoryMakers - why are there
 * singletons, e.g. Date of Interview > 1993 > January?
 * 
 * Document BV ontology. eg are tags and categories the same kind of object?
 * They use the same color scheme, so why can't you click on Oscar to select
 * Oscar-winning movies?
 * 
 * in expert mode, could have tabs like informedia. A "Save" button would save
 * the current query in a new tab. Then you could have a tab menu to AND or OR
 * with the current query. You could also do "compare" where the current query
 * becomes the OR and you restrict to either one.
 * 
 * Shot collector
 * 
 * Limit inf search names to width of frame, with rollover expansion like
 * summary description.
 * 
 * Tweedie-style bars showing expected, current, current except for us, current
 * except for 1 other.
 * 
 * Sort by relevance as one menu option. Cluster by similarity another option.
 * SELECT *, match(facet_names, title) against ('eastwood') relevance FROM item
 * order by relevance desc
 * 
 * new attribute for number of clicks (facets and items). Doesn't make sense for
 * facets. Instead color background of bar to show popularity.
 * 
 * This file has way too much crap in it, which should be moved to
 * javaExtentions, FacetText, or it's own file.
 * 
 * Rationalize startup: print start/end time for creating each frame, and its
 * "delayed init", and see whether we're really saving anything with the extra
 * complication. Also nest instantiatedPerspective inside Perspective.
 * 
 * Widget to limit cluster size (especially to 1)
 * 
 * Brush grid scrollbar if ordered by an attribute. scrollbar intervals
 * correspond to tags. Intervals can be computed from perspective cumOnCounts.
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
 * Endeca automatically drills down when only one tag has non-zero count
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
 * For HPJmeter, use -Xrunhprof:cpu=samples,thread=y,depth=20,cutoff=0 and
 * possibly heap=all
 */

//import edu.cmu.cs.bungee.faceImage.FaceImage;
import javax.imageio.ImageIO;
import javax.jnlp.BasicService;
import javax.jnlp.ClipboardService;
import javax.jnlp.ServiceManager;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Insets;
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
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import javax.swing.Timer;

//import com.sun.image.codec.jpeg.ImageFormatException;

import edu.cmu.cs.bungee.client.query.Cluster;
import edu.cmu.cs.bungee.client.query.ItemPredicate;
import edu.cmu.cs.bungee.client.query.Markup;
import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.client.query.tetrad.NonAlchemyModel;
import edu.cmu.cs.bungee.javaExtensions.PerspectiveObserver;
import edu.cmu.cs.bungee.javaExtensions.URLQuery;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.javaExtensions.threads.QueueThread;
import edu.cmu.cs.bungee.javaExtensions.threads.UpdateNoArgsThread;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.Menu;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PInputManager;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolox.PFrame;

final class Bungee extends PFrame {

	static final String version = "7 February 2008";

	private static final boolean isPrintUserActions = true;

	/** ************* Start of tweakable parameters *************** */

	static final int ThumbQuality = 50;

	static final int ImageQuality = 100;

	// int textH = 14;
	static final int MIN_TEXT_HEIGHT = 8;

	// private int minW = 612;

	private int minH = 459;

	boolean smallWindowOK;

	/**
	 * The margin between Summary/Results and Results/SelectedItem. Redundant
	 * with ResultsGrid.marginSize
	 */
	private static final double regionMargins = 0;

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
	final static Color PERCENT_LABEL_COLOR = Markup.UNASSOCIATED_COLORS[1];

	static final Paint selectedItemOutlineColor = Color.yellow; // new

	// Color(0xFF9900);

	static final Color headerBG = new Color(0x001a66);

	static final Color headerFG = new Color(0x699999);

	static final Color helpColor = Color.orange;

	static final Color mouseDocFG = headerFG; // helpColor; // new
	// Color(0x99CCCC);

	static final Color summaryFG = Markup.UNASSOCIATED_COLORS[0]; // new Color(
	// 0x666699
	// );

	static final Color summaryBG = Color.black;
	// summaryFG.darker().darker().darker().darker().darker();

	// Color.black; // anything but black makes the

	// light beams look funny.

	final static Color selectedItemFG = new Color(0x996666); // new
	// Color(0x999966);

	final static Color selectedItemBG = Color.black;
	// selectedItemFG.darker().darker().darker().darker().darker();

	final static Color gridFG = selectedItemFG; // new Color(0x896699); // new
	// Color(0x669966);

	final static Color gridBG = Color.black;
	// gridFG.darker().darker().darker().darker().darker();

	final static Color textScrollFG = selectedItemFG; // new Color(0x666633);

	final static Color textScrollBG = textScrollFG.darker().darker(); // new
	// Color(0x333300);

	final static Color facetTreeScrollFG = Markup.UNASSOCIATED_COLORS[0]; // new
	// Color(0x666666);

	final static Color facetTreeScrollBG = facetTreeScrollFG.darker().darker(); // new
	// Color(0x333333);

	final static Color gridScrollFG = gridFG; // new Color(0x336633);

	final static Color gridScrollBG = gridScrollFG.darker().darker(); // new
	// Color(0x003300);

	static final Color checkColor = // Color.getHSBColor(0.33f, 1.0f, 0.8f);
	Markup.INCLUDED_COLORS[1];

	static final Color checkFG = // new Color(0x445544);
	Util.desaturate(Markup.POSITIVE_ASSOCIATION_COLORS[0], -1).darker();

	// static final Color checkBG = new Color(0x223322);

	static final Color xColor = // Color.getHSBColor(0.0f, 1.0f, 0.8f);
	Markup.NEGATIVE_ASSOCIATION_COLORS[1];

	static final Color xFG = // new Color(0x554444);
	Util.desaturate(Markup.EXCLUDED_COLORS[0], -1).darker();

	// static final Color xBG = new Color(0x332222);

	// Shows chars available in your Java environment:
	// http://www.pccl.demon.co.uk/java/unicode.html
	static final String childIndicatorSuffix = "\u2193";

	/**
	 * Enough space characters to equal the width of the boxes we'll draw.
	 */
	static final String checkBoxPrefix = "      "; // "\u2751 "; // "\u0000";

	// //'\u2193';

	// //'\u00BB'
	// '\u21B4'

	// '\u21B3'

	// delete button is F078; checkbox is F0FE; delete is FoFD

	double parentIndicatorWidth;

	// boolean isPopups = true;

	/** ************* End of tweakable parameters *************** */

	public Preferences features = Preferences.defaultFeatures;

	private double digitWidth;

	private double commaWidth;

	double spaceWidth;

	double childIndicatorWidth;

	double checkBoxWidth;

	int h;

	int w;

	Font font;

	// public FontMetrics fontMetrics;

	double lineH;

	double scrollBarWidth;

	/**
	 * Space to the right and left of scroll bars. Set in setTextSize to a
	 * fraction of the text size.
	 */
	double scrollMarginSize;

	// double headerH;

	double mouseDocH;

	// public static final boolean isApplet = PApplet.class
	// .isAssignableFrom(Art.class);

	// ServletInterface db;

	Query query;

	Header header;

	MouseDocLine mouseDoc;

	LazyPNode help;

	Summary summary;

	ResultsGrid grid;

	SelectedItem selectedItem;

	ExtremeTags extremeTags;

	ClusterViz clusterViz;

	private Cursor waitCursor;

	private Hashtable facetNameWidths;

	private transient SoftReference truncatedStrings;

	String dbName;

	DataUpdater updater;

	private transient FacetSelecter facetSelecter;

	DocumentShower documentShower;

	// ActionPrinter actionPrinter;

	Replayer replayer;

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
	static final FacetClickHandler facetClickHandler = new FacetClickHandler();

	/**
	 * Whether the query and frames have been created (though not necessarily
	 * validated)
	 */
	boolean isReady = false;

	// private APText tooSmall;

	BasicService basicJNLPservice;

	ClipboardService jnlpClipboardService;

	// AppletContext appletContext;

	// private StringBuffer userActions;

	private String argString;

	String server;

	private int maxCharW;

	/**
	 * Choose a number of thumbnail columns so that the grid is at least this
	 * tall and wide. (Thumbnails will be at least MIN_THUMB_SIZE - 2 *
	 * THUMB_BORDER)
	 */
	static final double MIN_THUMB_SIZE = 80;

	private final static String fontFamily = "SansSerif";

	private final static int fontStyle = Font.BOLD;

	Set highlightedFacets;

	Set highlightedClusters;

	Informedia informedia;

	Editing editing;

	public static void main(String[] args) {
		// Util.print("Starting Art");
		new Bungee(args);
	}

	private Bungee(String[] args) {
		// gross hack to pass arguments to initialize
		super(args.length > 0 ? args[0] : null, false, null);
	}

	/*
	 * Startup process:
	 * 
	 * 1. Gross hack: Arguments are passed via getTitle: db, fontSize, server,
	 * and Query. (Query is an initial state.)
	 * 
	 * 2. setDatabase stops everything from any previous database, shows the
	 * "Waiting for <server>" message, starts dataUpdater, and returns so that
	 * the message is displayed.
	 * 
	 * 3. dataUpdater initializes query in the background and then calls
	 * initializeFrames in the mouse process.
	 * 
	 * 4. initializeFrames removes the "Waiting" messagee and shows any error
	 * creating query. Otherwise, it adds and initializes all the frames. All
	 * the bars are now displayed.
	 * 
	 * 5. The first time Summary.paint is called, it schedules the
	 * initialization of queryViz, selectedItem, grid, percentLabels, hotzone,
	 * and possibly setInitialState.
	 * 
	 * 6. Meanwhile, prefetcher is getting child counts and offsets, and
	 * possibly names.
	 * 
	 * @see edu.umd.cs.piccolox.PFrame#initialize()
	 */
	public void initialize() {
		Runtime.runFinalizersOnExit(true);
		argString = getTitle();
		// Util.print("argString=" + argString);
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
		int textH = Preferences.defaultFeatures.fontSize;
		String fontSizeSpec = argURLQuery.getArgument("fontSize");
		if (fontSizeSpec.length() > 0) {
			textH = Integer.parseInt(fontSizeSpec);
		}

		// setTextSize is a no-op if the size doesn't change, and we need to
		// initialize font
		updateTextSize(textH);
		setTextSize(textH);

		server = argURLQuery.getArgument("server");

		// try {
		// browser = JSObject.getWindow(this);
		// } catch (JSException e) {
		// Util.err("JSException trying to get browser: " + e);
		// }
		getCanvas().setPanEventHandler(null);
		getCanvas().setZoomEventHandler(null);
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
		String informediaSocket = argURLQuery.getArgument("socket");
		if (informediaSocket.length() > 0)
			try {
				informedia = new Informedia(this, Integer
						.parseInt(informediaSocket));
				informedia.start();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}

		String mode = argURLQuery.getArgument("mode");
		if ("expert".equalsIgnoreCase(mode))
			setFeatures(new Preferences(null, Preferences.expertFeatureNames,
					true));
	}

	void setFeatures(Preferences options) {
		if (!options.equals(features)) {
			features = options;
			updateTextSize(options.fontSize);
			if (isReady) {
				summary.setFeatures();
				grid.setFeatures();
				validateIfReady();
			}
			printUserAction(Bungee.MODE, options.features2string().replace(',',
					'_'), 0);
		}
	}

	void setTextSize(int fontSize) {
		fontSize = Math.max(MIN_TEXT_HEIGHT, Math.min(fontSize, maxTextSize()));
		setFeatures(new Preferences(features, "fontSize=" + fontSize, true));
	}

	int getTextSize() {
		return features.fontSize;
	}

	int getLegacyTextSize() {
		return selectedItem.getFontSize();
	}

	int getDesiredNumResultsColumns() {
		return features.nColumns;
	}

	void setDesiredNumResultsColumns(int nCols) {
		setFeatures(new Preferences(features, "nColumns=" + nCols, true));
	}

	/**
	 * @param fontSize
	 * @return whether the new fontSize represents a change
	 */
	boolean updateTextSize(int fontSize) {
		// Util.print("Art.updateTextSize " + fontSize + " " + maxTextSize());
		// assert fontSize >= MIN_TEXT_HEIGHT;
		// assert fontSize <= maxTextSize();
		boolean result = font == null || fontSize != font.getSize();
		if (result) {
			// Util.print("Art.setTextSize " + fontSize);
			clearTextCaches();
			font = new Font(fontFamily, fontStyle, fontSize);
			FontMetrics fontMetrics = getGraphics().getFontMetrics(font);
			lineH = Math.ceil(APText.fontLineH(font));
			maxCharW = fontMetrics.getMaxAdvance();
			digitWidth = getStringWidth("0123456789") / 10;
			commaWidth = getStringWidth(",,,,,,,,,,") / 10;
			spaceWidth = getStringWidth("                    ") / 20;
			childIndicatorWidth = getStringWidth(childIndicatorSuffix);
			parentIndicatorWidth = getStringWidth(Markup.parentIndicatorPrefix);
			checkBoxWidth = getStringWidth(checkBoxPrefix);
			// headerH = Math.ceil(4.5 * lineH);
			mouseDocH = lineH;
			scrollBarWidth = Math.round(0.67 * lineH);
			scrollMarginSize = Math.round(0.17 * lineH);

			// validateIfReady();
		}
		return result;
	}

	void setDatabase(String _dbName) {
		if (dbName == null || !dbName.equals(_dbName)
				|| query.isRestrictedData()) {
			// Util.print("setDatabase/handleCursor true");
			handleCursor(true);
			isReady = false;
			removeAllChildren();

			APText stop = oneLineLabel();
			stop.setText("Waiting for " + server + "   ...");
			stop.setScale(1.5);
			stop.setOffset(lineH, lineH);
			addChild(stop);

			stop();

			dbName = _dbName;
			if (_dbName != null) {
				updater = new DataUpdater();
				updater.start();
			}
		}
	}

	private void addChild(PNode node) {
		PLayer layer = getCanvas().getLayer();
		layer.addChild(node);
	}

	void clearTextCaches() {
		facetNameWidths = null;
		truncatedStrings = null;
		facetTexts = null;
		if (grid != null)
			grid.clearTextCaches();
	}

	/**
	 * Scheduled by DataUpdater initialization after query is set
	 */
	void initializeFrames() {
		removeAllChildren();
		String errorMessage = query.errorMessage();
		if (errorMessage == null) {
			Util.print("initializeFrames " + font);
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
			extremeTags = new ExtremeTags(this);

			help = new InitialHelp();

			// No need for paint, as children occupy the whole space.
			// UPDATE! Now we use setBackground
			// layer.setPaint(Color.black);

			addChild(extremeTags);
			addChild(grid);
			addChild(selectedItem);
			addChild(help);
			addChild(summary);
			addChild(mouseDoc);
			addChild(header); // Last, to let menu overlap
			// other panes.
			summary.init();
			isReady = true;
			validateIfReady();
			setMouseDoc(null);
		} else {
			setNormalPaneVisibility(false);
			showError("Could not connect to " + server + " because\n"
					+ errorMessage);
		}
		// Util.print("...setInternal returning");
		handleCursor(false);
	}

	boolean isInitted() {
		return isReady && grid.isInitted();
	}

	private class InitialHelp extends LazyPNode {
		private APText msg;

		// private APText msg;
		// private Arrow arrow = new Arrow(Color.white, (int) (lineH / 3),
		// (int) (2 * lineH));

		InitialHelp() {
			// setPaint(Color.yellow);
			scale(2.0);
			setPickable(false);
			setChildrenPickable(false);

			// meta = new APText(new Font(fontFamily, Font.ITALIC,
			// getTextSize()));
			// meta.setTextPaint(Color.white);
			// meta.setText("New Users: Look here for orange usage tips.");
			// // If natural width set above is too big, positionHelp will fix
			// it
			// meta.setConstrainWidthToTextWidth(false);
			// addChild(meta);

			msg = new APText(new Font(fontFamily, Font.ITALIC, getTextSize()));
			msg.setTextPaint(Color.white);
			msg.setPickable(false);
			msg
					.setText("Click on a category,\nand then click on its tags\nto start diving into the collection.");
			msg.setConstrainWidthToTextWidth(false);
			addChild(msg);

			// arrow.setRotation(Math.PI / 2);
			// arrow.setVisible(Arrow.LEFT_TAIL, false);
			// addChild(arrow);

			setWidth(w);
		}

		public boolean setWidth(double w) {
			w = Math.round(w / getScale());
			msg.setWidth(w);
			// arrow.setOffset(lineH, meta.getMaxY());
			// msg.setWidth(w);
			// msg.setOffset(0, meta.getHeight() + lineH);
			// setHeight(msg.getMaxY());
			// return super.setWidth(w);
			// return setBounds(0, 0, w, (int) (meta.getMaxY()
			// + arrow.getGlobalBounds().height + lineH / 2));
			return setBounds(getFullBounds());
		}
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

	// void revalidate() {
	// isReady = false;
	// validate(w, h);
	// }

	int maxTextSize() {
		// assume minWidth is proportional to font size, with a fudge factor
		int result = w > 0 ? (int) (getLegacyTextSize() * 0.9 * Math.min(w
				/ (double) minWidth(), h / (double) minHeight()))
				: Integer.MAX_VALUE;
		Util.print("maxTextSize " + w + " " + result);
		return result;
	}

	/**
	 * This must not depend on frames being validated, so use getStringWidth
	 * instead of getWidth, for instance. It DOES depend on textH.
	 * 
	 * @return minimum width of window required for rendering
	 */
	int minWidth() {
		int result = 1;
		if (selectedItem != null)
			result = (int) (selectedItem.minWidth() + summary.minWidth(true)
					+ extremeTags.minWidth() + grid.minWidth() + regionMargins * 3);
		return result;
	}

	/**
	 * This must not depend on frames being validated, so use getStringWidth
	 * instead of getWidth, for instance. It DOES depend on textH.
	 * 
	 * @return minimum height of window required for rendering
	 */
	int minHeight() {
		int result = 1;
		if (selectedItem != null)
			result = (int) (header.minHeight() + summary.minHeight() + mouseDoc
					.minHeight());
		return result;
	}

	/**
	 * @param power
	 *            the growth rate of the frame as extra space becomes available.
	 * @return the width to make a frame (Summary, Results, or Selected Result)
	 *         as a fraction of its minimum width. Currently, Selected Item
	 *         scales as the square root, and Summary scales linearly.
	 */
	double scaleRatio(double power) {
		// gridW >= grid.minWidth() as long as w >= minWidth() and power <= 1
		assert power <= 1;
		double minWidth = minWidth();
		assert w >= minWidth : "text size=" + getTextSize() + " w=" + w
				+ " minWidth=" + minWidth + ", " + minWidth() + " SI:"
				+ selectedItem.minWidth() + " ET:" + extremeTags.minWidth()
				+ " Sum:" + summary.minWidth(true) + " Grid:" + grid.minWidth();
		return Math.pow(w / minWidth, power);
	}

	public void setSize(int width, int height) {
		if (width != w || height != h) {
			// Util.print("setSize " + width + "x" + height);
			// Util.print(getSize());
			// Need to remember these for when we are ready to validate
			w = width;
			h = height;
			validateIfReady();
		}
	}

	void validateIfReady() {
		if (isReady) { // && !setTextSize(textH)) {
			Util.print("validateIfReady " + w + " " + h);
			// Util.printStackTrace();
			// This will reduce text size if necessary to fit in the window.
			// If it changes the text size, it will call validate itself
			printUserAction(SETSIZE, w, h);
			if (!setTooSmall()) {
				getCanvas().getLayer().setBounds(0, 0, w, h);
				double headerH = header.validate(w);
				mouseDoc.validate(w, mouseDocH);

				double internalH = h - headerH - mouseDocH;

				double selectedItemW = (int) (selectedItem.minWidth() * scaleRatio(0.5));
				double summaryW = (int) (summary.minWidth(true) * scaleRatio(1.0));
				double extremeW = (int) (extremeTags.minWidth() * scaleRatio(0.5));
				double gridW = (w - summaryW - selectedItemW - extremeW - 3 * regionMargins);
				assert gridW >= grid.minWidth() : "gridMinW " + grid.minWidth()
						+ " summaryMinW " + summary.minWidth(true)
						+ " selectedItemMinW " + selectedItem.minWidth()
						+ "\ngridW " + gridW + " summaryW " + summaryW
						+ " selectedItemW " + selectedItemW;
				summary.validate(summaryW, internalH);
				summary.setOffset(0.0, headerH + mouseDocH);
				extremeTags.validate(extremeW, internalH);
				extremeTags.setOffset(summary.getMaxX() + regionMargins,
						headerH + mouseDocH);
				grid.validate(gridW, internalH);
				grid.setOffset(extremeTags.getMaxX() + regionMargins, headerH
						+ mouseDocH);
				selectedItem.validate(selectedItemW, internalH);
				selectedItem.setOffset(grid.getMaxX() + regionMargins, headerH
						+ mouseDocH);
				if (clusterViz != null)
					clusterViz.validate();
				mouseDoc.setOffset(0.0, headerH);

				Util.print("validateIfReady OK " + header);

				positionHelp();
			}
		}
	}

	void positionHelp() {
		// Util.print("positionHelp ") + isShowingInitialHelp());
		if (isShowingInitialHelp()) {
			// help.moveToFront();
			double xOffset = summary.getMaxX() + lineH;
			double yOffset = header.getHeight() + 4 * lineH;
			help.setOffset(xOffset, yOffset);
			double helpRight = help.getMaxX();
			if (helpRight > w) {
				// help.setConstrainWidthToTextWidth(false);
				help.setWidth(w - xOffset - lineH);
			}
		}
	}

	void updateSummaryBoundary() {
		double offset = summary.getMaxX();
		extremeTags.setXoffset(offset);
		extremeTags.validate(grid.getXOffset() - offset, extremeTags
				.getHeight());
		// positionHelp();
	}

	void updateExtremesBoundary() {
		double offset = extremeTags.getMaxX();
		// Util.print("updateExtremesBoundary " + offset + " "
		// + (w - offset - selectedItem.getWidth()));
		grid.setXoffset(offset);
		grid.validate(w - offset - selectedItem.getWidth(), grid.getHeight());
		// positionHelp();
	}

	void updateGridBoundary() {
		double offset = grid.getXOffset() + grid.getWidth();
		selectedItem.setXoffset(offset);
		selectedItem.validate(w - offset, selectedItem.getHeight());
		if (clusterViz != null)
			clusterViz.validate();
	}

	private boolean setTooSmall() {
		removeTooSmall();
		boolean isTooSmall = !smallWindowOK && (w < minWidth() || h < minH);
		Util.print("setTooSmall " + isTooSmall + " " + w + "x" + h + " "
				+ minWidth() + "x" + minH);
		if (isTooSmall) {
			getCanvas().setBackground(Color.white);
			// if (maxTextSize() >= minTextH) {
			// isReady = false;
			// setTextSize(maxTextSize());
			// isReady = true;
			// isTooSmall = false;
			// } else {
			APText tooSmall = showError("For best results,\nBungee View requires at least "
					+ minWidth()
					+ " x "
					+ minH
					+ " pixels,"
					+ "\nbut your window is only "
					+ w
					+ " wide x "
					+ h
					+ " high.\n\n  1. Make sure your window is maximized, or"
					+ "\n  2. Increase your screen resolution, or");
			SmallWindowButton goAhead = new SmallWindowButton();
			goAhead.setOffset(tooSmall.getXOffset(), tooSmall.getMaxY());
			goAhead.setScale(tooSmall.getScale());
			addChild(goAhead);
		} else
			getCanvas().setBackground(Color.black);
		setNormalPaneVisibility(!isTooSmall);
		return isTooSmall;
	}

	final class SmallWindowButton extends BungeeTextButton {

		private static final String label1 = "  3. Use this window size anyway";

		SmallWindowButton() {
			// super(label1, art.font, 0, 0, art.getStringWidth(label1),
			// art.lineH /* / scale */+ 2, null, 1.8f, color,
			// Bungee.summaryBG);
			super(label1, Color.black, Color.lightGray, Bungee.this,
					"Reduce font size and truncate text as necessary");
			((APText) child).setConstrainWidthToTextWidth(true);
		}

		public boolean isEnabled() {
			return true;
		}

		public void doPick() {
			smallWindowOK = true;
			isReady = false;
			try {
				setTextSize(maxTextSize());
			} catch (AssertionError e) {
				Util.err(e);
			}
			initializeFrames();
		}

		// public void mayHideTransients(PNode node) {
		// art.mayHideTransients();
		// }

	}

	private void setNormalPaneVisibility(boolean isVisible) {
		// Util.print("setNormalPaneVisibility "+isVisible);
		if (summary != null)
			summary.setVisible(isVisible);
		if (extremeTags != null)
			extremeTags.setVisible(isVisible);
		if (grid != null)
			grid.setVisible(isVisible);
		if (selectedItem != null)
			selectedItem.setVisible(isVisible);
		if (mouseDoc != null)
			mouseDoc.setVisible(isVisible);
		if (help != null) {
			if (isVisible)
				setHelpVisibility();
			else
				help.setVisible(isVisible);
		}
		if (header != null)
			header.setVisible(isVisible);
	}

	private APText showError(String msg) {
		removeTooSmall();

		APText tooSmall = new APText(font);
		tooSmall.setScale(1.5);
		// label.setTextPaint(Color.white);
		tooSmall.setPickable(false);
		tooSmall.setOffset(50, 100);
		tooSmall.setConstrainWidthToTextWidth(false);
		tooSmall.setWidth(w - 2 * tooSmall.getXOffset());
		tooSmall.setText(msg);
		Util.err(msg);
		addChild(tooSmall);
		return tooSmall;
	}

	private void removeTooSmall() {
		for (Iterator it = getCanvas().getLayer().getChildrenIterator(); it
				.hasNext();) {
			PNode node = (PNode) it.next();
			if (node instanceof APText || node instanceof SmallWindowButton) {
				Util.print("removeTooSmall removing " + node);
				node.removeFromParent();
			}
		}
		// if (tooSmall != null) {
		// tooSmall.removeFromParent();
		// tooSmall = null;
		// }
	}

	private void removeAllChildren() {
		PLayer layer = getCanvas().getLayer();
		layer.removeAllChildren();
	}

	double getW() {
		return w;
	}

	// double getH() {
	// return h;
	// }

	void grabFocus() {
		PInputManager pim = getCanvas().getRoot().getDefaultInputManager();
		// Util.print("grabFocus "+ pim.getKeyboardFocus());
		pim.setKeyboardFocus(keyHandler);
		// Util.print("grabFocus "+ pim.getKeyboardFocus());

	}

	protected void finalize() throws Throwable {
		stop();
		super.finalize();
	}

	/**
	 * Stop all threads. Called by setDatabase and finalize.
	 */
	private void stop() {
		// Util.print("Art.stop bar: "+Bar.paintCount+" summary: " +
		// Summary.updateCount + " aptext: " + APText.paintCount+"
		// nHighlights="+nHighlights);

		// if (grid != null)
		// grid.thumbs = null;
		// if (itemImages != null)
		// itemImages = null;
		// System.gc();

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
		if (clusterViz != null) {
			clusterViz.stop();
			clusterViz = null;
		}
		// perspectiveList = null;
		// summary.highlighter.exit();
		// grid.thumbLoader.exit();
		if (grid != null) {
			grid.stop();
			grid = null;
		}

		if (summary != null) {
			summary.stop();
			summary = null;
		}

		// db = null;
		if (query != null) {
			query.exit();
			query = null;
		}
		if (documentShower != null) {
			documentShower.exit();
			documentShower = null;
		}

		clearTextCaches();
		highlightedClusters = new HashSet();
		highlightedFacets = new HashSet();

		// Need to lose all references to stale facets, or hash tables will have
		// collisions arrowFocus = null;

		header = null;
		mouseDoc = null;
		selectedItem = null;
		grid = null;
		summary = null;
		extremeTags = null;
		clusterViz = null;
	}

	boolean togglePopups() {
		showPopup(null);
		boolean state = !isPopups();
		features = new Preferences(features, "popups", state);
		setTip(state ? "Will now show tag information with popups"
				: "Will now show tag information here in the header");
		return state;
	}

	boolean isPopups() {
		return features.popups;
	}

	boolean isOpenClose() {
		return features.openClose;
	}

	boolean getShowCheckboxes() {
		return features.checkboxes;
	}

	boolean getIsShortcuts() {
		return features.shortcuts;
	}

	boolean getIsClustering() {
		return features.clustering;
	}

	boolean getIsBrushing() {
		return features.brushing;
	}

	boolean getShowPvalues() {
		return features.pvalues;
	}

	boolean getShowMedian() {
		return features.medians;
	}

	boolean getShowSortMenu() {
		return features.sortMenus;
	}

	boolean getUseArrowKeys() {
		return features.arrows;
	}

	boolean getShowBoundaries() {
		return features.boundaries;
	}

	boolean getShowTagLists() {
		return features.tagLists;
	}

	boolean getShowZoomLetters() {
		return features.zoom;
	}

	boolean getIsEditing() {
		return query.isEditable() && features.editing;
	}

	void setSelectedItem(Item item) {
		setSelectedItem(item, -1, -1, -1, -1, false);
	}

	void setSelectedItem(Item item, PNode source, boolean isExplicitly) {
		PBounds gStartRect = source.getGlobalBounds();
		Rectangle2D lStartRect = selectedItem.globalToLocal(gStartRect);
		double selectedY = lStartRect.getY();
		double selectedX = lStartRect.getX();
		double selectedW = lStartRect.getWidth();
		double selectedH = lStartRect.getHeight();
		setSelectedItem(item, selectedX, selectedY, selectedW, selectedH,
				isExplicitly);
	}

	void setSelectedItem(Item item, double selectedX, double selectedY,
			double selectedW, double selectedH, boolean isExplicitly) {
		// Util.print("art.setSelectedItemID " + item_id);

		// send if this is from a click/arrow or just the default item
		printUserAction(Bungee.THUMBNAIL, item.getId(), isExplicitly ? 1 : 0);
		selectedItem.animateOutline(item, selectedX, selectedY, selectedW,
				selectedH);
		if (clusterViz != null)
			clusterViz.hide();
	}

	Item selectedItemItem() {
		return selectedItem.currentItem;
	}

	public Item[] getItems(int startIndex, int endIndex) {
		return query.getItems(startIndex, endIndex);
	}

	void addItemList(String name, Item[] items) {
		if (items.length > 0) {
			query.toggleItemList(new Query.ItemList(name, items));
			mayHideTransients();
			updateAllData();
		} else {
			setTip("There are no items satisfying this filter");
		}
	}

	void showItemInNewWindow(Item item) {
		printUserAction(Bungee.IMAGE, item.getId(), 0);
		showDocument(item);
	}

	/**
	 * @param o
	 *            Either an Item or a String
	 */
	void showDocument(Object o) {
		if (documentShower == null) {
			documentShower = new DocumentShower();
			documentShower.start();
		}
		documentShower.add(o);
	}

	private BasicService maybeGetBasicService() {
		BasicService s = null;
		try {
			s = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
			jnlpClipboardService = (ClipboardService) ServiceManager
					.lookup("javax.jnlp.ClipboardService");
		} catch (Throwable e) {
			// Expect UnavailableServiceException or NoClassDefFoundError
			// if (s == null)
			// Util.err("jnlp.BasicService is not available");
			// if (jnlpClipboardService == null)
			// Util.err("jnlp.ClipboardService is not available");
			// Util.err("because " + e);
		}
		return s;
	}

	void copyBookmark() {
		StringSelection ss = new StringSelection(bookmark());
		if (jnlpClipboardService != null)
			jnlpClipboardService.setContents(ss);
	}

	String bookmark() {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("db=").append(URLEncoder.encode(dbName, "UTF-8"));
			assert selectedItem != null;
			assert query != null;
			if (selectedItemItem() != null) {
				String url = query.getItemURL(selectedItemItem());
				if (url != null) {
					buf.append("&SelectedItem=").append(
							URLEncoder.encode(url, "UTF-8"));
				} else {
					buf.append("&SelectedItemID=").append(
							selectedItemItem().getId());
				}
			}
			ItemPredicate cp = summary.connectedPerspective();
			if (cp != null) {
				buf.append("&SelectedFacet=").append(
						URLEncoder.encode(cp.getName(), "UTF-8"));
			}
			if (query.isRestricted()) {
				buf.append("&Query=");
				buf.append(URLEncoder.encode(query.bookmark(), "UTF-8"));
			}
			String result = codeBase() + "bungee.jsp?" + buf.toString();
			Util.print("bookmark " + result);
			return result;
		} catch (UnsupportedEncodingException e) {
			Util.err("Can't encode a bookmark for " + e);
			return null;
		}
	}

	private Set parsePerspectives(String s) {
		Set result = new HashSet();
		String[] disjuncts = s.split("\\|");
		for (int j = 0; j < disjuncts.length; j++) {
			String[] ancestors = disjuncts[j].split(" -- ");
			Perspective parent = null;
			for (int k = 0; k < ancestors.length; k++) {
				if (parent != null)
					parent.prefetchData();

				// This will barf if ancestor name isn't cached
				// Perspective child = query.findPerspective(ancestors[k],
				// parent);

				// Do it this way
				Perspective child = parent == null ? query.findUsedPerspective(
						ancestors[k], true) : parent.firstWithPrefix(Util
						.toCollationKey(ancestors[k]));

				assert child != null : parent + " " + ancestors[k];
				parent = child;
			}
			assert parent != null;
			result.add(parent);
		}
		return result;
	}

	void setInitialState() {
		if (argString != null) {
			// Util.print(argString);
			URLQuery argURLQuery = new URLQuery(argString);
			String q = argURLQuery.getArgument("Query");
			if (q.length() > 0) {
				// Util.print(q);
				String[] terms = Util.splitSemicolon(q);
				for (int i = 0; i < terms.length; i++) {
					Util.print("setInitialState: " + terms[i]);
					String[] x = terms[i].split(":");
					assert x.length == 2;
					if (x[0].equals("TextSearch")) {
						summary.q.addTextSearch(x[1]);
					} else if (x[0].equals("Cluster")) {
						Set facets = parsePerspectives(x[1]);
						query.toggleCluster(new Cluster(facets));
					} else {
						// Perspective facetType =
						// query.findUsedPerspective(x[0],
						// false);
						boolean required = ("+".equals(x[0]));
						// if (!required)
						// x[1] = x[1].substring(1);
						Set facets = parsePerspectives(x[1]);
						for (Iterator it = facets.iterator(); it.hasNext();) {
							Perspective child = (Perspective) it.next();
							if (!child.isRestriction(required))
								query.toggleFacet(child,
										required ? InputEvent.CTRL_DOWN_MASK
												: ItemPredicate.EXCLUDE_ACTION);
						}
					}
				}
				updateAllData();
			}
			setInitialSelectedItem(argURLQuery);
			String facetName = argURLQuery.getArgument("SelectedFacet");
			if (facetName.length() > 0) {
				PerspectiveViz pv = summary.findPerspectiveViz(facetName);
				pv.connectToPerspective();
			}

			// ops =
			// "3234,1,20,16;6209,1,35,16;12067,1,806,16;16724,1,806,16;23533,1,41,16;29001,4,32827,5;32176,4,38298,11;36182,4,36341,15;40548,6,5221,16;45165,6,1,16;50322,4,36383,7"
			// .split(";");
			// String opsSpec = argString.getArgument("ops");
			// opsSpec =
			// "3234,1,20,16;6209,1,35,16;12067,1,806,16;16724,1,806,16;23533,1,41,16;29001,4,32827,5;32176,4,38298,11;36182,4,36341,15;40548,6,5221,16;45165,6,1,16;50322,4,36383,7;59535,6,3,16"
			// ;
			// if (opsSpec.length() == 0) {
			String replayArg = argURLQuery.getArgument("sessions");
			if (replayArg.length() > 0) {
				replayer = new Replayer(this, replayArg);
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

	void setInitialSelectedItem(URLQuery argURLQuery) {
		int selectedItemID = -1;
		String itemURL = argURLQuery.getArgument("SelectedItem");
		if (itemURL.length() > 0) {
			selectedItemID = query.itemIndexFromURL(itemURL)[0];
		} else {
			String itemID = argURLQuery.getArgument("SelectedItemID");
			if (itemID.length() > 0) {
				selectedItemID = Integer.parseInt(itemID);
			}
		}
		if (selectedItemID >= 0) {
			// grid.selectedItem is passed to Query.updateOnItems
			Item selected = Item.ensureItem(selectedItemID);
			grid.selectedItem = selected;
			setSelectedItem(selected);
		}
	}

	String codeBase() {
		String result;
		if (basicJNLPservice != null) {
			result = basicJNLPservice.getCodeBase().toString();
		} else
			result = "http://localhost/bungee/"
			// "http://cityscape.inf.cs.cmu.edu/bungee/"
			;
		return result;
	}

	void showMedianArrowDesc(Perspective parent) {
		highlightFacet(parent, 0);
		summary.showMedianArrowPopup(parent);
	}

	private static int toConnectMask = InputEvent.CTRL_DOWN_MASK
			| InputEvent.SHIFT_DOWN_MASK | ItemPredicate.EXCLUDE_ACTION;

	void toggleFacet(Perspective facet, int modifiers) {
		 Util.print("Art.toggleFacet " + facet + " " + modifiers);
		if (!getIsShortcuts() && replayer == null)
			modifiers = 0;
		assert facet != null;
		assert facet.getParent() != null : facet;
		// arrowFocus = facet;
		if (query.isQueryValid() && query.zeroHits(facet, modifiers))
			setTip("There would be no matches if you added this filter.  (There are now "
					+ query.describeNfilters() + ")");
		else if (query.toggleFacet(facet, modifiers)) {
			Perspective toConnect = summary.connectedPerspective();
			if (toConnect == null || (modifiers & toConnectMask) == 0
					&& facet.getParent() != summary.listedPerspective())
				toConnect = facet;
			toConnect = toConnect.pv();
			updateAllData(toConnect);

			// Do this after updateAllData, because that will setQueryInvalid,
			// which it is after Query.toggleFacet
			highlightFacet(facet, modifiers);
		}
		setArrowFocus(facet);
	}

	void setArrowFocus(Perspective facet) {
		// Only allow arrow navigation from positive filters. See comment in
		// PerspectiveList.handleArrow.
		arrowFocus = facet.isRestriction(true) ? facet : null;
	}

	void toggleCluster(Cluster cluster) {
		// No, even a cluster of 1 has different semantics from its facet;
		// It behaves as an AND with respect to its siblings, while the facet
		// behaves as an OR.
		// if (cluster.nRestrictions() <= 2) {
		// for (Iterator it = cluster.allRestrictions().iterator(); it
		// .hasNext();) {
		// Perspective p = (Perspective) it.next();
		// assert !p.isRestriction(true);
		// assert !p.isRestriction(false);
		// toggleFacet(p, InputEvent.CTRL_DOWN_MASK);
		// }
		// } else {
		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for (Iterator it = cluster.allRestrictions().iterator(); it.hasNext();) {
			Perspective p = (Perspective) it.next();
			if (first)
				first = false;
			else
				buf.append("/");
			buf.append(p.getID());
		}
		printUserAction(TOGGLE_CLUSTER, buf.toString(), 0);
		query.toggleCluster(cluster);
		updateAllData();
		// }
	}

	// private int nHighlights;

	void highlightCluster(Cluster cluster) {
		Set prevFacets = highlightedClusters;
		highlightedClusters = new HashSet();
		if (cluster != null) {
			highlightedClusters.add(cluster);
			prevFacets.add(cluster);
		}
		assert !prevFacets.isEmpty();

		header.updateSelections(prevFacets);
		// summary.highlightCluster(prevFacets);
		summary.showCluster(cluster);
		// prevModifiers = -9999;
		// updateItemPredicateClickDesc(0, true);
		if (clusterViz != null)
			clusterViz.highlightFacet(prevFacets);

		if (!query.usesCluster(cluster)) {
			Markup desc = null;
			if (cluster != null) {
				desc = Query.emptyMarkup();
				desc.add("Add filter on ");
				desc.add(cluster);
			}
			setClickDesc(desc);
		}
	}

	// static final int OPEN_RANK_MODIFIER = -2;

	/**
	 * @param facet
	 *            what to highlight (brighten on mouse over). null means
	 *            unhighlight whatever was highlighted before.
	 * @param modifiers
	 *            -1 means no action; -2 means open rank; otherwise control &
	 *            shift masks
	 */
	void highlightFacet(Perspective facet, int modifiers) {
		// Util.print("Art.highlightFacet " + facet + " " + +modifiers);
		// Util.printStackTrace();
		// assert !highlightedFacets.contains(facet) : facet + " " +
		// highlightedFacets;
		// nHighlights++;
		Set prevFacets = highlightedFacets;
		highlightedFacets = new HashSet();
		if (facet != null) {
			highlightedFacets.add(facet);
		}
		if (!prevFacets.equals(highlightedFacets)) {
			if (facet != null) {
				prevFacets.add(facet);
			}
			assert !prevFacets.isEmpty();
			// highlightForSelectPerspectiveOnly = onlySelectPerspective;
			// highlightOnly = modifiers < 0; // it's a no-op facet in
			// SelectedItem.

			selectedItem.highlightFacet(prevFacets);
			if (getIsBrushing()) {
				grid.highlightFacet(prevFacets);
			}
			if (clusterViz != null)
				clusterViz.highlightFacet(prevFacets);

			getCanvas().paintImmediately();
			summary.highlightFacet(prevFacets);
			showPopup(facet);
			summary.repaintNow();
			getCanvas().paintImmediately();

			header.updateSelections(prevFacets);
			extremeTags.highlightFacet(prevFacets);

			// prevModifiers = -9999;
			// updateItemPredicateClickDesc(modifiers, true);
			// Util.print("PI mouse");
			// mouseDoc.repaintNow();
			// getCanvas().paintImmediately();
		}
	}

	void highlightFacets(Set facets, boolean state) {
		if (getIsBrushing()) {
			Set prevFacets = highlightedFacets;
			highlightedFacets = state ? facets : new HashSet();
			if (!prevFacets.equals(highlightedFacets)) {
				prevFacets = Util.symmetricDifference(new HashSet(
						highlightedFacets), prevFacets);
				assert !prevFacets.isEmpty();

				summary.highlightFacet(prevFacets);
				selectedItem.highlightFacet(prevFacets);
				if (clusterViz != null)
					clusterViz.highlightFacet(prevFacets);
				header.updateSelections(prevFacets);
				extremeTags.highlightFacet(prevFacets);

				showPopup(singleHighlightedFacet());
			}
		}
	}

	// // update mouse doc when shift keys change or you rollover a checkbox
	// void updateItemPredicateClickDesc(int modifiers, boolean dontCombine) {
	// if (!dontCombine) {
	// // need to combine new modifiers from input event with old
	// // modifiers,
	// // which may contain additional bits (e.g. exclude)
	// modifiers |= prevModifiers & ~(ItemPredicate.EXCLUDE_ACTION - 1);
	// }
	// assert modifiers >= 0 : InputEvent.getModifiersExText(modifiers);
	// if (prevModifiers != modifiers) {
	// prevModifiers = modifiers;
	// Markup desc = null;
	// // Util.print("Art.rehighlightFacet " + modifiers + " "
	// // + highlightedFacet + " " + highlightedCluster);
	// if (highlightedFacet != null) {
	// // if (modifiers == OPEN_RANK_MODIFIER) {
	// // desc = Query.emptyMarkup();
	// // desc.add("open category ");
	// // if (highlightedFacet.getParent() != null)
	// // desc.add(highlightedFacet.getParent());
	// // else
	// // // For the facet_type text labels
	// // desc.add(highlightedFacet);
	// // } else if (modifiers >= 0) {
	// desc = highlightedFacet.facetDoc(modifiers);
	// // }
	// } else if (highlightedCluster != null
	// && !query.usesCluster(highlightedCluster)) {
	// desc = Query.emptyMarkup();
	// desc.add("Add filter on ");
	// desc.add(highlightedCluster);
	// }
	// setClickDesc(desc);
	// }
	// }

	// private JSObject browser;
	//
	// private static final String[] scrollArgs = { "0", "0" };

	/**
	 * Called by
	 * 
	 * 1. MyInputHandler keyPress, mouseClick, mousePress
	 * 
	 * 2. QueryViz when you type return in the search box, or use a delete
	 * button
	 */
	void mayHideTransients() {
		// Util.print("Art.mayHideTransients");
		setTip(null);
		// try {
		// if (browser != null)
		// browser.call("scrollTo", scrollArgs);
		// } catch (JSException e) {
		// Util.err("JSException trying to scroll browser: " + e);
		// }

		// may on the way up; do on the way down
		if (header != null)
			header.doHideTransients();
		if (selectedItem != null)
			selectedItem.doHideTransients();
		if (editMenu() != null)
			editMenu().removeFromParent();

		// Lose ClusterViz iff selectedItem changes or query changes
		// if (clusterViz != null)
		// clusterViz.hide();
		// if (arrowFocus != null) {
		// arrowFocus = null;
		// // if (perspectiveList != null)
		// // perspectiveList.show(false);
		// }
	}

	Menu editMenu() {
		return editing == null ? null : editing.editMenu;
	}

	Perspective selectedForEdit() {
		return editing == null ? null : editing.selectedForEdit;
	}

	boolean setSelectedForEdit(Perspective facet, int modifiersEx) {
		return ensureEditing() ? editing.setSelectedForEdit(facet, modifiersEx)
				: false;
	}

	boolean facetMiddleMenu(Perspective facet) {
		return ensureEditing() ? editing.facetMiddleMenu(facet) : false;
	}

	boolean itemMiddleMenu(Item currentItem, boolean isRight) {
		return ensureEditing() ? editing.itemMiddleMenu(currentItem, isRight)
				: false;
	}

	private boolean ensureEditing() {
		if (!getIsEditing())
			// In case user has turned off editing using the Custom menu
			editing = null;
		if (editing == null) {
			if (getIsEditing()) {
				editing = new Editing(this);
			} else if (query.isEditable()) {
				setTip("Updating the Database is disabled. Enable it using the Custom Mode menu");
			} else {
				setTip("Updating this Database is disabled. Enable it by setting globals.isEditable = 'Y'");
			}
		}
		return editing != null;
	}

	void setTip(String s) {
		if (mouseDoc != null)
			mouseDoc.setTip(s);
	}

	void setClickDesc(Markup s) {
		// if (mouseDoc != null)
		mouseDoc.setClickDesc(s);
	}

	void setClickDesc(String s) {
		// Util.print("Art.setClickDesc " + s);
		if (mouseDoc != null)
			mouseDoc.setClickDesc(s);
	}

	// void setMouseDoc(PNode source, boolean state) {
	// assert false;
	// if (!(source instanceof Boundary) || getShowBoundaries()) {
	// String desc = null;
	// if (state) {
	// if (source instanceof Button)
	// desc = ((Button) source).mouseDoc;
	// else if (source instanceof Boundary)
	// desc = ((Boundary) source).mouseDoc;
	// }
	// setClickDesc(desc);
	// }
	// }

	void setMouseDoc(String doc) {
		setClickDesc(doc);
	}

	void setNonClickMouseDoc(String s) {
		if (mouseDoc != null) {
			Markup v = null;
			if (s != null) {
				v = Query.emptyMarkup();
				v.add(s);
			}
			mouseDoc.setClickDescInternal(v);
		}
	}

	/**
	 * The mouse doc to display when the mouse isn't over something you can
	 * click on.
	 */
	Markup defaultClickDesc() {
		Markup result = null;
		if (isShowingInitialHelp()) {
			result = Query.emptyMarkup();
			result.add("Click on a category, and then click on its tags "
					+ "to start diving into the collection.");
		} else if (getUseArrowKeys()) {
			if (arrowFocus != null) {
				result = Query.emptyMarkup();
				result.add("Arrow keys will move through: ");
				result.add(arrowFocus.getParent());
			} else if (grid != null && grid.onCount > 1) {
				result = Query.emptyMarkup();
				result.add("Arrow keys will move through: ");
				result.add(Bungee.gridFG);
				result.add("Matches");
			}
		}
		// Util.print("defaultClickDesc " + result);
		return result;
	}

	// void setMouseDoc(Markup doc, boolean state) {
	// setClickDesc(state ? doc : null);
	// }

	APText oneLineLabel() {
		return APText.oneLineLabel(font);
	}

	Color clusterTextColor(Cluster cluster) {
		// Color[] family;
		Color[] colorFamily;
		int fadeIndex = 0;
		if (highlightedClusters.contains(cluster))
			fadeIndex++;
		if (query.usesCluster(cluster)) {
			// fadeIndex++;
			colorFamily = Markup.INCLUDED_COLORS;
		} else if (cluster != null && cluster.pValue() < pValue()
		// Bonferronni correction for combinations of bars. (pValue() has
				// already corrected for the first choice.
				/ Math.pow(nBars(true), (cluster.nRestrictions() - 1))) {
			colorFamily = Markup.POSITIVE_ASSOCIATION_COLORS;
		} else {
			colorFamily = Markup.UNASSOCIATED_COLORS;
		}
		// 1 <= fadeIndex <= 3
		// return family[fadeIndex];
		return colorFamily[fadeIndex];
	}

	Color facetTextColor(ItemPredicate facet) {
		if (facet instanceof Perspective) {
			return facetTextColor((Perspective) facet);
		} else {
			return clusterTextColor((Cluster) facet);
		}
	}

	Color facetTextColor(Perspective facet, Cluster cluster) {
		if (facet != null)
			return facetTextColor(facet);
		else
			return clusterTextColor(cluster);
	}

	Color facetTextColor(Perspective facet) {
		// int fadeIndex = 1;
		// Color[] colorFamily = Markup.UNASSOCIATED_COLORS;
		// if (facet != null) {
		// if (highlightedFacets.contains(facet))
		// fadeIndex++;
		// else if (onCount == 0)
		// fadeIndex--;
		// if (facet.isRestriction(true)) {
		// fadeIndex++;
		// colorFamily = Markup.INCLUDED_COLORS;
		// } else if (facet.isRestriction(false)) {
		// fadeIndex++;
		// colorFamily = Markup.EXCLUDED_COLORS;
		// } else {
		// colorFamily = chiColorFamily(facet);
		// }
		// }

		// New scheme - ignore zero count. fadeIndex is always 0 or 1
		int fadeIndex = 0;
		Color[] colorFamily = Markup.UNASSOCIATED_COLORS;
		if (facet != null) {
			if (highlightedFacets.contains(facet))
				fadeIndex = 1;
			if (facet.isRestriction(true)) {
				colorFamily = Markup.INCLUDED_COLORS;
			} else if (facet.isRestriction(false)) {
				colorFamily = Markup.EXCLUDED_COLORS;
			} else {
				colorFamily = chiColorFamily(facet);
			}
		}
		return colorFamily[fadeIndex];
	}

	/**
	 * @param facet
	 * @param onCount
	 * @return fadeIndex for Bar background
	 */
	int bgFadeIndex(Perspective facet, int onCount) {
		// Util.print("Art.facetBarColor " + facet);
		int fadeIndex = 0;
		if (facet != null) {
			if (highlightedFacets.contains(facet))
				fadeIndex++;
			// else if (onCount == 0)
			// fadeIndex--;
		}
		// Util.print("facetTextColor " + facet + " " + colorFamily[fadeIndex]);
		return fadeIndex;
	}

	static Color[] significanceColorFamily(int significance) {
		switch (significance) {
		case -2:
			return Markup.EXCLUDED_COLORS;
		case -1:
			return Markup.NEGATIVE_ASSOCIATION_COLORS;
		case 0:
			return Markup.UNASSOCIATED_COLORS;
		case 1:
			return Markup.POSITIVE_ASSOCIATION_COLORS;
		case 2:
			return Markup.INCLUDED_COLORS;
		default:
			assert false : significance;
			return null;
		}
	}

	static Color significanceColor(int significance, int fadeIndex) {
		return significanceColorFamily(significance)[fadeIndex];
	}

	private double benjaminiYekutieliThreshold = 0.0001;

	private int nBars = 0;

	private int nNonZeroBars = 0;

	double pValue() {
		return benjaminiYekutieliThreshold;
	}

	// /**
	// * Correct for multiple significance tests with the Bonferroni procedure
	// */
	// void updatePvalueOLD() {
	// pValue = 0.01 / summary.nBars();
	// // Util.print("pValue => " + pValue);
	// }

	/**
	 * Correct for multiple significance tests with the Benjamini and Yekutieli
	 * procedure, assuming positive correlation among tests.
	 * 
	 * Bar colors depend on this, so always have to recalculate when query
	 * changes.
	 * 
	 * see <a *
	 * href="http://en.wikipedia.org/wiki/False_discovery_rate">Benjamini * and
	 * Yekutieli procedure< /a>
	 */
	void updatePvalue() {
		assert query.isQueryValid();
//		 Util.print(query + "\n" + query.topTags(10));
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
		final double bonferroniThreshold = 0.01 / nBars;
		benjaminiYekutieliThreshold = bonferroniThreshold;
		for (int i = nNonZeroBars - 1; i > 1
				&& pValues[i] > benjaminiYekutieliThreshold; i--) {
			// if(i % 1000 == 0)
			// Util.print(i + " " + pValues[i] + " " + (i*x) + " " + pValue);
			if (pValues[i] <= i * bonferroniThreshold) {
				benjaminiYekutieliThreshold = pValues[i];
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

	Color[] chiColorFamily(ItemPredicate facet) {
		return significanceColorFamily(facet
				.chiColorFamily(benjaminiYekutieliThreshold));
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
		summary.flagConnectedRank(null);
		updateAllData();
		// if (isMultipleFilters)
		// setTip("To clear a single filter, click on the tag again.", false);
	}

	void restrict() {
		printUserAction(Bungee.RESTRICT, 0, 0);
		if (query.getOnCount() > 0 && query.isRestricted()) {
			query.restrictData();
			header.setDBdescription(query.getNameIfPossible());
			updateAllData();
			summary.restrict();
		} else {
			Util.err("Can't restrict when there are no results.");
		}
	}

	void setHelpVisibility() {
		// When we open a category, lose the message that would cause occlusion,
		// but don't stop showing the message in the mouse doc line.
		help.setVisible(isShowingInitialHelp()
				&& summary.connectedRank() == null);
	}

	boolean removeInitialHelp() {
		boolean result = help.getParent() != null;
		if (result) {
			help.removeFromParent();
		}
		return result;
	}

	boolean isShowingInitialHelp() {
		return help.getParent() != null;
	}

	void updateAllData() {
		updateAllData(null);
	}

	/**
	 * All query changes go through here
	 */
	void updateAllData(Perspective toConnect) {
		// Util.print("Art.updateAllData ");
		query.setQueryInvalid();
		summary.synchronizeWithQuery(toConnect);
		grid.onItemsInvalid = true;
		if (clusterViz != null)
			clusterViz.hide();

		// Do this in redraw instead, because counts might be transiently zero
		// here.
		// selectedItem.synchronizeWithQuery(query);
		// if (perspectiveList != null)
		// perspectiveList.redraw();
		if (updater.update()) {
			// Calls q.updateAllData and then doRedraw.
			// Util.print("updateAllData/handleCursor true");
			handleCursor(true);
			// } else {
			// query.setQueryInvalid();
		}
	}

	/**
	 * Called by DataUpdater
	 * 
	 * @return getOnCounts?
	 */
	boolean updateOnItems() {
		return query.updateOnItems(grid.selectedItem, grid
				.maxThumbs(Integer.MAX_VALUE));
	}

	int waiting;

	/**
	 * Called in thread Bungee.dataUpdater if wait > 1
	 * 
	 * @param wait
	 */
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

	private transient Runnable redrawer;

	/**
	 * @return Runnable to call after Query becomes valid (i.e. counts have been
	 *         updated).
	 */
	Runnable getRedrawer() {
		if (redrawer == null)
			redrawer = new Runnable() {

				public void run() {
					try {
						// Check to make sure it hasn't become invalid again.
						// If we're unrestricting, isReady might be false.
						// Util.print("doRedraw " + query.isQueryValid());
						if (query.isQueryValid() && isReady) {
							// query.setQueryValid();
							updatePvalue();
							// assert highlightedFacet != null;
							showPopup(singleHighlightedFacet());
							extremeTags.updateData();
							summary.updateData();
							header.updateData();
							selectedItem.setVisibility();
							selectedItem.updateColors();
							if (clusterViz != null)
								clusterViz.hide();
							// if (clusterViz != null)
							// clusterViz.redraw();
						}
					} catch (Throwable e) {
						System.err
								.println("Aborting doRedraw - probably user has updated query since we were called.");
						e.printStackTrace();
					} finally {
						// Util.print("...getRedrawer done/handleCursor false");
						handleCursor(false);
					}
				}
			};
		return redrawer;
	}

	Perspective singleHighlightedFacet() {
		return highlightedFacets.size() == 1 ? (Perspective) highlightedFacets
				.iterator().next() : null;
	}

	void waitForValidQuery() {
		query.waitForValidQuery();
	}

	public Rectangle getDefaultFrameBounds() {
		return new Rectangle(0, 0, 1000, 740);
	}

	// /**
	// * keyPress can't call Bungee.mayHideTransients directly, as it is hidden
	// */
	// void artHideTransients() {
	// mayHideTransients();
	// }

	double numWidth(int n) {
		// space width is about the same as minus sign width
		double result = n >= 0 ? 0 : spaceWidth;
		n = Math.abs(n);
		if (n < 10)
			result += digitWidth;
		else if (n < 100)
			result += 2 * digitWidth;
		else if (n < 1000)
			result += 3 * digitWidth;
		else if (n < 10000)
			result += 4 * digitWidth + commaWidth;
		else
			result += 5 * digitWidth + commaWidth;
		return result;
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

	/**
	 * @param facet
	 * @param numW
	 *            if < 0, don't show count; if > 0 pad onCount to exactly this
	 *            width. If numFirst, add two space characters between onCount
	 *            and name; if !numFirst assume numW takes care of separating
	 *            them.
	 * @param nameW
	 *            if > 0, trucate name to this width.
	 * @param onCount
	 *            count to draw
	 * @param numFirst
	 *            put count before name?
	 * @param showChildIndicator
	 * @param showCheckBox
	 * @param justify
	 *            pad name to exactly match nameW?
	 * @param redraw
	 * @return label for a FacetText
	 */
	String facetLabel(ItemPredicate facet, double numW, double nameW,
			int onCount, boolean numFirst, boolean showChildIndicator,
			boolean showCheckBox, boolean justify, PerspectiveObserver redraw) {
		if (nameW < 0) {
			assert !justify;
			nameW = Double.POSITIVE_INFINITY;
		}
		StringBuffer name = new StringBuffer(100);
		Perspective parent = (facet instanceof Perspective) ? ((Perspective) facet)
				.getParent()
				: null;
		if (showCheckBox && parent != null && getShowCheckboxes()) {
			name.append(checkBoxPrefix);
			nameW -= checkBoxWidth;
		}
		boolean hasChildren = showChildIndicator && parent != null
				&& facet.isEffectiveChildren();
		if (hasChildren)
			nameW -= childIndicatorWidth;
		if (numFirst && numW > 0) {
			name.append(stringForWidth(numW - numWidth(onCount)));
			name.append(Util.addCommas(onCount));
			name.append("  ");
		}
		String namePart = truncateText(facet.getName(redraw), nameW);
		name.append(namePart);
		// can't append before truncation or the suffix would be truncated.
		if (hasChildren)
			name.append(childIndicatorSuffix);
		if (justify)
			name
					.append(stringForWidth(nameW
							- truncatedTextWidth(facet, nameW)));
		if (!numFirst && numW > 0) {
			name.append(stringForWidth(numW - numWidth(onCount)));
			name.append(Util.addCommas(onCount));
		}
		// Util.print("facetLabel " + facet + " " + showChildIndicator + " "
		// + onCount + " '" + name.toString() + "'");
		return name.toString(); // Don't trim -- need initial spaces to position
		// past checkboxes
	}

	String facetLabel(ItemPredicate v, double numW, double nameW,
			boolean numFirst, boolean showChildIndicator, boolean showCheckBox,
			boolean justify, PerspectiveObserver redraw) {
		int onCount = v.guessOnCount();
		if (onCount < 0) {
			// If we've just selected the first facet, query will be restricted
			// but onCount won't be computed yet.
			onCount = v.getTotalCount();
		}
		return facetLabel(v, numW, nameW, onCount, numFirst,
				showChildIndicator, showCheckBox, justify, redraw);
	}

	double getFacetStringWidth(Object facet, boolean showChildIndicator,
			boolean showCheckBox) {
		if (facet == null) {
			return 5;
		}
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
		if (p != null && showCheckBox && p.getParent() != null
				&& getShowCheckboxes())
			result += checkBoxWidth;
		return result;
	}

	String truncateText(String name, double _w) {
		// Util.print("truncateText " + _w + " " + name);
		if (_w < 1.0)
			return "";
		int iw = (int) _w;
		// String name = facet.getName(redraw);
		int wBound = maxCharW * name.length();
		if (wBound <= iw)
			return name;
		Hashtable[] tables = truncatedStrings == null ? null
				: (Hashtable[]) truncatedStrings.get();
		if (tables == null || tables.length <= iw) {
			tables = new Hashtable[iw + 500];
			// Util.print("new truncatedStrings");
			truncatedStrings = new SoftReference(tables);
		}
		Hashtable truncatedStringsTable = tables[iw];
		if (truncatedStringsTable == null) {
			truncatedStringsTable = new Hashtable();
			tables[iw] = truncatedStringsTable;
		}
		String trunc = (String) truncatedStringsTable.get(name);
		if (trunc == null) {
			trunc = edu.cmu.cs.bungee.piccoloUtils.gui.Util.truncateText(name,
					iw, font);
			// if (facet.getNameIfPossible() != null)
			// Don't put "?" in table
			truncatedStringsTable.put(name, trunc);
		}
		return trunc;
	}

	double truncatedTextWidth(ItemPredicate facet, double _w) {
		// Util.print(w + " " + getStringWidth(s) + " " + s);
		return Math.min(_w, getFacetStringWidth(facet, false, false));
	}

	double getStringWidth(String s) {
		assert s != null;
		double result = Math.ceil(edu.cmu.cs.bungee.piccoloUtils.gui.Util
				.getStringWidth(s, font));
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

	private final class KeyEventHandler extends MyInputEventHandler {

		KeyEventHandler() {
			super(Bungee.class);
		}

		protected boolean keyPress(char key, PInputEvent e) {
			// Util.print("keyPress " + key);
			int modifiers = e.getModifiersEx();
			char keyChar = e.getKeyChar();

			// Handle keys without Unicode equivalents here, and normal keys in
			// handleKey
			if (Util.isMember(MyInputEventHandler.shiftKeys, key)) {
				// updateItemPredicateClickDesc(e.getModifiersEx(), false);
				return false;
			} else if (Util.isMember(MyInputEventHandler.arrowKeys, key)
					|| keyChar == CONTROL_A) {
				return handleArrow(key, modifiers);
				// } else if (Tetrad.handleKey(keyChar, modifiers)) {
				// return summary.facetDesc.updateTetrad();
			} else {
				return handleKey(keyChar);
			}
		}

		static final char CONTROL_A = 1;
		static final char CONTROL_C = 3;
		static final char CONTROL_P = 16;

		private boolean handleKey(char keyChar) {
			if (keyChar == ' ') {
				printUserAction(Bungee.SHOW_MORE_HELP, 0, 0);
				showMoreHelp();
			} else if (keyChar == CONTROL_P) {
				printUserAction(Bungee.TOGGLE_POPUPS, 0, 0);
				togglePopups();
			} else if (keyChar == CONTROL_C) {
				if (getIsClustering())
					showClusters();
				else {
					setTip("Finding clusters is disabled in beginner mode");
					return false;
				}
			} else if (editMenu() != null
					&& Character.digit(keyChar, editMenu().nChoices()) > 0) {
				editMenu().choose(keyChar - '1');
			} else if (summary != null && isNameChar(keyChar)
					|| keyChar == '\b') {
				printUserAction(Bungee.ZOOM, 0, keyChar);
				return summary.keyPress(keyChar);

				// This is the default for typing any character
				// } else if (keyChar == java.awt.event.KeyEvent.VK_ESCAPE) {
				// mayHideTransients(null);

			} else {
				return false;
			}
			return true;
		}

		boolean isNameChar(char c) {
			// Util.print("isNameChar " + c + (c >= 32 && c != 127));
			return c >= 32 && c != 127;
		}

		protected void mayHideTransients(PNode ignore) {
			// Util.print("keyHandler.mayHideTransients");
			assert Util.ignore(ignore);
			Bungee.this.mayHideTransients();
		}

		// protected boolean keyRelease(int key, PInputEvent e) {
		// if (Util.isMember(shiftKeys, key)) {
		// updateItemPredicateClickDesc(e.getModifiersEx(), false);
		// return true;
		// }
		// return false;
		// }
	}

	/**
	 * Governs what happens on typing the arrow keys. Null means navigate
	 * through Results list; A Perspective means navigate from this Bar,
	 * (de)selecting the specified adjacent Bar(s).
	 */
	Perspective arrowFocus;

	boolean handleArrow(char key, int modifiers) {
		// Util.print("Art.handleArrow " + arrowFocus);
		if (!getUseArrowKeys()) {
			setTip("Arrow keys are disabled in beginner mode");
			return false;
			// } else if (Util.isAltDown(modifiers) && editArrow(key)) {
			// return true;
		} else if (arrowFocus == null) {
			grid.handleArrow(key);
			return true;
		} else {
			// Don't automatically show the list if you use the arrow keys. Only
			// show it if you click on the rank label. We just need it to
			// [possibly invisibly] handle
			// this arrow key.
			Perspective newFocus = summary.handleArrow(arrowFocus, key,
					modifiers);
			if (newFocus != null) {
				assert newFocus.getParent() != null : newFocus;
				setArrowFocus(newFocus);
			}
			return newFocus != null;
		}
	}

	void showClusters() {
		// Perspective[] associates = query.associates();
		// Util.print(associates.length + " Associates:");
		// Util.printDeep(associates);
		// Perspective[] previous = null;
		// for (int i = 0; i < associates.length; i++) {
		// if (!Util.isMember(previous, associates[i])) {
		// Cluster cluster = query.cluster(associates[i]);
		// if (cluster != null) {
		// Util.print("Cluster of " + cluster.size());
		// Util.printDeep(cluster);
		// previous = (Perspective[]) Util.append(previous,
		// cluster.facets, Perspective.class);
		// }
		// }
		// }
		printUserAction(SHOW_CLUSTERS, 0, 0);
		if (query.isRestricted()) {
			if (clusterViz == null) {
				clusterViz = new ClusterViz(this);
			}
			if (clusterViz.isHidden())
				addChild(clusterViz);
			clusterViz.showClusters();
		} else {
			setTip("Can't find clusters if there are no filters.");
		}
	}

	String aboutCollection() {
		return query.aboutCollection();
	}

	// /**
	// * @param item
	// * warp the original image associated with this item
	// * @param faceImage
	// * computes the warp parameters that map its image's actual
	// * points to its hardcoded desired points.
	// */
	// void warpImage(Item item, FaceImage faceImage) {
	// String srcFilename = query.getItemURL(item);
	// assert srcFilename.endsWith(".jpg") : "warp filname=" + srcFilename;
	// String dstFilename =
	// "C:\\Documents and Settings\\mad\\Desktop\\50thAnniversary\\FlipImages"
	// + srcFilename.substring(srcFilename.lastIndexOf('\\'));
	// // Util.print(dstFilename);
	// try {
	// BufferedImage bigImage = Util.read(srcFilename);
	// FaceImage bigFace = faceImage.getScaledInstance(bigImage);
	// Util.writeImage(bigFace.getWarpedImage(), 85, dstFilename);
	// } catch (ImageFormatException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// }

	/**
	 * Waits for valid query and then calls toggleFacet(selectedForEdit, 0)
	 */
	void toggleFacetLater() {
		if (facetSelecter == null) {
			facetSelecter = new FacetSelecter();
			facetSelecter.start();
		}
		facetSelecter.update();
	}

	private transient Runnable doSelectFacet;

	Runnable getDoSelectFacet() {
		if (doSelectFacet == null)
			doSelectFacet = new Runnable() {

				public void run() {
					toggleFacet(selectedForEdit(), 0);
				}
			};
		return doSelectFacet;
	}

	// boolean editArrow(char key) {
	// if (getIsEditing()) {
	// if (selectedForEdit() != null) {
	// ItemPredicate updated = null;
	// ItemPredicate parent = selectedForEdit().getParent();
	// switch (key) {
	// case java.awt.event.KeyEvent.VK_KP_LEFT:
	// case java.awt.event.KeyEvent.VK_LEFT:
	// if (parent != null) {
	// updated = parent;
	// if (parent.isRestriction(selectedForEdit(), true)) {
	// toggleFacetLater();
	// }
	// }
	// break;
	// case java.awt.event.KeyEvent.VK_KP_UP:
	// case java.awt.event.KeyEvent.VK_UP:
	// updated = selectedForEdit().previousSibling();
	// break;
	// case java.awt.event.KeyEvent.VK_KP_RIGHT:
	// case java.awt.event.KeyEvent.VK_RIGHT:
	// if (selectedForEdit().isEffectiveChildren()) {
	// selectedForEdit().prefetchData();
	// // query.waitForPrefetch(selectedForEdit);
	// updated = selectedForEdit().getNthChild(0);
	// }
	// break;
	// case java.awt.event.KeyEvent.VK_KP_DOWN:
	// case java.awt.event.KeyEvent.VK_DOWN:
	// updated = selectedForEdit().nextSibling();
	// break;
	// default:
	// assert false : key;
	// }
	// if (updated != null) {
	// editing.setSelectedForEdit((Perspective) updated, 0);
	// return true;
	// }
	// }
	// } else
	// Util.err("Editing is disabled.");
	// return false;
	// }

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

	synchronized ItemImage ensureItemImage(Item item, int rawW, int rawH,
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
			Image bi = null;
			try {
				bi = Util.readCompatibleImage(blobStream);
				// if (result!=null)
				// Util.print("ensureItemImage "+item+" "+result+"
				// "+result.bigEnough(bi, quality)+rawW+" "+rawH);
			} catch (Exception e) {
				Util.err("Exception reading blob for item " + item + ":\n" + e);
			}
			if (result == null)
				result = new ItemImage(this, item, rawW, rawH, quality, bi);
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

	void decacheCurrentItem() {
		Item item = selectedItemItem();
		// Util.print("decacheItem " + item);
		selectedItem.removeFacetTree(item);
		Map table = getItemImagesTable();
		if (table != null)
			table.remove(item);
		updateSelectedItem();
	}

	void decacheItems() {
		selectedItem.facetTrees = null;
		updateSelectedItem();
	}

	private transient SoftReference facetTexts;

	private Hashtable getFacetTextTable() {
		Hashtable table = facetTexts == null ? null : (Hashtable) facetTexts
				.get();
		if (table == null) {
			table = new Hashtable();
			// Util.print("new getFacetTextTable");
			facetTexts = new SoftReference(table);
		}
		return table;
	}

	List lookupFacetText(Object treeObject) {
		Hashtable table = getFacetTextTable();
		return (List) table.get(treeObject);
	}

	void putFacetText(Object treeObject, List texts) {
		Hashtable table = getFacetTextTable();
		table.put(treeObject, texts);
	}

	/**
	 * Only called when editing
	 */
	void updateSelectedItem() {
		Item item = selectedItemItem();
		selectedItem.currentItem = null;
		setSelectedItem(item);
	}

	void saveVideoSet() {
		informedia.newVideoSet();
	}

	void showPopup(Perspective facet) {
		if (!isShowingInitialHelp()) {
			if (isPopups()) {
				summary.showPopup(facet);
				mouseDoc.showPopup(null);
			} else
				mouseDoc.showPopup(facet);
		}
	}

	void showMoreHelp() {
		if (isPopups() && summary.facetDesc.getVisible())
			summary.facetDesc.showMoreHelp();
		// else
		// summary.barHelp.phase2();
	}

	void setItemDescription(Item currentItem, String description) {
		query.setItemDescription(currentItem, description);
		decacheCurrentItem();
	}

	String getBugInfo() {
		return "db=" + dbName + "&session=" + query.getSession();
	}

	void printUserAction(int location, Perspective facet, int modifiers) {
		printUserAction(location, Integer.toString(facet.getID()), modifiers);
	}

	void printUserAction(int location, int item, int modifiers) {
		printUserAction(location, Integer.toString(item), modifiers);
	}

	void printUserAction(int location, String object, int modifiers) {
		if (isPrintUserActions && replayer == null) {
			// assert location >= BAR && location <= ERROR : location;
			if (query == null) {
				Util
						.err("Tried to printUserAction before query is initialized: "
								+ location + " " + modifiers + " " + object);
			} else {
				query.printUserAction(location, object, modifiers);

				// Useful for debugging
				// switch (location) {
				// case BAR:
				// case BAR_LABEL:
				// case FACET_TREE:
				// Util.print(query.findPerspective(Integer.parseInt(object)));
				// break;
				// }
			}
		}
	}

	static final int BAR = 1;

	static final int BAR_LABEL = 2;

	static final int RANK_LABEL = 3;

	static final int THUMBNAIL = 4;

	static final int IMAGE = 5;

	static final int FACET_TREE = 6;

	static final int SCROLL = 7;

	// static final int KEYPRESS = 8;

	static final int BUTTON = 9;

	static final int SEARCH = 10;

	static final int SETSIZE = 11;

	// static final int CHECKBOX = 12;

	static final int RESTRICT = 13;

	static final int SHOW_CLUSTERS = 14;

	static final int GRID_ARROW = 15;

	static final int FACET_ARROW = 16;

	static final int REORDER = 17;

	static final int TOGGLE_CLUSTER_EXCLUSION = 18;

	static final int TOGGLE_CLUSTER = 19;

	static final int TOGGLE_POPUPS = 20;

	static final int SHOW_MORE_HELP = 21;

	static final int ZOOM = 22;

	static final int MODE = 23;

	/**
	 * Query secretly knows this value, so keep synchronized
	 */
	// static final int ERROR = 24;
	static final int WRITEBACK = 25;

	void replayOp() {
		try {
			mayHideTransients();
			String[] args = replayer.getArgs();
			Util.print("\nReplay op #" + (replayer.opNum - 1) + " "
					+ Util.valueOfDeep(args));
			int modifiers = args.length > 3 ? Integer.parseInt(args[3]) : -1;
			switch (Integer.parseInt(args[1])) {
			case BAR:
			case BAR_LABEL:
				Util.print("  clickBar " + replayPerspective(args[2]) + " "
						+ modifiers);
				summary.clickBar(replayPerspective(args[2]), modifiers);
				break;
			case RANK_LABEL:
				Util.print("  clickRank " + replayPerspective(args[2]));
				summary.clickRank(replayPerspective(args[2]), modifiers);
				break;
			case THUMBNAIL:
				// Always select the item so that it's facets get loaded, in
				// case
				// user clicks on a deeply nested one.
				// if (modifiers == 1)
			{
				Item item = Item.ensureItem(Integer.parseInt(args[2]));
				Util.print("  Click thumb "
						+ (modifiers == 0 ? " unintentionally "
								: "intentionally ") + item);
				grid.clickThumb(item, 5);
				// } else {
				// Util.print(" no-op set selected item");
			}
				break;
			case IMAGE:
				Item item = Item.ensureItem(Integer.parseInt(args[2]));
				Util.print("  clickImage " + item);
				grid.clickThumb(item, 5);
				selectedItem.clickImage(item);
				break;
			case FACET_TREE:
				Util.print("  clickText " + replayPerspective(args[2]) + " "
						+ modifiers);
				toggleFacet(replayPerspective(args[2]), modifiers);
				// selectedItem.clickText(replayPerspective(args[2]),
				// modifiers);
				break;
			case SCROLL:
				// This is obsolete. Now we just capture the setSelectedItem.
				if (args[2].equals("grid")) {
					int offset = Integer.parseInt(args[3]);
					Util.print("  scroll - ignoring " + offset);
					// grid.scrollTo(offset);
					//grid.clickThumb(Item.ensureItem(Integer.parseInt(args[3]))
					// );
				}
				break;
			case 8:
				Util.print("  keypress - ignoring ");
				break;
			case GRID_ARROW:
				// grid.clickThumb(Integer.parseInt(args[3]));
				Util.print("  ignoring arrow key grid " + args[2]);
				break;
			case FACET_ARROW:
				Util.print("  Facet Arrow " + args[2] + " " + modifiers);
				Perspective p = replayPerspective(args[2]);
				summary.connectToPerspective(p.getParent());
				summary.clickBar(p, modifiers);
				break;
			case SHOW_CLUSTERS:
				Util.print("  find clusters");
				showClusters();
				break;
			case BUTTON:
				Util.print("  button " + args[2]);
				if (args[2].equals("Clear")) {
					header.clickClear();
				} else if (args[2].equals("Ellipsis")) {
					// summary.clickEllipsis();
				} else {
					// summary.removeSearch(args[2]);
					query.removeTextSearch(args[2]);
				}
				break;
			case SEARCH:
				Util.print("  search " + args[2]);
				summary.q.addTextSearch(args[2]);
				updateAllData();
				break;
			case SETSIZE:
				int width = Integer.parseInt(args[2]);
				int height = modifiers;
				Util.print("  setSize " + width + " " + height);
				Insets insets = getInsets();
				width += insets.left + insets.right;
				height += insets.top + insets.bottom;
				super.setSize(width, height);
				validate();
				break;
			case RESTRICT:
				Util.print("  Restrict");
				restrict();
				break;
			case REORDER:
				int facetType = Integer.parseInt(args[2]);
				String name = facetType == -1 ? "random"
						: facetType == 0 ? "ID" : query.findPerspective(
								facetType).getName();
				Util.print("  Reorder by " + name);
				grid.chooseReorder(facetType);
				break;
			case TOGGLE_CLUSTER_EXCLUSION:
				Perspective facet = replayPerspective(args[2]);
				Util.print("  Toggle cluster exclusion " + facet);
				clusterViz.toggleClusterExclusion(facet);
				break;
			case TOGGLE_CLUSTER:
				Util.print("  Toggle Cluster " + args[2]);
				String[] facet_ids = args[2].split("/");
				Set facets = new HashSet();
				for (int i = 0; i < facet_ids.length; i++) {
					facets.add(query.findPerspective(Integer
							.parseInt(facet_ids[i])));
				}
				Cluster cluster = new Cluster(facets);
				toggleCluster(cluster);
				break;
			case TOGGLE_POPUPS:
				Util.print("  Toggle Popups ");
				togglePopups();
				break;
			case SHOW_MORE_HELP:
				Util.print("  Show More Help ");
				showMoreHelp();
				break;
			case ZOOM:
				char keyChar = (char) modifiers;
				Util.print("  Zoom to " + keyChar);
				summary.keyPress(keyChar);
				break;
			case MODE:
				String mode = args[2].replace('_', ',');
				Util.print("  Change mode to " + mode);
				setFeatures(new Preferences(null, mode, true));
				break;
			case Query.ERROR:
				Util.print(args[2]);
				break;
			case WRITEBACK:
				Util.print("Writeback");
				break;
			default:
				assert false : args[1];
			}
		} catch (Throwable e) {
			System.err.println("Ignoring exception in replayer: " + e);
			e.printStackTrace();
		}
		replayer.update();
	}

	Perspective replayPerspective(String arg) {
		return query.findPerspective(Integer.parseInt(arg));
	}

	/**
	 * movie db session -1730120890 on playfair has the whole contest video
	 * sequence as well as text search, restrict, and reorder.
	 * 
	 */
	final class Replayer extends UpdateNoArgsThread {

		private List sessions;

		private String[] ops;

		int opNum = -1;

		private long opTime = now();

		final Bungee art;

		Replayer(Bungee _art, String sessionsArg) {
			super("Replayer", 0);
			sessions = new ArrayList(Arrays.asList(sessionsArg.split(",")));
			sessions.remove("");
			art = _art;
			opTimer.setRepeats(false);
		}

		private Timer opTimer = new Timer(100, new ActionListener() {

			public void actionPerformed(ActionEvent ignore) {
				replaySessions();
			}
		});

		public void process() {
			opTimer.setInitialDelay(2000);
			// There's a race condition I can't figure out setting the size of
			// the frame.
			// Delay the first session and hope the size gets set in the mean
			// time.
			opTimer.restart();
		}

		void replaySessions() {
			// Util.print("replaySessions " + opNum + " " + sessions.size() + "
			// "
			// + isInitted());
			if (opNum < 0) {
				if (sessions.size() > 0) {
					if (isInitted()) {
						if (query.isRestrictedData()) {
							// hide this process from stop, which is called by
							// setDatabase
							replayer = null;
							setDatabase(dbName);
							replayer = this;
							// wait for re-initialization
							process();
							return;
						} else if (query.isRestricted())
							clearQuery();
						String session = (String) sessions.remove(0);
						Util.print("Replaying session: " + session);
						ops = query.opsSpec(session);
						opNum = 0;
					} else {
						// wait for re-initialization
						process();
						return;
					}
				} else {
					Util.print("replayOps done.");
					replayer = null;
					exit();
					return;
				}
			}
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
							// String[] args = Util.splitComma(ops[opNum]);
							// String[] prevArgs = Util.splitComma(ops[opNum -
							// 1]);
							// delay = 1000 * (Integer.parseInt(args[0]) -
							// Integer
							// .parseInt(prevArgs[0]));
							// if (delay > 3000)
							delay = 3000;
							opTime = now + delay;
							// Util.print("replayOp " + (opNum - 1) + " delay =
							// "
							// + delay);
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
			} else {
				Util.print("Session done.");
				opNum = -1;
				opTimer.setInitialDelay(100);
				opTimer.restart();
			}
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

	final class DocumentShower extends QueueThread {

		DocumentShower() {
			super("DocumentShower", 0);
		}

		// Handle both Items and Strings in one class to minimize the number of
		// Threads. For items, look up the URL and recurse.
		public void process(final Object objectToShow) {
			if (objectToShow instanceof Item) {
				Item item = (Item) objectToShow;
				if (informedia != null) {
					informedia.playSegment(item);
					return;
				}
				String URLs = query == null ? null : query.getItemURL(item);
				// Util.print("Show document " + URLs);
				if (URLs != null && URLs.length() != 0)
					process(URLs);
			} else {
				String URLs = (String) objectToShow;
				// if (URLs.equals("About this collection"))
				// URLs = aboutCollection();
				// if (URLs.indexOf("library.pitt.edu") > 0) {
				// // LoC & MedART can't handle additional query args
				// String prefix = "?";
				// if (URLs.indexOf('?') > 0)
				// prefix = "&";
				// try {
				// URLs += prefix + "referer="
				// + URLEncoder.encode(bookmark(), "UTF-8");
				// } catch (UnsupportedEncodingException e) {
				// e.printStackTrace();
				// }
				// }

				URL url = null;
				try {
					url = new URL(new URL(codeBase()), URLs);
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
				if (basicJNLPservice != null) {
					basicJNLPservice.showDocument(url);
				} else {
					try {
						// Util.print("firefox " + URLs);
						// Util.copyFile(URLs,
						// "C:\\Documents and
						// Settings\\mad\\Desktop\\50thAnniversary\\FlipImages"
						// + URLs.substring(URLs.lastIndexOf('\\')));
						// if (false)
						Runtime.getRuntime().exec(
								"C:\\Program Files\\Mozilla Firefox\\firefox.exe \""
										+ url + "\"");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	final class FacetSelecter extends UpdateNoArgsThread {

		FacetSelecter() {
			super("FacetSelecter", 0);
		}

		public void process() {
			waitForValidQuery();
			javax.swing.SwingUtilities.invokeLater(getDoSelectFacet());
		}
	}

	final class DataUpdater extends UpdateNoArgsThread {

		DataUpdater() {
			super("DataUpdater", 0);
			// Util.print("Enter DataUpdater");
			// setPriority(getPriority() + 1); // This doesn't do any good
			// - it's
			// MySQL's priority we want to lower
		}

		public void init() {
			query = new Query(server, dbName);

			Runnable continueSetDatabase = new Runnable() {

				public void run() {
					initializeFrames();
					NonAlchemyModel.test(query, 50);
				}
			};

			javax.swing.SwingUtilities.invokeLater(continueSetDatabase);
			super.init();
		}

		public void process() {
			// Util.print("DataUpdater.process");
			boolean mustGetCounts = updateOnItems();
			if (isUpToDate()) {
				// Util.print("DataUpdater queueing getOnItemsUpdated");
				final int updateIndex = query.updateIndex;
				Runnable doOnItemsUpdated = new Runnable() {
					public void run() {
						if (updateIndex == query.updateIndex) {
							// Util.print("Art.onItemsUpdated " +
							// query.getOnCount());
							grid.dataUpdated();

							// if (showImages) {
							// updateOnItems(onCount);
							// }

							// Do this after counts updated, so FacetText colors
							// are
							// right.
							// selectedItem.updateOnItems();
							if (query.getOnCount() == 0
									&& !query.isShortSearch()) {
								setTip("No results. You need to remove some filters.  (There are now "
										+ query.describeNfilters() + ")");
							}
						}
					}
				};

				javax.swing.SwingUtilities.invokeLater(doOnItemsUpdated);
				query.updateData(!mustGetCounts);
				if (isUpToDate()) {
					query.setQueryValid();
					javax.swing.SwingUtilities.invokeLater(getRedrawer());
				} else {
					assert waiting > 1;
					// Util.print("updater aborting/handleCursor false");
					handleCursor(false);
				}
			} else {
				assert waiting > 1;
				// Util.print("updater aborting/handleCursor false");
				handleCursor(false);
			}
		}
	}

	boolean isExpertMode() {
		return new Preferences(features, Preferences.expertFeatureNames, true)
				.equals(features);
	}

	void expertMode() {
		setFeatures(new Preferences(features, Preferences.expertFeatureNames,
				true));
	}

	void beginnerMode() {
		setFeatures(new Preferences(features, Preferences.expertFeatureNames,
				false));
	}
}