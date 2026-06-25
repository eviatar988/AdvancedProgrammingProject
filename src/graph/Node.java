package graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents one node in a directed graph.
 *
 * A node has a name, a list of outgoing edges and optionally a message.
 * I kept it simple because in this exercise the graph is only a list of nodes.
 */
public class Node {

    /**
     * The node name.
     */
    private String name;

    /**
     * Nodes that this node points to.
     */
    private List<Node> edges;

    /**
     * A message that can be saved on this node.
     */
    private Message message;

    /**
     * Creates a new node with the given name.
     *
     * @param name the node name
     */
    public Node(String name) {
        this.name = (name == null) ? "" : name;
        this.edges = new ArrayList<>();
        this.message = null;
    }

    /**
     * @return the node name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the node name.
     *
     * @param name new node name
     */
    public void setName(String name) {
        this.name = (name == null) ? "" : name;
    }

    /**
     * @return the outgoing edges list
     */
    public List<Node> getEdges() {
        return edges;
    }

    /**
     * Sets the outgoing edges list.
     * If null is received, an empty list is used instead.
     *
     * @param edges new edges list
     */
    public void setEdges(List<Node> edges) {
        this.edges = (edges == null) ? new ArrayList<>() : edges;
    }

    /**
     * @return the message saved in this node
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Sets the node message.
     *
     * @param message new message
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    /**
     * Adds an outgoing edge from this node to another node.
     * Null nodes and duplicate edges are ignored.
     *
     * @param node the node to point to
     */
    public void addEdge(Node node) {
        if (node == null) {
            return;
        }

        if (!edges.contains(node)) {
            edges.add(node);
        }
    }

    /**
     * Checks if there is a cycle that can be reached from this node.
     *
     * The check is done with DFS. The recursion stack is used to notice
     * when we visited the same node again inside the same path.
     *
     * @return true if a cycle exists from this node, otherwise false
     */
    public boolean hasCycles() {
        return hasCycles(new HashSet<>(), new HashSet<>());
    }

    /**
     * Helper method for DFS cycle detection.
     *
     * @param visited nodes that were already checked
     * @param inStack nodes that are currently in the DFS path
     * @return true if a cycle was found
     */
    private boolean hasCycles(Set<Node> visited, Set<Node> inStack) {
        if (inStack.contains(this)) {
            return true;
        }

        if (visited.contains(this)) {
            return false;
        }

        visited.add(this);
        inStack.add(this);

        for (Node next : edges) {
            if (next != null && next.hasCycles(visited, inStack)) {
                return true;
            }
        }

        inStack.remove(this);
        return false;
    }
}
