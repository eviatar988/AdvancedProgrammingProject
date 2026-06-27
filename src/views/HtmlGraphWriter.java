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
        lines.add("<style>");
        lines.add("body{margin:0;font-family:Arial;background:#eef2f7}");
        lines.add(".wrap{padding:20px;text-align:center}");
        lines.add("svg{background:white;border:1px solid #ccd3dd;border-radius:14px;box-shadow:0 2px 10px #0002}");
        lines.add(".topic{fill:#9ae6b4;stroke:#2f855a;stroke-width:2}");
        lines.add(".agent{fill:#bee3f8;stroke:#2b6cb0;stroke-width:2}");
        lines.add(".edge{stroke:#2d3748;stroke-width:2.2;fill:none;marker-end:url(#arrow)}");
        lines.add(".label{font-size:13px;text-anchor:middle;dominant-baseline:middle}");
        lines.add(".value{font-size:14px;font-weight:bold;text-anchor:middle}");
        lines.add(".title{font-size:22px;font-weight:bold}");
        lines.add("</style></head><body><div class='wrap'>");

        lines.add("<svg width='1250' height='620' viewBox='0 0 1250 620'>");

        lines.add("<defs>");
        lines.add("<marker id='arrow' markerWidth='12' markerHeight='12' refX='10' refY='3' orient='auto' markerUnits='strokeWidth'>");
        lines.add("<path d='M0,0 L0,6 L10,3 z' fill='#2d3748'/>");
        lines.add("</marker>");
        lines.add("</defs>");

        lines.add("<text x='450' y='35' class='title'>Computational Graph</text>");

        for (Node from : nodes) {
            double[] p1 = pos.get(from);
            if (p1 == null) {
                continue;
            }

            for (Node to : from.getEdges()) {
                double[] p2 = pos.get(to);
                if (p2 == null) {
                    continue;
                }

                double[] edge = shortenEdge(p1[0], p1[1], p2[0], p2[1], radiusOf(from), radiusOf(to));

                lines.add("<line class='edge' x1='" + edge[0] + "' y1='" + edge[1]
                        + "' x2='" + edge[2] + "' y2='" + edge[3] + "'/>");
            }
        }

        for (Node node : nodes) {
            double[] p = pos.get(node);
            if (p == null) {
                continue;
            }

            String name = node.getName();
            boolean isTopic = name != null && name.startsWith("T");
            String shown = name == null ? "" : name.substring(Math.min(1, name.length()));

            if (isTopic) {
                lines.add("<rect class='topic' x='" + (p[0] - 32) + "' y='" + (p[1] - 22)
                        + "' width='64' height='44' rx='5'/>");
                lines.add("<text class='label' x='" + p[0] + "' y='" + p[1] + "'>" + esc(shown) + "</text>");

                String val = topicValue(shown);
                if (!val.isEmpty()) {
                    lines.add("<text class='value' x='" + p[0] + "' y='" + (p[1] - 35)
                            + "'>" + esc(val) + "</text>");
                }
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

    /**
     * Gives every node a position by columns/layers instead of a circle.
     * This makes the graph flow from left to right.
     */
    private static Map<Node, double[]> createPositions(List<Node> nodes) {
        Map<Node, double[]> pos = new HashMap<>();

        Map<String, double[]> fixed = new HashMap<>();

        fixed.put("TA", new double[]{90, 210});
        fixed.put("TB", new double[]{90, 390});

        fixed.put("Aplus", new double[]{230, 210});
        fixed.put("Aminus", new double[]{230, 390});

        fixed.put("TC", new double[]{370, 210});
        fixed.put("TD", new double[]{370, 390});

        fixed.put("Amul", new double[]{510, 300});

        fixed.put("TE", new double[]{650, 300});

        fixed.put("Adiv", new double[]{790, 300});

        fixed.put("TF", new double[]{930, 300});

        fixed.put("Ainc", new double[]{1070, 300});

        fixed.put("TG", new double[]{1210, 300});

        for (Node node : nodes) {
            String name = node.getName();

            if (fixed.containsKey(name)) {
                pos.put(node, fixed.get(name));
            }
        }

        // fallback for unexpected nodes
        int extraIndex = 0;
        for (Node node : nodes) {
            if (!pos.containsKey(node)) {
                pos.put(node, new double[]{90 + extraIndex * 120, 560});
                extraIndex++;
            }
        }

        return pos;
    }

    /** Returns approximate node radius for shortening arrow edges. */
    private static double radiusOf(Node node) {
        String name = node.getName();
        boolean isTopic = name != null && name.startsWith("T");

        if (isTopic) {
            return 38;
        }

        return 40;
    }

    /** Shortens a line so the arrow starts and ends near the node borders. */
    private static double[] shortenEdge(double x1, double y1, double x2, double y2,
                                        double startRadius, double endRadius) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);

        if (len == 0) {
            return new double[]{x1, y1, x2, y2};
        }

        double ux = dx / len;
        double uy = dy / len;

        double sx = x1 + ux * startRadius;
        double sy = y1 + uy * startRadius;
        double ex = x2 - ux * endRadius;
        double ey = y2 - uy * endRadius;

        return new double[]{sx, sy, ex, ey};
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