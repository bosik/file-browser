package org.bosik.filebrowser.core.dataProvider.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.bosik.filebrowser.core.dataProvider.Node;

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
	public NodeFtpFolder(NodeFtp ftpRoot, String parentPath, Path path)
	{
		super(ftpRoot, parentPath, path);
	}

	@Override
	public boolean isLeaf()
	{
		return false;
	}

	@Override
	public List<Node> getChildren()
	{
		List<Node> children = new ArrayList<>();
		try
		{
			FTPClient client = getFtpRoot().getClient();

			String currentPath = getPath().toString();

			FTPFile[] folders = client.listDirectories(currentPath);
			for (FTPFile folder : folders)
			{
				children.add(new NodeFtpFolder(getFtpRoot(), getFullPath(), Paths.get(getName()).resolve(folder.getName())));
			}

			FTPFile[] files = client.listFiles(currentPath);
			for (FTPFile file : files)
			{
				children.add(new NodeFtpFile(getFtpRoot(), getFullPath(), Paths.get(getName()).resolve(file.getName())));
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
