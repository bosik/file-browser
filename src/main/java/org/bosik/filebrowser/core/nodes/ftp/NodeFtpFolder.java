package org.bosik.filebrowser.core.nodes.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.bosik.filebrowser.core.nodes.Node;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public class NodeFtpFolder extends NodeFtpItem
{
	public NodeFtpFolder(FTPClient client, ServerURL url)
	{
		super(client, url);
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
			FTPClient client = getClient();

			String currentPath = getUrl().getPath();

			FTPFile[] folders = client.listDirectories(currentPath);
			for (FTPFile folder : folders)
			{
				if (folder.getType() == FTPFile.DIRECTORY_TYPE)
				{
					String host = getUrl().getHost();
					int port = getUrl().getPort();
					String path = Paths.get(getUrl().getPath()).resolve(folder.getName()).toString();

					children.add(new NodeFtpFolder(client, new ServerURL(host, port, path)));
				}
			}

			FTPFile[] files = client.listFiles(currentPath);
			for (FTPFile file : files)
			{
				// some servers may include folders in listFiles(), so we have to explicitly check the type
				if (file.getType() == FTPFile.FILE_TYPE)
				{
					String host = getUrl().getHost();
					int port = getUrl().getPort();
					String path = Paths.get(getUrl().getPath()).resolve(file.getName()).toString();

					children.add(new NodeFtpFile(client, new ServerURL(host, port, path)));
				}
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
