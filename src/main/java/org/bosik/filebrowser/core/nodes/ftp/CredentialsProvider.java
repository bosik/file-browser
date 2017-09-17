package org.bosik.filebrowser.core.nodes.ftp;

/**
 * @author Nikita Bosik
 * @since 2017-09-13
 */
public interface CredentialsProvider
{
	Credentials getCredentials(String serverUrl);
}
