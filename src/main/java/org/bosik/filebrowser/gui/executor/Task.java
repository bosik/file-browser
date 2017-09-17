package org.bosik.filebrowser.gui.executor;

/**
 * @author Nikita Bosik
 * @since 2017-09-15
 */
public interface Task<Input, Output>
{
	void onBeforeExecution(Input input);

	Output doInBackground(Input input) throws Exception;

	void onDone(Input input, Output output);

	void onError(Input input, Exception e);
}
