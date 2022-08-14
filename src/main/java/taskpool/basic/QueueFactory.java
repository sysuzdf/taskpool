package taskpool.basic;

import taskpool.api.IQueue;

public class QueueFactory {
    public static IQueue createDefaultPriorityQueue(String id,int concurrentLimit
        ,int maximumPoolSize){
        IQueue queue=new DefaultPriorityQueue(id,concurrentLimit,maximumPoolSize,maximumPoolSize,600000);
        return queue;
    }
}
