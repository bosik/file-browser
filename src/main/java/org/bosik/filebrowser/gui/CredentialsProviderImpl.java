package org.bosik.filebrowser.gui;

import org.bosik.filebrowser.core.nodes.ftp.Credentials;
import org.bosik.filebrowser.core.nodes.ftp.CredentialsProvider;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/**
 * @author Nikita Bosik
 * @since 2017-09-13
 */
public class CredentialsProviderImpl implements CredentialsProvider
{
	private final JFrame frame;

	public CredentialsProviderImpl(JFrame frame)
	{
		this.frame = frame;
	}

	@Override
	public Credentials getCredentials(String serverUrl)
	{
		JTextField username = new JTextField();
		JPasswordField password = new JPasswordField();

		JPanel panel = new JPanel(new BorderLayout(5, 5))
		{
			{
				add(new JPanel(new GridLayout(0, 1, 2, 2))
				{
					{
						add(new JLabel("User", SwingConstants.RIGHT));
						add(new JLabel("Password", SwingConstants.RIGHT));
					}
				}, BorderLayout.WEST);

				add(new JPanel(new GridLayout(0, 1, 2, 2))
				{
					{
						add(username);
						add(password);
					}
				}, BorderLayout.CENTER);
			}
		};

		username.addAncestorListener(new FocusAncestorListener());

		int result = JOptionPane.showConfirmDialog(frame, panel, "Credentials for " + serverUrl, JOptionPane.OK_CANCEL_OPTION);

		if (result == JOptionPane.OK_OPTION)
		{
			return new Credentials(username.getText(), new String(password.getPassword()));
		}
		else
		{
			// user canceled the dialog
			return null;
		}
	}

	@Override
	public void notifyWrongCredentials()
	{
		JOptionPane.showMessageDialog(frame, "Wrong credentials", "Error", JOptionPane.ERROR_MESSAGE);
	}
}
