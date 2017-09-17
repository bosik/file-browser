package org.bosik.filebrowser.core.browser.exceptions;

/**
 * General path exception
 *
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public abstract class PathException extends Exception
{
	private final String path;

	public PathException(String path, String message)
	{
		super(message);
		this.path = path;
	}

	public String getPath()
	{
		return path;
	}
}
