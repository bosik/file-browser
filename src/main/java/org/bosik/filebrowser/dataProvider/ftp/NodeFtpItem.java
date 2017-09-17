package org.bosik.filebrowser.dataProvider.ftp;

import org.bosik.filebrowser.dataProvider.Node;
import org.bosik.filebrowser.dataProvider.NodeAbstract;

import javax.swing.Icon;
import java.nio.file.Path;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
public abstract class NodeFtpItem extends NodeAbstract
{
	private NodeFtp ftpRoot;
	private Path    path;

	public NodeFtpItem(NodeFtp ftpRoot, Node parent, Path path)
	{
		super(parent);
		this.ftpRoot = ftpRoot;
		this.path = path;
	}

	@Override
	public String getName()
	{
		return path.getFileName().toString();
	}

	@Override
	public String getFullPath()
	{
		String parent = ftpRoot.getFullPath();
		while (parent.endsWith("/") || parent.endsWith("\\"))
		{
			parent = parent.substring(0, parent.length() - 1);
		}

		String next = path.toString();
		while (next.startsWith("/") || next.startsWith("\\"))
		{
			next = next.substring(1);
		}

		return parent + "/" + next;
	}

	@Override
	public Icon getIcon()
	{
		return null;
	}

	protected NodeFtp getFtpRoot()
	{
		return ftpRoot;
	}

	protected Path getPath()
	{
		return path;
	}
}
