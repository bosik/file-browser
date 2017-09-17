package org.bosik.filebrowser.core.nodes.zip;

import org.bosik.filebrowser.core.Util;
import org.bosik.filebrowser.core.nodes.Node;

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
	public NodeZipFolder(String parentPath, Path path, Path parentArchive)
	{
		super(parentPath, path, parentArchive);
	}

	@Override
	public boolean isLeaf()
	{
		return false;
	}

	@Override
	public List<Node> getChildren()
	{
		final List<Node> children = new ArrayList<>();

		try
		{
			FileSystem zipFs = FileSystems.newFileSystem(getParentArchive(), NodeZipFolder.class.getClassLoader());

			for (Path root : zipFs.getRootDirectories())
			{
				Path zipChildPath = zipFs.getPath(getPath().toString());
				final Path path = root.resolve(zipChildPath);

				Files.walkFileTree(path, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
					{
						children.add(new NodeZipFile(NodeZipFolder.this.getFullPath(), file, getParentArchive()));

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
					{
						if (dir.getNameCount() > path.getNameCount())
						{
							children.add(new NodeZipFolder(NodeZipFolder.this.getFullPath(), dir, getParentArchive()));
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
