package org.bosik.filebrowser.dataProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class Util
{
	public static <T extends Node> List<T> sort(List<T> children)
	{
		List<T> result = new ArrayList<T>(children.size());

		for (T child : children)
		{
			if (!child.isLeaf())
			{
				result.add(child);
			}
		}

		for (T child : children)
		{
			if (child.isLeaf())
			{
				result.add(child);
			}
		}

		return result;
	}
}
