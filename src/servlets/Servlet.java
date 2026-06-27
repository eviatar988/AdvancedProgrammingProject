package servlets;

import java.io.IOException;
import java.io.OutputStream;
import server.RequestParser.RequestInfo;
/**
 * Represents an HTTP servlet that can process client requests.
 *
 * <p>Applications using the HTTP server should implement this interface
 * and register the servlet using
 * {@link server.HTTPServer#addServlet(String, String, Servlet)}.
 *
 * <p>Each servlet is responsible for handling a specific HTTP method
 * and URI prefix. When a matching request arrives, the server invokes
 * {@link #handle(RequestInfo, OutputStream)}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * HTTPServer server = new MyHTTPServer(8080, 5);
 *
 * server.addServlet("GET", "/hello", new HelloServlet());
 *
 * server.start();
 * }</pre>
 */
public interface Servlet {
    /**
     * Handles a single HTTP request.
     *
     * <p>The parsed request information is provided through a
     * {@link RequestInfo} object. The servlet should generate a valid
     * HTTP response and write it directly to the supplied output stream.
     *
     * @param ri parsed HTTP request information
     * @param toClient output stream connected to the client
     * @throws IOException if an I/O error occurs while writing the response
     */
    void handle(RequestInfo ri, OutputStream toClient) throws IOException;
    /**
     * Releases any resources used by the servlet.
     *
     * <p>This method is called when the HTTP server shuts down.
     * Servlets that do not allocate external resources may simply
     * implement an empty method.
     *
     * @throws IOException if an error occurs while releasing resources
     */
    void close() throws IOException;

}
