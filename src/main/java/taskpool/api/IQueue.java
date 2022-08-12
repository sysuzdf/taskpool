package taskpool.api;

import zdf.common.api.ApiResponse;

import java.util.List;

/**
 * queue for running tasks
 */
public interface IQueue {
    /**
     * unique id
     * @return
     */
    String getId();

    /**
     * how many task can run concurrently
     * @return
     */
    int getConcurrentLimit();

    /**
     * update the concurrent limit
     * @param limit
     */
    void updateConcurrentLimit(int limit);

    /**
     * submit the task
     * @param task
     */
    void submit(ITask task);

    /**
     *
     * @param taskId
     * @return
     */
    ApiResponse<Void> stop(String taskId);

    /**
     * query a task
     * @param taskId
     * @return
     */
    ITask getTask(String taskId);

    /**
     * get all task of the queue
     * @return
     */
    List<ITask> getTasks();

    /**
     * change priority of a task
     * @param taskId
     * @param priority
     */
    ApiResponse<Void> changePriority(String taskId, int priority);

    /**
     * return the task ids by the serving order
     * @return
     */
    List<String> listTaskIdsByServingOrder();

}
