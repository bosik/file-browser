package org.bosik.filebrowser.dataProvider.zip;

import org.bosik.filebrowser.dataProvider.Node;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class NodeZipFile extends NodeZipItem
{
	public NodeZipFile(Node parent, Path path, Path parentArchive)
	{
		super(parent, path, parentArchive);
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

	@Override
	public Icon getIcon()
	{
		return UIManager.getIcon("FileView.fileIcon");
	}
}
