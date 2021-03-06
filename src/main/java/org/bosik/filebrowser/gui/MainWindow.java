package org.bosik.filebrowser.gui;

import org.bosik.filebrowser.core.Util;
import org.bosik.filebrowser.core.browser.TreeBrowser;
import org.bosik.filebrowser.core.browser.exceptions.PathException;
import org.bosik.filebrowser.core.browser.resolvers.PathResolver;
import org.bosik.filebrowser.core.browser.resolvers.ResolverFS;
import org.bosik.filebrowser.core.browser.resolvers.ResolverFTP;
import org.bosik.filebrowser.core.browser.resolvers.ResolverRoot;
import org.bosik.filebrowser.core.browser.resolvers.ResolverZip;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.file.NodeFS;
import org.bosik.filebrowser.core.nodes.file.NodeFolder;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
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
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * @author Nikita Bosik
 * @since 2017-09-02
 */
public class MainWindow extends JFrame
{
    // CONSTANTS
    private static final String APP_TITLE = " - File Browser";
    private static final int ICON_PADDING = 6;
    private static final int BORDER_SMALL = 3;
    private static final int BORDER_BIG = 6;

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
            case EXPAND_PATH:
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
    private WatchService watcher;
    private WatchKey prevWatchKey;

    private volatile PreviewTask previewTask = null;
    private final Object previewTaskLock = new Object();

    // GUI
    private JPanel ui;
    private JButton buttonUp;
    private JTextField textAddress;
    private JTree tree;
    private JTable table;
    private JScrollPane panelTable;
    private JProgressBar progressBar;
    private JPanel panelPreview;
    private JLabel previewImage;
    private JScrollPane panelPreviewText;
    private JTextArea previewText;
    private TableModel tableModel;

    private File previewFile;

    // =========================================================================================
    // CORE & UI
    // =========================================================================================

    public MainWindow()
    {
        super(APP_TITLE);
        buildUi();
        initSystem();
        openRoot();
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

        // async image preview thread
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
                                SwingUtilities.invokeLater(() -> previewImageShow(preview));
                            }
                            else if (taskAfter == null)
                            {
                                SwingUtilities.invokeLater(() -> previewImageHide());
                            }
                        }
                    }
                    else if (lastTask != null)
                    {
                        SwingUtilities.invokeLater(() -> previewImageHide());
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

        try
        {
            watcher = FileSystems.getDefault().newWatchService();

            // watcher service thread
            new Thread()
            {
                {
                    setDaemon(true);
                }

                @Override
                public void run()
                {
                    for (; ; )
                    {
                        WatchKey key;
                        try
                        {
                            key = watcher.take();
                        }
                        catch (InterruptedException x)
                        {
                            System.out.println("Watcher service interrupted");
                            return;
                        }

                        for (WatchEvent<?> event : key.pollEvents())
                        {
                            WatchEvent.Kind<?> kind = event.kind();

                            if (kind != OVERFLOW)
                            {
                                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                                Path filename = ev.context();
                                System.out.println("Changed file: " + filename);

                                SwingUtilities.invokeLater(() -> navigate(currentNode, true));
                            }
                        }

                        if (!key.reset())
                        {
                            break;
                        }
                    }
                }
            }.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            // ok, we can face it and don't use watcher
        }

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
                                addActionListener(e -> handleAddressNavigation());
                            }
                        }, BorderLayout.CENTER);

                        add(buttonUp = new JButton("Up")
                        {
                            {
                                addActionListener(e -> handleButtonUp());
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
                handleTreeExpanded(event);
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
                handleTreeClicked(e);
            }
        });

        tree.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                handleTreeKeyPress(e);
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
        table.setRowHeight(16 + ICON_PADDING);
        table.setAutoCreateRowSorter(true);

        tableModel = new TableModel();
        table.setModel(tableModel);

        // custom sorting for size column
        TableRowSorter tableRowSorter = new TableRowSorter(tableModel);
        tableRowSorter.setComparator(Column.SIZE.ordinal(), (Comparator<Long>) (o1, o2) ->
        {
            long s1 = o1 != null ? o1 : 0;
            long s2 = o2 != null ? o2 : 0;
            return s1 > s2 ? +1 : -1;
        });
        table.setRowSorter(tableRowSorter);

        // custom renderer for size column
        final TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, (table1, value, isSelected, hasFocus, row, column) ->
        {
            Object myValue = (column == Column.SIZE.ordinal() && value != null) ? Util.formatFileSize((Long) value) : value;
            return renderer.getTableCellRendererComponent(table1, myValue, isSelected, hasFocus, row, column);
        });

        table.getSelectionModel().addListSelectionListener(e ->
        {
            if (!e.getValueIsAdjusting())
            {
                handleTableSelection(table);
            }
        });

        final String KEY_ACTION_OPEN = "table-key-enter";
        table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), KEY_ACTION_OPEN);
        table.getActionMap().put(KEY_ACTION_OPEN, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                handleTableEnterPressed(e);
            }
        });

        final String KEY_ACTION_REFRESH = "table-key-f5";
        AbstractAction actionRefresh = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                handleTableF5Pressed();
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
                handleTableBackspacePressed();
            }
        };
        table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), KEY_ACTION_UP);
        table.getActionMap().put(KEY_ACTION_UP, actionUp);

        table.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                handleTableMousePressed(e);
            }
        });
        table.addAncestorListener(new FocusAncestorListener());

        return table;
    }

    // =========================================================================================
    // HANDLERS
    // =========================================================================================

    private void handleButtonUp()
    {
        up();
    }

    private void handleAddressNavigation()
    {
        navigate(textAddress.getText(), false);
    }

    private void handleTreeKeyPress(KeyEvent e)
    {
        if (e.getKeyChar() == KeyEvent.VK_ENTER)
        {
            JTree tree = (JTree) e.getSource();
            onTreePathChanged(tree.getSelectionPath());
        }
    }

    private void handleTreeClicked(MouseEvent e)
    {
        JTree tree = (JTree) e.getSource();
        onTreePathChanged(tree.getPathForLocation(e.getX(), e.getY()));
    }

    private void handleTreeExpanded(TreeExpansionEvent event)
    {
        DefaultMutableTreeNode item = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
        refreshChildren(item, false);
    }

    private void handleTableMousePressed(MouseEvent e)
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

    private void handleTableBackspacePressed()
    {
        up();
    }

    private void handleTableF5Pressed()
    {
        navigate(currentNode, true);
    }

    private void handleTableEnterPressed(ActionEvent e)
    {
        JTable table = (JTable) e.getSource();
        int row = table.getSelectedRow();
        if (row != -1)
        {
            row = table.convertRowIndexToModel(row);
            openTableItem(row);
        }
    }

    private void handleTableSelection(JTable table)
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

    // =========================================================================================
    // CODE
    // =========================================================================================

    private void onTreePathChanged(TreePath path)
    {
        if (path != null)
        {
            DefaultMutableTreeNode item = (DefaultMutableTreeNode) path.getLastPathComponent();
            Node node = (Node) item.getUserObject();
            navigate(node, false);
        }
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
                                handleError(e);
                            }
                        }
                    });
                }
            }
            else
            {
                navigate(node, false);
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

    private void openRoot()
    {
        currentNode = new NodeFolder();
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

                    ((DefaultTreeModel) tree.getModel()).reload(item);

                    if (root)
                    {
                        tree.setSelectionInterval(0, 0);
                        tree.expandRow(0);

                        if (children.size() > 0)
                        {
                            navigate((Node) children.get(0).getUserObject(), false);
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
            navigate(parentPath, false);
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

    private void navigate(final Node node, final boolean forceRefresh)
    {
        // skip stub "Loading..." nodes
        if (node != null)
        {
            navigate(node.getFullPath(), forceRefresh);
        }
    }

    class View
    {
        final Node node;
        final List<Node> children;

        View(Node node, List<Node> children)
        {
            this.node = node;
            this.children = children;
        }
    }

    private void navigate(String path, final boolean forceRefresh)
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
                        if (forceRefresh)
                        {
                            browser.resetCache(node);
                        }
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

                                expandPath(path);

                                try
                                {
                                    if (watcher != null)
                                    {
                                        if (prevWatchKey != null)
                                        {
                                            prevWatchKey.cancel();
                                        }
                                        Path p = Paths.get(path);
                                        prevWatchKey = p.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
                                    }
                                }
                                catch (InvalidPathException | IOException e)
                                {
                                    // ignore as functionality is not critical
                                }
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

    private void expandPath(String path)
    {
        executorService2.submit(new Task<TaskType, String>(TaskType.EXPAND_PATH, path)
        {
            @Override
            public void run()
            {
                final List<String> p = new ArrayList<>();
                try
                {
                    // warming-up the cache & building path
                    Node node = browser.getNode(path);
                    p.add(node.getFullPath());
                    int counter = 0;
                    while (node.getParentPath() != null && counter++ < 1000) // prevent infinite loops
                    {
                        p.add(node.getParentPath());
                        node = browser.getNode(node.getParentPath());
                    }

                    Collections.reverse(p);
                }
                catch (PathException e)
                {
                    e.printStackTrace();
                }

                SwingUtilities.invokeLater(() ->
                {
                    DefaultMutableTreeNode currentTreeNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
                    List<Object> expandPath = new ArrayList<>();
                    expandPath.add(currentTreeNode);

                    for (String currentPath : p)
                    {
                        boolean found = false;

                        // if node is not loaded yet - populate it
                        if (currentTreeNode.getChildCount() == 1 && ((DefaultMutableTreeNode) currentTreeNode.getChildAt(0)).getUserObject() == null)
                        {
                            List<DefaultMutableTreeNode> children = new ArrayList<>();

                            if (currentTreeNode != null && !currentTreeNode.isLeaf())
                            {
                                for (Node childNode : browser.getChildren((Node) currentTreeNode.getUserObject()))
                                {
                                    if (!childNode.isLeaf())
                                    {
                                        DefaultMutableTreeNode e = new DefaultMutableTreeNode(childNode);
                                        e.add(new DefaultMutableTreeNode(null)); // "Loading..." stub
                                        children.add(e);
                                    }
                                }
                            }

                            currentTreeNode.removeAllChildren();
                            for (DefaultMutableTreeNode child : children)
                            {
                                currentTreeNode.add(child);
                            }

                            ((DefaultTreeModel) tree.getModel()).reload(currentTreeNode);
                        }

                        // search
                        for (int i = 0; i < currentTreeNode.getChildCount(); i++)
                        {
                            DefaultMutableTreeNode nextTreeNode = (DefaultMutableTreeNode) currentTreeNode.getChildAt(i);
                            Node node = (Node) nextTreeNode.getUserObject();

                            if (node.getFullPath().equalsIgnoreCase(currentPath))
                            {
                                found = true;
                                expandPath.add(nextTreeNode);
                                currentTreeNode = nextTreeNode;

                                //TreePath tp = new TreePath(expandPath.toArray(new Object[expandPath.size()]));
                                //tree.expandPath(tp);

                                break;
                            }
                        }

                        if (!found)
                        {
                            // path not found, giving up
                            return;
                        }
                    }

                    TreePath tp = new TreePath(expandPath.toArray(new Object[expandPath.size()]));
                    tree.expandPath(tp);
                    tree.setSelectionPath(tp);
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

enum TaskType
{
    POPULATE_TREE, EXPAND_PATH, SHOW_FILES, OPEN_FILE, PREVIEW_IMAGE;
}

class PreviewTask
{
    private final String fileName;
    private final int maxWidth;
    private final int maxHeight;

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
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        PreviewTask that = (PreviewTask) o;

        if (maxWidth != that.maxWidth)
        {
            return false;
        }
        if (maxHeight != that.maxHeight)
        {
            return false;
        }
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