package org.bosik.filebrowser.core.browser;

import org.bosik.filebrowser.core.browser.exceptions.PathException;
import org.bosik.filebrowser.core.browser.exceptions.PathNotFoundException;
import org.bosik.filebrowser.core.browser.resolvers.PathResolver;
import org.bosik.filebrowser.core.nodes.Node;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core navigation component. Methods are marked as fast/slow:
 * <ul>
 * <li><b>Fast</b> methods are ok to be called from UI</li>
 * <li><b>Slow</b> methods may be time-consuming and should be called in a separate thread</li>
 * </ul>
 *
 * @author Nikita Bosik
 * @since 2017-09-12
 */
public class TreeBrowser
{
	private final List<PathResolver> resolvers;
	private final Map<String, List<Node>> childrenCache = new ConcurrentHashMap<>();

	public TreeBrowser(List<PathResolver> resolvers)
	{
		this.resolvers = resolvers;
	}

	/**
	 * <i>Slow</i>. Fetch node for specified path
	 *
	 * @param path Path to navigate to
	 * @return Never {@code null}
	 * @throws PathException If path is invalid or missing
	 */
	public Node getNode(String path) throws PathException
	{
		for (PathResolver resolver : resolvers)
		{
			Node node = resolver.resolve(path);
			if (node != null)
			{
				return node;
			}
		}

		throw new PathNotFoundException(path);
	}

	/**
	 * <i>Slow</i>. Fetch children nodes for specified node. Requests are cached.
	 *
	 * @param node Parent node
	 * @return List of children items, never {@code null}
	 * @see TreeBrowser#resetCache(String)
	 * @see TreeBrowser#resetCache(Node)
	 * @see TreeBrowser#resetCache()
	 */
	public List<Node> getChildren(Node node)
	{
		Objects.requireNonNull(node, "Node is null");
		return childrenCache.computeIfAbsent(getKey(node), key -> node.getChildren());

		//		try
		//		{
		//			Thread.sleep(1000);
		//		}
		//		catch (InterruptedException e)
		//		{
		//			System.err.println("Loading " + node.getName() + " interrupted");
		//			return Collections.emptyList();
		//		}

		// return node.getChildren(); // FIXME
	}

	/**
	 * <i>Slow</i>. Fetch children nodes for specified path
	 *
	 * @param path Path to resolve
	 * @return List of children
	 * @throws PathException If path is invalid or missing
	 */
	public List<Node> getChildren(String path) throws PathException
	{
		return getChildren(getNode(path));
	}

	/**
	 * <i>Fast</i>. Reset cache for specified path and all sub-paths recursively
	 *
	 * @param path Path
	 */
	public void resetCache(String path)
	{
		Objects.requireNonNull(path, "Path is null");
		childrenCache.keySet().removeIf(key -> key.startsWith(path));
	}

	/**
	 * <i>Fast</i>. Reset cache for the node and all it's children recursively
	 *
	 * @param node Node
	 */
	public void resetCache(Node node)
	{
		Objects.requireNonNull(node, "Node is null");
		resetCache(getKey(node));
	}

	/**
	 * <i>Fast</i>. Resets cache completely
	 */
	public void resetCache()
	{
		childrenCache.clear();
	}

	private static String getKey(Node node)
	{
		String path = node.getFullPath();
		return (path != null) ? path : "";
	}
}