package org.bosik.filebrowser.core.browser.resolvers;

import org.bosik.filebrowser.core.browser.exceptions.InvalidPathException;
import org.bosik.filebrowser.core.nodes.Node;

/**
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public interface PathResolver
{
	/**
	 * Try to resolve specified path. Three outcomes are possible:
	 * <table>
	 * <tr>
	 * <th>Case</th>
	 * <th>Result</th>
	 * <th>Client</th>
	 * </tr>
	 * <tr>
	 * <td>Path is not recognized</td>
	 * <td>Returns {@code null}</td>
	 * <td>Client should try another {@link PathResolver}</td>
	 * </tr>
	 * <tr>
	 * <td>Path is recognized, but invalid</td>
	 * <td>Throws {@link InvalidPathException}</td>
	 * <td>Client should stop and handle the error</td>
	 * </tr>
	 * <tr>
	 * <td>Path is recognized and valid</td>
	 * <td>Returns resolved node</td>
	 * <td>Client should handle the node</td>
	 * </tr>
	 * </table>
	 *
	 * @param path Path to resolve
	 * @return Non-{@code null} if path is recognized and valid, {@code null} if path is not recognized
	 * @throws InvalidPathException If path is recognized, but malformed
	 */
	Node resolve(String path) throws InvalidPathException;
}
