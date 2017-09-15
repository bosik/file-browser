package org.bosik.filebrowser;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class IconSizeHolder
{
	private static final FileSystemView fileSystemView = FileSystemView.getFileSystemView();
	private static       int            cachedSize     = -1;

	public static int get(File file)
	{
		if (cachedSize == -1)
		{
			Icon icon = fileSystemView.getSystemIcon(file);
			if (icon != null)
			{
				cachedSize = icon.getIconHeight();
			}
		}

		return cachedSize != -1 ? cachedSize : 16;
	}
}
