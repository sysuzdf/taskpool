package taskpool.api;

import zdf.common.api.ApiResponse;

/**
 * interface for custom process
 * @param <T>
 */
public interface IProcess<T> {
    /**
     * start the task and wait for the result in blocking mode (will block the current thread)
     *
     * if the task is end normally , the code is SUC (is not the actual business meaning)
     * if the task is end with exception, the code is FAIL
     *
     * the caller should make sure idempotent, that means make some result checking inside process(),like querying in a while-loop
     *
     * @return
     */
    ApiResponse<T> process();


    /**
     * stop the task
     * @return
     */
    ApiResponse<Void> stop();
}
