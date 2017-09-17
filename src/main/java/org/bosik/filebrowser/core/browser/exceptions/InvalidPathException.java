package org.bosik.filebrowser.core.browser.exceptions;

/**
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public class InvalidPathException extends PathException
{
	public InvalidPathException(String path)
	{
		super(path, "Invalid path: " + path);
	}
}
