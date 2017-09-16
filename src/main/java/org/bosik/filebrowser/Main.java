package org.bosik.filebrowser;

import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.file.FSDataProvider;
import org.bosik.filebrowser.dataProvider.file.NodeFS;

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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
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

class Backend
{
	private FSDataProvider dataProvider;

	private Node       currentNode;
	private List<Node> items;

	public void init()
	{
		dataProvider = new FSDataProvider();
		currentNode = dataProvider.getRoot();
		items = currentNode.getChildren();
	}

	public Node getCurrentNode()
	{
		return currentNode;
	}

	public List<Node> getItems()
	{
		return items;
	}

	public FSDataProvider getDataProvider()
	{
		return dataProvider;
	}

	public void open(Node node)
	{
		currentNode = node;
		items = node.getChildren();
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

	// SYSTEM
	private final Backend        system         = new Backend();
	private final Desktop        desktop        = Desktop.getDesktop();
	private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

	// GUI
	private JPanel         gui;
	private JTree          tree;
	private JTable         table;
	private JProgressBar   progressBar;
	private JTextArea      previewText;
	private FileTableModel fileTableModel;
	private boolean cellSizesSet = false;

	// =========================================================================================

	private Container getGui()
	{
		if (gui == null)
		{
			gui = new JPanel(new BorderLayout(3, 3));
			gui.setBorder(new EmptyBorder(5, 5, 5, 5));

			JPanel detailView = new JPanel(new BorderLayout(3, 3));

			table = new JTable();
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

					if (row > -1 && row < model.getRowCount())
					{
						showFileDetails(model.getNode(row));
					}
					else
					{
						hideFileDetails();
					}
				}
			});
			table.addMouseListener(new MouseAdapter()
			{
				public void mousePressed(MouseEvent me)
				{
					JTable table = (JTable) me.getSource();
					Point p = me.getPoint();
					int row = table.rowAtPoint(p); // TODO: check
					if (me.getClickCount() == 2 && row != -1)
					{
						Node item = system.getItems().get(row);
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
								showThrowable(t);
							}
						}
						else
						{
							showFiles(item);
							// TODO: update tree
						}
					}
				}
			});

			JScrollPane tableScroll = new JScrollPane(table);
			tableScroll.getViewport().setBackground(Color.WHITE);
			detailView.add(tableScroll, BorderLayout.CENTER);

			tree = new JTree(new DefaultMutableTreeNode(null));
			tree.setRootVisible(false);
			tree.addTreeWillExpandListener(new TreeWillExpandListener()
			{
				@Override
				public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException
				{
					DefaultMutableTreeNode item = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
					Node node = (Node) item.getUserObject();

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
			tree.addTreeSelectionListener(new TreeSelectionListener()
			{
				@Override
				public void valueChanged(TreeSelectionEvent event)
				{
					DefaultMutableTreeNode item = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
					Node node = (Node) item.getUserObject();
					showFiles(node);
				}
			});
			tree.setCellRenderer(new FileTreeCellRenderer());
			tree.setVisibleRowCount(15);

			JScrollPane treeScroll = new JScrollPane(tree);
			Dimension preferredSize = treeScroll.getPreferredSize();
			treeScroll.setPreferredSize(new Dimension(200, (int) preferredSize.getHeight()));

			JPanel filePreview = new JPanel(new BorderLayout(3, 3));
			filePreview.setPreferredSize(new Dimension(200, (int) filePreview.getPreferredSize().getHeight()));

			previewText = new JTextArea("123");
			previewText.setLineWrap(true);
			//			previewText.setEditable(false);
			filePreview.add(previewText);

			detailView.add(filePreview, BorderLayout.EAST);

			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailView);
			gui.add(splitPane, BorderLayout.CENTER);

			JPanel simpleOutput = new JPanel(new BorderLayout(3, 3));
			progressBar = new JProgressBar();
			simpleOutput.add(progressBar, BorderLayout.EAST);
			progressBar.setVisible(false);

			gui.add(simpleOutput, BorderLayout.SOUTH);
		}

		return gui;
	}

	private void showRootFile()
	{
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(system.getCurrentNode());

		for (Node item : system.getItems())
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

	private void showErrorMessage(String errorMessage, String errorTitle)
	{
		JOptionPane.showMessageDialog(gui, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
	}

	private void showThrowable(Throwable t)
	{
		t.printStackTrace();
		JOptionPane.showMessageDialog(gui, t.toString(), t.getMessage(), JOptionPane.ERROR_MESSAGE);
		gui.repaint();
	}

	private void refreshChildren(final List<DefaultMutableTreeNode> nodes)
	{
		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);

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
				progressBar.setIndeterminate(false);
				progressBar.setVisible(false);
				//					tree.setEnabled(true);
			}
		}.execute();
	}

	private void showFiles(final Node node)
	{
		// update icon
		// TODO
		//		List<Icon> images = new ArrayList<>();
		//		images.add(fileSystemView.getSystemIcon(system.getCurrentNode().getFile()));
		//		JFrame frame = (JFrame) SwingUtilities.getRoot(gui);
		//		frame.setIconImage(images);

		// update title
		setTitle(node.getName());

		// load data
		//tree.setEnabled(false);
		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);

		new SwingWorker<List<Node>, Void>()
		{
			@Override
			public List<Node> doInBackground()
			{
				system.open(node);
				return system.getItems();
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

					//table.getSelectionModel().removeListSelectionListener(listSelectionListener);
					fileTableModel.setNodes(data);
					//table.getSelectionModel().addListSelectionListener(listSelectionListener);

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
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				catch (ExecutionException e)
				{
					e.printStackTrace();
				}
				finally
				{
					progressBar.setIndeterminate(false);
					progressBar.setVisible(false);
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

		// TODO
		// setTitle(file.getParent());
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
		JFrame f = (JFrame) gui.getTopLevelAncestor();
		if (f != null)
		{
			f.setTitle(title);
		}

		gui.repaint();
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
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(getGui());

		try
		{
			//			URL urlBig = getClass().getResource("fb-icon-32x32.png");
			//			URL urlSmall = getClass().getResource("fb-icon-16x16.png");
			//			List<Image> images = new ArrayList<>();
			//			images.add(ImageIO.read(urlBig));
			//			images.add(ImageIO.read(urlSmall));
			//			f.setIconImages(images);
		}
		catch (Exception e)
		{
		}

		f.pack();
		f.setLocationByPlatform(true);
		f.setMinimumSize(f.getSize());
		f.setVisible(true);
	}

	private void initSystem()
	{
		system.init(); // TODO: move to background thread
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

/**
 * A TreeCellRenderer for a File.
 */
class FileTreeCellRenderer extends DefaultTreeCellRenderer
{
	private FileSystemView fileSystemView;
	//	private JLabel         label;

	FileTreeCellRenderer()
	{
		// FIXME
		fileSystemView = FileSystemView.getFileSystemView();
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus)
	{
		DefaultMutableTreeNode item = (DefaultMutableTreeNode) value;
		Node node = (Node) item.getUserObject();

		JLabel label;
		label = new JLabel();
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
}
