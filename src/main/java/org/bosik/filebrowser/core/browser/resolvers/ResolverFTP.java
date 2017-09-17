package org.bosik.filebrowser.core.browser.resolvers;

import org.bosik.filebrowser.core.browser.exceptions.InvalidPathException;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.ftp.CredentialsProvider;
import org.bosik.filebrowser.core.nodes.ftp.NodeFtp;

/**
 * Resolves FTP addresses
 *
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public class ResolverFTP implements PathResolver
{
	private final CredentialsProvider credentialsProvider;

	public ResolverFTP(CredentialsProvider credentialsProvider)
	{
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public Node resolve(String path) throws InvalidPathException
	{
		if (path.startsWith("ftp://"))
		{
			return new NodeFtp(path, credentialsProvider);
		}
		else
		{
			return null;
		}
	}
}
