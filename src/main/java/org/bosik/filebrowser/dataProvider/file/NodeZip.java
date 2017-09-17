package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public class NodeZip extends NodeFS
{
	public NodeZip(Node parent, File file)
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
		final List<Node> children = new ArrayList<>();

		try
		{
			FileSystem zipFs = FileSystems.newFileSystem(getFile().toPath(), NodeZip.class.getClassLoader());

			for (Path root : zipFs.getRootDirectories())
			{
				Files.walkFileTree(root, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
					{
						// TODO: check file/inner archive
						children.add(new NodeZipFile(NodeZip.this, file, NodeZip.this.getFile().toPath()));

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
					{
						if (dir.getNameCount() > 0)
						{
							children.add(new NodeZipFolder(NodeZip.this, dir, NodeZip.this.getFile().toPath()));
							return FileVisitResult.SKIP_SUBTREE;
						}
						else
						{
							return FileVisitResult.CONTINUE;
						}
					}
				});
			}

			return Util.sort(children);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
