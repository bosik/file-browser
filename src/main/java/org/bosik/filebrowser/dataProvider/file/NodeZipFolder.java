package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.Util;

import javax.swing.Icon;
import javax.swing.UIManager;
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
public class NodeZipFolder extends NodeZipItem
{
	public NodeZipFolder(Node parent, Path path, Path parentArchive)
	{
		super(parent, path, parentArchive);
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
			FileSystem zipFs = FileSystems.newFileSystem(getParentArchive(), NodeZipFolder.class.getClassLoader());

			for (Path root : zipFs.getRootDirectories())
			{
				final Path path = root.resolve(getPath());

				Files.walkFileTree(path, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
					{
						// TODO: check file/inner archive
						children.add(new NodeZipFile(NodeZipFolder.this, file, getParentArchive()));

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
					{
						if (dir.getNameCount() > path.getNameCount())
						{
							children.add(new NodeZipFolder(NodeZipFolder.this, dir, getParentArchive()));
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

	@Override
	public Icon getIcon()
	{
		return UIManager.getIcon("FileView.directoryIcon");
	}
}
