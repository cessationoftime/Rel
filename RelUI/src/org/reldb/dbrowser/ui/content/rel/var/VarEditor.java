package org.reldb.dbrowser.ui.content.rel.var;

import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.reldb.dbrowser.ui.content.filtersorter.FilterSorterState;
import org.reldb.dbrowser.ui.content.rel.DbTreeAction;
import org.reldb.dbrowser.ui.content.rel.DbTreeItem;
import org.reldb.dbrowser.ui.content.rel.RelPanel;

public class VarEditor extends DbTreeAction {

	public VarEditor(RelPanel relPanel) {
		super(relPanel);
	}

	@Override
	public void go(DbTreeItem item, Image image) {
		FilterSorterState filterSorterState = null;		
		CTabItem tab = relPanel.getTab(item);
		if (tab != null) {
			if (tab instanceof ExpressionResultViewerTab)
				filterSorterState = ((ExpressionResultViewerTab)tab).getFilterSorterState();
			if (tab instanceof VarEditorTab) {
				relPanel.setTab(tab);
				return;
			} else
				tab.dispose();
		}
		VarEditorTab editor = new VarEditorTab(relPanel, item, filterSorterState);
		relPanel.setTab(editor, image);
	}

}
