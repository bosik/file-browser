package org.bosik.taskEngine.core;

/**
 * Defines strategy to deal with new task in presence of <i>same</i> tasks already submitted to the pool.
 * Meaning of <i>same</i> defines separately (e.g. by id, by id and param).
 *
 * @author Nikita Bosik
 * @since 2017-09-20
 */
public enum Mode
{
    /**
     * Accept new task, leave existing tasks
     */
    ACCEPT,

    /**
     * Deny new task, leave existing tasks
     */
    DENY,

    /**
     * Accept new task, cancel existing tasks
     */
    OVERRIDE,

    /**
     * Deny new task, cancel existing tasks
     */
    STOP;
}
