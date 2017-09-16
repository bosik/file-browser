package org.bosik.filebrowser;

import org.bosik.filebrowser.dataProvider.DataProvider;
import org.bosik.filebrowser.dataProvider.Node;

import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-12
 */
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
