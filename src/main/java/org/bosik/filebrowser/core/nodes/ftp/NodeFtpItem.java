package org.bosik.filebrowser.core.nodes.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.bosik.filebrowser.core.nodes.NodeAbstract;

import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public abstract class NodeFtpItem extends NodeAbstract
{
	private final FTPClient client;
	private final ServerURL url;

	public NodeFtpItem(FTPClient client, ServerURL url)
	{
		super(null);

		Objects.requireNonNull(client, "client is null");
		Objects.requireNonNull(url, "URL is null");

		this.client = client;
		this.url = url;
	}

	@Override
	public String getName()
	{
		if (url.getPath().isEmpty())
		{
			return url.getRoot().toString();
		}
		else
		{
			return Paths.get(url.getPath()).getFileName().toString();
		}
	}

	@Override
	public String getFullPath()
	{
		return url.toString();
	}

	@Override
	public String getParentPath()
	{
		return url.getParent() != null ? url.getParent().toString() : null;
	}

	public FTPClient getClient()
	{
		return client;
	}

	public ServerURL getUrl()
	{
		return url;
	}
}
