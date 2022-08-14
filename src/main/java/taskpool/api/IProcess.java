package taskpool.api;

import zdf.common.api.ApiResponse;

/**
 * interface for custom process
 * T is the result
 * @param <T>
 */
public interface IProcess<T> {
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
}
