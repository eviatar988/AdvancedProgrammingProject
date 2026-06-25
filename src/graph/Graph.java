package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a directed graph as a list of nodes.
 *
 * This class also knows how to build the graph from the existing topics in
 * TopicManagerSingleton, according to the exercise instructions.
 */
public class Graph extends ArrayList<Node> {

    /**
     * Checks if any component in the graph contains a cycle.
     *
     * @return true if at least one node can reach a cycle, otherwise false
     */
    public boolean hasCycles() {
        for (Node node : this) {
            if (node != null && node.hasCycles()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds this graph from the topics that currently exist in the topic manager.
     *
     * A Topic becomes a node named with prefix T, for example TA.
     * An Agent becomes a node named with prefix A, for example Aplus.
     * Edges are created from Topic to subscribers, and from publishers to Topic.
     */
    public void createFromTopics() {
        clear();

        Map<String, Node> nodes = new HashMap<>();

        for (Topic topic : TopicManagerSingleton.get().getTopics()) {
            if (topic == null) {
                continue;
            }

            Node topicNode = getOrCreate(nodes, "T" + topic.name);

            for (Agent sub : topic.subs) {
                if (sub == null) {
                    continue;
                }

                Node agentNode = getOrCreate(nodes, "A" + sub.getName());
                topicNode.addEdge(agentNode);
            }

            for (Agent pub : topic.pubs) {
                if (pub == null) {
                    continue;
                }

                Node agentNode = getOrCreate(nodes, "A" + pub.getName());
                agentNode.addEdge(topicNode);
            }
        }
    }

    /**
     * Gets an existing node by name or creates it if it does not exist yet.
     *
     * @param nodes map of names to nodes
     * @param name node name
     * @return the existing or new node
     */
    private Node getOrCreate(Map<String, Node> nodes, String name) {
        Node node = nodes.get(name);

        if (node == null) {
            node = new Node(name);
            nodes.put(name, node);
            add(node);
        }

        return node;
    }
}
