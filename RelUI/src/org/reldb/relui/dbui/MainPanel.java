package org.reldb.relui.dbui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.reldb.relui.dbui.StatusPanel;

public class MainPanel extends Composite {
	
	private CTabFolder tabFolder;
	private StatusPanel statusPanel;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public MainPanel(Composite parent, int style) {
		super(parent, style);
		setLayout(new FormLayout());
		
		tabFolder = new CTabFolder(this, SWT.None);
		FormData fd_tabFolder = new FormData();
		fd_tabFolder.top = new FormAttachment(0);
		fd_tabFolder.left = new FormAttachment(0);
		fd_tabFolder.right = new FormAttachment(100);
		tabFolder.setLayoutData(fd_tabFolder);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		
		statusPanel = new StatusPanel(this, SWT.None);
		FormData fd_statusPanel = new FormData();
		fd_statusPanel.left = new FormAttachment(0);
		fd_statusPanel.right = new FormAttachment(100);
		fd_statusPanel.bottom = new FormAttachment(100);
		statusPanel.setLayoutData(fd_statusPanel);
		
		fd_tabFolder.bottom = new FormAttachment(statusPanel);
	}

	public void setStatus(String s) {
		statusPanel.setStatus(s);
	}

	public CTabFolder getTabFolder() {
		return tabFolder;
	}
}