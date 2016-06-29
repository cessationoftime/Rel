package org.reldb.dbrowser.ui;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.wb.swt.ResourceManager;
import org.reldb.dbrowser.ui.version.Version;
import org.eclipse.wb.swt.SWTResourceManager;

public class AboutDialog extends Dialog {

	protected Shell shell;

	private static final int backgroundWidth = 500;
	private static final int backgroundHeight = 330;
	
	private static final int urlTop = 180;
	private static final int rightPos = backgroundWidth - 10;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public AboutDialog(Shell parent) {
		super(parent, SWT.None);
		setText("About Rel");
	}
	
	/**
	 * Open the dialog.
	 */
	public void open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private boolean isPointInText(Control control, String s, int rightPosn, int topPosn, Point p) {
		GC gc = new GC(control);
		gc.setFont(shell.getFont());
		Point dimensions = gc.textExtent(s);
		Rectangle controlRect = new Rectangle(rightPosn - dimensions.x, topPosn, dimensions.x, dimensions.y);
		return controlRect.contains(p);
	}
	
	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), SWT.APPLICATION_MODAL);
		shell.setText("About Rel");
		shell.setLayout(null);
		shell.setMinimumSize(backgroundWidth, backgroundHeight);
		shell.setSize(backgroundWidth, backgroundHeight);
		shell.setLocation(getParent().getLocation().x + (getParent().getSize().x - backgroundWidth) / 2, 
						  getParent().getLocation().y + (getParent().getSize().y - backgroundHeight) / 2);
		
		Button btnOk = new Button(shell, SWT.PUSH);
		btnOk.setText("Ok");
		btnOk.setFocus();
		btnOk.setSize(btnOk.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		btnOk.setLocation(rightPos - btnOk.getSize().x, backgroundHeight - btnOk.getSize().y - 10);
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.dispose();
			}
		});
		shell.setDefaultButton(btnOk);
    	
		Image background = ResourceManager.getPluginImage("RelUI", "icons/RelAboutAndSplash.png");
		
		shell.addPaintListener(new PaintListener() {
	        public void paintControl(PaintEvent e) {
	        	if (background != null)
	        		e.gc.drawImage(background, 0, 0);
	        	e.gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
	        	e.gc.setFont(SWTResourceManager.getFont("Arial", 18, SWT.BOLD));
	        	int width = e.gc.textExtent(Version.getVersion()).x;
	        	e.gc.drawText(Version.getVersion(), rightPos - width, urlTop - 62, true);
	        	e.gc.setFont(SWTResourceManager.getFont("Arial", 12, SWT.BOLD));
	        	width = e.gc.textExtent(Version.getCopyright()).x;
	        	e.gc.drawText(Version.getCopyright(), rightPos - width, urlTop - 28, true);
	        	e.gc.setForeground(SWTResourceManager.getColor(150, 200, 255));
	        	width = e.gc.textExtent(Version.getURL()).x;
	        	e.gc.drawText(Version.getURL(), rightPos - width, urlTop, true);
	        } 
	    });
		
		shell.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if (isPointInText(shell, Version.getURL(), rightPos, urlTop, new Point(e.x, e.y)))
					org.eclipse.swt.program.Program.launch(Version.getURL());
			}
		});
		
		shell.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				Cursor cursor = shell.getCursor();
				if (cursor != null)
					cursor.dispose();
				boolean isPointerInURL = isPointInText(shell, Version.getURL(), rightPos, urlTop, new Point(e.x, e.y));
				cursor = new Cursor(shell.getDisplay(), isPointerInURL ? SWT.CURSOR_HAND : SWT.CURSOR_ARROW); 
				shell.setCursor(cursor);
			}			
		});
		
		shell.pack();
	}

	public static void main(String args[]) {
		Display display = new Display();
		Shell shell = new Shell(display);
		(new AboutDialog(shell)).open();
	}
	
}
