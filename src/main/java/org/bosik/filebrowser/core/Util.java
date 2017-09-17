package org.bosik.filebrowser.core;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class Util
{
	/**
	 * Removes leading & trailing (back)slashes, at most one
	 *
	 * @param s
	 * @return
	 */
	public static String stripeSlashes(String s)
	{
		if (s.startsWith("/") || s.startsWith("\\"))
		{
			s = s.substring(1);
		}

		if (s.endsWith("/") || s.endsWith("\\"))
		{
			s = s.substring(0, s.length() - 1);
		}

		return s;
	}

	/**
	 * Concatenate two strings making sure there is exactly one slash between them
	 *
	 * @param parent
	 * @param child
	 * @return
	 */
	public static String concatenatePath(String parent, String child)
	{
		while (parent.endsWith("/") || parent.endsWith("\\"))
		{
			parent = parent.substring(0, parent.length() - 1);
		}

		while (child.startsWith("/") || child.startsWith("\\"))
		{
			child = child.substring(1);
		}

		return parent + "/" + child;
	}

	public static boolean looksLikeArchive(String fileName)
	{
		if (fileName == null || fileName.isEmpty())
		{
			return false;
		}

		fileName = fileName.toLowerCase();
		return fileName.endsWith(".zip") || fileName.endsWith(".jar") || fileName.endsWith(".war");
	}

	/**
	 * Replaces all backslashes with normal slashes
	 *
	 * @param s
	 * @return
	 */
	public static String fixBackslashes(String s)
	{
		if (s != null)
		{
			return s.replace('\\', '/');
		}
		else
		{
			return null;
		}
	}
}
