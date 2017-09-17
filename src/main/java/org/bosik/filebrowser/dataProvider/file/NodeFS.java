package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;

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
		return fileSystemView.getSystemDisplayName(file);
	}

	@Override
	public String getFullPath()
	{
		String path = file.getAbsolutePath();
		// standard Windows folders have GUIDed names
		return !path.contains("::") ? path : getName();
	}

	@Override
	public Icon getIcon()
	{
		return (getFile() != null) ? fileSystemView.getSystemIcon(getFile()) : null;
	}
}
