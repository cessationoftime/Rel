package org.reldb.dbrowser.ui.content.filtersorter;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.reldb.rel.v0.values.StringUtils;

public class SearchQuick extends Composite {
	
	private Text findText;
	private boolean wholeWordSearch = false;
	private boolean caseSensitiveSearch = false;
	private boolean regexSearch = false;

	private void fireUpdateIfSearch(FilterSorter filterSorter) {
		if (findText.getText().trim().length() > 0)
			filterSorter.fireUpdate();
	}
	
	public SearchQuick(FilterSorter filterSorter, Composite contentPanel) {
		super(contentPanel, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);		
		
		findText = new Text(this, SWT.BORDER);
		findText.addListener(SWT.Traverse, event -> {
			if (event.detail == SWT.TRAVERSE_RETURN) {
				filterSorter.fireUpdate();
			}
		});
		findText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		ToolBar toolBar = new ToolBar(this, SWT.NONE);
		
		ToolItem wholeWord = new ToolItem(toolBar, SWT.PUSH);
		wholeWord.addListener(SWT.Selection, e -> {
			wholeWordSearch = !wholeWordSearch;
			wholeWord.setText(wholeWordSearch ? "Whole word" : "Any match");
			layout();
			fireUpdateIfSearch(filterSorter);
		});
		wholeWord.setText("Any match");

		ToolItem caseSensitive = new ToolItem(toolBar, SWT.PUSH);
		caseSensitive.addListener(SWT.Selection, e -> {
			caseSensitiveSearch = !caseSensitiveSearch;
			caseSensitive.setText(caseSensitiveSearch ? "Case sensitive" : "Case insensitive");
			layout();
			fireUpdateIfSearch(filterSorter);
		});
		caseSensitive.setText("Case insensitive");
		
		ToolItem regex = new ToolItem(toolBar, SWT.CHECK);
		regex.addListener(SWT.Selection, e -> {
			regexSearch = regex.getSelection();
			wholeWord.setEnabled(!regexSearch);
			caseSensitive.setEnabled(!regexSearch);
			fireUpdateIfSearch(filterSorter);
		});
		regex.setText("Regex");
		
		ToolItem clear = new ToolItem(toolBar, SWT.PUSH);
		clear.addListener(SWT.Selection, e -> {
			if (findText.getText().trim().length() == 0)
				return;
			findText.setText("");
			filterSorter.fireUpdate();
		});
		clear.setText("Clear");
		
		toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
	}
	
	public String getQuery() {
		String needle = findText.getText().trim();
		if (needle.length() == 0)
			return "";
		String regex;
		if (regexSearch)
			regex = needle;
		else {
			if (wholeWordSearch)
				regex = ".*\\b" + Pattern.quote(needle) + "\\b.*";
			else
				regex = ".*" + Pattern.quote(needle) + ".*";
			if (!caseSensitiveSearch)
				regex = "(?i)" + regex;
		}
		return "WHERE SEARCH(TUP {*}, \"" + StringUtils.quote(regex) + "\")";
	}

	public void setState(String state) {
		this.findText.setText(state);
	}

	public String getState() {
		return findText.getText();
	}
	
}
