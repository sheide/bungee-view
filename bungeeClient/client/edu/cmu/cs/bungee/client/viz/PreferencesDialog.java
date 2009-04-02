package edu.cmu.cs.bungee.client.viz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import edu.cmu.cs.bungee.javaExtensions.Util;

class PreferencesDialog extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Bungee art;

	private JCheckBox popups;
	private JCheckBox openClose;
	private JLabel textFieldLabel;
	private JSpinner fontSpinner;
	private JCheckBox arrows;
	private JCheckBox boundaries;
	private JCheckBox tagLists;
	private JCheckBox zoom;
	private JCheckBox checkboxes;
	private JCheckBox shortcuts;
	private JCheckBox brushing;
	private JCheckBox pvalues;
	private JCheckBox medians;
	private JCheckBox sortMenus;
	private JCheckBox clustering;
	private JCheckBox editing;

	private JSpinner columnsSpinner;

	private Window window;

	PreferencesDialog(Bungee _art, Window _window) {
		art = _art;
		window = _window;
		Preferences features = art.features;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		JPanel leftPane = new JPanel(new BorderLayout());
		leftPane
				.add(generalPreferencesPanel(features), BorderLayout.PAGE_START);
		leftPane.add(expertPreferencesPanel(features), BorderLayout.CENTER);

		add(leftPane, BorderLayout.LINE_START);

		if (art.query.isEditable()) {
			add(superExpertPreferencesPanel(features));
		}

		add(okCancelPanel());
	}

	private JPanel generalPreferencesPanel(Preferences features) {
		JPanel generalPreferencesPane = new JPanel(new GridLayout(0, 1));
		generalPreferencesPane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("General Preferences"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		fontSpinner = new JSpinner(new SpinnerNumberModel(art.getTextSize(),
				Bungee.MIN_TEXT_HEIGHT, art.maxTextSize(), 1));
		fontPanel.add(fontSpinner);
		textFieldLabel = new JLabel("Font size");
		fontPanel.add(textFieldLabel);
		generalPreferencesPane.add(fontPanel);

		JPanel columnsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		columnsSpinner = new JSpinner(nColumnsSpinnerModel(art.grid
				.maxColumns()));
		setEditorWidth(columnsSpinner, "Bungee's choice");
		int desiredColumns = features.nColumns;
		// Util.print("gg "+desiredColumns);
		columnsSpinner
				.setValue(desiredColumns > 0 ? new Integer(desiredColumns)
						: (Object) "Bungee's choice");
		columnsPanel.add(columnsSpinner);
		textFieldLabel = new JLabel("Number of columns in Matches pane");
		columnsPanel.add(textFieldLabel);
		generalPreferencesPane.add(columnsPanel);

		popups = new JCheckBox(
				"Show popups on tag mouse-over (otherwise show in header)");
		popups.setSelected(features.popups);
		generalPreferencesPane.add(popups);

		openClose = new JCheckBox(
				"Show +/- buttons to open/close tag hierarchies in Selected Result pane");
		openClose.setSelected(features.openClose);
		generalPreferencesPane.add(openClose);

		return generalPreferencesPane;
	}

	private JPanel expertPreferencesPanel(Preferences features) {
		JPanel expertFeaturesPane = new JPanel(new GridLayout(0, 1));
		expertFeaturesPane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Expert features"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		JLabel multipleSelectionLabel = new JLabel(
				"Allow multiple selection and negation by:");
		expertFeaturesPane.add(multipleSelectionLabel);

		checkboxes = new JCheckBox("using checkboxes");
		checkboxes.setSelected(features.checkboxes);
		shortcuts = new JCheckBox("using control and shift keys");
		shortcuts.setSelected(features.shortcuts);
		JPanel multipleSelectionCheckboxesPanel = new JPanel(new FlowLayout(
				FlowLayout.LEADING));
		JPanel multipleSelectionShortcutPanel = new JPanel(new FlowLayout(
				FlowLayout.LEADING));
		multipleSelectionCheckboxesPanel.add(Box.createRigidArea(new Dimension(
				25, 1)));
		multipleSelectionCheckboxesPanel.add(checkboxes);
		multipleSelectionShortcutPanel.add(Box.createRigidArea(new Dimension(
				25, 1)));
		multipleSelectionShortcutPanel.add(shortcuts);
		expertFeaturesPane.add(multipleSelectionCheckboxesPanel);
		expertFeaturesPane.add(multipleSelectionShortcutPanel);

		brushing = new JCheckBox(
				"Use linked highlighting to show which matches have a tag, and which tags a result has");
		brushing.setSelected(features.brushing);
		expertFeaturesPane.add(brushing);

		pvalues = new JCheckBox(
				"Show the p-value for the difference between the percentage of items "
						+ "that satisfy the filters for items with the tag "
						+ "and the percentage for items with another tag in the same category");
		pvalues.setSelected(features.pvalues);
		expertFeaturesPane.add(pvalues);

		medians = new JCheckBox(
				"For tag categories that have a natural order, draw an arrow that compares the median tag for all items "
						+ "with the median tag for filtered items");
		medians.setSelected(features.medians);
		expertFeaturesPane.add(medians);

		sortMenus = new JCheckBox(
				"Show a menu for sorting matches by a tag category");
		sortMenus.setSelected(features.sortMenus);
		expertFeaturesPane.add(sortMenus);

		arrows = new JCheckBox(
				"Use arrow keys to navigate through Tags and Matches");
		arrows.setSelected(features.arrows);
		expertFeaturesPane.add(arrows);

		boundaries = new JCheckBox(
				"Show draggable pane boundaries on mouse over");
		boundaries.setSelected(features.boundaries);
		expertFeaturesPane.add(boundaries);

		tagLists = new JCheckBox(
				"Allow clicking on the open tag category label, to show a scrolling list of all tags in the category");
		tagLists.setSelected(features.tagLists);
		expertFeaturesPane.add(tagLists);

		zoom = new JCheckBox(
				"Show alphabetic prefixes above tag bars, and allow zooming by clicking, typing, or dragging");
		zoom.setSelected(features.zoom);
		expertFeaturesPane.add(zoom);

		clustering = new JCheckBox("Allow clustering by typing control-C");
		clustering.setSelected(features.clustering);
		expertFeaturesPane.add(clustering);

		JPanel shortcutPanel = new JPanel();
		JButton expert = new JButton("All expert features");
		expert.setActionCommand("expert");
		expert.addActionListener(this);
		shortcutPanel.add(expert);

		JButton beginner = new JButton("No expert features");
		beginner.setActionCommand("beginner");
		beginner.addActionListener(this);
		shortcutPanel.add(beginner);

		expertFeaturesPane.add(shortcutPanel);
		return expertFeaturesPane;
	}

	private JPanel okCancelPanel() {

		JButton ok = new JButton("OK");
		ok.setActionCommand("OK");
		ok.addActionListener(this);

		JButton cancel = new JButton("Cancel");
		cancel.setActionCommand("Cancel");
		cancel.addActionListener(this);

		JPanel okCancelPanel = new JPanel(new FlowLayout());
		okCancelPanel.add(ok);
		okCancelPanel.add(cancel);
		return okCancelPanel;

	}

	private JPanel superExpertPreferencesPanel(Preferences features) {

		JPanel superExpertFeaturesPane = new JPanel(new GridLayout(0, 1));
		superExpertFeaturesPane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Do not try this at home"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		editing = new JCheckBox(
				"Allow updating the database by clicking with the middle mouse button");
		editing.setSelected(features.editing);
		superExpertFeaturesPane.add(editing);
		return superExpertFeaturesPane;
	}

	SpinnerListModel nColumnsSpinnerModel(int maxCols) {
		List items = new LinkedList();
		items.add("Bungee's choice");
		for (int i = 1; i < maxCols; i++)
			items.add(new Integer(i));
		return new SpinnerListModel(items);
	}

	void setEditorWidth(JSpinner spinner, String value) {
		JFormattedTextField ftf = getTextField(spinner);
		if (ftf != null) {
			ftf.setColumns((int) (0.6 * value.length()));
		}
	}

	JFormattedTextField getTextField(JSpinner spinner) {
		JComponent editor = spinner.getEditor();
		if (editor instanceof JSpinner.DefaultEditor) {
			return ((JSpinner.DefaultEditor) editor).getTextField();
		} else {
			System.err.println("Unexpected editor type: "
					+ spinner.getEditor().getClass()
					+ " isn't a descendant of DefaultEditor");
			return null;
		}
	}

	public void actionPerformed(ActionEvent e) {
		if ("expert".equals(e.getActionCommand())) {
			setExpertFeatures(true);
		} else if ("beginner".equals(e.getActionCommand())) {
			setExpertFeatures(false);

		} else if ("OK".equals(e.getActionCommand())) {
			art.setFeatures(getFeatures());
			// Util.print(art.features);
			window.dispose();
		} else if ("Cancel".equals(e.getActionCommand())) {
			window.dispose();
		}
	}

	private Preferences getFeatures() {
		StringBuffer buf = new StringBuffer();
		buf.append("fontSize=").append(
				((Integer) fontSpinner.getValue()).intValue());
		Object nColumnsValue = columnsSpinner.getValue();
		if (nColumnsValue instanceof Integer) {
			buf.append(",nColumns=").append(
					((Integer) nColumnsValue).intValue());
		}
		if (arrows.isSelected())
			buf.append(",arrows");
		if (boundaries.isSelected())
			buf.append(",boundaries");
		if (brushing.isSelected())
			buf.append(",brushing");
		if (checkboxes.isSelected())
			buf.append(",checkboxes");
		if (clustering.isSelected())
			buf.append(",clustering");
		if (medians.isSelected())
			buf.append(",medians");
		if (openClose.isSelected())
			buf.append(",openClose");
		if (popups.isSelected())
			buf.append(",popups");
		if (pvalues.isSelected())
			buf.append(",pvalues");
		if (shortcuts.isSelected())
			buf.append(",shortcuts");
		if (sortMenus.isSelected())
			buf.append(",sortMenus");
		if (tagLists.isSelected())
			buf.append(",tagLists");
		if (zoom.isSelected())
			buf.append(",zoom");
		if (editing != null && editing.isSelected())
			buf.append(",editing");
		// Util.print("kk " + new Preferences(null, buf.toString(), true));
		return new Preferences(null, buf.toString(), true);
	}

	void setExpertFeatures(boolean state) {
		arrows.setSelected(state);
		boundaries.setSelected(state);
		tagLists.setSelected(state);
		zoom.setSelected(state);
		checkboxes.setSelected(state);
		shortcuts.setSelected(state);
		brushing.setSelected(state);
		pvalues.setSelected(state);
		medians.setSelected(state);
		sortMenus.setSelected(state);
		clustering.setSelected(state);
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event dispatch thread.
	 */
	static void createAndShowGUI(Bungee art) {
		// Create and set up the window.
		// JFrame frame = new JFrame("Choose Bungee View options");
		JDialog frame = new JDialog(art, "Choose Bungee View options");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Add content to the window.
		frame.getContentPane().add(new PreferencesDialog(art, frame));

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

}

class Preferences {
	final int fontSize;
	final int nColumns;
	final boolean arrows;
	final boolean boundaries;
	final boolean brushing; // h
	final boolean checkboxes; // x
	final boolean clustering;
	final boolean medians;
	final boolean openClose;
	final boolean popups;
	final boolean pvalues; // v
	final boolean shortcuts;
	final boolean sortMenus; // r
	final boolean tagLists;
	final boolean zoom;
	final boolean editing;

	private static final String[] featureNames = { "fontSize", "nColumns",
			"arrows", "boundaries", "brushing", "checkboxes", "clustering",
			"medians", "openClose", "popups", "pvalues", "shortcuts",
			"sortMenus", "tagLists", "zoom", "editing" };

	static String expertFeatureNames = "arrows,boundaries,brushing,checkboxes,clustering,medians,"
			+ "pvalues,shortcuts,sortMenus,tagLists,zoom";

	static Preferences defaultFeatures = new Preferences(null,
			"fontSize=14,popups", true);

	Preferences(Preferences base, String optionsToChange, boolean changeTo) {
		boolean a = base != null;
		String[] options = optionsToChange.split(",");
		int font = a ? base.fontSize : 14;
		int nCols = a ? base.nColumns : -1;
		for (int i = 0; i < options.length; i++) {
			String[] option = options[i].split("=");
			if (option.length > 1) {
				if (option[0].equals("fontSize"))
					font = Integer.parseInt(option[1]);
				else if (option[0].equals("nColumns"))
					nCols = Integer.parseInt(option[1]);
				else
					assert false : options[i];
			} else {
				assert Util.isMember(featureNames, options[i]) : options[i]
						+ " " + featureNames;
			}
		}
		fontSize = font;
		nColumns = nCols;
		arrows = Util.isMember(options, "arrows") ? changeTo : a && base.arrows;
		boundaries = Util.isMember(options, "boundaries") ? changeTo : a
				&& base.boundaries;
		brushing = Util.isMember(options, "brushing") ? changeTo : a
				&& base.brushing;
		checkboxes = Util.isMember(options, "checkboxes") ? changeTo : a
				&& base.checkboxes;
		clustering = Util.isMember(options, "clustering") ? changeTo : a
				&& base.clustering;
		medians = Util.isMember(options, "medians") ? changeTo : a
				&& base.medians;
		openClose = Util.isMember(options, "openClose") ? changeTo : a
				&& base.openClose;
		popups = Util.isMember(options, "popups") ? changeTo : a && base.popups;
		pvalues = Util.isMember(options, "pvalues") ? changeTo : a
				&& base.pvalues;
		shortcuts = Util.isMember(options, "shortcuts") ? changeTo : a
				&& base.shortcuts;
		sortMenus = Util.isMember(options, "sortMenus") ? changeTo : a
				&& base.sortMenus;
		tagLists = Util.isMember(options, "tagLists") ? changeTo : a
				&& base.tagLists;
		zoom = Util.isMember(options, "zoom") ? changeTo : a && base.zoom;
		editing = Util.isMember(options, "editing") ? changeTo : a
				&& base.editing;
	}

	String features2string() {
		StringBuffer buf = new StringBuffer();
		buf.append("fontSize=").append(fontSize);
		if (nColumns > 0)
			buf.append(",nColumns=").append(nColumns);
		if (arrows)
			buf.append(",arrows");
		if (boundaries)
			buf.append(",boundaries");
		if (brushing)
			buf.append(",brushing");
		if (checkboxes)
			buf.append(",checkboxes");
		if (clustering)
			buf.append(",clustering");
		if (medians)
			buf.append(",medians");
		if (openClose)
			buf.append(",openClose");
		if (popups)
			buf.append(",popups");
		if (pvalues)
			buf.append(",pvalues");
		if (shortcuts)
			buf.append(",shortcuts");
		if (sortMenus)
			buf.append(",sortMenus");
		if (tagLists)
			buf.append(",tagLists");
		if (zoom)
			buf.append(",zoom");
		if (editing)
			buf.append(",editing");
		return buf.toString();
	}

	public String toString() {
		return "<Preferences " + features2string() + ">";
	}

	public boolean equals(Object o) {
		if (!(o instanceof Preferences))
			return false;
		Preferences options = (Preferences) o;
		return options.fontSize == fontSize && options.nColumns == nColumns
				&& options.arrows == arrows && options.boundaries == boundaries
				&& options.brushing == brushing
				&& options.checkboxes == checkboxes
				&& options.clustering == clustering
				&& options.medians == medians && options.openClose == openClose
				&& options.popups == popups && options.pvalues == pvalues
				&& options.shortcuts == shortcuts
				&& options.sortMenus == sortMenus
				&& options.tagLists == tagLists && options.zoom == zoom
				&& options.editing == editing;
	}

	public int hashCode() {
		int result = 17;
		result = 37 * result + fontSize;
		result = 37 * result + nColumns;
		result = 37 * result + (arrows ? 0 : 1);
		result = 37 * result + (boundaries ? 0 : 1);
		result = 37 * result + (brushing ? 0 : 1);
		result = 37 * result + (checkboxes ? 0 : 1);
		result = 37 * result + (clustering ? 0 : 1);
		result = 37 * result + (medians ? 0 : 1);
		result = 37 * result + (openClose ? 0 : 1);
		result = 37 * result + (pvalues ? 0 : 1);
		result = 37 * result + (shortcuts ? 0 : 1);
		result = 37 * result + (sortMenus ? 0 : 1);
		result = 37 * result + (tagLists ? 0 : 1);
		result = 37 * result + (zoom ? 0 : 1);
		result = 37 * result + (editing ? 0 : 1);
		return result;
	}

}
