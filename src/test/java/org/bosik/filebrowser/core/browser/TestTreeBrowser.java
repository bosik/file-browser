package org.bosik.filebrowser.core.browser;

import org.bosik.filebrowser.core.browser.exceptions.InvalidPathException;
import org.bosik.filebrowser.core.browser.exceptions.PathException;
import org.bosik.filebrowser.core.browser.exceptions.PathNotFoundException;
import org.bosik.filebrowser.core.browser.resolvers.*;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.gui.CredentialsProviderImpl;
import org.bosik.filebrowser.gui.MainWindow;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-10-04
 */
public class TestTreeBrowser
{
    @Test
    public void test() throws PathException
    {
        Node nodeRoot = new DemoNode(null);
        Node nodeA = new DemoNode("A");
        Node nodeB = new DemoNode("B");
        Node nodeC = new DemoNode("C");

        TreeBrowser browser = new TreeBrowser(new ArrayList<PathResolver>()
        {
            {
                add(path -> path == null || path.isEmpty() ? nodeRoot : null);
                add(path -> "a".equals(path) ? nodeA : null);
                add(path -> "b".equals(path) ? nodeB : null);
                add(path -> "c".equals(path) ? nodeC : null);
            }
        });

        Assert.assertEquals(nodeRoot, browser.getNode(null));
        Assert.assertEquals(nodeRoot, browser.getNode(""));
        Assert.assertEquals(nodeA, browser.getNode("a"));
        Assert.assertEquals(nodeB, browser.getNode("b"));
        Assert.assertEquals(nodeC, browser.getNode("c"));

        try
        {
            browser.getNode("garbage");
            Assert.fail(PathNotFoundException.class.getSimpleName() + " expected");
        }
        catch (PathNotFoundException e)
        {
            // as expected
        }
    }
}

class DemoNode implements Node
{
    private String name;

    public DemoNode(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Icon getIcon()
    {
        return null;
    }

    @Override
    public String getFullPath()
    {
        return null;
    }

    @Override
    public String getParentPath()
    {
        return null;
    }

    @Override
    public boolean isLeaf()
    {
        return false;
    }

    @Override
    public List<Node> getChildren()
    {
        return null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        DemoNode demoNode = (DemoNode) o;

        return name != null ? name.equals(demoNode.name) : demoNode.name == null;
    }

    @Override
    public int hashCode()
    {
        return name != null ? name.hashCode() : 0;
    }
}
