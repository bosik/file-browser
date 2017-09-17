package org.bosik.filebrowser.core.browser;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public class PathNotFoundException extends Exception
{
	private final String path;

	public PathNotFoundException(String path)
	{
		super("Path not found: " + path);
		this.path = path;
	}

	public String getPath()
	{
		return path;
	}
}
