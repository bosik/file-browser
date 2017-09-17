package org.bosik.filebrowser.core.dataProvider.file;

import org.bosik.filebrowser.core.dataProvider.Node;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class NodeFile extends NodeFS
{
	public NodeFile(File file)
	{
		super(file);
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
