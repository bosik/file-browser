package org.bosik.filebrowser;

import org.bosik.filebrowser.dataProvider.FSDataProvider;
import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.file.NodeFS;
import org.bosik.filebrowser.dataProvider.ftp.CredentialsProviderImpl;
import org.bosik.filebrowser.dataProvider.ftp.NodeFtp;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Nikita Bosik
 * @since 2017-09-02
 */
public class MainWindow extends JFrame
{
	// CONSTANTS
	private static final String APP_TITLE    = " - File Browser";
	private static final int    ICON_PADDING = 6;
	private static final int    BORDER_SMALL = 3;
	private static final int    BORDER_BIG   = 6;

	// SYSTEM
	private TreeBrowser browser;
	private final Desktop         desktop         = Desktop.getDesktop();
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	// GUI
	private JPanel       ui;
	private JTextField   textAddress;
	private JTree        tree;
	private JTable       table;
	private JScrollPane  panelTable;
	private JProgressBar progressBar;
	private JTextArea    previewText;
	private TableModel   tableModel;
	private TreePath     currentPath;

	// =========================================================================================

	public MainWindow()
	{
		super(APP_TITLE);
		buildUi();
		initSystem();
		showRootFile();
	}

	private Container buildRootPanel()
	{
		return ui = new JPanel()
		{
			{
				setLayout(new BorderLayout(BORDER_SMALL, BORDER_SMALL));

				// address panel
				add(new JPanel()
				{
					{
						setLayout(new BorderLayout(BORDER_BIG, BORDER_BIG));
						setPreferredSize(new Dimension(getWidth(), 30));

						add(textAddress = new JTextField()
						{
							{
								//setLayout(new BorderLayout(BORDER_BIG, BORDER_BIG));
								addActionListener(e ->
								{
									String address = textAddress.getText();

									if (address.startsWith("ftp://"))
									{
										Node node = new NodeFtp(null, address, new CredentialsProviderImpl());
										showFiles(node);
									}
									else
									{
										//address = Paths.get(address).normalize().toString();
										//textAddress.setText(address);
									}
								});
							}
						}, BorderLayout.CENTER);

						add(new JButton("Up")
						{
							{
								addActionListener(e ->
								{
									// to skip technical root we check getParent() twice
									if (browser.getCurrentNode().getParent() != null
											&& browser.getCurrentNode().getParent().getFullPath() != null)
									{
										showFiles(browser.getCurrentNode().getParent());
									}
								});
							}
						}, BorderLayout.WEST);
					}
				}, BorderLayout.PAGE_START);

				// main panel
				add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
				{
					{
						setLeftComponent(new JScrollPane()
						{
							{
								setMinimumSize(new Dimension(200, (int) getMinimumSize().getHeight()));
								setPreferredSize(new Dimension(200, (int) getPreferredSize().getHeight()));
								setViewportView(tree = buildTree());
							}
						});
						setRightComponent(new JSplitPane(HORIZONTAL_SPLIT)
						{
							{
								setResizeWeight(1.0);

								setLeftComponent(panelTable = new JScrollPane()
								{
									{
										getViewport().setBackground(Color.WHITE);
										setViewportView(table = buildTable());
									}
								});
								setRightComponent(new JPanel()
								{
									{
										setLayout(new BorderLayout(BORDER_SMALL, BORDER_SMALL));
										setMinimumSize(new Dimension(200, (int) getMinimumSize().getHeight()));
										setPreferredSize(new Dimension(200, (int) getPreferredSize().getHeight()));

										add(previewText = new JTextArea()
										{
											{
												setLineWrap(true);
												setEditable(false);
												setVisible(false);
											}
										});
									}
								});
							}
						});
					}
				}, BorderLayout.CENTER);

				// status panel
				add(new JPanel()
				{
					{
						setLayout(new BorderLayout(BORDER_SMALL, BORDER_SMALL));
						setBorder(new BevelBorder(BevelBorder.LOWERED));
						setPreferredSize(new Dimension(getWidth(), 20));

						add(progressBar = new JProgressBar()
						{{
							setVisible(false);
						}}, BorderLayout.EAST);
					}
				}, BorderLayout.PAGE_END);
			}
		};
	}

	private JTree buildTree()
	{
		final JTree tree = new JTree(new DefaultMutableTreeNode(null));

		tree.setRootVisible(false);
		tree.setVisibleRowCount(15);
		tree.setCellRenderer(new DefaultTreeCellRenderer()
		{
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
					label.setIcon(node.getIcon());
				}
				else
				{
					label.setText("Loading...");
					label.setEnabled(false);
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
		tree.setSelectionModel(new DefaultTreeSelectionModel()
		{
			private boolean canPathBeAdded(TreePath treePath)
			{
				DefaultMutableTreeNode item = (DefaultMutableTreeNode) treePath.getLastPathComponent();
				return item.getUserObject() != null;
			}

			private TreePath[] getFilteredPaths(TreePath[] paths)
			{
				List<TreePath> returnedPaths = new ArrayList<TreePath>(paths.length);
				for (TreePath treePath : paths)
				{

					if (canPathBeAdded(treePath))
					{
						returnedPaths.add(treePath);
					}
					else
					{
						returnedPaths.add(treePath.getParentPath());
					}
				}
				return returnedPaths.toArray(new TreePath[returnedPaths.size()]);
			}

			@Override
			public void addSelectionPath(TreePath path)
			{
				if (canPathBeAdded(path))
				{
					super.addSelectionPath(path);
				}
				else
				{
					super.addSelectionPath(path.getParentPath());
				}
			}

			@Override
			public void setSelectionPath(TreePath path)
			{
				if (canPathBeAdded(path))
				{
					super.setSelectionPath(path);
				}
			}

			@Override
			public void setSelectionPaths(TreePath[] paths)
			{
				super.setSelectionPaths(getFilteredPaths(paths));
			}
		});
		tree.addTreeExpansionListener(new TreeExpansionListener()
		{
			@Override
			public void treeExpanded(TreeExpansionEvent event)
			{
				DefaultMutableTreeNode item = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
				refreshChildren(item, false);
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event)
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
					currentPath = tree.getSelectionPath();
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
					JTree tree = (JTree) e.getSource();
					TreePath path = tree.getSelectionPath();
					if (path != null)
					{
						currentPath = tree.getSelectionPath();
						DefaultMutableTreeNode item = (DefaultMutableTreeNode) path.getLastPathComponent();
						Node node = (Node) item.getUserObject();
						showFiles(node);
					}
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
		table.setRowHeight(16 + ICON_PADDING);

		tableModel = new TableModel();
		table.setModel(tableModel);

		table.getSelectionModel().addListSelectionListener(e ->
		{
			int row = table.getSelectionModel().getLeadSelectionIndex();
			TableModel model = (TableModel) table.getModel();

			if (row >= 0 && row < model.getRowCount())
			{
				showFileDetails(model.getNode(row)); // FIXME: move away from EDT
			}
			else
			{
				hideFileDetails();
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
				if (e.getClickCount() == 2) // double click
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
		// FIXME: move away from EDT

		if (row != -1)
		{
			Node node = browser.getItems().get(row);
			if (node.isLeaf())
			{
				try
				{
					if (node instanceof NodeFS)
					{
						desktop.open(((NodeFS) node).getFile());
					}
				}
				catch (IOException t)
				{
					handleError(t);
				}
			}
			else
			{
				showFiles(node);
				//				if (currentPath != null)
				//				{
				//					DefaultMutableTreeNode item = (DefaultMutableTreeNode) currentPath.getLastPathComponent();
				//					for (int i = 0; i < item.getChildCount(); i++)
				//					{
				//						DefaultMutableTreeNode child = (DefaultMutableTreeNode) item.getChildAt(i);
				//
				//						Node childNode = (Node) child.getUserObject();
				//
				//						if (childNode != null)
				//						{
				//							if (childNode.getFullPath().equals(node.getFullPath()))
				//							{
				//								currentPath = currentPath.pathByAddingChild(child);
				//								tree.expandPath(currentPath);
				//								tree.setSelectionPath(currentPath);
				//								tree.repaint();
				//								break;
				//							}
				//						}
				//						else
				//						{
				//							tree.expandPath(currentPath);
				//						}
				//					}
				//				}
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
		tree.setModel(new DefaultTreeModel(rootNode));
		refreshChildren(rootNode, true);
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

	private void refreshChildren(final DefaultMutableTreeNode item, final boolean root)
	{
		// add "Loading..." stub
		item.removeAllChildren();
		item.add(new DefaultMutableTreeNode(null));
		tree.repaint();

		executorService.submit(() ->
		{
			// calculate children

			List<DefaultMutableTreeNode> children = new ArrayList<>();

			Node node = (Node) item.getUserObject();
			if (node != null && !node.isLeaf())
			{
				for (Node childNode : node.getChildren())
				{
					if (!childNode.isLeaf())
					{
						DefaultMutableTreeNode e = new DefaultMutableTreeNode(childNode);
						e.add(new DefaultMutableTreeNode(null)); // "Loading..." stub
						children.add(e);
					}
				}
			}

			// remove stub & fill with actual data

			SwingUtilities.invokeLater(() ->
			{
				item.removeAllChildren();
				for (DefaultMutableTreeNode child : children)
				{
					item.add(child);
				}

				DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
				model.reload(item);

				if (root)
				{
					tree.setSelectionInterval(0, 0);
					tree.expandRow(0);

					if (children.size() > 0)
					{
						showFiles((Node) children.get(0).getUserObject());
					}
				}
			});
		});
	}

	private void showFiles(final Node node)
	{
		// skip stub "Loading..." nodes
		if (node == null)
		{
			return;
		}

		showProgressBar();
		textAddress.setText(node.getFullPath());
		setTitle(node.getName() + APP_TITLE);

		panelTable.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		tableModel.setNodes(Collections.emptyList());

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
					tableModel.setNodes(get());
					setColumnWidth(table, 0, 16 + ICON_PADDING);
				}
				catch (InterruptedException | ExecutionException e)
				{
					e.printStackTrace();
				}
				finally
				{
					hideProgressBar();
					panelTable.setCursor(Cursor.getDefaultCursor());
				}
			}
		}.execute();
	}

	private static void setColumnWidth(JTable table, int column, int width)
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

	private void buildUi()
	{
		try
		{
			// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setContentPane(buildRootPanel());

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent event)
			{
				executorService.shutdownNow();
			}
		});
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

		pack();
		setMinimumSize(getSize());
		setLocationByPlatform(true);
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
}

class TableModel extends AbstractTableModel
{
	private static final Column[]         COLUMNS = { Column.ICON, Column.NAME, Column.SIZE, Column.TIME_MODIFIED };
	private final        SimpleDateFormat FORMAT  = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private List<Node> nodes;

	TableModel()
	{
		this(new ArrayList<Node>());
	}

	TableModel(List<Node> nodes)
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
				return item.getIcon();
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
		switch (COLUMNS[column])
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