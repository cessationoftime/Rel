package org.reldb.dbrowser.ui.content.rel.var;

import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.reldb.dbrowser.ui.content.rel.DbTreeAction;
import org.reldb.dbrowser.ui.content.rel.DbTreeItem;
import org.reldb.dbrowser.ui.content.rel.RelPanel;

public class VarRealDesigner extends DbTreeAction {

	public VarRealDesigner(RelPanel relPanel) {
		super(relPanel);
	}

	@Override
	public void go(DbTreeItem item, Image image) {
		CTabItem tab = relPanel.getTab(item);
		if (tab != null) {
			if (tab instanceof RelvarDesignerTab) {
				tab.getParent().setSelection(tab);
				return;
			} else
				tab.dispose();
		}
		RelvarDesignerTab varDesignTab = new RelvarDesignerTab(relPanel, item);
		varDesignTab.setImage(image);
		relPanel.getTabFolder().setSelection(varDesignTab);
	}

}
