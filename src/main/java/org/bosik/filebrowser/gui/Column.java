package org.bosik.filebrowser.gui;

/**
 * @author Nikita Bosik
 * @since 2017-09-14
 */
enum Column
{
	ICON(""), NAME("Name"), SIZE("Size"), TIME_MODIFIED("Date modified");

	private final String caption;

	Column(String caption)
	{
		this.caption = caption;
	}

	public String getCaption()
	{
		return caption;
	}
}
