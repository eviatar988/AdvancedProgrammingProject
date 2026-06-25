package server;

import java.io.Closeable;
import servlets.Servlet;

/**
 * Interface for the simple HTTP server in the project.
 */
public interface HTTPServer extends Closeable {
    void addServlet(String httpCommanmd, String uri, Servlet s);
    void removeServlet(String httpCommanmd, String uri);
    void start();
}
