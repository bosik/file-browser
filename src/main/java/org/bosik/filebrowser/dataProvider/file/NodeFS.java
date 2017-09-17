package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.NodeAbstract;

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

	public NodeFS(Node parent, File file)
	{
		super(parent);
		this.file = file;
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
