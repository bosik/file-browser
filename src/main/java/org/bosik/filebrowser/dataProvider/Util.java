package org.bosik.filebrowser.dataProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class Util
{
	/**
	 * Sorts nodes so that non-leafs are before leafs
	 *
	 * @param nodes Nodes to sort. Will not be changed during the sort.
	 * @param <T>   Type of node
	 * @return Sorted list
	 */
	public static <T extends Node> List<T> sort(List<T> nodes)
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
