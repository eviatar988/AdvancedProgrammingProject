package servlets;

import server.RequestParser.RequestInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servlet that loads static HTML files from a folder.
 */
public class HtmlLoader implements Servlet {
    private final Path htmlDir;

    /**
     * @param htmlDir directory that contains the html files
     */
    public HtmlLoader(String htmlDir) {
        if (htmlDir == null || htmlDir.trim().isEmpty()) {
            throw new IllegalArgumentException("html directory cannot be empty");
        }
        this.htmlDir = Paths.get(htmlDir).toAbsolutePath().normalize();
    }

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        String fileName = getRequestedFile(ri);
        Path file = htmlDir.resolve(fileName).normalize();

        if (!file.startsWith(htmlDir) || !Files.isRegularFile(file)) {
            HttpUtil.writeNotFound(toClient, "HTML file not found: " + fileName);
            return;
        }

        HttpUtil.writeHtml(toClient, Files.readString(file));
    }

    /** Extracts the requested file name from the URI segments. */
    private String getRequestedFile(RequestInfo ri) {
        String[] segments = ri.getUriSegments();
        if (segments.length == 0) {
            return "index.html";
        }
        String last = segments[segments.length - 1];
        if (last == null || last.trim().isEmpty() || "app".equals(last)) {
            return "index.html";
        }
        return last.trim();
    }

    @Override
    public void close() {
        // no resources to close
    }
}
