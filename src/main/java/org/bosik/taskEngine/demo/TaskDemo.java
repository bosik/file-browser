package org.bosik.taskEngine.demo;

import javax.swing.SwingUtilities;

/**
 * @author Nikita Bosik
 * @since 2017-09-18
 */
public class TaskDemo
{
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(() ->
		{
			MainDemoWindow app = new MainDemoWindow();
			app.setVisible(true);
		});
	}
}
