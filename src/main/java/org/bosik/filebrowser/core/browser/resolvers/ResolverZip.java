package org.bosik.filebrowser.core.browser.resolvers;

import org.bosik.filebrowser.core.Util;
import org.bosik.filebrowser.core.browser.exceptions.InvalidPathException;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.zip.NodeZipFolder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public class ResolverZip implements PathResolver
{
	@Override
	public Node resolve(String url) throws InvalidPathException
	{
		Path path = Paths.get(url);

		List<Integer> indexZips = new ArrayList<>();
		for (int i = 0; i < path.getNameCount(); i++)
		{
			// NOTE: there also can be a normal folder named "folder.zip"
			String item = path.getName(i).toString().toLowerCase();
			if (Util.looksLikeArchive(item))
			{
				indexZips.add(i);
			}
		}

		if (indexZips.size() > 0)
		{
			List<Integer> archiveIndexes = new ArrayList<>();
			for (int i : indexZips)
			{
				Path sub = path.getRoot().resolve(path.subpath(0, i + 1));
				if (sub.toFile().isFile())
				{
					archiveIndexes.add(i);
				}
			}

			if (archiveIndexes.size() == 0)
			{
				// no archives - skip
				return null;
			}

			if (archiveIndexes.size() > 1)
			{
				// nested archives are not supported
				throw new InvalidPathException(url);
			}

			String zipParentPath = path.getParent().toString();
			Path zipParentArchive = path.getRoot().resolve(path.subpath(0, archiveIndexes.get(0) + 1));
			Path zipPath = zipParentArchive.relativize(path);
			return new NodeZipFolder(zipParentPath, zipPath, zipParentArchive);
		}
		else
		{
			return null;
		}
	}
}
