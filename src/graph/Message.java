package graph;

import java.util.Date;

/**
 * Represents a message in the system.
 *
 * The message saves the data in different forms
 * so it can be used easily later.
 */
public class Message {

    /**
     *  Message data as bytes.
     */
    public final byte[] data;

    /**
     *  Message as text.
     */
    public final String asText;

    /**
     * Message as a double value.
     * If it cannot convert, the value is NaN.
     */
    public final double asDouble;

    /**
     * Time the message was created.
     */
    public final Date date;

    /**
     * Creates a message from a string.
     *
     * @param s message text
     */
    public Message(String s) {

        if (s == null) {
            s = "";
        }

        this.data = s.getBytes();
        this.asText = s;
        this.date = new Date();

        double value;
        try {
            value = Double.parseDouble(s);
        } catch (Exception e) {
            value = Double.NaN;
        }

        this.asDouble = value;
    }

    /**
     * Creates a message from bytes.
     *
     * @param data byte array
     */
    public Message(byte[] data) {
        this(new String(data));
    }

    /**
     * Creates a message from a double.
     *
     * @param d number value
     */
    public Message(double d) {
        this("" + d);
    }
}