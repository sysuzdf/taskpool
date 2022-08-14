package taskpool.api;

import zdf.common.api.ApiResponse;

/**
 * Task
 * T represents the business result object
 * @param <T>
 */
public interface ITask<T> {
    /**
     * unique id
     * @return
     */
    String getId();

    /**
     * the id of the queue this task belongs
     * @return
     */
    String queueId();

    /**
     * get the current state
     * @return
     */
    TaskState<T> getTaskState();

    /**
     * start the task and wait for the result in blocking mode (will block the current thread)
     *
     * the caller should make sure idempotent, that means make some result checking inside process(),like querying in a while-loop
     *
     * @return
     */
    T process();


    /**
     * stop the task
     * @return
     */
    ApiResponse<Void> stop();

    /**
     * initial priority
     * @return
     */
    int getPriority();


}
