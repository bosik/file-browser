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
	private Path path;
	private Path parentArchive;

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
		return Util.concatenatePath(parentArchive.toString(), path.toString());
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
