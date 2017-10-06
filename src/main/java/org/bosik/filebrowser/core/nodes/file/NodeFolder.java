package org.bosik.filebrowser.core.nodes.file;

import org.bosik.filebrowser.core.Util;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.zip.NodeZipArchive;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class NodeFolder extends NodeFS
{
	/**
	 * Constructor. Creates root node.
	 */
	public NodeFolder()
	{
		super(null);
	}

	/**
	 * Constructor. Creates node associated with the specified file
	 * @param file
	 */
	public NodeFolder(File file)
	{
		super(file);
	}

	@Override
	public boolean isLeaf()
	{
		return false;
	}

	@Override
	public String getName()
	{
		return (getFile() != null) ? super.getName() : "(root)";
	}

	@Override
	public List<Node> getChildren()
	{
		System.out.println("Building children for " + getFile() + "...");

		List<Node> children = new ArrayList<>();

		File[] files;
		if (getFile() != null)
		{
			files = FileSystemView.getFileSystemView().getFiles(getFile(), false);
		}
		else
		{
			files = File.listRoots();
//			files = FileSystemView.getFileSystemView().getRoots();
		}

		for (File file : files)
		{
			if (file.isDirectory())
			{
				children.add(new NodeFolder(file));
			}
			else if (file.isFile())
			{
				if (Util.looksLikeArchive(file.getName()))
				{
					children.add(new NodeZipArchive(file));
				}
				else
				{
					children.add(new NodeFile(file));
				}
			}
			else
			{
				// TODO: think
				System.err.println("Unknown item type found: " + file.getName());
			}
		}

		System.out.println("Building children for " + getFile() + " finished");
		return sort(children);
	}

	@Override
	public Long getSize()
	{
		return null;
	}
}
