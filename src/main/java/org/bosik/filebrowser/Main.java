package org.bosik.filebrowser;

import org.bosik.filebrowser.dataProvider.DataProvider;
import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.file.FSDataProvider;
import org.bosik.filebrowser.dataProvider.file.NodeFS;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

class TreeBrowser
{
	private DataProvider dataProvider;
	private Node         currentNode;
	private List<Node>   items;

	public TreeBrowser(DataProvider dataProvider)
	{
		this.dataProvider = dataProvider;
	}

	public Node getCurrentNode()
	{
		ensureInitialized();
		return currentNode;
	}

	public List<Node> getItems()
	{
		ensureInitialized();
		return items;
	}

	public void openRoot()
	{
		open(dataProvider.getRoot());
	}

	public void open(Node node)
	{
		currentNode = node;
		items = node.getChildren();
	}

	private void ensureInitialized()
	{
		if (currentNode == null)
		{
			openRoot();
		}
	}
}

/**
 * @author Nikita Bosik
 * @since 2017-09-02
 */
public class Main
{
	// CONSTANTS
	private static final String APP_TITLE    = "File Browser";
	private static final int    ICON_PADDING = 6;
	private static final int    BORDER_SMALL = 3;
	private static final int    BORDER_BIG   = 5;

	// SYSTEM
	private TreeBrowser browser;
	private final Desktop desktop = Desktop.getDesktop();

	// GUI
	private JPanel         ui;
	private JTree          tree;
	private JTable         table;
	private JProgressBar   progressBar;
	private JTextArea      previewText;
	private FileTableModel fileTableModel;
	private boolean cellSizesSet = false;

	// =========================================================================================

	private Container buildRootPanel()
	{
		if (ui == null)
		{
			ui = new JPanel(new BorderLayout(BORDER_SMALL, BORDER_SMALL));
			//ui.setBorder(new EmptyBorder(0, 0, 0, 0));

			table = buildTable();
			JScrollPane tableScroll = new JScrollPane(table);
			tableScroll.getViewport().setBackground(Color.WHITE);

			JPanel detailView = new JPanel(new BorderLayout(BORDER_SMALL, BORDER_SMALL));
			detailView.add(tableScroll, BorderLayout.CENTER);

			tree = buildTree();
			JScrollPane treeScroll = new JScrollPane(tree);
			Dimension preferredSize = treeScroll.getPreferredSize();
			treeScroll.setPreferredSize(new Dimension(200, (int) preferredSize.getHeight()));

			JPanel filePreview = new JPanel(new BorderLayout(BORDER_SMALL, BORDER_SMALL));
			filePreview.setMinimumSize(new Dimension(200, (int) filePreview.getMinimumSize().getHeight()));
			filePreview.setPreferredSize(new Dimension(200, (int) filePreview.getPreferredSize().getHeight()));

			previewText = new JTextArea();
			previewText.setLineWrap(true);
			previewText.setEditable(false);
			previewText.setVisible(false);
			filePreview.add(previewText);

			detailView.add(filePreview, BorderLayout.LINE_END);

			JPanel panelStatus = new JPanel(new BorderLayout(BORDER_SMALL, BORDER_SMALL));
			panelStatus.setBorder(new BevelBorder(BevelBorder.LOWERED));
			progressBar = new JProgressBar();
			panelStatus.add(progressBar, BorderLayout.EAST);
			panelStatus.setPreferredSize(new Dimension(ui.getWidth(), 20));
			//			progressBar.setVisible(false);

			ui.add(panelStatus, BorderLayout.PAGE_END);

			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailView);
			ui.add(splitPane, BorderLayout.CENTER);
		}

		return ui;
	}

	private JTree buildTree()
	{
		final JTree tree = new JTree(new DefaultMutableTreeNode(null));

		tree.setRootVisible(false);
		tree.setVisibleRowCount(15);
		tree.setCellRenderer(new DefaultTreeCellRenderer()
		{
			private FileSystemView fileSystemView = FileSystemView.getFileSystemView();

			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
					int row, boolean hasFocus)
			{
				DefaultMutableTreeNode item = (DefaultMutableTreeNode) value;
				Node node = (Node) item.getUserObject();

				JLabel label = new JLabel();
				label.setOpaque(true);

				if (node != null)
				{
					label.setText(node.getName());

					if (node instanceof NodeFS)
					{
						NodeFS nodeFS = (NodeFS) node;
						if (nodeFS.getFile() != null)
						{
							label.setIcon(fileSystemView.getSystemIcon(nodeFS.getFile()));
						}
					}
				}
				else
				{
					label.setText("(root)");
				}

				if (selected)
				{
					label.setBackground(backgroundSelectionColor);
					label.setForeground(textSelectionColor);
				}
				else
				{
					label.setBackground(backgroundNonSelectionColor);
					label.setForeground(textNonSelectionColor);
				}

				return label;
			}
		});
		tree.addTreeWillExpandListener(new TreeWillExpandListener()
		{
			@Override
			public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException
			{
				DefaultMutableTreeNode item = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();

				// collect children and populate theirs children
				List<DefaultMutableTreeNode> nodes = new ArrayList<>();

				for (int i = 0; i < item.getChildCount(); i++)
				{
					nodes.add((DefaultMutableTreeNode) item.getChildAt(i));
				}

				refreshChildren(nodes);
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException
			{
			}
		});
		tree.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				JTree tree = (JTree) e.getSource();
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if (path != null)
				{
					DefaultMutableTreeNode item = (DefaultMutableTreeNode) path.getLastPathComponent();
					Node node = (Node) item.getUserObject();
					showFiles(node);
				}
			}
		});
		tree.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyChar() == KeyEvent.VK_ENTER)
				{
					TreePath path = tree.getSelectionPath();
					if (path != null)
					{
						DefaultMutableTreeNode item = (DefaultMutableTreeNode) path.getLastPathComponent();
						Node node = (Node) item.getUserObject();
						showFiles(node);
					}

					e.setKeyCode(0);
				}
			}
		});

		return tree;
	}

	private JTable buildTable()
	{
		final JTable table = new JTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(false);
		table.setBackground(Color.WHITE);
		//table.setAutoCreateRowSorter(true);

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent lse)
			{
				int row = table.getSelectionModel().getLeadSelectionIndex();
				FileTableModel model = (FileTableModel) table.getModel();

				if (row >= 0 && row < model.getRowCount())
				{
					showFileDetails(model.getNode(row));
				}
				else
				{
					hideFileDetails();
				}
			}
		});

		final String solve = "table-key-enter";
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, solve);
		table.getActionMap().put(solve, new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JTable table = (JTable) e.getSource();
				openTableItem(table.getSelectedRow());
			}
		});

		table.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				Point p = e.getPoint();
				if (e.getClickCount() == 2)
				{
					JTable table = (JTable) e.getSource();
					openTableItem(table.rowAtPoint(p));
				}
			}
		});
		return table;
	}

	private void openTableItem(int row)
	{
		if (row != -1)
		{
			Node item = browser.getItems().get(row);
			if (item.isLeaf())
			{
				try
				{
					if (item instanceof NodeFS)
					{
						desktop.open(((NodeFS) item).getFile());
					}
				}
				catch (IOException t)
				{
					handleError(t);
				}
			}
			else
			{
				showFiles(item);
				// TODO: update tree
			}
		}
	}

	private void showProgressBar()
	{
		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);
	}

	private void hideProgressBar()
	{
		progressBar.setIndeterminate(false);
		progressBar.setVisible(false);
	}

	private void showRootFile()
	{
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(browser.getCurrentNode());

		for (Node item : browser.getItems())
		{
			if (!item.isLeaf())
			{
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(item);

				for (Node subItem : item.getChildren())
				{
					if (!subItem.isLeaf())
					{
						child.add(new DefaultMutableTreeNode(subItem));
					}
				}

				rootNode.add(child);
			}
		}

		tree.setModel(new DefaultTreeModel(rootNode));
		tree.expandRow(0);
		tree.setSelectionInterval(0, 0);
	}

	private TreePath findTreePath(File find)
	{
		for (int i = 0; i < tree.getRowCount(); i++)
		{
			TreePath treePath = tree.getPathForRow(i);
			Object object = treePath.getLastPathComponent();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
			File nodeFile = (File) node.getUserObject();

			if (nodeFile == find)
			{
				return treePath;
			}
		}

		return null;
	}

	private void showErrorMessage(String title, String message)
	{
		JOptionPane.showMessageDialog(ui, message, title, JOptionPane.ERROR_MESSAGE);
	}

	private void handleError(Exception e)
	{
		e.printStackTrace();
		showErrorMessage("Error", e.getMessage());
		ui.repaint();
	}

	private void refreshChildren(final List<DefaultMutableTreeNode> nodes)
	{
		showProgressBar();

		new SwingWorker<Void, DefaultMutableTreeNode[]>()
		{
			@Override
			public Void doInBackground()
			{
				for (DefaultMutableTreeNode item : nodes)
				{
					final Node node = (Node) item.getUserObject();
					if (!node.isLeaf())
					{
						System.out.println("Building children for " + node.getName() + "...");
						item.removeAllChildren();

						for (Node childNode : node.getChildren())
						{
							if (!childNode.isLeaf())
							{
								System.out.println("\t" + childNode.getName());
								publish(new DefaultMutableTreeNode[] { item, new DefaultMutableTreeNode(childNode) });
							}
						}
					}
				}
				return null;
			}

			@Override
			protected void process(List<DefaultMutableTreeNode[]> children)
			{
				for (DefaultMutableTreeNode[] child : children)
				{
					child[0].add(child[1]);
				}
			}

			@Override
			protected void done()
			{
				hideProgressBar();
				tree.repaint();
				//									tree.setEnabled(true);
			}
		}.execute();
	}

	private void showFiles(final Node node)
	{
		// update icon
		// TODO
		//		List<Icon> images = new ArrayList<>();
		//		images.add(fileSystemView.getSystemIcon(browser.getCurrentNode().getFile()));
		//		JFrame frame = (JFrame) SwingUtilities.getRoot(ui);
		//		frame.setIconImage(images);

		// update title
		setTitle(node.getName());

		// load data
		//tree.setEnabled(false);
		showProgressBar();

		new SwingWorker<List<Node>, Void>()
		{
			@Override
			public List<Node> doInBackground()
			{
				browser.open(node);
				return browser.getItems();
			}

			@Override
			protected void done()
			{
				try
				{
					List<Node> data = get();

					if (fileTableModel == null)
					{
						fileTableModel = new FileTableModel();
						table.setModel(fileTableModel);
					}

					fileTableModel.setNodes(data);
					if (!cellSizesSet)
					{
						//						table.setRowSorter(new TableRowSorter<TableModel>(table.getModel())
						//						{
						//							@Override
						//							public boolean isSortable(int column)
						//							{
						//								return column > 1;
						//							}
						//						});

						int iconSize = 16;//IconSizeHolder.get(data.isEmpty() ? null : data.get(0).getFile());

						// size adjustment to better account for icons
						table.setRowHeight(iconSize + ICON_PADDING);

						setColumnWidth(0, iconSize + ICON_PADDING);
						//						setColumnWidth(3, 60);
						//						table.getColumnModel().getColumn(3).setMaxWidth(120);
						//						setColumnWidth(4, -1);
						//						setColumnWidth(5, -1);
						//						setColumnWidth(6, -1);
						//						setColumnWidth(7, -1);
						//						setColumnWidth(8, -1);
						//						setColumnWidth(9, -1);

						cellSizesSet = true;
					}
				}
				catch (InterruptedException | ExecutionException e)
				{
					e.printStackTrace();
				}
				finally
				{
					hideProgressBar();
					//tree.setEnabled(true);
				}
			}
		}.execute();
	}

	private void setColumnWidth(int column, int width)
	{
		// FIXME
		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		if (width < 0)
		{
			JLabel label = new JLabel((String) tableColumn.getHeaderValue());
			Dimension preferred = label.getPreferredSize();
			width = (int) preferred.getWidth() + 14;
		}
		tableColumn.setPreferredWidth(width);
		tableColumn.setMaxWidth(width);
		tableColumn.setMinWidth(width);
	}

	private void hideFileDetails()
	{
		previewText.setVisible(false);
	}

	private void showFileDetails(Node node)
	{
		if (node.isLeaf())
		{
			if (node instanceof NodeFS)
			{
				NodeFS nodeFS = (NodeFS) node;
				File file = nodeFS.getFile();
				previewText.setText(getPreviewText(file));
			}
		}
		else
		{
			previewText.setText("");
		}

		// TODO: image preview

		previewText.setVisible(true);
	}

	private static String getPreviewText(File file)
	{
		final int MAX_PREVIEW_SIZE = 256;

		try (FileInputStream fin = new FileInputStream(file); BufferedInputStream bin = new BufferedInputStream(fin))
		{
			int character;
			StringBuilder buf = new StringBuilder(MAX_PREVIEW_SIZE + 3);
			while ((character = bin.read()) != -1 && buf.length() < MAX_PREVIEW_SIZE)
			{
				buf.append((char) character);
			}

			if (character != -1)
			{
				buf.append("...");
			}

			return buf.toString();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return "Can't build preview";
		}
	}

	private void setTitle(String title)
	{
		JFrame f = (JFrame) ui.getTopLevelAncestor();
		if (f != null)
		{
			f.setTitle(title);
		}

		ui.repaint();
	}

	public static void main(String[] args) throws IOException
	{
		//		Path fromZip = Paths.get("C:\\Data\\Дистрибутивы\\jprofiler_windows-x64_10_0_3.zip");
		//		FileSystem zipFs = FileSystems.newFileSystem(fromZip, Main.class.getClassLoader());
		//
		//		for (Path root : zipFs.getRootDirectories())
		//		{
		//			Files.walkFileTree(root, new SimpleFileVisitor<Path>()
		//			{
		//				@Override
		//				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
		//				{
		//					System.out.println("\tVISIT: " + file);
		//					System.out.println("---> " + file);
		//					return FileVisitResult.CONTINUE;
		//				}
		//
		//				@Override
		//				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
		//				{
		//					System.out.println("PRE: " + dir);
		//
		//					if (dir.getNameCount() > 0)
		//					{
		//						System.out.println("---> " + dir);
		//						return FileVisitResult.SKIP_SUBTREE;
		//					}
		//					else
		//					{
		//						return FileVisitResult.CONTINUE;
		//					}
		//				}
		//			});
		//		}

		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				Main app = new Main();

				app.buildUi();
				app.initSystem();
				app.showRootFile();
			}
		});
	}

	private void buildUi()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		JFrame f = new JFrame(APP_TITLE);
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		f.setContentPane(buildRootPanel());

		//		try
		//		{
		//			URL urlBig = getClass().getResource("fb-icon-32x32.png");
		//			URL urlSmall = getClass().getResource("fb-icon-16x16.png");
		//			List<Image> images = new ArrayList<>();
		//			images.add(ImageIO.read(urlBig));
		//			images.add(ImageIO.read(urlSmall));
		//			f.setIconImages(images);
		//		}
		//		catch (Exception e)
		//		{
		//		}

		f.pack();
		f.setLocationByPlatform(true);
		f.setMinimumSize(f.getSize());
		f.setVisible(true);
	}

	private void initSystem()
	{
		// TODO: move to background thread
		browser = new TreeBrowser(new FSDataProvider());
	}
}

enum Column
{
	ICON(""), NAME("File"), SIZE("Size"), TIME_MODIFIED("Last Modified");

	private String caption;

	Column(String caption)
	{
		this.caption = caption;
	}

	public String getCaption()
	{
		return caption;
	}

	public static Column byIndex(int index)
	{
		return values()[index];
	}
}

class FileTableModel extends AbstractTableModel
{
	private static final Column[]         COLUMNS = { Column.ICON, Column.NAME, Column.SIZE, Column.TIME_MODIFIED };
	private final        SimpleDateFormat FORMAT  = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private List<Node> nodes;
	private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

	FileTableModel()
	{
		this(new ArrayList<Node>());
	}

	FileTableModel(List<Node> nodes)
	{
		this.nodes = nodes;
	}

	@Override
	public int getRowCount()
	{
		return nodes.size();
	}

	@Override
	public int getColumnCount()
	{
		return COLUMNS.length;
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		Node item = nodes.get(row);

		Column c = COLUMNS[column];
		switch (c)
		{
			case ICON:
			{
				File file;
				if (item instanceof NodeFS)
				{
					file = ((NodeFS) item).getFile();
				}
				else
				{
					file = new File(item.getName());
				}

				return fileSystemView.getSystemIcon(file);
			}
			case NAME:
			{
				return item.getName();
			}
			case SIZE:
			{
				if (item instanceof NodeFS)
				{
					File file = ((NodeFS) item).getFile();
					return file.isDirectory() ? "" : formatSize(file.length());
				}
				else
				{
					return "";
				}
			}
			case TIME_MODIFIED:
			{
				if (item instanceof NodeFS)
				{
					File file = ((NodeFS) item).getFile();
					return FORMAT.format(new Date(file.lastModified()));
				}
				else
				{
					return null;
				}
			}
			default:
			{
				throw new IllegalArgumentException("Unsupported column type: " + c.name());
			}
		}
	}

	private static String formatSize(long length)
	{
		final String[] NAMES = new String[] { "bytes", "KB", "MB", "GB", "TB" };
		int index = 0;
		while (length > 1024 && index < NAMES.length - 1)
		{
			length /= 1024;
			index++;
		}

		return String.format("%d %s", length, NAMES[index]);
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		Column c = COLUMNS[column];
		switch (c)
		{
			case ICON:
			{
				return ImageIcon.class;
			}

			default:
			{
				return String.class;
			}
		}
	}

	@Override
	public String getColumnName(int column)
	{
		return COLUMNS[column].getCaption();
	}

	public Node getNode(int row)
	{
		return nodes.get(row);
	}

	public void setNodes(List<Node> nodes)
	{
		this.nodes = nodes;
		fireTableDataChanged();
	}
}