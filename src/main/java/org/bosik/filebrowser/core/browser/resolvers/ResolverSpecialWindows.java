package org.bosik.filebrowser.core.browser.resolvers;

import org.bosik.filebrowser.core.browser.exceptions.InvalidPathException;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.file.NodeFolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves special Windows folders (like Network, Computer, etc.)
 *
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public class ResolverSpecialWindows implements PathResolver
{
	private Map<String, Node> specialFolders;

	@Override
	public Node resolve(String path) throws InvalidPathException
	{
		prepareSpecialFolders();
		return specialFolders.getOrDefault(path, null);
	}

	/**
	 * Scans root & sub-root levels to remember special Windows folders
	 */
	private void prepareSpecialFolders()
	{
		if (specialFolders == null)
		{
			Map<String, Node> map = new ConcurrentHashMap<>();

			NodeFolder root = new NodeFolder();
			for (Node node : root.getChildren())
			{
				map.put(node.getName(), node);
				for (Node subNode : node.getChildren())
				{
					map.put(subNode.getName(), subNode);
				}
			}

			specialFolders = map;
		}
	}
}
