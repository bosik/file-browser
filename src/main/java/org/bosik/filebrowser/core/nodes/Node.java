package org.bosik.filebrowser.core.nodes;

import javax.swing.Icon;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public interface Node
{
    /**
     * @return Short name of this node to display
     */
    String getName();

    /**
     * @return Cute node's icon to display, or {@code null} if there is no icon available
     */
    Icon getIcon();

    /**
     * @return Size of file (in bytes), or {@code null} if not available (e.g. node is folder)
     */
    Long getSize();

    /**
     * @return Absolute node's address, which may be used to locate (resolve) this node
     */
    String getFullPath();

    /**
     * @return Absolute path of node's parent node, or {@code null} if current node is root
     */
    String getParentPath();

    /**
     * @return {@code true} if this node is leaf (=can't has any children), {@code false} otherwise
     */
    boolean isLeaf();

    /**
     * @return List of children nodes, never {@code null}
     */
    List<Node> getChildren();
}
