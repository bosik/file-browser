package org.bosik.filebrowser.core.nodes.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.bosik.filebrowser.core.nodes.Node;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public class NodeFtpFile extends NodeFtpItem
{
	public NodeFtpFile(FTPClient client, ServerURL url)
	{
		super(client, url);
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
