package graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton class that manages all topics.
 *
 * Only one manager exists in the program.
 */
public class TopicManagerSingleton {

    /**
     *  Inner class for the singleton instance.
     */
    public static class TopicManager {

        /**
         * Stores all topics by name.
         */
        private final ConcurrentHashMap<String, Topic> topics;

        /**
         * Private constructor.
         */
        private TopicManager() {
            topics = new ConcurrentHashMap<>();
        }

        /**
         * The single instance of TopicManager.
         */
        private static final TopicManager instance = new TopicManager();

        /**
         * Returns a topic if it exists,
         * otherwise creates a new one.
         *
         * @param name topic name
         * @return topic object
         */
        public Topic getTopic(String name) {

            if (name == null) {
                throw new IllegalArgumentException("Topic name cannot be null");
            }

            return topics.computeIfAbsent(name, Topic::new);
        }

        /**
         * @return collection of all existing Topics
         */
        public Collection<Topic> getTopics() {
            return topics.values();
        }

        /**
         * Removes all Topics from the manager.
         */
        public void clear() {
            topics.clear();
        }
    }

    /**
     * Returns the singleton manager.
     *
     * @return TopicManager instance
     */
    public static TopicManager get() {
        return TopicManager.instance;
    }
}