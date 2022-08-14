package taskpool.basic;

import taskpool.api.IQueue;

public class QueueFactory {
    public static IQueue createDefaultPriorityQueue(String id,int concurrentLimit){
        IQueue queue=new DefaultPriorityQueue(id,concurrentLimit);
        return queue;
    }
}
