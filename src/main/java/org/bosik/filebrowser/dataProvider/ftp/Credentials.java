package org.bosik.filebrowser.dataProvider.ftp;

/**
 * @author Nikita Bosik
 * @since 2017-09-13
 */
public final class Credentials
{
	private final String userName;
	private final String password;

	public Credentials(String userName, String password)
	{
		this.userName = userName;
		this.password = password;
	}

	public String getUserName()
	{
		return userName;
	}

	public String getPassword()
	{
		return password;
	}
}
