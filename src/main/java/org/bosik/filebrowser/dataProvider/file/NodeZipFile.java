package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class NodeZipFile implements Node
{
	private Path path;
	private Path parentArchive;

	public NodeZipFile(Path path, Path parentArchive)
	{
		this.path = path;
		this.parentArchive = parentArchive;
	}

	@Override
	public String getName()
	{
		return path.getFileName().toString();
	}

	@Override
	public boolean isLeaf()
	{
		return true;
	}

	@Override
	public List<Node> getChildren()
	{
		return Collections.emptyList();
	}
}
