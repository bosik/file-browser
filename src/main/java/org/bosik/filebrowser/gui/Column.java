package org.bosik.filebrowser.gui;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
enum Column
{
	ICON(""), NAME("File"), SIZE("Size"), TIME_MODIFIED("Last Modified");

	private String caption;

	Column(String caption)
	{
		this.caption = caption;
	}

	public String getCaption()
	{
		return caption;
	}
}
