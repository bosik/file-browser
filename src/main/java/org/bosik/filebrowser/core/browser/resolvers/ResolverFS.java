package org.bosik.filebrowser.core.browser.resolvers;

import org.bosik.filebrowser.core.Util;
import org.bosik.filebrowser.core.browser.exceptions.InvalidPathException;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.file.NodeFolder;
import org.bosik.filebrowser.core.nodes.zip.NodeZipArchive;

import java.io.File;

/**
 * Resolves normal files/folders + local network items
 *
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public class ResolverFS implements PathResolver
{
	@Override
	public Node resolve(String path) throws InvalidPathException
	{
		File file = new File(path);
		if (file.exists())
		{
			if (Util.looksLikeArchive(file.getName()))
			{
				return new NodeZipArchive(file);
			}
			else
			{
				return new NodeFolder(file);
			}
		}

		return null;
	}
}
