package org.bosik.filebrowser.core;

import org.bosik.filebrowser.core.dataProvider.Node;
import org.bosik.filebrowser.core.dataProvider.file.NodeFolder;
import org.bosik.filebrowser.core.dataProvider.ftp.NodeFtp;
import org.bosik.filebrowser.core.dataProvider.zip.NodeZipArchive;
import org.bosik.filebrowser.core.dataProvider.zip.NodeZipFolder;
import org.bosik.filebrowser.gui.CredentialsProviderImpl;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Nikita Bosik
 * @since 2017-09-12
 */
public class TreeBrowser
{
	private final Map<String, List<Node>> childrenCache = new ConcurrentHashMap<>();
	private Map<String, Node> specialFolders;

	// slow
	public List<Node> getChildren(String path) throws PathNotFoundException
	{
		return getChildren(getNode(path));
	}

	// slow
	public List<Node> getChildren(Node node)
	{
		Objects.requireNonNull(node, "Node is null");

		return childrenCache.computeIfAbsent(getKey(node), key -> node.getChildren());
		//		return node.getChildren();
	}

	// fast

	/**
	 * Reset cache for specified path and all sub-paths recursively
	 *
	 * @param path
	 */
	public void resetCache(String path)
	{
		Objects.requireNonNull(path, "Path is null");

		childrenCache.keySet().removeIf(key -> key.startsWith(path));
	}

	// fast

	/**
	 * Reset cache for the node and all it's children recursively
	 *
	 * @param node
	 */
	public void resetCache(Node node)
	{
		Objects.requireNonNull(node, "Node is null");

		resetCache(getKey(node));
	}

	// fast
	public void resetCache()
	{
		childrenCache.clear();
	}

	// slow

	/**
	 * @param url
	 * @return Never {@code null}
	 * @throws PathNotFoundException
	 */
	public Node getNode(String url) throws PathNotFoundException
	{
		if (url == null || url.isEmpty())
		{
			return getRootNode();
		}

		// ==== FTP ===========================================
		if (url.startsWith("ftp://"))
		{
			return new NodeFtp(url, new CredentialsProviderImpl());
		}

		// ==== Normal/network file/directory ===========================================
		File file = new File(url);
		if (file.exists())
		{
			if (Util.looksLikeArchive(file.getName()))
			{
				return new NodeZipArchive(file);
			}
			else
			{
				return new NodeFolder(file);
			}
		}

		// ==== Special Windows folders (like Computer, Network, etc.) ===========================================
		prepareSpecialFolders();
		if (specialFolders.containsKey(url))
		{
			return specialFolders.get(url);
		}

		// ==== Zip archives ===========================================

		Path path = Paths.get(url);
		List<Integer> indexZips = new ArrayList<>();
		for (int i = 0; i < path.getNameCount(); i++)
		{
			// NOTE: there also can be a normal folder named "folder.zip"
			String item = path.getName(i).toString().toLowerCase();
			if (Util.looksLikeArchive(item))
			{
				indexZips.add(i);
			}
		}

		if (indexZips.size() > 0)
		{
			List<Integer> archiveIndexes = new ArrayList<>();
			for (int i : indexZips)
			{
				Path sub = path.getRoot().resolve(path.subpath(0, i + 1));
				if (sub.toFile().isFile())
				{
					archiveIndexes.add(i);
				}
			}

			if (archiveIndexes.size() == 0)
			{
				// no archives, but missing file - throw an error
				throw new PathNotFoundException(url);
			}

			if (archiveIndexes.size() > 1)
			{
				// nested archives are not supported
				throw new PathNotFoundException(url);
			}

			Path _parentArchive = path.getRoot().resolve(path.subpath(0, archiveIndexes.get(0) + 1));
			Path _path = _parentArchive.relativize(path);
			String _parentPath = path.getParent().toString();
			return new NodeZipFolder(_parentPath, _path, _parentArchive);
		}

		throw new PathNotFoundException(url);
	}

	// fast
	public Node getRootNode()
	{
		return new NodeFolder(null)
		{
			@Override
			public String getName()
			{
				return "(root)";
			}

			@Override
			public List<Node> getChildren()
			{
				List<Node> children = new ArrayList<>();

				for (File file : FileSystemView.getFileSystemView().getRoots())
				{
					children.add(new NodeFolder(file));
				}

				return children;
			}
		};
	}

	private static String getKey(Node node)
	{
		String path = node.getFullPath();
		return (path != null) ? path : "";
	}

	/**
	 * Scans root & sub-root levels to remember special Windows folders
	 */
	private void prepareSpecialFolders()
	{
		if (specialFolders == null)
		{
			Map<String, Node> map = new ConcurrentHashMap<>();

			for (Node node : getRootNode().getChildren())
			{
				map.put(node.getName(), node);
				for (Node subNode : node.getChildren())
				{
					map.put(subNode.getName(), subNode);
				}
			}

			specialFolders = map;
		}
	}
}
