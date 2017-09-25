package org.bosik.taskEngine.core;

/**
 * @author Nikita Bosik
 * @since 2017-09-19
 */
public interface AcceptStrategy<ID, Param>
{
	boolean onBeforeSubmit(MyExecutor<ID, Param> executor, Task<ID, Param> task);
}
