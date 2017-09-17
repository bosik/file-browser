package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;

import java.nio.file.Path;

/**
 * @author Nikita Bosik
 * @since 2017-09-13
 */
public abstract class NodeZipItem extends NodeAbstract
{
	private Path path;
	private Path parentArchive;

	public NodeZipItem(Node parent, Path path, Path parentArchive)
	{
		super(parent);
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
		return parentArchive.toString() + path.toString();
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
