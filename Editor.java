/*
 * Translation editor for Java properties files.
 *
 * Copyright (c) 2002 by Stephen Ostermiller
 * http://ostermiller.org/contact.pl?regarding=Attesoro
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by  the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.TXT.  If not, write to
 * the Free Software Foundation Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307 USA, or visit http://www.gnu.org/
 */

package com.Ostermiller.attesoro;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;
import com.Ostermiller.util.UberProperties;
import com.Ostermiller.util.StringHelper;
import com.Ostermiller.util.Browser;

public class Editor {

	private static String version = "1.3";
	private static ResourceBundle labels = ResourceBundle.getBundle("com.Ostermiller.attesoro.Editor",  Locale.getDefault());
	private static UberProperties props = new UberProperties();
	private static String[] userFile = new String[] {".java", "com","Ostermiller","attesoro","Editor.ini"};
	private static String defaultFile = "com/Ostermiller/attesoro/Editor.ini";
	private JFrame frame = new JFrame(Editor.labels.getString("title"));
	private JCheckBox useDefaultComment = new JCheckBox(Editor.labels.getString("use_none"));
	private JTextArea defaultCommentArea = new JTextArea();
	private JTextArea commentArea = new JTextArea();
	private JCheckBox useDefaultText = new JCheckBox(Editor.labels.getString("use_default"));
	private JTextArea defaultTextArea = new JTextArea();
	private JTextArea translationArea = new JTextArea();
	private JSplitPane textSplitPane, editingSplitPane, navigationSplitPane;
	private JComboBox languageBox, countryBox, variantBox;
	private JPanel newLocalePanel;
	private boolean modifying = false;
	private boolean globalModified = false;
	private DefaultMutableTreeNode workingNode = null;
	private String workingName = null;
	private int workingIndex = -1;
	private ArrayList names;
	private JTree tree;
	private DefaultTreeModel treeModel;
	private DefaultMutableTreeNode top;
	private JTable table;
	private JLabel defaultCommentLabel, commentLabel, translationLabel,
			defaultTranslationLabel;
	private JMenuItem revertKeyMenuItem, revertKeyPopupMenuItem, addKeyMenuItem,
			addKeyPopupMenuItem, addLangPopupMenuItem, addLangMenuItem,
			renameKeyMenuItem, renameKeyPopupMenuItem, deleteKeyMenuItem,
			deleteKeyPopupMenuItem;
	private JPopupMenu tablePopup, treePopup;
	private File openFile;
	private JMenuItem saveMenuItem;

	private final static Pattern ALREADY_TRANSLATED_FILE = Pattern.compile("^.*_(?:ar|be|bg|ca|cs|da|de|el|en|es|et|fi|fr|hi|hr|hu|is|it|iw|ja|ko|la|lt|lv|mk|nl|no|pl|pt|ro|ru|sh|sk|sl|sq|sr|sv|th|tr|uk|zh)(?:(?:_(?:AE|AL|AR|AT|AU|BE|BG|BH|BO|BR|BY|CA|CH|CL|CN|CO|Co|CR|CZ|DE|DK|DO|DZ|EC|EE|EG|ES|FI|FR|GB|GR|GT|HK|HN|HR|HU|IE|IL|IN|IQ|IS|IT|JO|JP|KR|KW|LB|LT|LU|LV|LY|MA|MK|MX|NI|NL|NO|NY|NZ|OM|PA|PE|PL|PR|PT|PY|QA|RO|RU|SA|SD|SE|SI|SK|SV|SY|TH|TN|TR|TW|UA|US|UY|VE|YE|YU|ZA))(?:_(?:[A-Za-z0-9]+))?)?\\.properties$");

	private Name getName(int ind){
		return (Name)names.get(ind);
	}

	private void addKey(String name){
		saveTextAreas();
		Name n = new Name(name);
		n.isModified = true;
		names.add(n);
		Collections.sort(names);
		TranslationData data = (TranslationData)top.getUserObject();
		data.properties.setProperty(name, "");
		data.addremove = true;
		table.revalidate();
		table.repaint(table.getBounds());
		int rowNum = names.indexOf(n);
		table.setRowSelectionInterval(rowNum, rowNum);
		table.scrollRectToVisible(table.getCellRect(rowNum, 0, true));
		globalModified = true;
	}

	private void renameKey(String oldName, String newName){
		saveTextAreas();
		Name n = getName(workingIndex);
		n.name = newName;
		n.isModified = true;
		for (Enumeration nodeList = top.depthFirstEnumeration(); nodeList.hasMoreElements();){
			TranslationData data = (TranslationData)(((DefaultMutableTreeNode)nodeList.nextElement()).getUserObject());
			data.properties.setProperty(newName, data.properties.getProperty(oldName), data.properties.getComment(oldName));
			data.properties.setProperty(oldName, null);
		}
		Collections.sort(names);
		int rowNum = names.indexOf(n);
		table.setRowSelectionInterval(rowNum, rowNum);
		table.scrollRectToVisible(table.getCellRect(rowNum, 0, true));
		globalModified = true;
	}

	private void deleteKey(String name){
		saveTextAreas();
		for (Enumeration nodeList = top.depthFirstEnumeration(); nodeList.hasMoreElements();){
			UberProperties p = ((TranslationData)(((DefaultMutableTreeNode)nodeList.nextElement()).getUserObject())).properties;
			p.setProperty(name, null);
		}
		workingName = null;
		names.remove(workingIndex);
		if (workingIndex >= names.size()){
			workingIndex--;
		}
		if (workingIndex != -1){
			table.setRowSelectionInterval(workingIndex, workingIndex);
			workingName = getName(workingIndex).name;
		}
		setTextAreas();
		table.revalidate();
		table.repaint(table.getBounds());
		globalModified = true;

	}

	private void saveTextAreas(){
		if (workingNode == null) return;
		if (workingName == null) return;
		TranslationData data = (TranslationData)workingNode.getUserObject();
		UberProperties saveProps = data.properties;
		String saveComment = null;
		if (!useDefaultComment.isSelected()){
			saveComment = commentArea.getText();
		}
		String saveTranslation = null;
		if (!useDefaultText.isSelected()){
			saveTranslation = translationArea.getText();
		}
		saveProps.setProperty(workingName, saveTranslation, saveComment);
		boolean wasModified = getName(workingIndex).isModified;
		boolean modified = isModified(workingName);
		getName(workingIndex).isDefault = isDefault(workingName);
		getName(workingIndex).isModified = modified;
		if (wasModified && !modified) data.modified--;
		if (!wasModified && modified) data.modified++;
		tree.repaint(tree.getBounds());
		table.repaint(table.getBounds());
	}

	private void fillNameData(){
		TranslationData data = null;
		if (workingNode != null) data = (TranslationData)workingNode.getUserObject();
		if (data != null) data.modified = 0;
		for (int i=0; i<names.size(); i++){
			getName(i).isDefault = isDefault(getName(i).name);
			getName(i).isModified = isModified(getName(i).name);
			if (data != null && getName(i).isModified) data.modified++;
		}
		if (tree != null) tree.repaint(tree.getBounds());
		if (table != null) table.repaint(table.getBounds());
	}

	private void showPopUpSmart(JPopupMenu popup, Component component, int x, int y){
		popup.show(component, x, y);
		boolean reshow = false;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension popupSize = popup.getSize();
		Point screenLocation = popup.getLocationOnScreen();
		x = screenLocation.x;
		y = screenLocation.y;
		if (screenLocation.x + popupSize.width > screenSize.width){
			x = screenSize.width - popupSize.width;
			reshow = true;
		}
		if (screenLocation.y + popupSize.height > screenSize.height){
			y = screenSize.height - popupSize.height;
			reshow = true;
		}
		if (reshow){
			popup.setLocation(x,y);
		}
	}

	private boolean isDefault(String name){
		if (workingNode == null) return false;
		TranslationData data = (TranslationData)workingNode.getUserObject();
		UberProperties saveProps = data.properties;
		UberProperties initProps = data.initProperties;
		return (
			saveProps.getProperty(name) == null &&
			saveProps.getComment(name) == null
		);
	}

	private boolean isModified(String name){
		if (workingNode == null) return false;
		TranslationData data = (TranslationData)workingNode.getUserObject();
		UberProperties saveProps = data.properties;
		UberProperties initProps = data.initProperties;
		return (
			(!stringsEqual(saveProps.getProperty(name), initProps.getProperty(name))) ||
			(!stringsEqual(saveProps.getComment(name), initProps.getComment(name)))
		);
	}

	private static boolean stringsEqual(String a, String b){
		if (a == null && b == null) return true;
		if (a == null) return false;
		if (b == null) return false;
		return(a.equals(b));
	}

	private void setTextAreas(){
		modifying = true;
		boolean enable = workingNode != null && workingName != null;
		boolean isTop = (workingNode == null || top == workingNode);

		defaultTranslationLabel.setEnabled(!isTop);
		defaultCommentLabel.setEnabled(!isTop);
		commentLabel.setText(Editor.labels.getString(isTop?"comment_label":"translated_comment_label"));
		translationLabel.setText(Editor.labels.getString(isTop?"text_label":"translation_label"));

		useDefaultText.setEnabled(!isTop && enable);
		String translation = getProperty();
		boolean useDefault = enable && (translation == null);
		useDefaultText.setSelected(!isTop && useDefault);
		translationArea.setText(((!enable) || (translation == null))?"":translation);
		translationArea.setEditable(enable);
		defaultCommentArea.setText(getDefaultComment());
		useDefaultComment.setText(Editor.labels.getString(isTop?"use_none":"use_default"));
		useDefaultComment.setEnabled(enable && !useDefault);
		String comment = getComment();
		useDefaultComment.setSelected(enable && (useDefault || (comment == null)));
		commentArea.setText((useDefault || (comment == null))?"":comment);
		commentArea.setEditable(enable && !useDefault);
		defaultTextArea.setText(getDefaultProperty());
		revertKeyMenuItem.setEnabled(enable);
		revertKeyPopupMenuItem.setEnabled(enable);
		addKeyMenuItem.setEnabled(enable);
		addKeyPopupMenuItem.setEnabled(enable);
		renameKeyMenuItem.setEnabled(enable);
		renameKeyPopupMenuItem.setEnabled(enable);
		deleteKeyMenuItem.setEnabled(enable);
		deleteKeyPopupMenuItem.setEnabled(enable);

		modifying = false;

	}

	private String getComment(){
		if (workingNode == null) return null;
		if (workingName == null) return null;
		return ((TranslationData)workingNode.getUserObject()).properties.getComment(workingName);
	}

	private String getProperty(){
		if (workingNode == null) return null;
		if (workingName == null) return null;
		return ((TranslationData)workingNode.getUserObject()).properties.getProperty(workingName);
	}

	private String getDefaultComment(){
		if (workingNode == null) return "";
		if (workingName == null) return "";
		DefaultMutableTreeNode tempNode = workingNode;
		while (!tempNode.isRoot()){
			tempNode = (DefaultMutableTreeNode)tempNode.getParent();
			String retval = ((TranslationData)tempNode.getUserObject()).properties.getComment(workingName);
			if (retval != null) return retval;
		}
		return "";
	}

	private String getDefaultProperty(){
		if (workingNode == null) return "";
		if (workingName == null) return "";
		DefaultMutableTreeNode tempNode = workingNode;
		while (!tempNode.isRoot()){
			tempNode = (DefaultMutableTreeNode)tempNode.getParent();
			String retval = ((TranslationData)tempNode.getUserObject()).properties.getProperty(workingName);
			if (retval != null) return retval;
		}
		return "";
	}

	private ArrayList getNames(String[] names){
		ArrayList fullNames = new ArrayList(names.length);
		for (int i=0; i<names.length; i++){
			fullNames.add(new Name(names[i]));
		}
		return fullNames;
	}

	private class Name implements Comparable {
		public String name;
		public boolean isDefault = false;
		public boolean isModified = false;

		public Name(String name){
			this.name = name;
		}

		public int compareTo(Object o){
			if (o == null) return -1;
			if (!(o instanceof Name)) return -1;
			return name.compareTo(((Name)o).name);
		}
		public String toString(){
			return name;
		}
	}

	private TableModel tableModel = new AbstractTableModel() {
		public int getColumnCount() {
			return 1;
		}
		public String getColumnName(int col) {
			return Editor.labels.getString("names");
		}
		public int getRowCount() {
			if (names == null) return 0;
			return names.size();
		}
		public Object getValueAt(int row, int col) {
			return getName(row);
		}
	};

	private static boolean isUpper(String s){
		for (int i=0; i<s.length(); i++){
			char c = s.charAt(i);
			if (c < 'A' || c > 'Z') return false;
		}
		return true;
	}

	private static boolean isLower(String s){
		for (int i=0; i<s.length(); i++){
			char c = s.charAt(i);
			if(c < 'a' || c > 'z') return false;
		}
		return true;
	}

	public void load(File f) throws IOException {
		if (f == null) throw new IOException();
		File parent = f.getCanonicalFile().getParentFile();
		if (parent == null) throw new IOException();
		String baseName = f.getName();
		if (baseName.endsWith(".properties")){
			baseName = baseName.substring(0, baseName.length() - 11);
		}
		final String finalBaseName = baseName;
		frame.setTitle(baseName + " - " + Editor.labels.getString("title"));
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		UberProperties props = new UberProperties();
		props.load(new FileInputStream(f));
		names = getNames(props.propertyNames());
		Collections.sort(names);
		top.removeAllChildren();
		top.setUserObject(new TranslationData(baseName, "", props));

		File[] transFiles = parent.listFiles(new FileFilter(){
			 public boolean accept(File pathname){
				String s = pathname.getName();
				if (!s.startsWith(finalBaseName + "_")) return false;
				if (!s.endsWith(".properties")) return false;
				String locale = s.substring(finalBaseName.length() + 1, s.length() -11);
				String[] localeData = StringHelper.split(locale, "_");
				if (localeData.length < 1) return false;
				if (localeData.length > 3) return false;
				if (localeData[0].length() != 2) return false;
				if (!isLower(localeData[0])) return false;
				if (localeData.length > 1){
					if (localeData[1].length() != 2) return false;
					if (!isUpper(localeData[1])) return false;
				}
				if (localeData.length > 2){
					if (localeData[2].length() == 0) return false;
					if (!isUpper(localeData[2])) return false;
				}
				return true;
			 }
		});
		Arrays.sort(transFiles);
		for (int i=0; i<transFiles.length; i++){
			String s = transFiles[i].getName();
			s = s.substring(finalBaseName.length() + 1, s.length() -11);
			String language = null;
			String country = null;
			String variant = null;
			StringTokenizer st = new StringTokenizer(s, "_");
			if (st.hasMoreTokens()) language = st.nextToken();
			if (st.hasMoreTokens()) country = st.nextToken();
			if (st.hasMoreTokens()) variant = st.nextToken();
			DefaultMutableTreeNode nextNode = createTreeNode(top, language, country, variant, new FileInputStream(transFiles[i]));
		}
		setTextAreas();
		fillNameData();
		workingNode = top;
		workingName = null;
		workingIndex = -1;
		addLangMenuItem.setEnabled(true);
		addLangPopupMenuItem.setEnabled(true);
		saveMenuItem.setEnabled(true);
		openFile = f.getAbsoluteFile();
		treeModel.reload();
		tree.expandPath(new TreePath(top));
		table.revalidate();
		table.repaint(table.getBounds());
		globalModified = false;
	}

	private DefaultMutableTreeNode createTreeNode(DefaultMutableTreeNode top, String language, String country, String variant, InputStream in) throws IOException {
		DefaultMutableTreeNode languageNode = null;
		String locale = language;
		for (Enumeration e = top.children() ; e.hasMoreElements() ;){
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
			TranslationData data = (TranslationData)node.getUserObject();
			if (data.locale.equals(locale)) languageNode = node;
		}
		if (languageNode == null){
			String nodeName;
			try {
				nodeName = Editor.labels.getString("language_" + language);
			} catch (MissingResourceException mre){
				nodeName = language;
			}
			UberProperties props = new UberProperties();
			if (country == null && in != null) props.load(in);
			languageNode = new DefaultMutableTreeNode(new TranslationData(nodeName, locale, props));
			top.add(languageNode);
		}
		if (country == null) return languageNode;
		DefaultMutableTreeNode countryNode = null;
		locale = language + "_" + country;
		for (Enumeration e = languageNode.children() ; e.hasMoreElements() ;){
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
			TranslationData data = (TranslationData)node.getUserObject();
			if (data.locale.equals(locale)) countryNode = node;
		}
		if (countryNode == null){
			String nodeName;
			try {
				nodeName = Editor.labels.getString("country_" + country);
			} catch (MissingResourceException mre){
				nodeName = country;
			}
			UberProperties props = new UberProperties();
			if (variant == null && in != null) props.load(in);
			countryNode = new DefaultMutableTreeNode(new TranslationData(nodeName, locale, props));
			languageNode.add(countryNode);
		}
		if (variant == null) return countryNode;
		DefaultMutableTreeNode variantNode = null;
		locale = language + "_" + country + "_" + variant;
		for (Enumeration e = countryNode.children() ; e.hasMoreElements() ;){
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
			TranslationData data = (TranslationData)node.getUserObject();
			if (data.locale.equals(locale)) variantNode = node;
		}
		if (variantNode == null){
			String nodeName;
			try {
				nodeName = Editor.labels.getString("variant_" + variant);
			} catch (MissingResourceException mre){
				nodeName = variant;
			}
			UberProperties props = new UberProperties();
			if (in != null) props.load(in);
			variantNode = new DefaultMutableTreeNode(new TranslationData(nodeName, locale, props));
			countryNode.add(variantNode);
		}
		return variantNode;
	}

	private class TranslationData {
		public String locale;
		public UberProperties properties;
		public UberProperties initProperties;
		public boolean addremove = false;
		public int modified = 0;
		public String name;
		TranslationData(String name, String locale, UberProperties properties){
			this.locale = locale;
			this.properties = properties;
			this.initProperties = new UberProperties(properties);
			this.name = name;
		}
		public String toString(){
			return name;
		}
	}

	public Editor() throws IOException {
		new Editor(null);
	}

	public Editor(File f) throws IOException {
		Browser.init();
		props.load(userFile, defaultFile);
		int x, y;
		try {
			x = Integer.parseInt(props.getProperty("window_size_x", "600"), 10);
			y = Integer.parseInt(props.getProperty("window_size_y", "440"), 10);
		} catch (NumberFormatException e){
			x = 600;
			y = 440;
		}
		frame.setSize(x,y);
		try {
			x = Integer.parseInt(props.getProperty("window_location_x", "50"), 10);
			y = Integer.parseInt(props.getProperty("window_location_y", "50"), 10);
		} catch (NumberFormatException e){
			x = 50;
			y = 50;
		}
		frame.setLocation(x,y);
		frame.setIconImage(
			new ImageIcon(
				ClassLoader.getSystemResource(
					"com/Ostermiller/attesoro/attesoro.png"
				)
			).getImage()
		);

		defaultCommentLabel = new JLabel(
			Editor.labels.getString("default_comment_label")
		);
		commentArea.setEditable(false);
		translationArea.setEditable(false);
		defaultCommentArea.setEditable(false);
		defaultCommentArea.setBackground(defaultCommentLabel.getBackground());
		defaultTextArea.setEditable(false);
		defaultTextArea.setBackground(defaultCommentLabel.getBackground());

		JPanel commentPane = new JPanel(new VerticalLayout());
		commentPane.add(defaultCommentLabel);
		commentPane.add(defaultCommentArea);
		BorderLayout commentLabelPanelLayout = new BorderLayout();
		commentLabelPanelLayout.setHgap(15);
		JPanel commentLabelPanel = new JPanel(commentLabelPanelLayout);
		commentLabel = new JLabel(Editor.labels.getString("comment_label"));
		commentLabelPanel.add(
			commentLabel,
			BorderLayout.WEST
		);
		commentLabelPanel.add(useDefaultComment,BorderLayout.CENTER);
		commentPane.add(commentLabelPanel);
		commentPane.add(commentArea);

		useDefaultComment.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (!modifying){
					modifying = true;
					commentArea.setText("");
					modifying = false;;
				}
			}
		});
		commentArea.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e){}
			public void insertUpdate(DocumentEvent e){
				if (!modifying){
					modifying = true;
					useDefaultComment.setSelected(false);
					if(!getName(workingIndex).isModified){
						getName(workingIndex).isModified = true;
						((TranslationData)workingNode.getUserObject()).modified++;
						tree.repaint(tree.getBounds());
						table.repaint(table.getBounds());
					}
					modifying = false;
				}
			}
			public void removeUpdate(DocumentEvent e){}
		});

		JPanel translationPane = new JPanel(new VerticalLayout());
		defaultTranslationLabel = new JLabel(Editor.labels.getString("default_translation_label"));
		translationPane.add(defaultTranslationLabel);
		translationPane.add(defaultTextArea);
		BorderLayout translationLabelPanelLayout = new BorderLayout();
		translationLabelPanelLayout.setHgap(15);
		JPanel translationLabelPanel = new JPanel(translationLabelPanelLayout);
		translationLabel = new JLabel(Editor.labels.getString("text_label"));
		translationLabelPanel.add(
			translationLabel,
			BorderLayout.WEST
		);
		translationLabelPanel.add(useDefaultText,BorderLayout.CENTER);
		translationPane.add(translationLabelPanel);
		translationPane.add(translationArea);

		useDefaultText.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (!modifying){
					modifying = true;
					translationArea.setText("");
					commentArea.setText("");
					boolean state = useDefaultText.isSelected();
					boolean isTop = (workingNode == null || top == workingNode);
					useDefaultComment.setEnabled(!isTop && !state);
					useDefaultComment.setSelected(!isTop);
					commentArea.setEditable(!state);
					modifying = false;;
				}
			}
		});
		translationArea.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e){}
			public void insertUpdate(DocumentEvent e){
				if (!modifying){
					modifying = true;
					useDefaultText.setSelected(false);
					if(!getName(workingIndex).isModified){
						getName(workingIndex).isModified = true;
						((TranslationData)workingNode.getUserObject()).modified++;
						tree.repaint(tree.getBounds());
						table.repaint(table.getBounds());
					}
					modifying = false;
				}
			}
			public void removeUpdate(DocumentEvent e){}
		});

		textSplitPane = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT,
			new JScrollPane(translationPane),
			new JScrollPane(commentPane)
		);
		top = new DefaultMutableTreeNode(new TranslationData("", "", new UberProperties()));
		treeModel = new DefaultTreeModel(top);
		tree = new JTree(treeModel);
		tree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				if (!tree.isRowSelected(selRow)){
					// select the row
					tree.setSelectionRow(selRow);
				}
				if(selRow != -1){
					if(e.isPopupTrigger() || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
						showPopUpSmart(treePopup, e.getComponent(), e.getX(), e.getY());
					}
				}
			}
		});
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				saveTextAreas();
				workingNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				setTextAreas();
				fillNameData();
			}
		});
		tree.setCellRenderer(new DefaultTreeCellRenderer(){
			public Component getTreeCellRendererComponent(
					JTree tree, Object value, boolean sel, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(
					tree, value, sel, expanded,
					leaf, row, hasFocus
				);
				TranslationData data = ((TranslationData)((DefaultMutableTreeNode)value).getUserObject());
				boolean modified = ((data.modified > 0) || data.addremove);
				setForeground(modified?Color.green.darker():Color.black);
				return this;
			}
		});
		table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = table.rowAtPoint(e.getPoint());
				if (!table.isRowSelected(selRow)){
					// select the row
					table.setRowSelectionInterval(selRow, selRow);
				}
				if(selRow != -1){
					if(e.isPopupTrigger() || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
						showPopUpSmart(tablePopup, e.getComponent(), e.getX(), e.getY());
					}
				}
			}
		});
		table.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e){
				if (e.getKeyCode() == KeyEvent.VK_DELETE){
					deleteKeyConfirm();
				}
			}
		});
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column){
				super.getTableCellRendererComponent(
					table, value, isSelected, hasFocus,  row, column
				);
				Color color = Color.black;
				Name name = (Name)value;
				if (name.isDefault) color = Color.blue.darker();
				if (name.isModified) color = Color.green.darker();
				setForeground(color);
				return this;
			}
		});

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				saveTextAreas();
				if (lsm.isSelectionEmpty()) {
					workingName = null;
					workingIndex = -1;
				} else {
					workingIndex = lsm.getMinSelectionIndex();
					workingName = getName(workingIndex).name;
				}
				setTextAreas();
			}
		});


		navigationSplitPane = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT,
			new JScrollPane(tree),
			new JScrollPane(table)
		);

		editingSplitPane = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			navigationSplitPane,
			textSplitPane
		);
		frame.getContentPane().add(editingSplitPane);

		int splitpaneLocation;

		splitpaneLocation = 100;
		try {
			splitpaneLocation = Integer.parseInt(props.getProperty("editing_split_pane_divider", ""), 10);
			if (splitpaneLocation <= 0){
				splitpaneLocation = 100;
			}
		} catch (NumberFormatException e){
		}
		editingSplitPane.setDividerLocation(splitpaneLocation);

		splitpaneLocation = 250;
		try {
			splitpaneLocation = Integer.parseInt(props.getProperty("text_split_pane_divider", ""), 10);
			if (splitpaneLocation <= 0){
				splitpaneLocation = 250;
			}
		} catch (NumberFormatException e){
		}
		textSplitPane.setDividerLocation(splitpaneLocation);

		splitpaneLocation = 100;
		try {
			splitpaneLocation = Integer.parseInt(props.getProperty("navigation_split_pane_divider", ""), 10);
			if (splitpaneLocation <= 0){
				splitpaneLocation = 100;
			}
		} catch (NumberFormatException e){
		}
		navigationSplitPane.setDividerLocation(splitpaneLocation);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exitRoutine();
			}
		});

		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu(Editor.labels.getString("file_menu_name"));
		fileMenu.setMnemonic(labels.getString("file_menu_key").charAt(0));
		fileMenu.getAccessibleContext().setAccessibleDescription(labels.getString("file_menu_description"));
		JMenuItem loadMenuItem = new JMenuItem(Editor.labels.getString("load_menu_name"));
		loadMenuItem.setMnemonic(labels.getString("load_menu_key").charAt(0));
		loadMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("load_menu_description"));
		loadMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		loadMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				JFileChooser chooser = new JFileChooser(props.getProperty("open_directory", System.getProperty("user.home")));
				chooser.setFileFilter(new javax.swing.filechooser.FileFilter(){
					public boolean accept(File f){
						if (f.isDirectory()) return true;
						String name = f.getName();
						if (!name.endsWith(".properties")) return false;
						Matcher alreadyTranslatedFileMatcher = ALREADY_TRANSLATED_FILE.matcher(name);
						if (alreadyTranslatedFileMatcher.matches()) return false;
						return true;
					}
					public String getDescription(){
						return (Editor.labels.getString("translation_files"));
					}
				});
				if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					props.setProperty("open_directory", chooser.getCurrentDirectory().getAbsolutePath());
					try {
						File f = chooser.getSelectedFile();
						load(f);
					} catch (IOException x){
						System.err.println(x.getMessage());
						JOptionPane.showMessageDialog(
							frame,
							Editor.labels.getString("error"),
							x.getMessage(),
							JOptionPane.ERROR_MESSAGE
						);
					}
				}
			}
		});
		fileMenu.add(loadMenuItem);
		saveMenuItem = new JMenuItem(Editor.labels.getString("save_menu_name"));
		saveMenuItem.setMnemonic(labels.getString("save_menu_key").charAt(0));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		saveMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("save_menu_description"));
		saveMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try {
					saveAll();
				} catch (IOException iox){
					System.err.println(iox.getMessage());
					JOptionPane.showMessageDialog(
						frame,
						Editor.labels.getString("error"),
						iox.getMessage(),
						JOptionPane.ERROR_MESSAGE
					);
				}
			}
		});
		saveMenuItem.setEnabled(false);
		fileMenu.add(saveMenuItem);
		menuBar.add(fileMenu);
		JMenu editMenu = new JMenu(Editor.labels.getString("edit_menu_name"));
		editMenu.setMnemonic(labels.getString("edit_menu_key").charAt(0));
		editMenu.getAccessibleContext().setAccessibleDescription(labels.getString("edit_menu_description"));
		JMenu keyMenu = new JMenu(Editor.labels.getString("key_menu_name"));
		keyMenu.setMnemonic(labels.getString("key_menu_key").charAt(0));
		keyMenu.getAccessibleContext().setAccessibleDescription(labels.getString("key_menu_description"));
		revertKeyMenuItem = new JMenuItem(Editor.labels.getString("revert_key_menu_name"));
		revertKeyMenuItem.setMnemonic(labels.getString("revert_key_menu_key").charAt(0));
		revertKeyMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("revert_key_menu_description"));
		revertKeyMenuItem.addActionListener(revertKeyActionListener);
		keyMenu.add(revertKeyMenuItem);
		addKeyMenuItem = new JMenuItem(Editor.labels.getString("add_key_menu_name"));
		addKeyMenuItem.setMnemonic(labels.getString("add_key_menu_key").charAt(0));
		addKeyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.CTRL_MASK));
		addKeyMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("add_key_menu_description"));
		addKeyMenuItem.addActionListener(addKeyActionListener);
		keyMenu.add(addKeyMenuItem);
		renameKeyMenuItem = new JMenuItem(Editor.labels.getString("rename_key_menu_name"));
		renameKeyMenuItem.setMnemonic(labels.getString("rename_key_menu_key").charAt(0));
		renameKeyMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("rename_key_menu_description"));
		renameKeyMenuItem.addActionListener(renameKeyActionListener);
		keyMenu.add(renameKeyMenuItem);
		deleteKeyMenuItem = new JMenuItem(Editor.labels.getString("delete_key_menu_name"));
		deleteKeyMenuItem.setMnemonic(labels.getString("delete_key_menu_key").charAt(0));
		deleteKeyMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("delete_key_menu_description"));
		deleteKeyMenuItem.addActionListener(deleteKeyActionListener);
		keyMenu.add(deleteKeyMenuItem);
		editMenu.add(keyMenu);
		JMenu langMenu = new JMenu(Editor.labels.getString("lang_menu_name"));
		langMenu.setMnemonic(labels.getString("lang_menu_key").charAt(0));
		langMenu.getAccessibleContext().setAccessibleDescription(labels.getString("lang_menu_description"));
		addLangMenuItem = new JMenuItem(Editor.labels.getString("add_lang_menu_name"));
		addLangMenuItem.setMnemonic(labels.getString("add_lang_menu_key").charAt(0));
		addLangMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
		addLangMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("add_lang_menu_description"));
		addLangMenuItem.addActionListener(addLangActionListener);
		addLangMenuItem.setEnabled(false);
		langMenu.add(addLangMenuItem);
		editMenu.add(langMenu);
		menuBar.add(editMenu);
		JMenu helpMenu = new JMenu(labels.getString("help_menu_name"));
		helpMenu.setMnemonic(labels.getString("help_menu_key").charAt(0));
		helpMenu.getAccessibleContext().setAccessibleDescription(labels.getString("help_menu_description"));
		JMenuItem websiteMenuItem = new JMenuItem(Editor.labels.getString("website_menu_name"));
		websiteMenuItem.setMnemonic(labels.getString("website_menu_key").charAt(0));
		websiteMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("website_menu_description"));
		websiteMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try {
						Browser.displayURL("http://ostermiller.org/attesoro/");
				} catch (IOException ioe){
						System.err.println(ioe.getMessage());
				}
			}
		});
		helpMenu.add(websiteMenuItem);
		JMenuItem aboutMenuItem = new JMenuItem(Editor.labels.getString("about_menu_name"));
		aboutMenuItem.setMnemonic(labels.getString("about_menu_key").charAt(0));
		aboutMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("about_menu_description"));
		aboutMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				JOptionPane.showMessageDialog(
					frame,
					MessageFormat.format(
						labels.getString("about_message"),
						new Object[] {
							version,
							"2002 Stephen Ostermiller http://ostermiller.org/contact.pl?regarding=Attesoro"
						}
					),
					labels.getString("about_title"),
					JOptionPane.INFORMATION_MESSAGE
				);
			}
		});
		helpMenu.add(aboutMenuItem);
		menuBar.add(helpMenu);
		frame.setJMenuBar(menuBar);

		tablePopup = new JPopupMenu();
		revertKeyPopupMenuItem = new JMenuItem(Editor.labels.getString("revert_key_menu_name"));
		revertKeyPopupMenuItem.setMnemonic(labels.getString("revert_key_menu_key").charAt(0));
		revertKeyPopupMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("revert_key_menu_description"));
		revertKeyPopupMenuItem.addActionListener(revertKeyActionListener);
		tablePopup.add(revertKeyPopupMenuItem);
		addKeyPopupMenuItem = new JMenuItem(Editor.labels.getString("add_key_menu_name"));
		addKeyPopupMenuItem.setMnemonic(labels.getString("add_key_menu_key").charAt(0));
		addKeyPopupMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("add_key_menu_description"));
		addKeyPopupMenuItem.addActionListener(addKeyActionListener);
		tablePopup.add(addKeyPopupMenuItem);
		renameKeyPopupMenuItem = new JMenuItem(Editor.labels.getString("rename_key_menu_name"));
		renameKeyPopupMenuItem.setMnemonic(labels.getString("rename_key_menu_key").charAt(0));
		renameKeyPopupMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("rename_key_menu_description"));
		renameKeyPopupMenuItem.addActionListener(renameKeyActionListener);
		tablePopup.add(renameKeyPopupMenuItem);
		deleteKeyPopupMenuItem = new JMenuItem(Editor.labels.getString("delete_key_menu_name"));
		deleteKeyPopupMenuItem.setMnemonic(labels.getString("delete_key_menu_key").charAt(0));
		deleteKeyPopupMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("delete_key_menu_description"));
		deleteKeyPopupMenuItem.addActionListener(deleteKeyActionListener);
		tablePopup.add(deleteKeyPopupMenuItem);

		treePopup = new JPopupMenu();
		addLangPopupMenuItem = new JMenuItem(Editor.labels.getString("add_lang_menu_name"));
		addLangPopupMenuItem.setMnemonic(labels.getString("add_lang_menu_key").charAt(0));
		addLangPopupMenuItem.getAccessibleContext().setAccessibleDescription(labels.getString("add_lang_menu_description"));
		addLangPopupMenuItem.addActionListener(addLangActionListener);
		addLangPopupMenuItem.setEnabled(false);
		treePopup.add(addLangPopupMenuItem);

		newLocalePanel = new JPanel(new GridLayout(6,1));
		ArrayList languages = new ArrayList();
		ArrayList countries = new ArrayList();
		ArrayList variants = new ArrayList();
		countries.add(new LocaleElement(labels.getString("all"), null));
		variants.add(new LocaleElement(labels.getString("all"), null));
		for (Enumeration e = labels.getKeys(); e.hasMoreElements();){
			String key = (String)e.nextElement();
			if (key.startsWith("language_")){
				languages.add(new LocaleElement(labels.getString(key), key.substring(9)));
			} else if (key.startsWith("country_")){
				countries.add(new LocaleElement(labels.getString(key), key.substring(8)));
			} else if (key.startsWith("variant_")){
				variants.add(new LocaleElement(labels.getString(key), key.substring(8)));
			}
		}
		Collections.sort(languages);
		Collections.sort(countries);
		Collections.sort(variants);
		languageBox = new JComboBox(languages.toArray(new LocaleElement[languages.size()]));
		newLocalePanel.add(new JLabel(labels.getString("language")), 0);
		newLocalePanel.add(languageBox, 1);
		countryBox = new JComboBox(countries.toArray(new LocaleElement[countries.size()]));
		newLocalePanel.add(new JLabel(labels.getString("country")), 2);
		newLocalePanel.add(countryBox, 3);
		variantBox = new JComboBox(variants.toArray(new LocaleElement[variants.size()]));
		newLocalePanel.add(new JLabel(labels.getString("variant")), 4);
		newLocalePanel.add(variantBox, 5);

		setTextAreas();
		if (f != null) load(f);
		frame.setVisible(true);
	}

	private class LocaleElement implements Comparable {
		public String name;
		public String code;
		public LocaleElement(String name, String code){
			this.name = name;
			this.code = code;
		}
		public String toString(){
			return (name + ((code == null)?"":(" (" + code + ")")));
		}
		public int compareTo(Object o){
			if (o instanceof LocaleElement){
				LocaleElement l = (LocaleElement)o;
				if (code == null) return -1;
				if (l.code == null) return 1;
				return name.compareTo(l.name);
			} else {
				return -1;
			}
		}
	}

	private boolean someModified(){
		if (globalModified) return true;
		for (Enumeration nodeList = top.depthFirstEnumeration(); nodeList.hasMoreElements();){
			TranslationData data = (TranslationData)((DefaultMutableTreeNode)nodeList.nextElement()).getUserObject();
			if (data.modified > 0) return true;
		}
		return false;
	}

	private void saveAll() throws IOException {
		saveTextAreas();
		String name = ((TranslationData)top.getUserObject()).name;
		File saveDir = openFile.getParentFile();
		for (Enumeration nodeList = top.depthFirstEnumeration(); nodeList.hasMoreElements();){
			saveNode((DefaultMutableTreeNode)nodeList.nextElement());
		}
		for (int i=0; i<names.size(); i++){
			Name n = getName(i);
			n.isModified = false;
		}
		globalModified = false;
		tree.repaint(tree.getBounds());
		table.repaint(table.getBounds());
	}

	private void saveNode(DefaultMutableTreeNode node) throws IOException{
		String name = ((TranslationData)top.getUserObject()).name;
		File saveDir = openFile.getParentFile();
		TranslationData data = (TranslationData)node.getUserObject();
		if (globalModified || data.modified > 0){
			data.properties.save(
				new FileOutputStream(
					new File(
						saveDir,
						name + ((node==top)?"":"_") + data.locale + ".properties"
					)
				)
			);
			data.initProperties = new UberProperties(data.properties);
			data.modified = 0;
		}
	}

	private ActionListener revertKeyActionListener = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			saveTextAreas();
			TranslationData data = (TranslationData)workingNode.getUserObject();
			data.properties.setProperty(workingName, data.initProperties.getProperty(workingName),data.initProperties.getComment(workingName));
			setTextAreas();
		}
	};

	private ActionListener addKeyActionListener = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			String inputValue = JOptionPane.showInputDialog(Editor.labels.getString("add_key_message"));
			TranslationData data = (TranslationData)top.getUserObject();
			if (inputValue != null){
				if (data.properties.contains(inputValue)){
					JOptionPane.showMessageDialog(
						frame,
						MessageFormat.format(
							Editor.labels.getString("key_exists_error"),
							new Object[] {
								inputValue
							}
						),
						Editor.labels.getString("error"),
						JOptionPane.ERROR_MESSAGE
					);
				} else {
					addKey(inputValue);
				}
			}
		}
	};

	private ActionListener renameKeyActionListener = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			String oldName = workingName;
			String inputValue = JOptionPane.showInputDialog(Editor.labels.getString("rename_key_message"));
			TranslationData data = (TranslationData)top.getUserObject();
			if (inputValue != null){
				if (data.properties.contains(inputValue)){
					JOptionPane.showMessageDialog(
						frame,
						MessageFormat.format(
							Editor.labels.getString("key_exists_error"),
							new Object[] {
								inputValue
							}
						),
						Editor.labels.getString("error"),
						JOptionPane.ERROR_MESSAGE
					);
				} else {
					renameKey(oldName, inputValue);
				}
			}
		}
	};

	private ActionListener deleteKeyActionListener = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			deleteKeyConfirm();
		}
	};

	private void deleteKeyConfirm(){
		if (workingName != null && 	workingIndex >= 0){
			int result = JOptionPane.showConfirmDialog(
				frame,
				Editor.labels.getString("delete_key_message"),
				Editor.labels.getString("delete_key_title"),
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE
			);
			if (result == JOptionPane.OK_OPTION ){
				deleteKey(workingName);
			}
		}
	}


	private ActionListener addLangActionListener = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			int result = JOptionPane.showConfirmDialog(
				frame,
				newLocalePanel,
				Editor.labels.getString("add_lang_title"),
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE
			);
			if (result == JOptionPane.OK_OPTION ){
				saveTextAreas();
				String language = ((LocaleElement)(languageBox.getSelectedItem())).code;
				String country = ((LocaleElement)(countryBox.getSelectedItem())).code;
				String variant = ((LocaleElement)(variantBox.getSelectedItem())).code;
				try {
					DefaultMutableTreeNode newNode = createTreeNode(top, language, country, variant, null);
					treeModel.reload();
					TreePath newPath = new TreePath(newNode);
					tree.setSelectionPath(newPath);
					tree.expandPath(newPath);
					tree.scrollPathToVisible(newPath);
					globalModified = true;
				} catch (IOException iox){
					// cannot happen, stream is null.
				}
			}
		}
	};

	private void exitRoutine() {
		boolean exit = true;
		if (someModified()){
			exit = false;
			Object[] options = {
				Editor.labels.getString("save"),
				Editor.labels.getString("exit"),
				Editor.labels.getString("cancel")
			};
			int result = JOptionPane.showOptionDialog(
				frame,
				Editor.labels.getString("save_first"),
				Editor.labels.getString("save_first_title"),
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[2]
			);
			if (result == 0){
				try {
					saveAll();
					exit = true;
				} catch (IOException iox){
					System.err.println(iox.getMessage());
					JOptionPane.showMessageDialog(
						frame,
						Editor.labels.getString("error"),
						iox.getMessage(),
						JOptionPane.ERROR_MESSAGE
					);
				}
			} else if (result == 1){
				exit = true;
			}
		}
		if (exit){
			Dimension d = frame.getSize();
			props.setProperty("window_size_x", ("" + d.width));
			props.setProperty("window_size_y", ("" + d.height));
			Point p = frame.getLocation();
			props.setProperty("window_location_x", ("" + p.x));
			props.setProperty("window_location_y", ("" + p.y));
			props.setProperty("text_split_pane_divider", ("" + textSplitPane.getDividerLocation()));
			props.setProperty("editing_split_pane_divider", ("" + editingSplitPane.getDividerLocation()));
			props.setProperty("navigation_split_pane_divider", ("" + navigationSplitPane.getDividerLocation()));

			try {
				props.save(userFile);
			} catch (IOException iox){
				System.err.println(iox.getMessage());
			}
			System.exit(0);
		} else {
			frame.setVisible(true);
		}
	}


	/**
	 * Main method.
	 */
	public static void main(String[] args) throws IOException {
		String load = null;
		if (args.length > 0) load = args[0];
		Editor e = new Editor((load == null)?null:new File(load));
	}

	private class VerticalLayout implements LayoutManager{
		/**
		 * Adds the specified component with the specified name
		 * to the layout.
		 */
		public void addLayoutComponent(String name, Component comp){
		}
		/**
		 * Lays out the container in the specified panel.
		 */
		public void layoutContainer(Container parent){
			int currentYPos = 0;
			int width = parent.getWidth();
			Insets insets = parent.getInsets();
			width -= insets.left + insets.right;
			int numberComponents = parent.getComponentCount();
			for (int i = 0; i < numberComponents; i++) {
				Component c = parent.getComponent(i);
				if (c.isVisible()) {
					Dimension d = c.getPreferredSize();
					d.width = width;
					if (i == numberComponents - 1 &&
							currentYPos + d.height + insets.top + insets.bottom < parent.getHeight()){
						d.height = parent.getHeight() - (currentYPos + insets.top + insets.bottom);
					}
					c.setBounds(0, currentYPos, d.width, d.height);
					currentYPos += d.height;
				}
			}
		}
		/**
		 * Calculates the minimum size dimensions for the specified
		 * panel given the components in the specified parent container.
		 */
		public Dimension minimumLayoutSize(Container parent){
			return preferredLayoutSize(parent);
		}
		/**
		 * Calculates the preferred size dimensions for the specified
		 * panel given the components in the specified parent container.
		 */
		public Dimension preferredLayoutSize(Container parent){
			Dimension preferredSize = new Dimension(0, 0);
			int numberComponents = parent.getComponentCount();
			for (int i = 0; i < numberComponents; i++) {
				Component c = parent.getComponent(i);
				if (c.isVisible()) {
					Dimension d = c.getPreferredSize();
					if (d.width > preferredSize.width) preferredSize.width = d.width;
					preferredSize.height += d.height;
				}
			}
			Insets insets = parent.getInsets();
			preferredSize.width += insets.left + insets.right;
			preferredSize.height += insets.top + insets.bottom;
			return preferredSize;
		}
		/**
		 * Removes the specified component from the layout.
		 */
		public void removeLayoutComponent(Component comp){
		}
	}
}
