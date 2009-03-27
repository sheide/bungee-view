package edu.cmu.cs.bungee.piccoloUtils.gui;

public  class AbstractMenuItem implements MenuItem{
	
	public String label;
	String mouseDoc;
//	Runnable action;
//	public Object args;

//	DefaultMenuItem(Runnable action, String label, String mouseDoc) {
//		super();
////		this.action = action;
//		this.label = label;
//		this.mouseDoc = mouseDoc;
//	}

	protected AbstractMenuItem(String label) {
		super();
		this.label = label;
		this.mouseDoc = label;
	}

	public AbstractMenuItem(String label, String mouseDoc) {
		super();
		this.label = label;
		this.mouseDoc = mouseDoc;
	}

	/* 
	 * No-op command. Our answer to graying out menu items. mouse doc should explain why it's "gray"
	 */
	public String doCommand() {
		return null;
	}

	public String getLabel() {
		return label;
	}

	public String getMouseDoc() {
		return mouseDoc;
	}

}
