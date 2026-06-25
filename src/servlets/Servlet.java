package servlets;

import java.io.IOException;
import java.io.OutputStream;
import server.RequestParser.RequestInfo;
/**
 * Small servlet interface used by the local HTTP server.
 */
public interface Servlet {
    /**
     * Handles a parsed HTTP request and writes the response.
     *
     * @param ri parsed request info
     * @param toClient output stream to the browser
     * @throws IOException if writing fails
     */
    void handle(RequestInfo ri, OutputStream toClient) throws IOException;
    /**
     * Releases servlet resources.
     *
     * @throws IOException if closing fails
     */
    void close() throws IOException;

}
