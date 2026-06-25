package servlets;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import server.RequestParser.RequestInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Servlet for /publish.
 *
 * It receives a topic name and a message, publishes the message to that topic,
 * and returns an HTML table with the last value of every topic.
 */
public class TopicDisplayer implements Servlet {

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        Map<String, String> params = ri.getParameters();
        String topicName = first(params, "topic", "topicName", "name", "Topic");
        String message = first(params, "message", "msg", "value", "Message");

        if (topicName != null && !topicName.trim().isEmpty()) {
            TopicManagerSingleton.get().getTopic(topicName.trim()).publish(new Message(message == null ? "" : message));
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        HttpUtil.writeHtml(toClient, buildTopicsTable());
    }

    /** Builds the HTML table showing all topics and their last values. */
    private String buildTopicsTable() {
        List<Topic> topics = new ArrayList<>(TopicManagerSingleton.get().getTopics());
        topics.sort(Comparator.comparing(t -> t.name));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
                .append("<style>body{font-family:Arial;margin:15px;background:#fafafa}")
                .append("table{border-collapse:collapse;width:100%;background:white}")
                .append("th,td{border:1px solid #ccc;padding:8px;text-align:left}")
                .append("th{background:#2d3748;color:white}.empty{color:#777}</style></head><body>")
                .append("<h3>Topics values</h3><table><tr><th>Topic</th><th>Last value</th></tr>");

        for (Topic topic : topics) {
            Message last = topic.getLastMessage();
            html.append("<tr><td>").append(HttpUtil.escape(topic.name)).append("</td><td>")
                    .append(last == null ? "<span class='empty'>-</span>" : HttpUtil.escape(last.asText))
                    .append("</td></tr>");
        }

        html.append("</table></body></html>");
        return html.toString();
    }

    /** Returns the first existing parameter from several possible names. */
    private String first(Map<String, String> params, String... names) {
        for (String name : names) {
            if (params.containsKey(name)) {
                return params.get(name);
            }
        }
        return null;
    }

    @Override
    public void close() {
        // no resources to close
    }
}
