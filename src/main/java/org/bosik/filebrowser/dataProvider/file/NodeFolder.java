package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.Util;

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
	public NodeFolder(Node parent, File file)
	{
		super(parent, file);
	}

	@Override
	public boolean isLeaf()
	{
		return false;
	}

	@Override
	public List<Node> fetchChildren()
	{
		System.out.println("Building children for " + getName() + "...");

		List<Node> children = new ArrayList<>();

		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
			return children;
		}

		File[] files = FileSystemView.getFileSystemView().getFiles(getFile(), true);
		for (File file : files)
		{
			if (file.isDirectory())
			{
				children.add(new NodeFolder(this, file));
			}
			else if (file.isFile())
			{
				String name = file.getName().toLowerCase();
				if (name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".war"))
				{
					children.add(new NodeZip(this, file));
				}
				else
				{
					children.add(new NodeFile(this, file));
				}
			}
			else
			{
				// TODO: think
				System.out.println("Unknown item type found: " + file.getName());
			}
		}

		System.out.println("Building children for " + getName() + " finished");

		return Util.sort(children);
	}
}
