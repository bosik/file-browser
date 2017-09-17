package org.bosik.filebrowser.dataProvider.zip;

import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.Util;
import org.bosik.filebrowser.dataProvider.file.NodeFS;

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
public class NodeZipArchive extends NodeFS
{
	public NodeZipArchive(Node parent, File file)
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
			FileSystem zipFs = FileSystems.newFileSystem(getFile().toPath(), NodeZipArchive.class.getClassLoader());

			for (Path root : zipFs.getRootDirectories())
			{
				Files.walkFileTree(root, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
					{
						// TODO: check file/inner archive
						children.add(new NodeZipFile(NodeZipArchive.this, file, NodeZipArchive.this.getFile().toPath()));

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
					{
						if (dir.getNameCount() > 0)
						{
							children.add(new NodeZipFolder(NodeZipArchive.this, dir, NodeZipArchive.this.getFile().toPath()));
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
