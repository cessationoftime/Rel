package org.reldb.dbrowser.ui.content.rev.operators;

import org.eclipse.swt.widgets.Composite;
import org.reldb.dbrowser.ui.content.rev.OperatorWithControlPanel;
import org.reldb.dbrowser.ui.content.rev.Rev;

public class Order extends OperatorWithControlPanel {

	public Order(Rev rev, String name, int xpos, int ypos) {
		super(rev, name, "ORDER", xpos, ypos);
		addParameter("Operand"); 
	}

	public void buildControlPanel(Composite container) {}
	
	@Override
	public String getQuery() {
		// TODO Auto-generated method stub
		return null;
	}

}
