package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;

import javax.swing.Icon;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nikita Bosik
 * @since 2017-09-10
 */
public class NodeCached implements Node
{
	private Node       node;
	private List<Node> children;

	public NodeCached(Node node)
	{
		this.node = node;
	}

	@Override
	public String getName()
	{
		return node.getName();
	}

	@Override
	public Icon getIcon()
	{
		return node.getIcon();
	}

	@Override
	public boolean isLeaf()
	{
		return node.isLeaf();
	}

	@Override
	public List<Node> getChildren()
	{
		if (children == null)
		{
			children = wrap(node.getChildren());
		}

		return children;
	}

	public void updateCache()
	{
		if (children != null)
		{
			children = wrap(node.getChildren());
		}
	}

	private static List<Node> wrap(List<Node> nodes)
	{
		if (nodes != null)
		{
			return nodes.stream().map(node -> node.isLeaf() ? node : new NodeCached(node)).collect(Collectors.toList());
		}
		else
		{
			return null;
		}
	}
}
