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
	public List<Node> getChildren()
	{
		System.out.println("Building children for " + getName() + "...");

		List<Node> children = new ArrayList<>();

		//		try
		//		{
		//			Thread.sleep(500);
		//		}
		//		catch (InterruptedException e)
		//		{
		//			e.printStackTrace();
		//			return children;
		//		}

		File[] files = FileSystemView.getFileSystemView().getFiles(getFile(), false);

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

		System.out.println("Building children for " + getName() + " finished");
		return Util.sort(children);
	}
}
