package configs;

import graph.Agent;
import graph.ParallelAgent;
import graph.Topic;
import graph.TopicManagerSingleton;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic configuration that creates agents from a text file.
 *
 * The file format is very simple: every agent is described by 3 lines.
 * First line is the full class name, second line is the topics to subscribe to,
 * and third line is the topics to publish to.
 */
public class GenericConfig implements Config {

    /** Default queue size for each ParallelAgent. */
    private static final int DEFAULT_PARALLEL_CAPACITY = 100;

    /** Configuration file path. */
    private String confFile;

    /** The agents that were created and wrapped. */
    private final List<AgentRecord> agents;

    /**
     * Small helper class so we can close and disconnect the wrapper correctly.
     */
    private static class AgentRecord {
        final Agent realAgent;
        final ParallelAgent parallelAgent;
        final String[] subs;
        final String[] pubs;

        AgentRecord(Agent realAgent, ParallelAgent parallelAgent, String[] subs, String[] pubs) {
            this.realAgent = realAgent;
            this.parallelAgent = parallelAgent;
            this.subs = subs;
            this.pubs = pubs;
        }
    }

    /**
     * Creates an empty generic config.
     */
    public GenericConfig() {
        this.confFile = null;
        this.agents = new ArrayList<>();
    }

    /**
     * Sets the path of the configuration file.
     *
     * @param confFile path to the config file
     */
    public void setConfFile(String confFile) {
        if (confFile == null || confFile.trim().isEmpty()) {
            throw new IllegalArgumentException("configuration file cannot be empty");
        }
        this.confFile = confFile.trim();
    }

    /**
     * Creates all agents from the configuration file.
     *
     * Each real agent is created using reflection, then it is wrapped with a
     * ParallelAgent. Because the real agent subscribes in its constructor, this
     * method replaces the real agent in the topics with the parallel wrapper.
     */
    @Override
    public void create() {
        if (confFile == null || confFile.trim().isEmpty()) {
            throw new IllegalStateException("configuration file was not set");
        }

        close();

        List<String> lines = readConfigLines();
        if (lines.size() % 3 != 0) {
            throw new IllegalArgumentException("configuration file must contain 3 lines per agent");
        }

        for (int i = 0; i < lines.size(); i += 3) {
            String className = lines.get(i).trim();
            String[] subs = parseTopics(lines.get(i + 1));
            String[] pubs = parseTopics(lines.get(i + 2));

            Agent realAgent = createAgent(className, subs, pubs);
            ParallelAgent parallelAgent = new ParallelAgent(realAgent, DEFAULT_PARALLEL_CAPACITY);

            replaceRealAgentWithParallel(realAgent, parallelAgent, subs, pubs);
            agents.add(new AgentRecord(realAgent, parallelAgent, subs, pubs));
        }
    }

    /**
     * @return configuration name
     */
    @Override
    public String getName() {
        return "Generic Config";
    }

    /**
     * @return configuration version
     */
    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * Closes all agents that were created by this config.
     */
    @Override
    public void close() {
        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();

        for (AgentRecord record : new ArrayList<>(agents)) {
            for (String sub : record.subs) {
                if (sub != null && !sub.trim().isEmpty()) {
                    tm.getTopic(sub).unsubscribe(record.parallelAgent);
                }
            }

            for (String pub : record.pubs) {
                if (pub != null && !pub.trim().isEmpty()) {
                    tm.getTopic(pub).removePublisher(record.parallelAgent);
                }
            }

            record.parallelAgent.close();
        }

        agents.clear();
    }

    /**
     * Reads all non-empty lines from the config file.
     *
     * @return lines from the file
     */
    private List<String> readConfigLines() {
        try {
            List<String> rawLines = Files.readAllLines(Paths.get(confFile));
            List<String> lines = new ArrayList<>();

            for (String line : rawLines) {
                if (line != null && !line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }

            return lines;
        } catch (IOException e) {
            throw new IllegalArgumentException("could not read configuration file", e);
        }
    }

    /**
     * Creates an agent by class name using reflection.
     *
     * @param className full class name from the config file
     * @param subs subscribe topics
     * @param pubs publish topics
     * @return created agent
     */
    private Agent createAgent(String className, String[] subs, String[] pubs) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("agent class name cannot be empty");
        }

        try {
            Class<?> clazz = loadClass(className.trim());
            if (!Agent.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(className + " is not an Agent");
            }

            Constructor<?> constructor = clazz.getConstructor(String[].class, String[].class);
            Object obj = constructor.newInstance((Object) subs, (Object) pubs);
            return (Agent) obj;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("agent must have constructor(String[], String[])", e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("could not create agent " + className, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalArgumentException("agent constructor failed", cause);
        }
    }

    /**
     * Loads a class. If the full name does not work, it also tries the same
     * simple class name inside package test. This helps when the file was copied
     * from the example project name.
     *
     * @param className class name from the file
     * @return loaded class
     */
    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            String simpleName = className.substring(className.lastIndexOf('.') + 1);
            try {
                return Class.forName("test." + simpleName);
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException("could not find agent class " + className, ex);
            }
        }
    }

    /**
     * Parses one topic line into an array of topic names.
     *
     * @param line comma separated topic names
     * @return topic names array
     */
    private String[] parseTopics(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new String[0];
        }

        String[] parts = line.split(",");
        List<String> topics = new ArrayList<>();

        for (String part : parts) {
            String topic = part.trim();
            if (!topic.isEmpty()) {
                topics.add(topic);
            }
        }

        return topics.toArray(new String[0]);
    }

    /**
     * Moves subscriptions/publications from the real agent to the parallel one.
     *
     * @param realAgent the agent created by reflection
     * @param parallelAgent the wrapper agent
     * @param subs subscribe topics
     * @param pubs publish topics
     */
    private void replaceRealAgentWithParallel(Agent realAgent, ParallelAgent parallelAgent,
                                              String[] subs, String[] pubs) {
        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();

        for (String sub : subs) {
            if (sub != null && !sub.trim().isEmpty()) {
                Topic topic = tm.getTopic(sub);
                topic.unsubscribe(realAgent);
                topic.subscribe(parallelAgent);
            }
        }

        for (String pub : pubs) {
            if (pub != null && !pub.trim().isEmpty()) {
                Topic topic = tm.getTopic(pub);
                topic.removePublisher(realAgent);
                topic.addPublisher(parallelAgent);
            }
        }
    }
}
