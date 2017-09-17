package org.bosik.filebrowser.dataProvider;

import org.bosik.filebrowser.dataProvider.file.NodeFolder;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class FSDataProvider implements DataProvider
{
	@Override
	public Node getRoot()
	{
		return new NodeFolder(null, null)
		{
			@Override
			public String getName()
			{
				return "(root)";
			}

			@Override
			public List<Node> fetchChildren()
			{
				List<Node> children = new ArrayList<>();

				for (File file : FileSystemView.getFileSystemView().getRoots())
				{
					children.add(new NodeFolder(this, file));
				}

				return children;
			}
		};
	}
}
