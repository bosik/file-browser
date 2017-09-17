package org.bosik.filebrowser.dataProvider.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.NodeAbstract;

import javax.swing.Icon;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public class NodeFtp extends NodeAbstract
{
	private String      url;
	private Credentials credentials;
	private FTPClient   client;

	public NodeFtp(Node parent, String url, Credentials credentials)
	{
		super(parent);
		this.url = url;
		this.credentials = credentials;
	}

	public NodeFtp(Node parent, String url)
	{
		super(parent);
		this.url = url;
		this.credentials = null;
	}

	public NodeFtp(Node parent, String url, CredentialsProvider credentialsProvider)
	{
		super(parent);
		this.url = url;
		this.credentials = credentialsProvider.getCredentials(url);
	}

	@Override
	public String getName()
	{
		return url;
	}

	@Override
	public String getFullPath()
	{
		return url;
	}

	@Override
	public Icon getIcon()
	{
		return null;
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
			FTPClient client = getClient();

			FTPFile[] folders = client.listDirectories();
			for (FTPFile folder : folders)
			{
				children.add(new NodeFtpFolder(this, this, Paths.get(folder.getName())));
			}

			FTPFile[] files = client.listFiles();
			for (FTPFile file : files)
			{
				children.add(new NodeFtpFile(this, this, Paths.get(file.getName())));
			}

			return children;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	protected FTPClient getClient() throws IOException
	{
		if (client == null)
		{
			client = buildFtpClient();
		}

		return client;
	}

	private FTPClient buildFtpClient() throws IOException
	{
		// TODO: port support
		// TODO: remove debug System.out.println()

		FTPClient client = new FTPClient();
		client.setControlEncoding("UTF-8"); // before connect!
		client.connect(stripe(url));
		System.out.print(client.getReplyString());
		if (!client.isConnected())
		{
			throw new RuntimeException("Failed to connect to " + url);
		}

		client.enterLocalPassiveMode();

		boolean isLoggedIn = credentials != null ? client.login(credentials.getUserName(), credentials.getPassword()) : true;
		System.out.print(client.getReplyString());
		if (!isLoggedIn)
		{
			// TODO: handle
			throw new RuntimeException("Failed to login to " + url + " as " + credentials.getUserName());
		}

		client.setFileType(FTP.BINARY_FILE_TYPE);
		System.out.print(client.getReplyString());

		return client;
	}

	private String stripe(String url)
	{
		if (url == null)
		{
			return null;
		}

		if (url.startsWith("ftp://"))
		{
			url = url.substring(6);
		}

		if (url.endsWith("/"))
		{
			url = url.substring(0, url.length() - 1);
		}

		return url;
	}
}
