package org.bosik.filebrowser;

import javax.swing.SwingUtilities;

/**
 * @author Nikita Bosik
 * @since 2017-09-12
 */
public class FileBrowser
{
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(() ->
		{
			MainWindow app = new MainWindow();
			app.setVisible(true);
		});
	}
}
