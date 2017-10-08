package org.bosik.filebrowser.core.nodes.ftp;

import org.bosik.filebrowser.core.Util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Represents file/folder URL. Immutable.
 *
 * @author Nikita Bosik
 * @since 2017-09-17
 */
public class ServerURL
{
	public static final int DEFAULT_FTP_PORT = 21;

	private final String host;
	private final int    port;
	private final String path;

	public ServerURL(String host, int port, String path)
	{
		Objects.requireNonNull(host, "host is null");
		Objects.requireNonNull(path, "path is null");

		this.host = Util.stripeSlashes(host);
		this.port = port;
		this.path = Util.fixBackslashes(Util.stripeSlashes(path));
	}

	public String getHost()
	{
		return host;
	}

	public int getPort()
	{
		return port;
	}

	public String getPath()
	{
		return path;
	}

	public ServerURL getParent()
	{
		if (!path.isEmpty())
		{
			Path path = Paths.get(getPath());
			path = path.getParent();

			if (path != null)
			{
				return new ServerURL(host, port, path.toString());
			}
			else
			{
				return new ServerURL(host, port, "");
			}
		}
		else
		{
			return null;
		}
	}

	public ServerURL getRoot()
	{
		return new ServerURL(host, port, "");
	}

	@Override
	public String toString()
	{
		if (getPort() == DEFAULT_FTP_PORT)
		{
			return String.format("ftp://%s/%s", getHost(), getPath());
		}
		else
		{
			return String.format("ftp://%s:%s/%s", getHost(), getPort(), getPath());
		}
	}

	public static ServerURL parse(String url)
	{
		Objects.requireNonNull(url, "url is null");

		String host;
		int port;
		String path;

		if (url.startsWith("ftp://"))
		{
			url = url.substring(6);
		}

		url = Util.fixBackslashes(url);

		{
			String[] t = separate(url, '/');
			host = t[0];
			path = t[1];
		}

		{
			String[] t = separate(host, ':');
			host = t[0];
			port = t[1].isEmpty() ? DEFAULT_FTP_PORT : Integer.parseInt(t[1]);
		}

		return new ServerURL(host, port, path);
	}

	public static String[] separate(String s, Character separator)
	{
		int k = s.indexOf(separator);
		if (k > -1)
		{
			return new String[] { s.substring(0, k), s.substring(k + 1) };
		}
		else
		{
			return new String[] { s, "" };
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ServerURL serverURL = (ServerURL) o;

		if (port != serverURL.port)
			return false;
		if (!host.equals(serverURL.host))
			return false;
		return path.equals(serverURL.path);
	}

	@Override
	public int hashCode()
	{
		int result = host.hashCode();
		result = 31 * result + port;
		result = 31 * result + path.hashCode();
		return result;
	}
}
