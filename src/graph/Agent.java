package graph;

/**
 * Interface for agents in the pub/sub system.
 *
 * Every agent can subscribe to topics,
 * receive messages and publish data.
 */
public interface Agent {

    /**
     * @return agent name
     */
    String getName();

    /**
     * Resets the agent data.
     */
    void reset();

    /**
     * Called when a topic sends a message.
     *
     * @param topic topic name
     * @param msg message that was received
     */
    void callback(String topic, Message msg);

    /**
     * Closes the agent.
     */
    void close();
}