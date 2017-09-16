package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public abstract class NodeFS implements Node
{
	// TODO: check thread-safety
	private static final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

	private File file;

	public NodeFS(File file)
	{
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
		//		return file.getName();
	}

	@Override
	public Icon getIcon()
	{
		return (getFile() != null) ? fileSystemView.getSystemIcon(getFile()) : null;
	}
}
