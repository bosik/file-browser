package org.bosik.filebrowser.gui;

import org.bosik.filebrowser.core.Util;
import org.bosik.filebrowser.core.nodes.Node;
import org.bosik.filebrowser.core.nodes.file.NodeFS;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
class TableModel extends AbstractTableModel
{
	private static final Column[]         COLUMNS = { Column.ICON, Column.NAME, Column.SIZE, Column.TIME_MODIFIED };
	private final        SimpleDateFormat FORMAT  = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private List<Node> nodes;

	TableModel()
	{
		this(new ArrayList<Node>());
	}

	TableModel(List<Node> nodes)
	{
		this.nodes = nodes;
	}

	@Override
	public int getRowCount()
	{
		return nodes.size();
	}

	@Override
	public int getColumnCount()
	{
		return COLUMNS.length;
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		Node item = nodes.get(row);

		Column c = COLUMNS[column];
		switch (c)
		{
			case ICON:
			{
				return item.getIcon();
			}
			case NAME:
			{
				return item.getName();
			}
			case SIZE:
			{
				if (item instanceof NodeFS)
				{
					File file = ((NodeFS) item).getFile();
					return file.isDirectory() ? "" : Util.formatFileSize(file.length());
				}
				else
				{
					return "";
				}
			}
			case TIME_MODIFIED:
			{
				if (item instanceof NodeFS)
				{
					File file = ((NodeFS) item).getFile();
					return FORMAT.format(new Date(file.lastModified()));
				}
				else
				{
					return null;
				}
			}
			default:
			{
				throw new IllegalArgumentException("Unsupported column type: " + c.name());
			}
		}
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		switch (COLUMNS[column])
		{
			case ICON:
			{
				return ImageIcon.class;
			}

			default:
			{
				return String.class;
			}
		}
	}

	@Override
	public String getColumnName(int column)
	{
		return COLUMNS[column].getCaption();
	}

	public Node getNode(int row)
	{
		return nodes.get(row);
	}

	public void setNodes(List<Node> nodes)
	{
		this.nodes = nodes;
		fireTableDataChanged();
	}
}
