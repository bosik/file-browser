package org.bosik.filebrowser.dataProvider.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.bosik.filebrowser.dataProvider.Node;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public class NodeFtpFolder extends NodeFtpItem
{
	public NodeFtpFolder(NodeFtp ftpRoot, Node parent, Path path)
	{
		super(ftpRoot, parent, path);
	}

	@Override
	public boolean isLeaf()
	{
		return false;
	}

	@Override
	protected List<Node> fetchChildren()
	{
		List<Node> children = new ArrayList<>();
		try
		{
			FTPClient client = getFtpRoot().getClient();

			String currentPath = getPath().toString();

			FTPFile[] folders = client.listDirectories(currentPath);
			for (FTPFile folder : folders)
			{
				children.add(new NodeFtpFolder(getFtpRoot(), this, Paths.get(getName()).resolve(folder.getName())));
			}

			FTPFile[] files = client.listFiles(currentPath);
			for (FTPFile file : files)
			{
				children.add(new NodeFtpFile(getFtpRoot(), this, Paths.get(getName()).resolve(file.getName())));
			}

			return children;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	@Override
	public Icon getIcon()
	{
		return UIManager.getIcon("FileView.directoryIcon");
	}
}
