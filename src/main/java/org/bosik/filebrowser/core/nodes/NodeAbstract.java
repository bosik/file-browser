package org.bosik.filebrowser.core.nodes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-10
 */
public abstract class NodeAbstract implements Node
{
	private String parent;

	public NodeAbstract(String parent)
	{
		this.parent = parent;
	}

	@Override
	public String getParentPath()
	{
		return parent;
	}

	@Override
	public String toString()
	{
		return String.format("%s [%s]", getName(), getFullPath());
	}

	/**
	 * Sorts nodes so that non-leafs are before leafs
	 *
	 * @param nodes Nodes to sort. Will not be changed during the sort.
	 * @param <T>   Type of node
	 * @return Sorted list
	 */
	protected static <T extends Node> List<T> sort(List<T> nodes)
	{
		List<T> result = new ArrayList<T>(nodes.size());

		for (T child : nodes)
		{
			if (!child.isLeaf())
			{
				result.add(child);
			}
		}

		for (T child : nodes)
		{
			if (child.isLeaf())
			{
				result.add(child);
			}
		}

		return result;
	}
}
