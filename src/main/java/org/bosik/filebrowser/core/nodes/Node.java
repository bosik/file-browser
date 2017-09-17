package org.bosik.filebrowser.core.nodes;

import javax.swing.Icon;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public interface Node
{
	String getName();

	Icon getIcon();

	String getFullPath();

	String getParentPath();

	boolean isLeaf();

	/**
	 * @return List of children nodes, never {@code null}
	 */
	List<Node> getChildren();
}
