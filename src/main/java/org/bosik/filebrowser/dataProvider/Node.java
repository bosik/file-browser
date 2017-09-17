package org.bosik.filebrowser.dataProvider;

import javax.swing.Icon;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public interface Node
{
	String getName();

	String getFullPath();

	Icon getIcon();

	Node getParent();

	boolean isLeaf();

	List<Node> getChildren();
}
