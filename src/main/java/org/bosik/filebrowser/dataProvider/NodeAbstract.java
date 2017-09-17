package org.bosik.filebrowser.dataProvider;

import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-10
 */
public abstract class NodeAbstract implements Node
{
	private Node       parent;
	private List<Node> children;

	public NodeAbstract(Node parent)
	{
		this.parent = parent;
	}

	@Override
	public Node getParent()
	{
		return parent;
	}

	@Override
	public final List<Node> getChildren()
	{
		if (children == null)
		{
			children = fetchChildren();
		}

		return children;
	}

	public void updateCache()
	{
		if (children != null)
		{
			children = fetchChildren();
		}
	}

	protected abstract List<Node> fetchChildren();
}
