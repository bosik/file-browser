package org.bosik.filebrowser.core.nodes;

/**
 * @author Nikita Bosik
 * @since 2017-09-10
 */
public abstract class NodeAbstract implements Node
{
	private String parent;

	public NodeAbstract(String parent)
	{
		this.parent = parent;
	}

	@Override
	public String getParentPath()
	{
		return parent;
	}

	@Override
	public String toString()
	{
		return String.format("%s [%s]", getName(), getFullPath());
	}
}
