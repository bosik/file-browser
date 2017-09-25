package org.bosik.taskEngine.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Nikita Bosik
 * @since 2017-09-19
 */
public class MyExecutor<ID, Param>
{
	private final ExecutorService              executorService = Executors.newCachedThreadPool();
	private final Map<Task<ID, Param>, Future> tasks           = new ConcurrentHashMap<>();
	private final AcceptStrategy<ID, Param> acceptStrategy;

	public MyExecutor(AcceptStrategy<ID, Param> acceptStrategy)
	{
		this.acceptStrategy = acceptStrategy;
	}

	public MyExecutor()
	{
		this.acceptStrategy = StrategyTemplate.acceptAll();
	}

	public void submit(Task<ID, Param> task, AcceptStrategy<ID, Param> acceptStrategy)
	{
		removeDone();

		if (acceptStrategy.onBeforeSubmit(this, task))
		{
			Future<?> future = executorService.submit(() ->
			{
				try
				{
					task.run();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			});

			tasks.put(task, future);
		}
	}

	public void submit(Task<ID, Param> task)
	{
		submit(task, acceptStrategy);
	}

	public boolean exists(ID id)
	{
		return tasks.entrySet().stream().filter(e -> satisfy(e.getKey(), id) && !e.getValue().isDone()).count() > 0;
	}

	public boolean exists(ID id, Param param)
	{
		return tasks.entrySet().stream().filter(e -> satisfy(e.getKey(), id, param) && !e.getValue().isDone()).count() > 0;
	}

	private void removeDone()
	{
		Set<Task> doneTasks = new HashSet<>();

		for (Map.Entry<Task<ID, Param>, Future> entry : tasks.entrySet())
		{
			if (entry.getValue().isDone())
			{
				doneTasks.add(entry.getKey());
			}
		}

		for (Task task : doneTasks)
		{
			tasks.remove(task);
		}
	}

	/**
	 * Cancel tasks satisfying specified predicate
	 *
	 * @param predicate Predicate to define which tasks to cancel
	 * @return <code>true</code> if any tasks where cancelled, {@code false} otherwise
	 */
	private boolean cancel(Predicate<Map.Entry<Task<ID, Param>, Future>> predicate)
	{
		AtomicBoolean result = new AtomicBoolean(false);
		Stream<Map.Entry<Task<ID, Param>, Future>> entries = tasks.entrySet().stream().filter(predicate);
		entries.forEach(e ->
		{
			e.getValue().cancel(true);
			result.set(true);
		});
		return result.get();
	}

	/**
	 * Cancel all tasks
	 *
	 * @return <code>true</code> if any tasks where cancelled, {@code false} otherwise
	 */
	public boolean cancel()
	{
		return cancel(e -> true);
	}

	/**
	 * Cancel all tasks having specified ID
	 *
	 * @param id ID
	 * @return <code>true</code> if any tasks where cancelled, {@code false} otherwise
	 */
	public boolean cancel(ID id)
	{
		return cancel(e -> satisfy(e.getKey(), id));
	}

	/**
	 * Cancel all tasks having specified ID and param
	 *
	 * @param id    ID
	 * @param param param
	 * @return <code>true</code> if any tasks where cancelled, {@code false} otherwise
	 */
	public boolean cancel(ID id, Param param)
	{
		return cancel(e -> satisfy(e.getKey(), id, param));
	}

	private boolean satisfy(Task<ID, Param> task, ID id)
	{
		return task.getId().equals(id);
	}

	private boolean satisfy(Task<ID, Param> task, ID id, Param param)
	{
		return task.getId().equals(id) && (task.getParam() == null && param == null || task.getParam().equals(param));
	}

	public void shutdown()
	{
		executorService.shutdown();
	}

	public List<Runnable> shutdownNow()
	{
		return executorService.shutdownNow();
	}
}

