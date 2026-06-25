package views;


import graph.Graph;
import graph.Node;
import graph.Topic;
import graph.Message;
import graph.TopicManagerSingleton;
import servlets.HttpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates an HTML view for the computational graph.
 */
public class HtmlGraphWriter {

    /**
     * Returns HTML lines that draw the given graph.
     * Topics are rectangles and agents are circles, as required in the PDF.
     *
     * @param graph graph to draw
     * @return HTML lines
     */
    public static List<String> getGraphHTML(Graph graph) {
        List<String> lines = new ArrayList<>();
        List<Node> nodes = graph == null ? new ArrayList<>() : new ArrayList<>(graph);
        Map<Node, double[]> pos = createPositions(nodes);

        lines.add("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Graph</title>");
        lines.add("<style>body{margin:0;font-family:Arial;background:#eef2f7}.wrap{padding:20px;text-align:center}");
        lines.add("svg{background:white;border:1px solid #ccd3dd;border-radius:14px;box-shadow:0 2px 10px #0002}");
        lines.add(".topic{fill:#9ae6b4;stroke:#2f855a;stroke-width:2}.agent{fill:#bee3f8;stroke:#2b6cb0;stroke-width:2}.edge{stroke:#2d3748;stroke-width:1.8;fill:none}.label{font-size:13px;text-anchor:middle;dominant-baseline:middle}.value{font-size:14px;font-weight:bold;text-anchor:middle}.title{font-size:22px;font-weight:bold}</style></head><body><div class='wrap'>");
        lines.add("<svg width='900' height='620' viewBox='0 0 900 620'>");
        lines.add("<defs><marker id='arrow' markerWidth='10' markerHeight='10' refX='8' refY='3' orient='auto'><path d='M0,0 L0,6 L9,3 z' fill='#2d3748'/></marker></defs>");
        lines.add("<text x='450' y='35' class='title'>Computational Graph</text>");

        for (Node from : nodes) {
            double[] p1 = pos.get(from);
            if (p1 == null) continue;
            for (Node to : from.getEdges()) {
                double[] p2 = pos.get(to);
                if (p2 != null) {
                    lines.add("<line class='edge' x1='" + p1[0] + "' y1='" + p1[1] + "' x2='" + p2[0] + "' y2='" + p2[1] + "' marker-end='url(#arrow)'/>");
                }
            }
        }

        for (Node node : nodes) {
            double[] p = pos.get(node);
            if (p == null) continue;
            String name = node.getName();
            boolean isTopic = name != null && name.startsWith("T");
            String shown = name == null ? "" : name.substring(Math.min(1, name.length()));
            if (isTopic) {
                lines.add("<rect class='topic' x='" + (p[0] - 32) + "' y='" + (p[1] - 22) + "' width='64' height='44' rx='5'/>");
                lines.add("<text class='label' x='" + p[0] + "' y='" + p[1] + "'>" + esc(shown) + "</text>");
                String val = topicValue(shown);
                if (!val.isEmpty()) lines.add("<text class='value' x='" + p[0] + "' y='" + (p[1] - 35) + "'>" + esc(val) + "</text>");
            } else {
                lines.add("<circle class='agent' cx='" + p[0] + "' cy='" + p[1] + "' r='36'/>");
                lines.add("<text class='label' x='" + p[0] + "' y='" + p[1] + "'>" + esc(shown) + "</text>");
            }
        }

        if (nodes.isEmpty()) {
            lines.add("<text x='450' y='310' class='title'>No graph loaded</text>");
        }

        lines.add("</svg></div></body></html>");
        return lines;
    }

    /** Gives every node a position on a circle. */
    private static Map<Node, double[]> createPositions(List<Node> nodes) {
        Map<Node, double[]> pos = new HashMap<>();
        int n = Math.max(nodes.size(), 1);
        double cx = 450, cy = 320, r = 220;
        for (int i = 0; i < nodes.size(); i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
            pos.put(nodes.get(i), new double[]{cx + r * Math.cos(angle), cy + r * Math.sin(angle)});
        }
        return pos;
    }

    /** Gets the last value of a topic. */
    private static String topicValue(String topicName) {
        for (Topic topic : TopicManagerSingleton.get().getTopics()) {
            if (topic.name.equals(topicName)) {
                Message msg = topic.getLastMessage();
                return msg == null ? "" : msg.asText;
            }
        }
        return "";
    }

    private static String esc(String s) {
        return HttpUtil.escape(s);
    }
}
