package org.bosik.filebrowser.core;

import org.bosik.filebrowser.core.dataProvider.Node;

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

	public static String concatenatePath(String parent, String child)
	{
		while (parent.endsWith("/") || parent.endsWith("\\"))
		{
			parent = parent.substring(0, parent.length() - 1);
		}

		while (child.startsWith("/") || child.startsWith("\\"))
		{
			child = child.substring(1);
		}

		return parent + "/" + child;
	}

	public static boolean looksLikeArchive(String fileName)
	{
		if (fileName == null || fileName.isEmpty())
		{
			return false;
		}

		fileName = fileName.toLowerCase();
		return fileName.endsWith(".zip") || fileName.endsWith(".jar") || fileName.endsWith(".war");
	}
}
