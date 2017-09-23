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

    //        @Override
    //        public boolean equals(Object o)
    //        {
    //            if (this == o) return true;
    //            if (o == null || getClass() != o.getClass()) return false;
    //
    //            Task<?, ?> task = (Task<?, ?>)o;
    //
    //            if (!id.equals(task.id)) return false;
    //            return param != null ? param.equals(task.param) : task.param == null;
    //        }
    //
    //        @Override
    //        public int hashCode()
    //        {
    //            int result = id.hashCode();
    //            result = 31 * result + (param != null ? param.hashCode() : 0);
    //            return result;
    //        }

    public boolean equalsTo(Task o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task<?, ?> task = (Task<?, ?>)o;

        if (!id.equals(task.id)) return false;
        return param != null ? param.equals(task.param) : task.param == null;
    }
}
