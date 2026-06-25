package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

    public static void writeHtml(OutputStream out, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

        String header =
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "Content-Length: " + bytes.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";

        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
    }

    public static void writeError(OutputStream out, String message) throws IOException {
        String html =
                "<html><body><h2>Error</h2><p>" +
                        escapeHtml(message) +
                        "</p></body></html>";

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

        String header =
                "HTTP/1.1 500 Internal Server Error\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "Content-Length: " + bytes.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";

        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
    }

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static void writeNotFound(OutputStream out, String message) throws IOException {
        String html =
                "<html><body><h2>404 Not Found</h2><p>" +
                        escapeHtml(message) +
                        "</p></body></html>";

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

        String header =
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "Content-Length: " + bytes.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";

        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
    }


    public static String escape(String text) {
        return escapeHtml(text);
    }
}