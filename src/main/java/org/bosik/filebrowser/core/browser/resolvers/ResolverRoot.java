package org.bosik.filebrowser.core.browser.resolvers;

import org.bosik.filebrowser.core.browser.exceptions.InvalidPathException;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.file.NodeFS;

/**
 * Resolves empty path to a root node
 *
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public class ResolverRoot implements PathResolver
{
	@Override
	public Node resolve(String path) throws InvalidPathException
	{
		if (path == null || path.isEmpty())
		{
			return NodeFS.getRootNode();
		}
		else
		{
			return null;
		}
	}
}
