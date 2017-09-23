package org.bosik.filebrowser.core.browser.resolvers;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.bosik.filebrowser.core.browser.exceptions.InvalidPathException;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.ftp.Credentials;
import org.bosik.filebrowser.core.nodes.ftp.CredentialsProvider;
import org.bosik.filebrowser.core.nodes.ftp.NodeFtpFolder;
import org.bosik.filebrowser.core.nodes.ftp.ServerURL;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves FTP addresses
 *
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public class ResolverFTP implements PathResolver
{
	private final CredentialsProvider credentialsProvider;
	private final Map<ServerURL, FTPClient> clients = new ConcurrentHashMap<>();
	private final Map<String, Credentials>  cache   = new HashMap<>();

	public ResolverFTP(CredentialsProvider credentialsProvider)
	{
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public Node resolve(String path) throws InvalidPathException
	{
		if (path != null && path.startsWith("ftp://"))
		{
			ServerURL url = ServerURL.parse(path);
			FTPClient client = clients.computeIfAbsent(url.getRoot(), e -> buildFtpClient(url, credentialsProvider));
			if (client != null)
			{
				return new NodeFtpFolder(client, url);
			}
			else
			{
				// failed to login
				throw new InvalidPathException(path);
			}
		}
		else
		{
			return null;
		}
	}

	private FTPClient buildFtpClient(ServerURL address, CredentialsProvider credentialsProvider)
	{
		System.out.println("Building new FTP client for " + address);

		try
		{
			FTPClient client = new FTPClient();
			client.setControlEncoding("UTF-8"); // before connect!

			client.connect(address.getHost(), address.getPort());
			System.out.println("FTP: " + client.getReplyString());

			if (!client.isConnected())
			{
				throw new RuntimeException("Failed to connect to " + address);
			}

			boolean isLoggedIn = client.login("anonymous", "");

			if (!isLoggedIn)
			{
				if (credentialsProvider != null)
				{
					String serverUrl = address.getRoot().toString();

					for (;;)
					{
						Credentials credentials = getCredentials(credentialsProvider, serverUrl);
						if (credentials != null)
						{
							isLoggedIn = client.login(credentials.getUserName(), credentials.getPassword());
							System.out.println("FTP: " + client.getReplyString());

							if (isLoggedIn)
							{
								saveCredentials(serverUrl, credentials);
								client.changeWorkingDirectory(address.getPath());
								System.out.println("FTP: " + client.getReplyString());
								break;
							}
							else
							{
								credentialsProvider.notifyWrongCredentials();
							}
						}
						else
						{
							return null;
						}
					}
				}
				else
				{
					// TODO: handle
					throw new RuntimeException("No credentials provider is supplied to login to " + address);
				}
			}

			client.enterLocalPassiveMode();
			System.out.println("FTP: " + client.getReplyString());

			if (!address.getPath().isEmpty())
			{
				client.changeWorkingDirectory(address.getPath());
				System.out.println("FTP: " + client.getReplyString());
			}

			client.setFileType(FTP.BINARY_FILE_TYPE);
			return client;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Credentials getCredentials(CredentialsProvider credentialsProvider, String serverUrl)
	{
		if (cache.containsKey(serverUrl))
		{
			return cache.get(serverUrl);
		}

		return credentialsProvider.getCredentials(serverUrl);
	}

	private void saveCredentials(String serverUrl, Credentials credentials)
	{
		cache.put(serverUrl, credentials);
	}
}
