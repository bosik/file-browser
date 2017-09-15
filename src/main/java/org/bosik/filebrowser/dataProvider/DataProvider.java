package org.bosik.filebrowser.dataProvider;

/**
 * @author Nikita Bosik
 * @since 2017-09-03
 */
public interface DataProvider<T extends Node>
{
	T getRoot();
}
