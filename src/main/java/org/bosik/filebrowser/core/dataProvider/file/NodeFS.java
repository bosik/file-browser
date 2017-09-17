package org.bosik.filebrowser.core.dataProvider.file;

import org.bosik.filebrowser.core.dataProvider.NodeAbstract;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public abstract class NodeFS extends NodeAbstract
{
	// TODO: check thread-safety
	private static final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

	private File file;

	public NodeFS(File file)
	{
		super(null);
		this.file = file;
	}

	@Override
	public String getParentPath()
	{
		//		if (super.getParentPath() != null)
		//		{
		//			return super.getParentPath();
		//		}

		return file != null ? file.getParent() : null;
	}

	public File getFile()
	{
		return file;
	}

	@Override
	public String getName()
	{
		return file != null ? fileSystemView.getSystemDisplayName(file) : null;
	}

	@Override
	public String getFullPath()
	{
		if (file != null)
		{
			String path = file.getAbsolutePath();
			// standard Windows folders have GUIDed names
			return !path.contains("::") ? path : getName();
			// return path;
		}
		else
		{
			return null;
		}
	}

	@Override
	public Icon getIcon()
	{
		return (file != null) ? fileSystemView.getSystemIcon(file) : null;
	}
}
