package org.bosik.filebrowser.core.nodes.ftp;

import org.bosik.filebrowser.core.Util;
import org.bosik.filebrowser.core.nodes.NodeAbstract;

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

	public NodeFtpItem(NodeFtp ftpRoot, String parentPath, Path path)
	{
		super(parentPath);
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
		return Util.concatenatePath(ftpRoot.getFullPath(), path.toString());
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
