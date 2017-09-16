package org.bosik.filebrowser.dataProvider.file;

import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.Util;

import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;
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
public class NodeZipFolder implements Node
{
	// TODO: check thread-safety
	private static final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

	private Path path;
	private Path parentArchive;

	public NodeZipFolder(Path path, Path parentArchive)
	{
		this.path = path;
		this.parentArchive = parentArchive;
	}

	@Override
	public String getName()
	{
		return path.getFileName().toString();
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
			FileSystem zipFs = FileSystems.newFileSystem(parentArchive, NodeZipFolder.class.getClassLoader());

			for (Path root : zipFs.getRootDirectories())
			{
				root = root.resolve(path);
				final Path finalRoot = root;

				Files.walkFileTree(root, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
					{
						// TODO: check file/inner archive
						children.add(new NodeZipFile(file, parentArchive));

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
					{
						if (dir.getNameCount() > finalRoot.getNameCount())
						{
							children.add(new NodeZipFolder(dir, parentArchive));
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
