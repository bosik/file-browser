package org.bosik.filebrowser.dataProvider.ftp;

/**
 * @author Nikita Bosik
 * @since 2017-09-13
 */
public interface CredentialsProvider
{
	Credentials getCredentials(String serverUrl);
}
