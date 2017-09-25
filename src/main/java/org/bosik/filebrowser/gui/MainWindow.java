package org.bosik.filebrowser.gui;

import org.bosik.filebrowser.core.Util;
import org.bosik.filebrowser.core.browser.TreeBrowser;
import org.bosik.filebrowser.core.browser.exceptions.PathException;
import org.bosik.filebrowser.core.browser.resolvers.PathResolver;
import org.bosik.filebrowser.core.browser.resolvers.ResolverFS;
import org.bosik.filebrowser.core.browser.resolvers.ResolverFTP;
import org.bosik.filebrowser.core.browser.resolvers.ResolverRoot;
import org.bosik.filebrowser.core.browser.resolvers.ResolverSpecialWindows;
import org.bosik.filebrowser.core.browser.resolvers.ResolverZip;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.file.NodeFS;
import org.bosik.taskEngine.core.MyExecutor;
import org.bosik.taskEngine.core.StrategyTemplate;
import org.bosik.taskEngine.core.Task;

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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

enum TaskType
{
	POPULATE_TREE, SHOW_FILES, OPEN_FILE, PREVIEW_IMAGE;
}

class PreviewTask
{
	private String fileName;
	private int    maxWidth;
	private int    maxHeight;

	public PreviewTask(String fileName, int maxWidth, int maxHeight)
	{
		this.fileName = fileName;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
	}

	public String getFileName()
	{
		return fileName;
	}

	public int getMaxWidth()
	{
		return maxWidth;
	}

	public int getMaxHeight()
	{
		return maxHeight;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		PreviewTask that = (PreviewTask) o;

		if (maxWidth != that.maxWidth)
			return false;
		if (maxHeight != that.maxHeight)
			return false;
		return fileName.equals(that.fileName);
	}

	@Override
	public int hashCode()
	{
		int result = fileName.hashCode();
		result = 31 * result + maxWidth;
		result = 31 * result + maxHeight;
		return result;
	}
}

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
	//private final ExecutorService              executorService = Executors.newCachedThreadPool();
	private final MyExecutor<TaskType, String> executorService2 = new MyExecutor<TaskType, String>((executor, task) ->
	{
		switch (task.getId())
		{
			case POPULATE_TREE:
			{
				return StrategyTemplate.<TaskType, String>singletonByIdAndParam().onBeforeSubmit(executor, task);
			}
			case SHOW_FILES:
			case PREVIEW_IMAGE:
			{
				return StrategyTemplate.<TaskType, String>overrideById().onBeforeSubmit(executor, task);
			}
			case OPEN_FILE:
			default:
			{
				return true;
			}
		}
	});
	private Node currentNode;

	private volatile PreviewTask previewTask     = null;
	private          Object      previewTaskLock = new Object();

	// GUI
	private JPanel       ui;
	private JButton      buttonUp;
	private JTextField   textAddress;
	private JTree        tree;
	private JTable       table;
	private JScrollPane  panelTable;
	private JProgressBar progressBar;
	private JPanel       panelPreview;
	private JLabel       previewImage;
	private JScrollPane  panelPreviewText;
	private JTextArea    previewText;
	private TableModel   tableModel;

	private File previewFile;

	// =========================================================================================

	public MainWindow()
	{
		super(APP_TITLE);
		buildUi();
		initSystem();
		showRootFile();
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

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setContentPane(buildRootPanel());

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent event)
			{
				executorService2.shutdownNow();
			}
		});

		new Thread()
		{
			{
				setDaemon(true);
			}

			@Override
			public void run()
			{
				PreviewTask lastTask = null;
				for (; ; )
				{
					synchronized (previewTaskLock)
					{
						try
						{
							previewTaskLock.wait(500);
						}
						catch (InterruptedException e)
						{
							System.err.println("Interrupted, something new is out there");
						}
					}

					PreviewTask taskBefore = previewTask;
					if (taskBefore != null)
					{
						if (!taskBefore.equals(lastTask) || previewImage.getIcon() == null)
						{
							System.out.println("Loading " + taskBefore.getFileName());
							ImageIcon preview = process(taskBefore);

							PreviewTask taskAfter = previewTask;

							if (taskBefore == taskAfter)
							{
								SwingUtilities.invokeLater(() ->
								{
									previewImageShow(preview);
								});
							}
							else if (taskAfter == null)
							{
								SwingUtilities.invokeLater(() ->
								{
									previewImageHide();
								});
							}
						}
					}
					else if (lastTask != null)
					{
						SwingUtilities.invokeLater(() ->
						{
							previewImageHide();
						});
					}

					lastTask = taskBefore;
				}
			}

			private ImageIcon process(PreviewTask t)
			{
				try
				{
					return Util.buildPreviewImage(t.getFileName(), t.getMaxWidth(), t.getMaxHeight());
				}
				catch (IOException e)
				{
					// TODO: handle
					e.printStackTrace();
				}
				catch (InterruptedException e)
				{
					// never occurs
					e.printStackTrace();
				}
				return null;
			}
		}.start();

		pack();
		setMinimumSize(getSize());
		setLocationByPlatform(true);
	}

	private void initSystem()
	{
		browser = new TreeBrowser(new ArrayList<PathResolver>()
		{
			{
				add(new ResolverRoot()); // must be first
				add(new ResolverFS());
				add(new ResolverFTP(new CredentialsProviderImpl(MainWindow.this)));
				add(new ResolverSpecialWindows());
				add(new ResolverZip());
			}
		});
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
								addActionListener(e ->
								{
									showFiles(textAddress.getText());
								});
							}
						}, BorderLayout.CENTER);

						add(buttonUp = new JButton("Up")
						{
							{
								addActionListener(e ->
								{
									up();
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

								// table
								setLeftComponent(panelTable = new JScrollPane()
								{
									{
										getViewport().setBackground(Color.WHITE);
										setViewportView(table = buildTable());
									}
								});

								// preview
								setRightComponent(panelPreview = new JPanel()
								{
									{
										setLayout(new BorderLayout(BORDER_SMALL, BORDER_SMALL));
										setMinimumSize(new Dimension(200, (int) getMinimumSize().getHeight()));
										setPreferredSize(new Dimension(200, (int) getPreferredSize().getHeight()));

										add(previewImage = new JLabel()
										{
											{
												setVisible(false);
												setHorizontalAlignment(SwingConstants.CENTER);
												setVerticalAlignment(SwingConstants.CENTER);
												setBackground(Color.WHITE);
											}
										}, BorderLayout.NORTH);

										add(panelPreviewText = new JScrollPane()
										{
											{
												setViewportView(previewText = new JTextArea()
												{
													{
														setLineWrap(true);
														setEditable(false);
													}
												});
											}
										}, BorderLayout.CENTER);

										addComponentListener(new ComponentAdapter()
										{
											public void componentResized(ComponentEvent e)
											{
												if (previewImage.isVisible())
												{
													showPreviewImage(previewFile.getAbsolutePath());
												}
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
				List<TreePath> returnedPaths = new ArrayList<>(paths.length);
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
				onTreePathChanged(tree.getPathForLocation(e.getX(), e.getY()));
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
					onTreePathChanged(tree.getSelectionPath());
				}
			}
		});

		return tree;
	}

	private void onTreePathChanged(TreePath path)
	{
		if (path != null)
		{
			DefaultMutableTreeNode item = (DefaultMutableTreeNode) path.getLastPathComponent();
			Node node = (Node) item.getUserObject();
			showFiles(node);
		}
	}

	private JTable buildTable()
	{
		final JTable table = new JTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(false);
		table.setBackground(Color.WHITE);
		table.setRowHeight(16 + ICON_PADDING);
		table.setAutoCreateRowSorter(true);

		tableModel = new TableModel();
		table.setModel(tableModel);

		table.getSelectionModel().addListSelectionListener(e ->
		{
			if (!e.getValueIsAdjusting())
			{
				int row = table.getSelectionModel().getLeadSelectionIndex();
				TableModel model = (TableModel) table.getModel();

				if (row >= 0 && row < model.getRowCount())
				{
					row = table.convertRowIndexToModel(row);
					showPreview(model.getNode(row));
				}
				else
				{
					hideFileDetails();
				}
			}
		});

		final String KEY_ACTION_OPEN = "table-key-enter";
		table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), KEY_ACTION_OPEN);
		table.getActionMap().put(KEY_ACTION_OPEN, new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JTable table = (JTable) e.getSource();
				int row = table.getSelectedRow();
				if (row != -1)
				{
					row = table.convertRowIndexToModel(row);
					openTableItem(row);
				}
			}
		});

		final String KEY_ACTION_REFRESH = "table-key-f5";
		AbstractAction actionRefresh = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				browser.resetCache(currentNode);
				showFiles(currentNode);
			}
		};
		table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), KEY_ACTION_REFRESH);
		table.getActionMap().put(KEY_ACTION_REFRESH, actionRefresh);
		buttonUp.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), KEY_ACTION_REFRESH);
		buttonUp.getActionMap().put(KEY_ACTION_REFRESH, actionRefresh);

		final String KEY_ACTION_UP = "table-key-up";
		AbstractAction actionUp = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				up();
			}
		};
		table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), KEY_ACTION_UP);
		table.getActionMap().put(KEY_ACTION_UP, actionUp);

		table.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				Point p = e.getPoint();
				if (e.getClickCount() == 2) // double click
				{
					JTable table = (JTable) e.getSource();
					int row = table.rowAtPoint(p);
					if (row != -1)
					{
						row = table.convertRowIndexToModel(row);
						openTableItem(row);
					}
				}
			}
		});
		table.addAncestorListener(new FocusAncestorListener());

		return table;
	}

	private void openTableItem(int row)
	{
		if (row != -1)
		{
			Node node = tableModel.getNode(row);
			if (node.isLeaf())
			{
				if (node instanceof NodeFS)
				{
					File file = ((NodeFS) node).getFile();

					executorService2.submit(new Task<TaskType, String>(TaskType.OPEN_FILE, file.getAbsolutePath())
					{
						@Override
						public void run()
						{
							try
							{
								Desktop.getDesktop().open(file);
							}
							catch (IOException e)
							{
								// TODO: handle
								e.printStackTrace();
							}
						}
					});
				}
			}
			else
			{
				showFiles(node);
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
		currentNode = NodeFS.getRootNode();
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(currentNode);
		tree.setModel(new DefaultTreeModel(rootNode));
		refreshChildren(rootNode, true);
	}

	private void refreshChildren(final DefaultMutableTreeNode item, final boolean root)
	{
		// add "Loading..." stub
		item.removeAllChildren();
		item.add(new DefaultMutableTreeNode(null));
		tree.repaint();

		final Node node = (Node) item.getUserObject();
		final String param = (node != null) ? node.getFullPath() : "";

		executorService2.submit(new Task<TaskType, String>(TaskType.POPULATE_TREE, param)
		{
			@Override
			public void run()
			{
				// calculate children

				List<DefaultMutableTreeNode> children = new ArrayList<>();

				if (node != null && !node.isLeaf())
				{
					for (Node childNode : browser.getChildren(node))
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
			}
		});
	}

	private void up()
	{
		String parentPath = currentNode.getParentPath();
		if (parentPath != null)
		{
			showFiles(parentPath);
		}
	}

	private void previewImageLoading(String fileName)
	{
		previewImage.setVisible(true);
		previewImage.setIcon(null);
		previewImage.setText("Loading " + fileName + "...");
	}

	private void previewImageShow(ImageIcon preview)
	{
		if (preview != null)
		{
			previewImage.setIcon(preview);
			previewImage.setVisible(true);
			previewImage.setText("");
		}
		else
		{
			previewImage.setVisible(false);
		}
	}

	private void previewImageHide()
	{
		previewImage.setVisible(false);
		submitPreviewTask(null);
	}

	private void submitPreviewTask(PreviewTask task)
	{
		if (task != null)
		{
			previewImageLoading(new File(task.getFileName()).getName());
			System.out.println("Submitted new task for preview " + task.getFileName());
		}
		else
		{
			previewImage.setVisible(false);
		}

		previewTask = task;
		synchronized (previewTaskLock)
		{
			previewTaskLock.notify();
		}
	}

	private void showFiles(final Node node)
	{
		// skip stub "Loading..." nodes
		if (node != null)
		{
			showFiles(node.getFullPath());
		}
	}

	class View
	{
		Node       node;
		List<Node> children;

		View(Node node, List<Node> children)
		{
			this.node = node;
			this.children = children;
		}
	}

	private void showFiles(String path)
	{
		showProgressBar();
		textAddress.setText(path != null ? path : "");
		panelTable.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		tableModel.setNodes(Collections.emptyList());

		executorService2.submit(new Task<TaskType, String>(TaskType.SHOW_FILES, path)
		{
			@Override
			public void run()
			{
				try
				{
					View output;

					try
					{
						Node node = browser.getNode(path);
						List<Node> nodes = browser.getChildren(node);
						output = new View(node, nodes);
					}
					catch (PathException e)
					{
						output = new View(null, Collections.emptyList());
					}

					final View finalOutput = output;

					SwingUtilities.invokeLater(() ->
					{
						try
						{
							if (finalOutput.node != null)
							{
								currentNode = finalOutput.node;
								MainWindow.this.setTitle(currentNode.getName() + APP_TITLE);
								tableModel.setNodes(finalOutput.children);
								setColumnWidth(table, 0, 16 + ICON_PADDING);
							}
							else
							{
								System.err.println("Path not found: " + path);
								MainWindow.this.showErrorMessage("Error", "Path not found: " + path);
								tableModel.setNodes(Collections.emptyList());
							}
						}
						finally
						{
							MainWindow.this.hideProgressBar();
							panelTable.setCursor(Cursor.getDefaultCursor());
						}
					});
				}
				catch (Exception e)
				{
					SwingUtilities.invokeLater(() ->
					{
						MainWindow.this.hideProgressBar();
						panelTable.setCursor(Cursor.getDefaultCursor());
						MainWindow.this.handleError(e);
					});
				}
			}
		});
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

	private void showPreview(Node node)
	{
		if (node.isLeaf())
		{
			if (node instanceof NodeFS)
			{
				NodeFS nodeFS = (NodeFS) node;
				previewFile = nodeFS.getFile();

				if (previewFile != null && Util.looksLikeImage(previewFile.getName()))
				{
					showPreviewImage(previewFile.getAbsolutePath());
					hidePreviewText();
				}
				else
				{
					showPreviewText(previewFile);
					previewImageHide();
				}
			}
			else
			{
				hideFileDetails();
			}
		}
		else
		{
			hideFileDetails();
		}
	}

	private void showPreviewText(File file)
	{
		final int MAX_PREVIEW_SIZE = 1024; // bytes
		previewText.setText(Util.buildPreviewText(file, MAX_PREVIEW_SIZE));
		panelPreviewText.setVisible(true);
		panelPreviewText.repaint();
	}

	private void hideFileDetails()
	{
		hidePreviewText();
		previewImageHide();
	}

	private void hidePreviewText()
	{
		panelPreviewText.setVisible(false);
		panelPreviewText.repaint();
	}

	private void showPreviewImage(String fileName)
	{
		submitPreviewTask(new PreviewTask(fileName, panelPreview.getWidth(), panelPreview.getHeight()));
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
}