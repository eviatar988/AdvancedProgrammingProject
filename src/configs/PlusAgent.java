package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;

/**
 * A simple agent that adds two numbers.
 *
 * The agent listens to the first two topics in subs. The last value from the
 * first topic is saved as x and the last value from the second topic is saved
 * as y. When a legal number arrives, it publishes x + y to the first topic in
 * pubs.
 */
public class PlusAgent implements Agent {

    /** Topics that this agent listens to. */
    private final String[] subs;

    /** Topics that this agent publishes to. */
    private final String[] pubs;

    /** Last value from the first input topic. */
    private double x;

    /** Last value from the second input topic. */
    private double y;

    /** True after close() was called. */
    private boolean closed;

    /**
     * Creates a PlusAgent and connects it to the topics.
     *
     * @param subs topics to subscribe to, must contain at least two names
     * @param pubs topics to publish to, must contain at least one name
     * @throws IllegalArgumentException if the arrays are missing required topics
     */
    public PlusAgent(String[] subs, String[] pubs) {
        checkTopics(subs, 2, "PlusAgent needs two input topics");
        checkTopics(pubs, 1, "PlusAgent needs one output topic");

        this.subs = cleanCopy(subs);
        this.pubs = cleanCopy(pubs);
        this.closed = false;
        reset();

        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        tm.getTopic(this.subs[0]).subscribe(this);
        tm.getTopic(this.subs[1]).subscribe(this);
        tm.getTopic(this.pubs[0]).addPublisher(this);
    }

    /**
     * @return the agent name
     */
    @Override
    public String getName() {
        return "plus";
    }

    /**
     * Resets the saved values to zero.
     */
    @Override
    public void reset() {
        x = 0;
        y = 0;
    }

    /**
     * Receives a message from one of the input topics and publishes x + y.
     * Invalid numeric messages are ignored.
     *
     * @param topic the topic that sent the message
     * @param msg the received message
     */
    @Override
    public void callback(String topic, Message msg) {
        if (closed || topic == null || msg == null || Double.isNaN(msg.asDouble)) {
            return;
        }

        if (topic.equals(subs[0])) {
            x = msg.asDouble;
        } else if (topic.equals(subs[1])) {
            y = msg.asDouble;
        } else {
            return;
        }

        double result = x + y;
        if (!Double.isNaN(result) && !Double.isInfinite(result)) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(result));
        }
    }

    /**
     * Disconnects this agent from all topics it used.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        tm.getTopic(subs[0]).unsubscribe(this);
        tm.getTopic(subs[1]).unsubscribe(this);
        tm.getTopic(pubs[0]).removePublisher(this);
    }

    /**
     * Checks that a topics array has enough legal names.
     *
     * @param topics topics array
     * @param min minimum number of topics needed
     * @param message exception message
     */
    private static void checkTopics(String[] topics, int min, String message) {
        if (topics == null || topics.length < min) {
            throw new IllegalArgumentException(message);
        }

        for (int i = 0; i < min; i++) {
            if (topics[i] == null || topics[i].trim().isEmpty()) {
                throw new IllegalArgumentException(message);
            }
        }
    }

    /**
     * Creates a trimmed copy of a String array.
     *
     * @param arr source array
     * @return copied array with trimmed values
     */
    private static String[] cleanCopy(String[] arr) {
        String[] copy = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            copy[i] = (arr[i] == null) ? "" : arr[i].trim();
        }
        return copy;
    }
}
