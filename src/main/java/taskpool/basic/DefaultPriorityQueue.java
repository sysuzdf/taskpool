package taskpool.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskpool.api.IQueue;
import taskpool.api.ITask;
import taskpool.api.TaskState;
import zdf.common.api.ApiResponse;
import zdf.common.api.ResponseCode;
import zdf.common.utils.log.LogTool;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultPriorityQueue implements IQueue {

    private static Logger logger = LoggerFactory.getLogger(LogTool.class);

    private String id;
    private int concurrentLimit;

    private PriorityBlockingQueue<Callable> queue;

    private Map<String,ITask> taskMap=new ConcurrentHashMap<>();

    private Map<String,FutureTask> futureMap=new ConcurrentHashMap<>();

    private Thread submitThread;

    private ReentrantLock queueLock=new ReentrantLock();

    private Condition takeFromQueue;

    private int used=0;

    public DefaultPriorityQueue(String id,int concurrentLimit){
        this.id=id;
        this.concurrentLimit=concurrentLimit;
        queue=new PriorityBlockingQueue<>();
        takeFromQueue =queueLock.newCondition();
        submitThread=new Thread(new Runnable() {
            @Override
            public void run() {
                LogTool.beginTrace(UUID.randomUUID().toString());
                while(!Thread.interrupted()) {
                    queueLock.lock();
                    while (queue.size() == 0 || used >= DefaultPriorityQueue.this.concurrentLimit) {
                        try {
                            takeFromQueue.await();
                        } catch (InterruptedException e) {
                            logger.error("submitThread interrupted. but continue", e);
                        }
                    }
                    try {
                        InnerRunner runner = (InnerRunner) queue.take();
                        FutureTask task=new FutureTask(runner);
                        Thread taskThread=new Thread(task,
                                "["+DefaultPriorityQueue.this.id+"]["+runner.getTask().getId()+"]");
                        taskThread.start();
                        futureMap.put(runner.getTask().getId(), task);
                        used = used + 1;
                    } catch (InterruptedException e) {
                        logger.error("submitThread queue.take interrupted. but continue", e);
                    } finally {
                        queueLock.unlock();
                    }
                }
                logger.error("submitThread exited");
            }
        },"QUEUE["+id+"]-SUBMIT-THREAD");
        submitThread.start();
    }

    private class InnerRunner implements Callable<TaskState>,Comparable<InnerRunner>{

        private ITask task;

        private int priority=0;

        private String createTimeStamp;

        public InnerRunner(ITask task){
            this.task=task;
            this.priority=task.getPriority();
            this.createTimeStamp=String.valueOf(System.currentTimeMillis());
        }
        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(InnerRunner o) {
            int result=0;
            result=o.getPriority()-getPriority();
            if(result==0){
                return getCreateTimeStamp().compareTo(o.getCreateTimeStamp());
            }
            return result;
        }

        public ITask getTask(){
            return task;
        }

        public String getCreateTimeStamp() {
            return createTimeStamp;
        }


        @Override
        public TaskState call() {
            LogTool.beginTrace("["+id+"]["+task.getId()+"]");
            TaskState taskState=task.getTaskState();
            try {
                taskState.setState(TaskState.State.RUNNING);
                Object result = task.process();
                taskState.setState(TaskState.State.FINISH);
                taskState.setResult(result);
                return taskState;
            }catch(Exception ex){
                logger.error("[{}][{}] process error",id,task.getId(),ex);
                taskState.setState(TaskState.State.ERROR);
                taskState.setException(ex);
                return taskState;
            }finally {
                finishTask(task.getId());
                LogTool.endTrace();
            }
        }
    }

    private void finishTask(String taskId){
        synchronized (taskMap){
            if(!taskMap.containsKey(taskId)){
                logger.error("finishTask fail {}",taskId);
                return;
            }
            queueLock.lock();
            try{
                used=used-1;
                if(used<concurrentLimit) {
                    takeFromQueue.signalAll();
                }
                taskMap.remove(taskId);
                futureMap.remove(taskId);
            }finally {
                queueLock.unlock();
            }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getConcurrentLimit() {
        return concurrentLimit;
    }

    @Override
    public void updateConcurrentLimit(int limit) {
        queueLock.lock();
        try{
            concurrentLimit=limit;
            if(concurrentLimit>used){
                takeFromQueue.signalAll();
            }
        }finally {
            queueLock.unlock();
        }
    }

    @Override
    public void submit(ITask task) {
        synchronized (taskMap){
            if(!taskMap.containsKey(task.getId())){
                taskMap.put(task.getId(), task);
            }else{
                String err="submit fail, duplicate task id:"+task.getId();
                throw new RuntimeException(err);
            }
        }
        queueLock.lock();
        try {
            InnerRunner runner = new InnerRunner(task);
            task.getTaskState().setState(TaskState.State.WAITING);
            queue.put(runner);
            takeFromQueue.signalAll();
        }finally {
            queueLock.unlock();
        }
    }

    @Override
    public ApiResponse<Void> stop(String taskId) {
        synchronized (taskMap){
            if(!taskMap.containsKey(taskId)){
                String err="stop fail, task not exists id:"+taskId;
                throw new RuntimeException(err);
            }
        }
        ITask task=taskMap.get(taskId);
        task.getTaskState().setState(TaskState.State.STOP);
        try {
            ApiResponse<Void> resp = task.stop();
            return resp;
        }finally {
            Future future=futureMap.get(taskId);
            future.cancel(true);
            finishTask(task.getId());
        }
    }

    @Override
    public ITask getTask(String taskId) {
        synchronized (taskMap){
            return taskMap.get(taskId);
        }
    }

    @Override
    public List<ITask> getTasks() {
        List<ITask> tasks=new ArrayList<>();
        for(ITask task:taskMap.values()){
            tasks.add(task);
        }
        return tasks;
    }

    @Override
    public ApiResponse<Void> changePriority(String taskId, int priority) {
        ApiResponse<Void> resp=new ApiResponse<Void>();
        queueLock.lock();
        try{
            InnerRunner innerRunner=null;
            Iterator itr=queue.iterator();
            while(itr.hasNext()){
                InnerRunner tmpTask= (InnerRunner) itr.next();
                if(tmpTask.getTask().getId().equals(taskId)){
                    innerRunner=tmpTask;
                    break;
                }
            }
            //change the priority
            if(innerRunner!=null){
                innerRunner.setPriority(priority);
                //update the priority queue
                if(queue.remove(innerRunner)){
                    queue.put(innerRunner);
                    resp.setCode(ResponseCode.SUCCESS);
                    return resp;
                }
            }
        }finally {
            queueLock.unlock();
        }
        resp.setErrMsg("task:"+taskId+" not in the queue");
        resp.setCode(ResponseCode.FAIL);
        return resp;
    }

    @Override
    public List<String> listTaskIdsByServingOrder() {
        List<String> result=new ArrayList<>();
        //get the actual order in the queue
        queueLock.lock();
        try {
            PriorityBlockingQueue<Callable> tmpQueue=new PriorityBlockingQueue<>();
            //retrieve from the queue and re-oder in tmpQueue
            Iterator itr=queue.iterator();
            while(itr.hasNext()){
                InnerRunner tmpTask= (InnerRunner) itr.next();
                tmpQueue.put(tmpTask);
            }
            //now the order is copied
            while(tmpQueue.size()>0){
                InnerRunner tmpTask= (InnerRunner) tmpQueue.take();
                result.add(tmpTask.getTask().getId());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            queueLock.unlock();
        }
        return result;
    }
}
