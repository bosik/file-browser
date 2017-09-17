package org.bosik.filebrowser.core.dataProvider.ftp;

import org.bosik.filebrowser.core.dataProvider.Node;

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
	public NodeFtpFile(NodeFtp ftpRoot, String parentPath, Path path)
	{
		super(ftpRoot, parentPath, path);
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
