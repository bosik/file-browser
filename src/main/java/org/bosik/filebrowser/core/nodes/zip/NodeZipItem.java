package org.bosik.filebrowser.core.nodes.zip;

import org.bosik.filebrowser.core.Util;
import org.bosik.filebrowser.core.nodes.NodeAbstract;

import java.nio.file.Path;

/**
 * @author Nikita Bosik
 * @since 2017-09-13
 */
public abstract class NodeZipItem extends NodeAbstract
{
	private final Path path;
	private final Path parentArchive;

	public NodeZipItem(String parentPath, Path path, Path parentArchive)
	{
		super(parentPath);
		this.path = path;
		this.parentArchive = parentArchive;
	}

	@Override
	public String getName()
	{
		return path.getFileName().toString().replace("\\", "").replace("/", "");
	}

	@Override
	public String getFullPath()
	{
		// Path resolving doesn't work for file's path
		String s = Util.concatenatePath(parentArchive.toString(), path.toString());
		s = Util.stripeSlashes(s);
		s = Util.fixSlashesDefault(s);
		return s;
	}

	public Path getPath()
	{
		return path;
	}

	public Path getParentArchive()
	{
		return parentArchive;
	}
}
