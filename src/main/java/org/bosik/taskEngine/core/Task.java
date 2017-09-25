package org.bosik.taskEngine.core;

/**
 * @author Nikita Bosik
 * @since 2017-09-19
 */
public abstract class Task<ID, Param> implements Runnable
{
	private final ID    id;
	private final Param param;

	public Task(ID id, Param param)
	{
		this.id = id;
		this.param = param;
	}

	public ID getId()
	{
		return id;
	}

	public Param getParam()
	{
		return param;
	}

	public boolean equalsTo(Task o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Task<?, ?> task = (Task<?, ?>) o;

		if (!id.equals(task.id))
			return false;
		return param != null ? param.equals(task.param) : task.param == null;
	}
}
