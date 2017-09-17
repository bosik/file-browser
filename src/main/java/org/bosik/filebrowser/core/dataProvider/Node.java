package org.bosik.filebrowser.core.dataProvider;

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

	String getParentPath();

	boolean isLeaf();

	List<Node> getChildren();
}
