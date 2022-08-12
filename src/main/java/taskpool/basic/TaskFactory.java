package taskpool.basic;

import taskpool.api.IProcess;
import taskpool.api.ITask;
import zdf.common.api.ApiResponse;


public class TaskFactory {
    public static <T> ITask<T> createDefaultTask(String id, String queueId,int priority, IProcess<T> iProcess){
        ITask<T> result=new DefaultTask<T>(id,queueId) {
            @Override
            public ApiResponse<Void> stop() {
                return iProcess.stop();
            }

            @Override
            public ApiResponse<T> process() {
                return iProcess.process();
            }

            @Override
            public int getPriority(){
                return priority;
            }
        };

        return result;
    }
}
