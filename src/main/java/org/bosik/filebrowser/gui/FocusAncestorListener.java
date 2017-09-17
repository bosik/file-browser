package org.bosik.filebrowser.gui;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * @author Nikita Bosik
 * @since 2017-09-17
 */
public class FocusAncestorListener implements AncestorListener
{
	@Override
	public void ancestorAdded(AncestorEvent e)
	{
		JComponent component = e.getComponent();
		component.requestFocusInWindow();
		component.removeAncestorListener(this);
	}

	@Override
	public void ancestorMoved(AncestorEvent e)
	{
	}

	@Override
	public void ancestorRemoved(AncestorEvent e)
	{
	}
}
