package taskpool.test;

import org.junit.Assert;
import org.junit.Test;
import taskpool.api.IProcess;
import taskpool.api.IQueue;
import taskpool.api.ITask;
import taskpool.api.TaskState;
import taskpool.basic.DefaultTask;
import taskpool.basic.QueueFactory;
import taskpool.basic.TaskFactory;
import zdf.common.api.ApiResponse;
import zdf.common.api.ResponseCode;

import java.util.ArrayList;
import java.util.List;

public class Test001 {
    @Test
    public void test001(){
        String queueId="q1";

        List<ITask<String>> processList=new ArrayList<>();
        IQueue queue= QueueFactory.createDefaultPriorityQueue(queueId,5,10);

        for(int i=0;i<10;i++){
            TestProcess tp=new TestProcess(String.valueOf(i));
            ITask<String> t= TaskFactory.createDefaultTask(tp.getId(),queueId,0,tp);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            queue.submit(t);
        }
        //since the concurrent limit =5 ,so first 5 process is running
        //5-10 waiting
        List<String> waitings=queue.listTaskIdsByServingOrder();
        Assert.assertEquals(5,waitings.size());
        Assert.assertEquals("5",waitings.get(0));
        ITask<String> task5=queue.getTask("5");
        Assert.assertEquals(TaskState.State.WAITING,task5.getState().getState());
        Assert.assertEquals("9",waitings.get(4));
        //stop the task0, then task5 can run
        ApiResponse<Void> stop0=queue.stop("0");
        Assert.assertEquals(ResponseCode.SUCCESS,stop0.getCode());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(TaskState.State.RUNNING,task5.getState().getState());
        //update the concurrent limit, then task6 can run
        queue.updateConcurrentLimit(6);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ITask<String> task6=queue.getTask("6");
        Assert.assertEquals(TaskState.State.RUNNING,task6.getState().getState());
        //update task9's priority
        ApiResponse<Void> chresp=queue.changePriority("9",99);
        Assert.assertEquals(ResponseCode.SUCCESS,chresp.getCode());
        waitings=queue.listTaskIdsByServingOrder();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(3,waitings.size());
        //task9 will be the next to run
        Assert.assertEquals("9",waitings.get(0));


    }

    public class TestProcess implements IProcess<String> {

        private String id;

        public TestProcess(String id){
            this.id=id;
        }

        public String getId(){
            return id;
        }
        @Override
        public ApiResponse<String> process() {
            System.out.println("process:"+id+" start");

            ApiResponse<String> resp=new ApiResponse<String>();
            resp.setCode(ResponseCode.SUCCESS);
            resp.setBody(id);
            while(!Thread.interrupted()){
                try {
                    Thread.sleep(900000);
                } catch (InterruptedException e) {
                    System.out.println("process:"+id+" interrupted");
                    resp.setCode(ResponseCode.FAIL);
                    break;
                }
            }
            System.out.println("process:"+id+" done");
            return resp;
        }

        @Override
        public ApiResponse<Void> stop() {
            ApiResponse<Void> resp=new ApiResponse<>();
            resp.setCode(ResponseCode.SUCCESS);
            System.out.println("process:"+id+" stopped");
            return resp;
        }
    }

}
