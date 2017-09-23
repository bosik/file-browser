package org.bosik.filebrowser.core;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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

	public static boolean hasExtension(String fileName, String[] extensions)
	{
		if (fileName == null || fileName.isEmpty())
		{
			return false;
		}

		fileName = fileName.toLowerCase();

		for (String extension : extensions)
		{
			if (fileName.endsWith(extension.toLowerCase()))
			{
				return true;
			}
		}

		return false;
	}

	public static boolean looksLikeArchive(String fileName)
	{
		return hasExtension(fileName, new String[] { ".zip", ".jar", ".war" });
	}

	public static boolean looksLikeImage(String fileName)
	{
		return hasExtension(fileName, new String[] { ".bmp", ".gif", ".jpeg", ".jpg", ".png", ".tif" });
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

	public static ImageIcon buildPreviewImage(String fileName, int maxWidth, int maxHeight)
	{
		try
		{
			BufferedImage myPicture = ImageIO.read(new File(fileName));

			// TODO: check if thread interrupted
			if (Thread.interrupted())
			{
				System.err.println("Preview for " + fileName + " interrupted");
				return null;
			}

			if (myPicture != null)
			{
				if (myPicture.getWidth() > maxWidth || myPicture.getHeight() > maxHeight)
				{
					double kx = (double) myPicture.getWidth() / maxWidth;
					double ky = (double) myPicture.getHeight() / maxHeight;

					int resizedWidth;
					int resizedHeight;

					if (kx > ky)
					{
						resizedWidth = (int) (myPicture.getWidth() / kx);
						resizedHeight = (int) (myPicture.getHeight() / kx);
					}
					else
					{
						resizedWidth = (int) (myPicture.getWidth() / ky);
						resizedHeight = (int) (myPicture.getHeight() / ky);
					}

					return new ImageIcon(myPicture.getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_FAST));
				}
				else
				{
					return new ImageIcon(myPicture);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return null;
	}
}
