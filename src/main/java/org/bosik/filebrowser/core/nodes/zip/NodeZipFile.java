package org.bosik.filebrowser.core.nodes.zip;

import org.bosik.filebrowser.core.nodes.Node;

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
	public NodeZipFile(String parentPath, Path path, Path parentArchive)
	{
		super(parentPath, path, parentArchive);
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

	@Override
	public Icon getIcon()
	{
		return UIManager.getIcon("FileView.fileIcon");
	}
}
