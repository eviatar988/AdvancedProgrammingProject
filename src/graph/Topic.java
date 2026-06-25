package graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a topic in the pub/sub system.
 *
 * Subscribers can subscribe to the topic
 * and publishers can publish messages.
 */
public class Topic {

    /**
     * Topic name.
     */
    public final String name;

    /**
     * Subscribers listening to this topic.
     */
    public final List<Agent> subs;

    /**
     * Publishers allowed to publish to this topic.
     */
    public final List<Agent> pubs;


    private Message lastMessage;


    /**
     * Creates a new topic.
     *
     * @param name topic name
     */
    Topic(String name) {
        this.name = name;
        this.subs = new ArrayList<>();
        this.pubs = new ArrayList<>();
    }

    /**
     * Register an Agent as subscriber.
     *
     * @param agent subscriber agent
     */
    public void subscribe(Agent agent) {
        if (!subs.contains(agent)) {
            subs.add(agent);
        }
    }

    /**
     * Remove an Agent from subscribers list.
     *
     * @param agent subscriber to remove
     */
    public void unsubscribe(Agent agent) {
        subs.remove(agent);
    }

    /**
     * Publish a message to all subscribers.
     * Each subscriber receives callback().
     *
     * @param msg message to publish
     */





    public void publish(Message msg) {
        lastMessage = msg;

        for (Agent agent : new ArrayList<>(subs)) {
            agent.callback(name, msg);
        }
    }

    /**
     * Add an Agent to publishers list.
     *
     * @param agent publisher agent
     */
    public void addPublisher(Agent agent) {
        if (!pubs.contains(agent)) {
            pubs.add(agent);
        }
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    /**
     * Remove an Agent from publishers list.
     *
     * @param agent publisher to remove
     */
    public void removePublisher(Agent agent) {
        pubs.remove(agent);
    }


}