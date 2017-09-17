package org.bosik.filebrowser.core.browser.exceptions;

/**
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public class PathNotFoundException extends PathException
{
	public PathNotFoundException(String path)
	{
		super(path, "Path not found: " + path);
	}
}
