package edu.cmu.cs.bungee.client.viz;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyContainer;
import edu.umd.cs.piccolox.event.PStyledTextEventHandler;
import edu.umd.cs.piccolox.nodes.PStyledText;

public class TextSearch extends LazyContainer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private PStyledText searchBox;

	JTextComponent editor;

	private PStyledTextEventHandler textHandler;

	private APText searchLabel;

	private Bungee art;

	TextSearch(Bungee art) {
		this.art = art;
		setPickable(false);

		searchLabel = oneLineLabel();
		searchLabel.setTextPaint(Bungee.headerFG);
		searchLabel.setPickable(false);
		searchLabel.setVisible(false);
		searchLabel.setText("Text Search");
		addChild(searchLabel);

		editor = createEditor();
		editor.setFont(font());
		// editor.setText(" ");
		textHandler = new PStyledTextEventHandler(art().getCanvas(), editor);
		addInputEventListener(textHandler);

		searchBox = new PStyledText();
		searchBox.setPaint(Bungee.headerFG);
		searchBox.setVisible(false);
		searchBox.setConstrainHeightToTextHeight(false);
		searchBox.setConstrainWidthToTextWidth(false);
		// searchBox = textHandler.createText();
		Document doc = editor.getUI().getEditorKit(editor)
				.createDefaultDocument();
		searchBox.setDocument(doc);
		searchBox.setHeight(lineH() - 2);
		addChild(searchBox);

		InputMap inputMap = editor.getInputMap();
		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(key, new KeypressEnterAction());
	}

	void positionLabels() {
		double x = 0;
		searchLabel.setFont(font());
		searchLabel.setOffset(x, 0);
		x += searchLabel.getWidth() + 6;

		// Util.print("positionLabels " + w+" "+x);

		// searchBox.setHeight(lineH() - 2);
		searchBox.setOffset(x, 0); // x + 10, y);
		searchBox.setWidth(Math.max(1, w - x)); // 10 * lineH());
		if (searchBox.getParent() == null)
			addChild(searchBox); // If edited string is empty and you
		// click
		// outside, it is removed.
		// searchBox gets taller when you click on it.
		// Can't figure out how to make it get taller initially,
		// so just add a fudge factor.

	}

	void doSearch() {
		String s = editor.getText().trim();
		art().printUserAction(Bungee.SEARCH, s, 0);
//		Util.print("doSearch '" + s + "'");
		String error = Query.isShortSearch(s);
		if (error != null) {
			art().setTip(error);
			// shortSearchWarning.setText(error);
			// addChild(shortSearchWarning);
		} else {
			editor.setText(null);
			art.mayHideTransients();
			query().addTextSearch(s);
			art().updateAllData();

			// This doesn't work - it is already grabbed from PInputManager's
			// point of view.
			// art().grabFocus();
		}
		if (searchBox.getParent() == null)
			addChild(searchBox);
	}

	final class KeypressEnterAction extends AbstractAction {

		// QueryViz qv;

		// /**
		// * What to do when Enter key is pressed
		// */
		// KeypressEnterAction(QueryViz _q) {
		// qv = _q;
		// }

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			doSearch();
		}
	}

	// copied from PStyledTextEventHandler
	private JTextComponent createEditor() {
		JTextPane tComp = new JTextPane() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			/**
			 * Set some rendering hints - if we don't then the rendering can be
			 * inconsistent. Also, Swing doesn't work correctly with fractional
			 * metrics.
			 */
			@Override
			public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING,
						RenderingHints.VALUE_RENDER_QUALITY);
				g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
						RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

				super.paint(g);
			}
		};
		return tComp;
	}

	void setSearchVisibility(boolean isVisible) {
		// Util.print("TextSearch.setSearchVisibility " + isVisible);
		isVisible = isVisible || query().nSearches() > 0;
		searchLabel.setVisible(isVisible);
		searchBox.setVisible(isVisible);
		if (searchBox.getParent() == null)
			addChild(searchBox);
	}

	void setSelectedForEdit(Perspective facet) {
		// Util.print("QueryViz.setSelectedForEdit");
		try {
			Document doc = searchBox.getDocument();
			doc.remove(0, doc.getLength());
			doc.insertString(0, facet.toString(), null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		// searchBox.setVisible(true);
		searchBox.syncWithDocument();
		searchBox.validateFullPaint();
		searchBox.repaint();
	}

	void doHideTransients() {
		// if (isInitted) {
		// shortSearchWarning.removeFromParent();
		textHandler.stopEditing();
		art().grabFocus();
		// }
	}

	APText oneLineLabel() {
		return art().oneLineLabel();
	}

	Bungee art() {
		return art;
	}

	Query query() {
		return art.query;
	}

	double lineH() {
		return art().lineH + 1.0;
	}

	Font font() {
		return art().font;
	}

}
