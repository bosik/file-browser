package org.bosik.filebrowser.dataProvider;

import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public interface Node
{
	String getName();

	boolean isLeaf();

	List<Node> getChildren();
}
