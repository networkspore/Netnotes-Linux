package com.netnotes;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

import com.utils.Utils;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;

public class BoundedExecutor {
    
    private final ExecutorService m_exec;
    private final Semaphore m_semaphore;

    public BoundedExecutor(ExecutorService exec, int bound) {
        m_exec = exec;
        m_semaphore = new Semaphore(bound);
    }

    public void submitBoundedRunnable(Runnable runnable){
       
        
        Task<Object> boundedTask = new Task<Object>() {
            @Override
            public Object call() throws InterruptedException {
                m_semaphore.acquire();
                runnable.run();
                
                return null;
            }
        };

        boundedTask.setOnFailed(onFailed->{
            m_semaphore.release();
        });

        boundedTask.setOnSucceeded(onSucceeded->{
            m_semaphore.release();
        });

        m_exec.submit(boundedTask);

        
    }

    public void submitBoundedTask(Task<Object> task, EventHandler<WorkerStateEvent> onResult, EventHandler<WorkerStateEvent> onFailedResult){
       
        
        Task<Object> boundedTask = new Task<Object>() {
            @Override
            public Object call() throws InterruptedException {
                m_semaphore.acquire();

                task.setOnFailed(onFailed->{
                    m_semaphore.release();
                    
                    Utils.returnObject(onFailed.getSource().getException(), m_exec, onResult, onFailedResult);
                });
        
                task.setOnSucceeded(onSucceeded->{
                    m_semaphore.release();
                    Utils.returnObject(onSucceeded.getSource().getValue(), m_exec, onResult, onFailedResult);
                });
        
                m_exec.submit(task);
                
                return null;
            }
        };

        boundedTask.setOnFailed(onFailed->{
            m_semaphore.release();
        });

        boundedTask.setOnSucceeded(onSucceeded->{
          
        });

        m_exec.submit(boundedTask);

        
    }
}
