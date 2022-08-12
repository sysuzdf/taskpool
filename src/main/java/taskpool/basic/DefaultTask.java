package taskpool.basic;

import taskpool.api.ITask;
import taskpool.api.TaskState;
import zdf.common.api.ApiResponse;

/**
 * default task
 * @param <T>
 */
public abstract class DefaultTask<T> implements ITask<T> {

    private String id;
    private String queueId;
    private TaskState taskState=new TaskState();

    private static final Object stateLock=new Object();


    public DefaultTask(String id, String queueId){
        this.id=id;
        this.queueId=queueId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String queueId() {
        return queueId;
    }

    @Override
    public TaskState getState() {
        return taskState;
    }


    @Override
    public abstract ApiResponse<Void> stop();

    @Override
    public abstract ApiResponse<T> process();

    @Override
    public int getPriority() {
        return 0;
    }




}
