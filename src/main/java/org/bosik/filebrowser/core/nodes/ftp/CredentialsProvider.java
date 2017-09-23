package org.bosik.filebrowser.core.nodes.ftp;

/**
 * Provides methods for fetching credentials
 *
 * @author Nikita Bosik
 * @since 2017-09-13
 */
public interface CredentialsProvider
{
	/**
	 * Ask client for credentials
	 *
	 * @param serverUrl
	 * @return Credentials or {@code null} if user refused to answer (e.g. cancelled prompt dialog)
	 */
	Credentials getCredentials(String serverUrl);

	/**
	 * Notify client side credentials are wrong
	 */
	void notifyWrongCredentials();
}
