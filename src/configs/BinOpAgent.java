package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;

import java.util.function.BinaryOperator;

/**
 * An Agent that gets two numeric inputs and publishes one numeric output.
 *
 * The operation itself is received as a lambda expression, so the same class
 * can be used for plus, minus, multiply and other binary operations.
 */
public class BinOpAgent implements Agent {

    /**
     * Agent name.
     */
    private final String name;

    /**
     * First input topic name.
     */
    private final String input1;

    /**
     * Second input topic name.
     */
    private final String input2;

    /**
     * Output topic name.
     */
    private final String output;

    /**
     * The binary operation to run on the two inputs.
     */
    private final BinaryOperator<Double> operation;

    /**
     * Last value received from the first input.
     */
    private double x;

    /**
     * Last value received from the second input.
     */
    private double y;

    /**
     * True after close() was called.
     */
    private boolean closed;

    /**
     * Creates a binary operation agent and connects it to the needed topics.
     *
     * The agent subscribes to the two input topics and is added as publisher
     * of the output topic already in the constructor, as required in the PDF.
     *
     * @param name the agent name
     * @param input1 first input topic name
     * @param input2 second input topic name
     * @param output output topic name
     * @param operation binary operation to apply
     * @throws IllegalArgumentException if one of the required values is null
     */
    public BinOpAgent(String name, String input1, String input2,
                      String output, BinaryOperator<Double> operation) {
        if (name == null || input1 == null || input2 == null || output == null || operation == null) {
            throw new IllegalArgumentException("BinOpAgent arguments cannot be null");
        }

        this.name = name;
        this.input1 = input1;
        this.input2 = input2;
        this.output = output;
        this.operation = operation;
        this.closed = false;
        reset();

        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        tm.getTopic(input1).subscribe(this);
        tm.getTopic(input2).subscribe(this);
        tm.getTopic(output).addPublisher(this);
    }

    /**
     * @return the agent name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Resets the saved input values to zero.
     */
    @Override
    public void reset() {
        x = 0;
        y = 0;
    }

    /**
     * Gets a message from one of the input topics.
     *
     * If the message contains a valid double value, the relevant input is
     * updated and the result is published to the output topic.
     * Invalid messages are ignored so the agent will not crash on bad input.
     *
     * @param topic the topic that sent the message
     * @param msg the received message
     */
    @Override
    public void callback(String topic, Message msg) {
        if (closed || topic == null || msg == null || Double.isNaN(msg.asDouble)) {
            return;
        }

        if (topic.equals(input1)) {
            x = msg.asDouble;
        } else if (topic.equals(input2)) {
            y = msg.asDouble;
        } else {
            return;
        }

        Double result = operation.apply(x, y);
        if (result != null && !Double.isNaN(result)) {
            TopicManagerSingleton.get().getTopic(output).publish(new Message(result));
        }
    }

    /**
     * Disconnects this agent from its topics.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        tm.getTopic(input1).unsubscribe(this);
        tm.getTopic(input2).unsubscribe(this);
        tm.getTopic(output).removePublisher(this);
    }
}
