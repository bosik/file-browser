package org.bosik.filebrowser.core.nodes.file;

import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.NodeAbstract;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public abstract class NodeFS extends NodeAbstract
{
	private final File file;

	public NodeFS(File file)
	{
		super(null);
		this.file = file;
	}

	@Override
	public String getParentPath()
	{
		return (file != null) ? file.getParent() : null;
	}

	public File getFile()
	{
		return file;
	}

	@Override
	public String getName()
	{
		return (file != null) ? FileSystemView.getFileSystemView().getSystemDisplayName(file) : null;
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
		return (file != null) ? FileSystemView.getFileSystemView().getSystemIcon(file) : null;
	}

	/**
	 * Builds root node
	 *
	 * @return Root node
	 */
	public static Node getRootNode()
	{
		// FIXME: move this method

		return new NodeFolder(null)
		{
			@Override
			public String getName()
			{
				return "(root)";
			}

			@Override
			public List<Node> getChildren()
			{
				List<Node> children = new ArrayList<>();

				for (File file : FileSystemView.getFileSystemView().getRoots())
				{
					children.add(new NodeFolder(file));
				}

				return children;
			}
		};
	}
}
