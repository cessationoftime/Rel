package org.reldb.dbrowser.ui.content.rev;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.reldb.dbrowser.ui.DbTab;
import org.reldb.dbrowser.ui.IconLoader;
import org.reldb.dbrowser.ui.content.cmd.CmdPanelOutput;
import org.reldb.dbrowser.ui.content.rev.operators.Diadic;
import org.reldb.dbrowser.ui.content.rev.operators.Extend;
import org.reldb.dbrowser.ui.content.rev.operators.GroupOrWrap;
import org.reldb.dbrowser.ui.content.rev.operators.Order;
import org.reldb.dbrowser.ui.content.rev.operators.Project;
import org.reldb.dbrowser.ui.content.rev.operators.Rename;
import org.reldb.dbrowser.ui.content.rev.operators.Restrict;
import org.reldb.dbrowser.ui.content.rev.operators.Summarize;
import org.reldb.dbrowser.ui.content.rev.operators.TableDee;
import org.reldb.dbrowser.ui.content.rev.operators.TableDum;
import org.reldb.dbrowser.ui.content.rev.operators.UngroupOrUnwrap;
import org.reldb.dbrowser.ui.content.rev.operators.Update;
import org.reldb.dbrowser.ui.preferences.PreferenceChangeAdapter;
import org.reldb.dbrowser.ui.preferences.PreferenceChangeEvent;
import org.reldb.dbrowser.ui.preferences.PreferenceChangeListener;
import org.reldb.dbrowser.ui.preferences.PreferencePageGeneral;
import org.reldb.dbrowser.ui.preferences.Preferences;
import org.reldb.rel.client.Connection;
import org.reldb.rel.client.Tuple;
import org.reldb.rel.client.Tuples;
import org.reldb.rel.client.connection.CrashHandler;

public class Rev extends Composite {
	private Connection connection;
	private Model model;
	private CmdPanelOutput outputView;
	private SashForm revPane;
	private DatabaseAbstractionLayer database;
    
    private PreferenceChangeListener preferenceChangeListener;
	
	private ToolItem loadBtn = null;
	private ToolItem saveBtn = null;
	private ToolItem stopBtn = null;
	
	private Label modelLabel;
	
	private static final String scratchModelName = "scratchpad";
	
	public Rev(Composite parent, DbTab parentTab, CrashHandler crashHandler) {
		super(parent, SWT.None);
		
		try {
			connection = new Connection(parentTab.getURL(), false, crashHandler, null);
		} catch (Exception e) {
			System.out.println("Rev: Unable to establish connection.");
			e.printStackTrace();
		}

		database = new DatabaseAbstractionLayer(connection);
		
		setLayout(new FillLayout());
				
		revPane = new SashForm(this, SWT.NONE);
		revPane.setOrientation(SWT.VERTICAL);
		
		try {
			outputView = new CmdPanelOutput(revPane, parentTab, SWT.NONE) {
				@Override
				protected void notifyInputDone() {
					stopBtn.setEnabled(false);
				}
				@Override
				public void go(String text, boolean copyInputToOutput) {
					stopBtn.setEnabled(true);
					super.go(text, copyInputToOutput);
				}
			};
		} catch (Exception e) {
			System.out.println("Rev: Unable to open output panel.");
			e.printStackTrace();
		}

		Composite inputView = new Composite(revPane, SWT.NONE);
		inputView.setLayout(new FormLayout());
		
		ToolBar revTools = new ToolBar(inputView, SWT.NONE);
		
		loadBtn = new ToolItem(revTools, SWT.PUSH);
		loadBtn.setToolTipText("Load");
		loadBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doLoad();
			}
		});
		
		saveBtn = new ToolItem(revTools, SWT.PUSH);
		saveBtn.setToolTipText("Save as");
		saveBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doSaveAs();
			}
		});
		
		stopBtn = new ToolItem(revTools, SWT.PUSH);
		stopBtn.setToolTipText("Cancel running query.");
		stopBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				outputView.notifyStop();
			}
		});
		stopBtn.setEnabled(false);
		
		modelLabel = new Label(inputView, SWT.NONE);
		modelLabel.setAlignment(SWT.CENTER);
		
		ScrolledComposite scrollPanel = new ScrolledComposite(inputView, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

		FormData fd_revTools = new FormData();
		fd_revTools.left = new FormAttachment(0);
		fd_revTools.top = new FormAttachment(0);
		fd_revTools.right = new FormAttachment(100);
		revTools.setLayoutData(fd_revTools);

		FormData fd_modelLabel = new FormData();
		fd_modelLabel.left = new FormAttachment(0);
		fd_modelLabel.top = new FormAttachment(0);
		fd_modelLabel.right = new FormAttachment(100);
		fd_modelLabel.bottom = new FormAttachment(scrollPanel);
		modelLabel.setLayoutData(fd_modelLabel);
		
		FormData fd_scrollPanel = new FormData();
		fd_scrollPanel.left = new FormAttachment(0);
		fd_scrollPanel.top = new FormAttachment(revTools);
		fd_scrollPanel.right = new FormAttachment(100);
		fd_scrollPanel.bottom = new FormAttachment(100);
		scrollPanel.setLayoutData(fd_scrollPanel);

		model = new Model(this, scratchModelName, scrollPanel);
		model.setSize(10000, 10000);
		
		scrollPanel.setContent(model);
		scrollPanel.setExpandHorizontal(true);
		scrollPanel.setExpandVertical(true);
		scrollPanel.setMinSize(model.getSize());

		revPane.setWeights(new int[] {1, 1});

		setupIcons();
		
		preferenceChangeListener = new PreferenceChangeAdapter("Rev") {
			@Override
			public void preferenceChange(PreferenceChangeEvent evt) {
				setupIcons();
			}
		};
		Preferences.addPreferenceChangeListener(PreferencePageGeneral.LARGE_ICONS, preferenceChangeListener);

		loadModel();
	}

	protected void doSaveAs() {
		String oldName = model.getModelName();
		SaveQueryAsDialog saveAs = new SaveQueryAsDialog(getShell(), oldName);
		if (saveAs.open() == Dialog.OK) {
			String newName = saveAs.getName();
			if (newName.trim().length() == 0)
				return;
			if (oldName.equals(newName)) {
				MessageDialog.openInformation(getShell(), "No need to save", "No need to save.  Changes are automatically saved while you edit.");
				return;
			}
			if (database.modelExists(newName)) {
				if (!MessageDialog.openConfirm(getShell(), "Overwrite?", "A query named '" + newName + "' already exists.  Overwrite it?"))
					return;
			}
			if (saveAs.keepOriginal())
				database.modelCopyTo(oldName, newName);
			else
				database.modelRename(oldName, newName);
			model.setModelName(newName);			
			modelLabel.setText(model.getModelName());
		}
	}

	protected void doLoad() {
		LoadQueryDialog load = new LoadQueryDialog(getShell(), database.getModels());
		if (load.open() == Dialog.OK && load.getSelectedItem() != null && load.getSelectedItem().trim().length() > 0) {
			model.setModelName(load.getSelectedItem());
			loadModel();
		}
	}

	private void setupIcons() {
		loadBtn.setImage(IconLoader.loadIcon("loadIcon"));
		saveBtn.setImage(IconLoader.loadIcon("saveIcon"));
		stopBtn.setImage(IconLoader.loadIcon("stopIcon"));
	}

	public CmdPanelOutput getCmdPanelOutput() {
		return outputView;
	}
	
	public DatabaseAbstractionLayer getDatabase() {
		return database;
	}
	
	public long getUniqueNumber() {
		return database.getUniqueNumber();
	}
	
	public void refresh() {
		loadModel();
	}

	public Model getModel() {
		return model;
	}

	public Connection getConnection() {
		return connection;
	}

	private void loadModel() {
		model.clear();
		
		modelLabel.setText(model.getModelName());
		
		if (getMenu() != null)
			getMenu().dispose();
	
		Menu menuBar = new Menu(getShell(), SWT.POP_UP);
		model.setMenu(menuBar);

		// Custom relvars
		MenuItem customRelvarsItem = new MenuItem(menuBar, SWT.CASCADE);
		customRelvarsItem.setText("Variables");
		customRelvarsItem.setMenu(obtainRelvarsMenu(menuBar, false));
		
		// System relvars
		MenuItem systemRelvarsItem = new MenuItem(menuBar, SWT.CASCADE);
		systemRelvarsItem.setText("System variables");
		systemRelvarsItem.setMenu(obtainRelvarsMenu(menuBar, true));
		
		// Operators
		OpSelector[] queryOperators = getOperators();
		MenuItem insOperatorsItem = new MenuItem(menuBar, SWT.CASCADE);
		insOperatorsItem.setText("Operators and constants");
		Menu insOperatorsMenu = new Menu(menuBar);
		for (int i=0; i < queryOperators.length; i++) {
			String opName = queryOperators[i].toString();
			if (opName == null)
				new MenuItem(insOperatorsMenu, SWT.SEPARATOR);
			else {
				MenuItem item = new MenuItem(insOperatorsMenu, SWT.PUSH);
				item.setText(opName);
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						Point lastMousePosition = model.getLastMousePosition();
						obtainOperatorForKind(opName, opName + getUniqueNumber(), lastMousePosition.x, lastMousePosition.y);
					}
				});
			}
		}
		insOperatorsItem.setMenu(insOperatorsMenu);
		
		// Refresh
		MenuItem refreshRev = new MenuItem(menuBar, SWT.PUSH);
		refreshRev.setText("Refresh");
		refreshRev.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				refresh();
			}
		});
		
		// Clear
		MenuItem clearRev = new MenuItem(menuBar, SWT.PUSH);
		clearRev.setText("Clear");
		clearRev.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (!MessageDialog.openConfirm(getShell(), "Rev", "Remove everything from this query?"))
					return;
				model.removeEverything();
			}
		});

		// load
		int version = hasRevExtensions();
		if (version < 0) {
			if (!installRevExtensions())
				MessageDialog.openError(getShell(), "Rev", "Unable to install Rev extensions.  Check the system log.");
			else
				refresh();
		} else if (version < DatabaseAbstractionLayer.EXPECTED_REV_VERSION) {
			upgrade(version);
		} else {
			presentRelvars();
			presentQueries();
			
			// Uninstall
			MenuItem uninstallRev = new MenuItem(menuBar, SWT.PUSH);
			uninstallRev.setText("Re-install Rev extensions");
			uninstallRev.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					uninstall();
				}
			});	
		}
	}
	
	private Menu obtainRelvarsMenu(Menu parent, boolean systemOnly) {
		Menu subMenu = new Menu(parent);
		Tuples tuples = database.getRelvars();
		if (tuples != null) {
			Iterator<Tuple> it = tuples.iterator();
			while (it.hasNext()) {
				Tuple tuple = it.next();
				if (tuple != null) {
					String owner = tuple.get("Owner").toString();
					String name = tuple.get("Name").toString();
					boolean isSystemVar = owner.equals("Rel") || name.startsWith("sys.rev");
					if (systemOnly != isSystemVar)
						continue;
					MenuItem item = new MenuItem(subMenu, SWT.PUSH);
					item.setText(name);
					SelectionListener listener = new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							Point lastMousePosition = model.getLastMousePosition();
							new Relvar(Rev.this, name + getUniqueNumber(), name, lastMousePosition.x, lastMousePosition.y);
						}
					};
					item.addSelectionListener(listener);
				}
			}
		}
		return subMenu;
	}
	
	private static interface OpSelectorRun {
		public Operator obtain(Rev rev, String name, int xpos, int ypos);
	}
	
	private static class OpSelector {
		private String menuTitle;
		private OpSelectorRun run;
		public OpSelector(String menuTitle, OpSelectorRun run) {
			this.menuTitle = menuTitle;
			this.run = run;
		}
		public OpSelector() {
			this.menuTitle = null;
			this.run = null;
		}
		public String toString() {
			return menuTitle;
		}
		public Operator getOperator(Rev rev, String name, int xpos, int ypos) {
			return run.obtain(rev, name, xpos, ypos);
		}
	}

	private OpSelector[] getOperators() {
		OpSelector[] operators = {
			new OpSelector("Project", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Project(rev, name, xpos, ypos);
			}}),
			new OpSelector("Restrict", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Restrict(rev, name, xpos, ypos);
			}}),
			new OpSelector("RENAME", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Rename(rev, name, xpos, ypos);
				}}),
			/*
			new OpSelector("DIVIDEBY", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new VisualiserOfOperatorDivideby(rev, name, xpos, ypos);
				}}),
			*/
			new OpSelector("JOIN", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Diadic(rev, name, "JOIN", xpos, ypos);
				}}),
			new OpSelector("COMPOSE", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Diadic(rev, name, "COMPOSE", xpos, ypos);
				}}),
			new OpSelector("MATCHING", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Diadic(rev, name, "MATCHING", xpos, ypos);
				}}),
			new OpSelector("NOT MATCHING", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Diadic(rev, name, "NOT MATCHING", xpos, ypos);
				}}),
			new OpSelector("TIMES", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Diadic(rev, name, "TIMES", xpos, ypos);
				}}),
			new OpSelector("UNION", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Diadic(rev, name, "UNION", xpos, ypos);
				}}),
			new OpSelector("INTERSECT", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Diadic(rev, name, "INTERSECT", xpos, ypos);
				}}),
			new OpSelector("MINUS", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Diadic(rev, name, "MINUS", xpos, ypos);
				}}),
			new OpSelector("ORDER", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Order(rev, name, xpos, ypos);
				}}),
			new OpSelector("GROUP", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new GroupOrWrap(rev, name, "GROUP", xpos, ypos);
				}}),
			new OpSelector("UNGROUP", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new UngroupOrUnwrap(rev, name, "UNGROUP", "RELATION", xpos, ypos);
				}}),
			new OpSelector("WRAP", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new GroupOrWrap(rev, name, "WRAP", xpos, ypos);
				}}),
			new OpSelector("UNWRAP", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new UngroupOrUnwrap(rev, name, "UNWRAP", "TUPLE", xpos, ypos);
				}}),
			new OpSelector("UPDATE", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Update(rev, name, xpos, ypos);
				}}),
			new OpSelector("EXTEND", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Extend(rev, name, xpos, ypos);
				}}),
			new OpSelector("SUMMARIZE", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new Summarize(rev, name, xpos, ypos);
				}}),
			new OpSelector(),
			new OpSelector("TABLE_DEE", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new TableDee(rev, name, xpos, ypos);
				}}),
			new OpSelector("TABLE_DUM", new OpSelectorRun() {
				public Operator obtain(Rev rev, String name, int xpos, int ypos) {
					return new TableDum(rev, name, xpos, ypos);
				}})
		};
		return operators;
	}
	
	private Operator obtainOperatorForKind(String kind, String name, int xpos, int ypos) {
		for (OpSelector selector: getOperators())
			if (selector.toString() != null && selector.toString().compareTo(kind) == 0)
				return selector.getOperator(Rev.this, name, xpos, ypos);
		System.out.println("Query kind '" + kind + "' not recognised.");
		return null;
	}
	
	private void presentRelvars() {
		int nextX = 10;
		int nextY = 10;
		Tuples tuples = database.getRelvars(model.getModelName());
		if (tuples == null)
			return;
		for (Tuple tuple: tuples) {
			int xpos = tuple.get("xpos").toInt();
			int ypos = tuple.get("ypos").toInt();
			// String modelName = tuple.get("model").toString();
			// Set up relvar position
			if (xpos == -1 && ypos == -1) {
				xpos = nextX;
				ypos = nextY;
			}
			// Create a new relvar
			Visualiser relvar = new Relvar(this, tuple.get("Name").toString(), tuple.get("relvarName").toString(), xpos, ypos);
			nextY += relvar.getBounds().height + 10;
		}
	}
	
	private void presentQueries() {
		HashMap<String, LinkedList<Parameter>> unresolved = new HashMap<String, LinkedList<Parameter>>();
		// Load in the regular query visualisers
		for (Tuple tuple: database.getQueries(model.getModelName())) {
			String name = tuple.get("Name").toString();
			int xpos = tuple.get("xpos").toInt();
			int ypos = tuple.get("ypos").toInt();
			String kind = tuple.get("kind").toString();
			// String modelName = tuple.get("model").toString();
			Operator operator = obtainOperatorForKind(kind, name, xpos, ypos);
			if (operator == null)
				continue;
			LinkedList<Parameter> unresolvedParms = unresolved.get(name);
			if (unresolvedParms != null) {
				unresolved.remove(name);
				for (Parameter parm: unresolvedParms) {
					parm.getArgument().setOperand(operator);
				}
			}
			for (Tuple connection: (Tuples)tuple.get("connections")) {
				int parameterNumber = connection.get("parameter").toInt();
				Parameter parameter = operator.getParameter(parameterNumber);
				String visualiserName = connection.get("Name").toString();
				Visualiser operand = model.getVisualiser(visualiserName);
				if (operand != null && !(operand instanceof Connector)) {
					parameter.getArgument().setOperand(operand);
				} else {
					LinkedList<Parameter> unresolvedParameters = unresolved.get(visualiserName);
					if (unresolvedParameters == null)
						unresolvedParameters = new LinkedList<Parameter>();
					unresolvedParameters.add(parameter);						
					unresolved.put(visualiserName, unresolvedParameters);
				}
			}
		}
	}
	
	// Return version number of Rev extensions.  Return -1 if not installed.
	private int hasRevExtensions() {
		return database.hasRevExtensions();
	}
	
	private boolean installRevExtensions() {
		boolean pass = database.installRevExtensions();
		if (pass)
			loadModel();
		return pass;
	}

	private boolean removeRevExtensions() {
		boolean pass = database.removeRevExtensions();
		if (pass)
			loadModel();
		return pass;
	}
	
	private void uninstall() {
		if (hasRevExtensions() < 0)
        	MessageDialog.openInformation(getShell(), "Rev", "Rev is not installed.");
		if (!MessageDialog.openConfirm(getShell(), "Rev", "Are you sure?  This will remove all Rev query definitions."))
			return;
		if (removeRevExtensions())
			refresh();
		else
			MessageDialog.openError(getShell(), "Rev", "Unable to remove Rev extensions.  You may have to remove them manually.");
	}
	
	private void upgrade(int currentVersionOfRevFromDatabase) {
		// Perform upgrade from currentVersionOfRevFromDatabase to EXPECTED_REV_VERSION
	}
	
}