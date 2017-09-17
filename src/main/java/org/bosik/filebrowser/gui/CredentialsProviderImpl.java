package org.bosik.filebrowser.gui;

import org.bosik.filebrowser.core.nodes.ftp.Credentials;
import org.bosik.filebrowser.core.nodes.ftp.CredentialsProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Nikita Bosik
 * @since 2017-09-13
 */
public class CredentialsProviderImpl implements CredentialsProvider
{
	private final Map<String, Credentials> cache = new HashMap<>();

	@Override
	public Credentials getCredentials(String serverUrl)
	{
		if (cache.containsKey(serverUrl))
		{
			return cache.get(serverUrl);
		}

		// TODO: UI dialog
		Credentials credentials = new Credentials("anonymous", "");
		// Credentials credentials = new Credentials(null ,null);
		// Credentials credentials = null;

		cache.put(serverUrl, credentials);
		return credentials;
	}
}
