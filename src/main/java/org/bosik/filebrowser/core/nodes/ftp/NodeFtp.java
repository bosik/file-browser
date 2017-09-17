package org.bosik.filebrowser.core.nodes.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.NodeAbstract;

import javax.swing.Icon;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public class NodeFtp extends NodeAbstract
{
	private String      url;
	private Credentials credentials;
	private FTPClient   client;

	public NodeFtp(String url, Credentials credentials)
	{
		super(null);

		Objects.requireNonNull(url, "URL is null");

		this.url = url;
		this.credentials = credentials;
	}

	public NodeFtp(String url)
	{
		this(url, (Credentials) null);
	}

	public NodeFtp(String url, CredentialsProvider credentialsProvider)
	{
		this(url, credentialsProvider.getCredentials(url));
	}

	@Override
	public String getParentPath()
	{
		String stripedUrl = stripe(url);
		Path path = Paths.get(stripedUrl);
		return "ftp://" + path.getParent().toString();
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
	public List<Node> getChildren()
	{
		List<Node> children = new ArrayList<>();
		try
		{
			FTPClient client = getClient();

			FTPFile[] folders = client.listDirectories();
			for (FTPFile folder : folders)
			{
				children.add(new NodeFtpFolder(this, getFullPath(), Paths.get(folder.getName())));
			}

			FTPFile[] files = client.listFiles();
			for (FTPFile file : files)
			{
				children.add(new NodeFtpFile(this, getFullPath(), Paths.get(file.getName())));
			}

			return children;
		}
		catch (IOException e)
		{
			// TODO: handle
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
		String[] address = getServerAndPath(url);
		String urlServer = address[0];
		String urlPath = address[1];
		client.connect(urlServer);
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

		client.changeWorkingDirectory(urlPath);

		client.setFileType(FTP.BINARY_FILE_TYPE);
		System.out.print(client.getReplyString());

		return client;
	}

	private static String stripe(String url)
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

	private static String[] getServerAndPath(String url)
	{
		String server = url;
		String path = "";

		if (server != null)
		{
			if (server.startsWith("ftp://"))
			{
				server = server.substring(6);
			}

			server.replace('\\', '/');
			int k = server.indexOf('/');
			if (k > -1)
			{
				path = server.substring(k + 1);
				server = server.substring(0, k);
			}
		}

		return new String[] { server, path };
	}
}
