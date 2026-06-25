package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;

/**
 * A simple agent that increases a number by one.
 *
 * The agent listens to the first topic in subs. When a legal number is received,
 * it publishes the number + 1 to the first topic in pubs.
 */
public class IncAgent implements Agent {

    /** Topics that this agent listens to. */
    private final String[] subs;

    /** Topics that this agent publishes to. */
    private final String[] pubs;

    /** True after close() was called. */
    private boolean closed;

    /**
     * Creates an IncAgent and connects it to the topics.
     *
     * @param subs topics to subscribe to, must contain at least one name
     * @param pubs topics to publish to, must contain at least one name
     * @throws IllegalArgumentException if the arrays are missing required topics
     */
    public IncAgent(String[] subs, String[] pubs) {
        checkTopics(subs, 1, "IncAgent needs one input topic");
        checkTopics(pubs, 1, "IncAgent needs one output topic");

        this.subs = cleanCopy(subs);
        this.pubs = cleanCopy(pubs);
        this.closed = false;

        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        tm.getTopic(this.subs[0]).subscribe(this);
        tm.getTopic(this.pubs[0]).addPublisher(this);
    }

    /**
     * @return the agent name
     */
    @Override
    public String getName() {
        return "inc";
    }

    /**
     * Nothing special to reset here because this agent has no saved input.
     */
    @Override
    public void reset() {
        // no saved state
    }

    /**
     * Receives a number and publishes number + 1.
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

        if (!topic.equals(subs[0])) {
            return;
        }

        double result = msg.asDouble + 1;
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
