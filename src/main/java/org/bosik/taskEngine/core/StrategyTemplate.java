package org.bosik.taskEngine.core;

import java.util.Objects;

/**
 * @author Nikita Bosik
 * @since 2017-09-20
 */
public class StrategyTemplate<ID, Param> implements AcceptStrategy<ID, Param>
{
	private final Mode modeIdParam;
	private final Mode modeId;
	private final Mode modeDefault;

	/**
	 * @param modeIdParam Defines how to deal with existing tasks having same ID and param as submitting one
	 * @param modeId      Defines how to deal with existing tasks having same ID (and different param) as submitting one
	 * @param modeDefault Defines how to deal with existing tasks having different ID and param as submitting one
	 */
	public StrategyTemplate(Mode modeIdParam, Mode modeId, Mode modeDefault)
	{
		Objects.requireNonNull(modeIdParam, "modeIdParam is null");
		Objects.requireNonNull(modeId, "modeId is null");
		Objects.requireNonNull(modeDefault, "modeDefault is null");

		this.modeIdParam = modeIdParam;
		this.modeId = modeId;
		this.modeDefault = modeDefault;
	}

	@Override
	public boolean onBeforeSubmit(MyExecutor<ID, Param> executor, Task<ID, Param> task)
	{
		if (executor.exists(task.getId(), task.getParam()))
		{
			switch (modeIdParam)
			{
				case ACCEPT:
				{
					return true;
				}
				case DENY:
				{
					return false;
				}
				case OVERRIDE:
				{
					executor.cancel(task.getId(), task.getParam());
					return true;
				}
				case STOP:
				{
					executor.cancel(task.getId(), task.getParam());
					return false;
				}
				default:
				{
					throw new IllegalArgumentException("Unsupported mode: " + modeIdParam);
				}
			}
		}

		if (executor.exists(task.getId()))
		{
			switch (modeId)
			{
				case ACCEPT:
				{
					return true;
				}
				case DENY:
				{
					return false;
				}
				case OVERRIDE:
				{
					executor.cancel(task.getId());
					return true;
				}
				case STOP:
				{
					executor.cancel(task.getId());
					return false;
				}
				default:
				{
					throw new IllegalArgumentException("Unsupported mode: " + modeId);
				}
			}
		}

		switch (modeDefault)
		{
			case ACCEPT:
			{
				return true;
			}
			case DENY:
			{
				return false;
			}
			case OVERRIDE:
			{
				executor.cancel();
				return true;
			}
			case STOP:
			{
				executor.cancel();
				return false;
			}
			default:
			{
				throw new IllegalArgumentException("Unsupported mode: " + modeDefault);
			}
		}
	}

	public static <ID, Param> StrategyTemplate<ID, Param> acceptAll()
	{
		return new StrategyTemplate<>(Mode.ACCEPT, Mode.ACCEPT, Mode.ACCEPT);
	}

	public static <ID, Param> StrategyTemplate<ID, Param> singleton()
	{
		return new StrategyTemplate<>(Mode.DENY, Mode.DENY, Mode.DENY);
	}

	public static <ID, Param> StrategyTemplate<ID, Param> singletonById()
	{
		return new StrategyTemplate<>(Mode.DENY, Mode.DENY, Mode.ACCEPT);
	}

	public static <ID, Param> StrategyTemplate<ID, Param> singletonByIdAndParam()
	{
		return new StrategyTemplate<>(Mode.DENY, Mode.ACCEPT, Mode.ACCEPT);
	}

	public static <ID, Param> StrategyTemplate<ID, Param> override()
	{
		return new StrategyTemplate<>(Mode.OVERRIDE, Mode.OVERRIDE, Mode.OVERRIDE);
	}

	public static <ID, Param> StrategyTemplate<ID, Param> overrideById()
	{
		return new StrategyTemplate<>(Mode.OVERRIDE, Mode.OVERRIDE, Mode.ACCEPT);
	}

	public static <ID, Param> StrategyTemplate<ID, Param> overrideByIdAndParam()
	{
		return new StrategyTemplate<>(Mode.OVERRIDE, Mode.ACCEPT, Mode.ACCEPT);
	}

	public static <ID, Param> StrategyTemplate<ID, Param> stop()
	{
		return new StrategyTemplate<>(Mode.STOP, Mode.STOP, Mode.STOP);
	}

	public static <ID, Param> StrategyTemplate<ID, Param> stopById()
	{
		return new StrategyTemplate<>(Mode.STOP, Mode.STOP, Mode.ACCEPT);
	}

	public static <ID, Param> StrategyTemplate<ID, Param> stopByIdAndParam()
	{
		return new StrategyTemplate<>(Mode.STOP, Mode.ACCEPT, Mode.ACCEPT);
	}
}
