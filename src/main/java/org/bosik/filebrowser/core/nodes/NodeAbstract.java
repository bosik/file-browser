package org.bosik.filebrowser.core.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-10
 */
public abstract class NodeAbstract implements Node
{
	private final String parent;

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
	 * Sorts nodes alphabetically, plus non-leafs are before leafs
	 *
	 * @param nodes Nodes to sort. Will not be changed during the sort.
	 * @param <T>   Type of node
	 * @return Sorted list
	 */
	protected static <T extends Node> List<T> sort(List<T> nodes)
	{
		List<T> result = new ArrayList<T>(nodes);
		Collections.sort(result, (o1, o2) ->
		{
			if (o1.isLeaf())
			{
				if (!o2.isLeaf())
				{
					return +1;
				}
			}
			else
			{
				if (o2.isLeaf())
				{
					return -1;
				}
			}

			return o1.getName().compareTo(o2.getName());
		});

		return result;
	}
}
