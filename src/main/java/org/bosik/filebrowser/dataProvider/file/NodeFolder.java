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
		List<Node> children = new ArrayList<>();

		File[] files = FileSystemView.getFileSystemView().getFiles(getFile(), true);
		for (File child : files)
		{
			if (child.isDirectory())
			{
				children.add(new NodeFolder(child));
			}
			else if (child.isFile())
			{
				String name = child.getName().toLowerCase();
				if (name.endsWith(".zip") || name.endsWith(".jar"))
				{
					children.add(new NodeZip(child));
				}
				else
				{
					children.add(new NodeFile(child));
				}
			}
			else
			{
				// TODO: think
				System.out.println("Unknown item type found: " + child.getName());
			}
		}

		return Util.sort(children);
	}
}
