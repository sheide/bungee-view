package edu.cmu.cs.bungee.client.viz;

import java.awt.Color;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query;
import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.cmu.cs.bungee.piccoloUtils.gui.AbstractMenuItem;
import edu.cmu.cs.bungee.piccoloUtils.gui.Menu;
import edu.umd.cs.piccolo.PLayer;

public class Editing {

	/**
	 * This is the "argument" facet, in case the command needs more than the
	 * facet clicked on. Also used by the right-click addItemFacet gesture
	 */
	Perspective selectedForEdit;

	Menu editMenu;

	// /**
	// * This is the facet we [middle] clicked on.
	// */
	// Perspective editMenuPerspective;

	Item editMenuItem;

	private Bungee art;

	Editing(Bungee art) {
		super();
		this.art = art;
	}

	Bungee art() {
		return art;
	}

	Query query() {
		return art.query;
	}

	Summary summary() {
		return art.summary;
	}

	Item selectedItemItem() {
		return art().selectedItemItem();
	}

	PLayer getLayer() {
		return art().getCanvas().getLayer();
	}

	boolean getIsEditing() {
		return art().getIsEditing();
	}

	private abstract class Edit extends AbstractMenuItem {

		private Perspective editMenuPerspective;

		Edit(Perspective editMenuPerspective, String label) {
			super(label);
			this.editMenuPerspective = editMenuPerspective;
		}

		public String doCommand() {
			Util.print("doCommand " + editMenuPerspective + " " + this);
			Perspective connected = summary().connectedPerspective();

			try {
				Collection updated = doEditCommand(editMenuPerspective);

				if (updated != null && updated.size() > 0) {
					// Util.print("Reverting PerspectiveViz's for "
					// + Util.valueOfDeep(updated));
					Collection unchanged = new ArrayList(query()
							.displayedPerspectives());
					unchanged.removeAll(updated);

					// This will retain only the unchanged perspectives,
					// deleting the changed ones. Then updateAllData will add
					// them back again.
					summary().synchronizePerspectives(unchanged, null);
				}
				art().updateAllData(connected);
				// if (connected != null
				// && summary().connectedPerspective() != connected)
				// summary().connectToPerspective(connected);

			} catch (IllegalArgumentException e) {
				art().setTip(e.getMessage());
			}
			return null;
		}

		abstract Collection doEditCommand(Perspective facet);
	}

	private class RotateCommand extends AbstractMenuItem {
		private Object[] args;

		RotateCommand(Item item, int degrees) {
			super("Rotate " + degrees + " degrees clockwise: ");
			Object[] args1 = { item, String.valueOf(degrees) };
			this.args = args1;
		}

		public String doCommand() {
			Item item = (Item) args[0];
			String degrees = (String) args[1];
			query().rotate(item, degrees);
			art().decacheCurrentItem();
			return null;
		}
	}

	private class SetSelectedForEditCommand extends Edit {
		SetSelectedForEditCommand(Perspective facet) {
			super(facet, "Set default tag to " + facet
					+ " (right mouse button)");
		}

		Collection doEditCommand(Perspective facet) {
			setSelectedForEdit(facet, 0);
			return null;
		}
	}

	// private class AddToSelectedResultCommand extends Edit {
	// AddToSelectedResultCommand(Perspective facet) {
	// super(facet, "Add " + facet + " to Selected Result");
	// }
	//
	// Collection doEditCommand(Perspective facet) {
	// return addToSelectedResult(facet);
	// }
	// }
	//
	// private class AddToSelectedFacetCommand extends AbstractMenuItem {
	// private Item item;
	//
	// AddToSelectedFacetCommand(Item item) {
	// super("Add tag " + selectedForEdit + " (right mouse button)");
	// this.item = item;
	// }
	//
	// public String doCommand() {
	// return addItemFacet(item, selectedForEdit);
	// }
	// }

	private class AddTagCommand extends Edit {
		private Item item;

		AddTagCommand(Item item, Perspective facet, String label) {
			super(facet, label);
			this.item = item;
		}

		Collection doEditCommand(Perspective facet) {
			Collection updated = query().addItemFacet(facet, item);
			if (item == selectedItemItem()) {
				art().decacheCurrentItem();
			}
			return updated;
		}
	}

	private class RemoveFromSelectedResultCommand extends Edit {
		RemoveFromSelectedResultCommand(Perspective facet) {
			super(facet, "Remove " + facet + " from Selected Result");
		}

		Collection doEditCommand(Perspective facet) {
			Collection updated = query().removeItemFacet(facet,
					selectedItemItem());
			art().decacheCurrentItem();
			return updated;
		}
	}

	private class AddToResultSetCommand extends Edit {
		AddToResultSetCommand(Perspective facet) {
			super(facet, "Add " + facet + " to entire Result Set");
		}

		Collection doEditCommand(Perspective facet) {
			Collection updated = query().addItemsFacet(facet);
			art().decacheItems();
			return updated;
		}
	}

	private class RemoveFromResultSetCommand extends Edit {
		RemoveFromResultSetCommand(Perspective facet) {
			super(facet, "Remove " + facet + " from entire Result Set");
		}

		Collection doEditCommand(Perspective facet) {
			Collection updated = query().removeItemsFacet(facet);
			art().decacheItems();
			return updated;
		}
	}

	private class AddChildTagCommand extends Edit {
		private String name;

		AddChildTagCommand(String name, Perspective facet) {
			super(facet, "Add child tag " + name + " to " + facet);
			this.name = name;
		}

		Collection doEditCommand(Perspective facet) {
			return query().addChildFacet(facet, name);
		}
	}

	private class ReparentCommand extends Edit {
		ReparentCommand(Perspective facet) {
			super(facet, "Reparent " + selectedForEdit + " to " + facet);
		}

		Collection doEditCommand(Perspective facet) {
			return query().reparent(facet, selectedForEdit);
		}
	}

	private class RenameTagCommand extends Edit {
		private String name;

		RenameTagCommand(String name, Perspective facet) {
			super(facet, "Rename " + facet + " to " + name);
			this.name = name;
		}

		Collection doEditCommand(Perspective facet) {
			String newName = name;
			query().rename(facet, newName);
			facet.setName(newName);
			art().clearTextCaches();
			return null;
		}
	}

	private class AddNewTagTypeCommand extends Edit {
		private String name;

		AddNewTagTypeCommand(String name) {
			super(null, "Add new tag type " + name);
			this.name = name;
		}

		Collection doEditCommand(Perspective facet) {
			return query().addChildFacet(null, name);
		}
	}

	private class WritebackCommand extends AbstractMenuItem {
		WritebackCommand() {
			super("Save changes and exit");
		}

		public String doCommand() {
			art().printUserAction(Bungee.WRITEBACK, 0, 0);
			query().writeback();
			art().setDatabase(null);
			art().dispose();
			return null;
		}
	}

	// Show user parsed date as verification
	 static String prettyPrintDate(String date1) {
		String result = date1;
		try {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			result = format.parse(date1).toString();
		} catch (ParseException e) {
			// return date1 if error
		}
		return result;
	}

	private class RevertCommand extends AbstractMenuItem {
		private String date;

		RevertCommand(String date) {
			super("Revert database to " + prettyPrintDate(date));
			this.date = date;
		}

		public String doCommand() {
			try {
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				format.parse(date);

				query().revert(date);
				art().setDatabase(null);
				art().dispose();

			} catch (ParseException e) {
				art()
						.setTip(
								date
										+ " is not a date of the form yyyy-MM-dd HH:mm:ss");
			}
			return null;
		}
	}

	boolean itemMiddleMenu(Item item, boolean isRight) {
		if (getIsEditing()) {
			if (isRight) {
				if (selectedForEdit == null)
					Util
							.err("Can't add tag to item because selectedForEdit is null");
				new AddTagCommand(item, selectedForEdit, null).doCommand();
				// addItemFacet(item, selectedForEdit);
			} else {
				if (editMenu != null)
					editMenu.removeFromParent();
				editMenu = new Menu(Color.black, Color.white, art().font);
				editMenuItem = item;
				addEditButton(new RotateCommand(item, 90));
				addEditButton(new RotateCommand(item, 180));
				addEditButton(new RotateCommand(item, 270));
				if (selectedForEdit != null) {
					addEditButton(new AddTagCommand(item, selectedForEdit,
							"Add tag " + selectedForEdit
									+ " (right mouse button)"));
				}
				editMenu.setText("");
				getLayer().addChild(editMenu);
				editMenu.pick();
			}
		} else
			Util.err("Editing is disabled (" + item + ")");
		return getIsEditing();
	}

	boolean facetMiddleMenu(Perspective facet) {
		if (getIsEditing()) {
			if (editMenu != null)
				editMenu.removeFromParent();
			editMenu = new Menu(Color.black, Color.white, art().font);
			// editMenuPerspective = facet;
			addEditButton(new SetSelectedForEditCommand(facet));
			if (!art().selectedItem.hasFacet(facet))
				addEditButton(new AddTagCommand(selectedItemItem(), facet,
						"Add " + facet
								+ " to Selected Result (control + right click)"));
			addEditButton(new AddToResultSetCommand(facet));
			String name = art().header.textSearch.editor.getText().trim();
			if (name.length() > 0) {
				addEditButton(new AddChildTagCommand(name, facet));
				addEditButton(new AddNewTagTypeCommand(name));
			}
			// addEditButton("Delete this tag");
			if (facet.getOnCount() != 0) {
				// If it's a deeply nested facet, getOnCount = -1, but we know
				// it is true of the selected result
				if (!art().selectedItem.lacksFacet(facet))
					addEditButton(new RemoveFromSelectedResultCommand(facet));
				addEditButton(new RemoveFromResultSetCommand(facet));
			}
			// addEditButton("Remove from Result Set");
			if (selectedForEdit != null && selectedForEdit != facet) {
				addEditButton(new ReparentCommand(facet));
			}
			if (name.length() > 0 && !name.equals(facet.getName()))
				addEditButton(new RenameTagCommand(name, facet));
			addEditButton(new WritebackCommand());
			if (name.length() > 0) {
				// If it looks like user is trying to enter a date, show the
				// command. Then print error if it's not really a date.
				Pattern p = Pattern.compile("[0-9 /:\\-.]+");
				Matcher m = p.matcher(name);
				if (m.matches())
					addEditButton(new RevertCommand(name));
			}
			editMenu.setText("");
			getLayer().addChild(editMenu);
			editMenu.pick();
		} else
			Util.err("Editing is disabled.");
		return getIsEditing();
	}

	void addEditButton(AbstractMenuItem command) {
		command.label = (editMenu.nChoices() + 1) + ". " + command.label;
		editMenu.addButton(command);
	}

	// private transient Runnable edit;
	//
	// Runnable getEdit() {
	// if (edit == null)
	// edit = new Runnable() {
	//
	// public void run() {
	// String which = (String) editMenu.getData();
	// doEdit(which);
	// editMenu.removeFromParent();
	// editMenu = null;
	// }
	// };
	// return edit;
	// }

	// void doEdit(String which) {
	// Util.print(editMenuPerspective + " " + which);
	// Perspective connected = summary.connectedPerspective();
	// Collection updated = null;
	// if (which.startsWith("Set edit selection to ")) {
	// setSelectedForEdit(editMenuPerspective, 0);
	// } else if (which.startsWith("Add to Selected Result: ")) {
	// decacheItem(selectedItem.currentItem);
	// updated = query.addItemFacet(editMenuPerspective,
	// selectedItem.currentItem);
	// updateSelectedItem();
	// } else if (which.startsWith("Add to Result Set: ")) {
	// decacheItems();
	// updated = query.addItemsFacet(editMenuPerspective);
	// updateSelectedItem();
	// } else if (which.startsWith("Add child tag ")) {
	// updated = query.addChildFacet(editMenuPerspective, which.substring(
	// 16).split(" to ")[0]);
	// } else if (which.startsWith("Remove from Selected Result: ")) {
	// decacheItem(selectedItem.currentItem);
	// updated = query.removeItemFacet(editMenuPerspective,
	// selectedItem.currentItem);
	// updateSelectedItem();
	// } else if (which.startsWith("Remove from Result Set: ")) {
	// decacheItems();
	// updated = query.removeItemsFacet(editMenuPerspective);
	// updateSelectedItem();
	// } else if (which.startsWith("Reparent ")) {
	// updated = query.reparent(editMenuPerspective, selectedForEdit);
	// } else if (which.startsWith("Rename ")) {
	// String newName = which.split(" to ")[1];
	// query.rename(editMenuPerspective, newName);
	// editMenuPerspective.setName(newName);
	// } else if (which.equals("Writeback")) {
	// query.writeback();
	// dispose();
	// } else if (which.startsWith("Add new tag type ")) {
	// updated = query.addChildFacet(null, which.substring(19));
	// } else if (which.startsWith("Rotate ")) {
	// decacheItem(selectedItem.currentItem);
	// query.rotate(editMenuItem, which.substring(7, 10).trim());
	// updateSelectedItem();
	// } else {
	// Util.err("Unknown choice: " + which);
	// }
	// updateForEdit(updated);
	// updateAllData();
	// if (connected != null && summary.connectedPerspective() != connected)
	// summary.connectToPerspective(connected);
	// }

	// private void updateForEdit(Collection updated) {
	// if (updated != null && updated.size() > 0) {
	// Util.print("Reverting PerspectiveViz's for "
	// + Util.valueOfDeep(updated));
	// Collection unchanged = new ArrayList(query.displayedPerspectives());
	// unchanged.removeAll(updated);
	// summary.synchronizePerspectives(unchanged, null);
	// }
	// updateAllData();
	// if (connected != null && summary.connectedPerspective() != connected)
	// summary.connectToPerspective(connected);
	// }

	boolean setSelectedForEdit(Perspective facet, int modifiers) {
		if (getIsEditing()) {
			// Util.print("setSelectedForEdit " + facet + " " + modifiers + " "
			// + InputEvent.CTRL_DOWN_MASK + " "
			// + (modifiers & InputEvent.CTRL_DOWN_MASK) + " "
			// + Perspective.isControlDown(modifiers) + " "
			// + selectedItem.currentItem);
			selectedForEdit = facet;
			// editMenuPerspective = facet;
			art.header.textSearch.setSelectedForEdit(facet);
			if (Util.isControlDown(modifiers) && selectedItemItem() != null) {
				new AddTagCommand(selectedItemItem(), facet, null).doCommand();
				// addToSelectedResult(facet);
				// if (selectedForEdit.nChildren() > 0) {
				// art().toggleFacetLater();
				// }
			}
		} else
			Util.err("Editing is disabled.");
		return getIsEditing();
	}

	// Collection addToSelectedResult(Perspective facet) {
	// art().decacheCurrentItem();
	// Collection updated = query().addItemFacet(facet, selectedItemItem());
	// art().updateSelectedItem();
	// return updated;
	// }
	//
	// String addItemFacet(Item item, Perspective facet) {
	// if (facet != null) {
	// Item oldItem = selectedItemItem();
	// art().selectedItem.currentItem = item;
	// addToSelectedResult(facet);
	// art().selectedItem.currentItem = oldItem;
	// } else {
	// Util.err("Can't add tag to item because selectedForEdit is null");
	// }
	// return null;
	// }

}
