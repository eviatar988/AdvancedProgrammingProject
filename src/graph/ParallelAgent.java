package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * ParallelAgent is a decorator for an Agent.
 *
 * Instead of running the real agent callback immediately, it puts the message
 * in a queue, and another thread handles the callbacks one by one.
 */
public class ParallelAgent implements Agent {

    /**
     * Small helper class that saves both the topic name and the message.
     */
    private static class Task {
        final String topic;
        final Message msg;

        Task(String topic, Message msg) {
            this.topic = topic;
            this.msg = msg;
        }
    }

    private final Agent agent;
    private final BlockingQueue<Task> queue;
    private final Thread worker;

    private volatile boolean closed = false;

    /**
     * Creates a new ParallelAgent.
     *
     * @param agent the agent we wrap
     * @param capacity the maximum size of the queue
     * @throws IllegalArgumentException if agent is null or capacity is not positive
     */
    public ParallelAgent(Agent agent, int capacity) {
        if (agent == null) {
            throw new IllegalArgumentException("agent cannot be null");
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }

        this.agent = agent;
        this.queue = new ArrayBlockingQueue<>(capacity);

        this.worker = new Thread(() -> {
            while (!closed) {
                try {
                    Task task = queue.take();
                    agent.callback(task.topic, task.msg);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        worker.start();
    }

    /**
     * Returns the name of the wrapped agent.
     *
     * @return the agent name
     */
    @Override
    public String getName() {
        return agent.getName();
    }

    /**
     * Resets the wrapped agent.
     */
    @Override
    public void reset() {
        agent.reset();
    }

    /**
     * Adds the callback request to the queue.
     * The real callback will run later in the worker thread.
     *
     * @param topic the topic name
     * @param msg the message
     */
    @Override
    public void callback(String topic, Message msg) {
        if (closed) {
            return;
        }

        try {
            queue.put(new Task(topic, msg));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Closes this ParallelAgent.
     * It stops the worker thread and then closes the wrapped agent.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        worker.interrupt();

        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        agent.close();
    }
}