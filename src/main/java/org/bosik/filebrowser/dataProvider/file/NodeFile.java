package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class NodeFile extends NodeFS
{
	public NodeFile(Node parent, File file)
	{
		super(parent, file);
	}

	@Override
	public boolean isLeaf()
	{
		return true;
	}

	@Override
	public List<Node> fetchChildren()
	{
		return Collections.emptyList();
	}
}
