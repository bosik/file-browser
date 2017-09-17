package org.bosik.filebrowser.gui;

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

import javax.imageio.ImageIO;
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
import java.awt.Image;
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
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	private final ExecutorService executorService = Executors.newCachedThreadPool();
	private Node currentNode;

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
				executorService.shutdownNow();
			}
		});

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
													showPreviewImage(previewFile);
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
				openTableItem(table.getSelectedRow());
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
					openTableItem(table.rowAtPoint(p));
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
					submitTask(() ->
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

		submitTask(() ->
		{
			// calculate children

			List<DefaultMutableTreeNode> children = new ArrayList<>();

			Node node = (Node) item.getUserObject();
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

	private Future<?> futureShowFiles;

	private void showFiles(String path)
	{
		showProgressBar();
		textAddress.setText(path != null ? path : "");
		panelTable.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		tableModel.setNodes(Collections.emptyList());

		//		if (futureShowFiles != null)
		//		{
		//			futureShowFiles.cancel(true);
		//		}

		futureShowFiles = executorService.submit(() ->
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
							this.setTitle(currentNode.getName() + APP_TITLE);
							tableModel.setNodes(finalOutput.children);
							setColumnWidth(table, 0, 16 + ICON_PADDING);
						}
						else
						{
							System.err.println("Path not found: " + path);
							this.showErrorMessage("Error", "Path not found: " + path);
							tableModel.setNodes(Collections.emptyList());
						}
					}
					finally
					{
						this.hideProgressBar();
						panelTable.setCursor(Cursor.getDefaultCursor());
					}
				});
			}
			catch (Exception e)
			{
				SwingUtilities.invokeLater(() ->
				{
					this.hideProgressBar();
					panelTable.setCursor(Cursor.getDefaultCursor());
					this.handleError(e);
				});
			}
		});
	}

	private void submitTask(Runnable task)
	{
		executorService.submit(() ->
		{
			try
			{
				task.run();
			}
			catch (Exception e)
			{
				SwingUtilities.invokeLater(() ->
				{
					handleError(e);
				});
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
		// FIXME: move away from EDT

		if (node.isLeaf())
		{
			if (node instanceof NodeFS)
			{
				NodeFS nodeFS = (NodeFS) node;
				previewFile = nodeFS.getFile();

				if (isImage(previewFile))
				{
					showPreviewImage(previewFile);
					hidePreviewText();
				}
				else
				{
					showPreviewText(previewFile);
					hidePreviewImage();
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
		previewText.setText(getPreviewText(file, MAX_PREVIEW_SIZE));
		panelPreviewText.setVisible(true);
		panelPreviewText.repaint();
	}

	private void hideFileDetails()
	{
		hidePreviewText();
		hidePreviewImage();
	}

	private void hidePreviewImage()
	{
		previewImage.setVisible(false);
	}

	private void hidePreviewText()
	{
		panelPreviewText.setVisible(false);
		panelPreviewText.repaint();
	}

	private static boolean isImage(File file)
	{
		if (file == null)
		{
			return false;
		}

		String name = file.toString();
		if (name == null || name.isEmpty())
		{
			return false;
		}

		name = name.toLowerCase();

		for (String ext : new String[] { "bmp", "gif", "jpeg", "jpg", "png", "tif" })
		{
			if (name.endsWith(ext))
			{
				return true;
			}
		}

		return false;
	}

	private static String getPreviewText(File file, final int maxPreviewSize)
	{
		try (FileInputStream fin = new FileInputStream(file); BufferedInputStream bin = new BufferedInputStream(fin))
		{
			int character;
			StringBuilder buf = new StringBuilder(maxPreviewSize + 3);
			while ((character = bin.read()) != -1 && buf.length() < maxPreviewSize)
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

	private void showPreviewImage(File file)
	{
		try
		{
			BufferedImage myPicture = ImageIO.read(file);

			if (myPicture != null)
			{
				final int MAX_WIDTH = panelPreview.getWidth();
				final int MAX_HEIGHT = panelPreview.getHeight();
				ImageIcon icon;

				if (myPicture.getWidth() > MAX_WIDTH || myPicture.getHeight() > MAX_HEIGHT)
				{
					double kx = (double) myPicture.getWidth() / MAX_WIDTH;
					double ky = (double) myPicture.getHeight() / MAX_HEIGHT;

					int resizedWidth;
					int resizedHeight;

					if (kx > ky)
					{
						resizedWidth = (int) (myPicture.getWidth() / kx);
						resizedHeight = (int) (myPicture.getHeight() / kx);
					}
					else
					{
						resizedWidth = (int) (myPicture.getWidth() / ky);
						resizedHeight = (int) (myPicture.getHeight() / ky);
					}

					icon = new ImageIcon(myPicture.getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_FAST));
				}
				else
				{
					icon = new ImageIcon(myPicture);
				}
				previewImage.setIcon(icon);
				previewImage.setVisible(true);
			}
			else
			{
				previewImage.setVisible(false);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
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