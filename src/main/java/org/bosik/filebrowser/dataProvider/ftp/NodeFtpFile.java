package org.bosik.filebrowser.dataProvider.ftp;

import org.bosik.filebrowser.dataProvider.Node;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public class NodeFtpFile extends NodeFtpItem
{
	public NodeFtpFile(NodeFtp ftpRoot, Node parent, Path path)
	{
		super(ftpRoot, parent, path);
	}

	@Override
	public boolean isLeaf()
	{
		return true;
	}

	@Override
	protected List<Node> fetchChildren()
	{
		return Collections.emptyList();
	}

	@Override
	public Icon getIcon()
	{
		return UIManager.getIcon("FileView.fileIcon");
	}
}
