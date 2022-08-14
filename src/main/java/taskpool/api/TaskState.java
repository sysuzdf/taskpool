package taskpool.api;

import zdf.common.api.IBaseEnum;


/**
 * state of the task
 * T is the business result of the task (when the task is done)
 */
public class TaskState <T>{


    private T result;



    /**
     * exception thrown during task's execution
     */
    private Throwable exception;
    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    private State state=State.INIT;

    public enum State implements IBaseEnum {
        INIT("INIT","initialized"),
        WAITING("WAITING","wait for the queue's slot"),
        RUNNING("RUNNING","running"),
        FINISH("FINISH","finish"),
        INTERRUPT("INTERRUPT","interrupted"),
        STOP("STOP","stopped manual"),
        ERROR("ERROR","error, the process thrown an exception")
        ;

        private String code;
        private String description;
        State(String code,String desc){
            this.code=code;
            this.description=desc;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

}
