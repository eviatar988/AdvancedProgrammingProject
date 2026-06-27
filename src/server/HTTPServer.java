package server;

import java.io.Closeable;
import servlets.Servlet;

/**
 * A lightweight HTTP server interface.
 *
 * <p>The server allows applications to register servlets that handle
 * specific HTTP methods and URI prefixes.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * HTTPServer server = new MyHTTPServer(8080, 5);
 *
 * server.addServlet("GET", "/hello", new HelloServlet());
 * server.addServlet("POST", "/upload", new UploadServlet());
 *
 * server.start();
 * }</pre>
 *
 * <p>The server dispatches incoming HTTP requests to the matching servlet.
 * When the application terminates, {@link #close()} should be called
 * to release all server resources.
 */
public interface HTTPServer extends Closeable {
    /**
     * Registers a servlet for a specific HTTP method and URI prefix.
     *
     * <p>Whenever a request matches both the HTTP method and the URI,
     * the given servlet will handle the request.
     *
     * @param httpCommanmd the HTTP method (for example: GET, POST or DELETE)
     * @param uri the URI prefix handled by the servlet
     * @param s the servlet instance that will process matching requests
     */
    void addServlet(String httpCommanmd, String uri, Servlet s);
    /**
     * Removes a previously registered servlet.
     *
     * <p>After removal, requests matching the specified method and URI
     * will no longer be handled.
     *
     * @param httpCommanmd the HTTP method
     * @param uri the URI prefix of the servlet to remove
     */
    void removeServlet(String httpCommanmd, String uri);
    /**
     * Starts the HTTP server.
     *
     * <p>The server begins listening for client connections on the configured
     * port. Incoming requests are processed concurrently using the server's
     * worker thread pool.
     */
    void start();
}
