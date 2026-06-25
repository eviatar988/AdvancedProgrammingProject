package server;

import servlets.Servlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A small generic HTTP server for the project.
 *
 * The server listens on a port, parses every client request with RequestParser,
 * finds the matching servlet by command and URI prefix, and lets the servlet
 * write the answer back to the client.
 */
public class MyHTTPServer extends Thread implements HTTPServer {

    /** How long accept() waits before checking if the server should close. */
    private static final int ACCEPT_TIMEOUT_MS = 1000;

    /** Server listening port. */
    private final int port;

    /** Maximum amount of worker threads. */
    private final int nThreads;

    /** GET URI prefixes to servlets. */
    private final ConcurrentHashMap<String, Servlet> getServlets;

    /** POST URI prefixes to servlets. */
    private final ConcurrentHashMap<String, Servlet> postServlets;

    /** DELETE URI prefixes to servlets. */
    private final ConcurrentHashMap<String, Servlet> deleteServlets;

    /** Pool that handles clients in parallel. */
    private final ExecutorService executor;

    /** The socket that waits for clients. */
    private ServerSocket serverSocket;

    /** True after close() was called. */
    private volatile boolean closed;

    /** True after start() was called once. */
    private volatile boolean started;

    /**
     * Creates a new HTTP server.
     *
     * @param port port to listen on
     * @param nThreads maximum number of worker threads
     * @throws IllegalArgumentException if port or nThreads are illegal
     */
    public MyHTTPServer(int port, int nThreads) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (nThreads <= 0) {
            throw new IllegalArgumentException("number of threads must be positive");
        }

        this.port = port;
        this.nThreads = nThreads;
        this.getServlets = new ConcurrentHashMap<>();
        this.postServlets = new ConcurrentHashMap<>();
        this.deleteServlets = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(nThreads);
        this.closed = false;
        this.started = false;
    }

    /**
     * Adds a servlet for a command and URI prefix.
     *
     * @param httpCommanmd HTTP command, for example GET, POST or DELETE
     * @param uri URI prefix
     * @param s servlet to run
     */
    @Override
    public void addServlet(String httpCommanmd, String uri, Servlet s) {
        if (httpCommanmd == null || uri == null || s == null) {
            return;
        }

        Map<String, Servlet> map = getMap(httpCommanmd);
        if (map == null) {
            return;
        }

        map.put(normalizeUri(uri), s);
    }

    /**
     * Removes a servlet from a command and URI prefix.
     *
     * @param httpCommanmd HTTP command
     * @param uri URI prefix
     */
    @Override
    public void removeServlet(String httpCommanmd, String uri) {
        if (httpCommanmd == null || uri == null) {
            return;
        }

        Map<String, Servlet> map = getMap(httpCommanmd);
        if (map != null) {
            map.remove(normalizeUri(uri));
        }
    }

    /**
     * Starts the server thread. Calling it more than once is ignored.
     */
    @Override
    public synchronized void start() {
        if (started || closed) {
            return;
        }
        started = true;
        super.start();
    }

    /**
     * The main server loop.
     *
     * It waits for clients. Every accepted socket is sent to the worker pool,
     * so several clients can be handled at the same time.
     */
    @Override
    public void run() {
        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;
            ss.setSoTimeout(ACCEPT_TIMEOUT_MS);

            while (!closed) {
                try {
                    Socket client = ss.accept();
                    executor.execute(() -> handleClient(client));
                } catch (SocketTimeoutException e) {
                    // normal case: wake up once a second and check closed
                } catch (IOException e) {
                    if (!closed) {
                        // bad client or socket problem, continue running if possible
                    }
                }
            }
        } catch (IOException e) {
            // If the server cannot open the port there is nothing more to do here.
        } finally {
            closed = true;
            shutdownExecutor();
        }
    }

    /**
     * Closes the server, all registered servlets and the worker pool.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // close should be safe to call, so we ignore this
        }

        closeServlets(getServlets);
        closeServlets(postServlets);
        closeServlets(deleteServlets);
        shutdownExecutor();
    }

    /**
     * Handles one client socket.
     *
     * @param client client socket
     */
    private void handleClient(Socket client) {
        try (Socket socket = client;
             BufferedReader fromClient = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream toClient = socket.getOutputStream()) {

            RequestParser.RequestInfo ri = RequestParser.parseRequest(fromClient);
            Servlet servlet = findServlet(ri.getHttpCommand(), ri.getUri());

            if (servlet != null) {
                servlet.handle(ri, toClient);
            } else {
                writeNotFound(toClient);
            }

            toClient.flush();
        } catch (IOException e) {
            // A single client error should not stop the server.
        }
    }

    /**
     * Finds the servlet with the longest matching URI prefix.
     *
     * @param command HTTP command
     * @param uri request URI
     * @return matching servlet, or null if no match exists
     */
    private Servlet findServlet(String command, String uri) {
        Map<String, Servlet> map = getMap(command);
        if (map == null || map.isEmpty()) {
            return null;
        }

        String cleanUri = normalizeUri(removeQuery(uri));
        String bestUri = null;
        Servlet bestServlet = null;

        for (Map.Entry<String, Servlet> entry : map.entrySet()) {
            String prefix = entry.getKey();
            if (matchesPrefix(cleanUri, prefix)) {
                if (bestUri == null || prefix.length() > bestUri.length()) {
                    bestUri = prefix;
                    bestServlet = entry.getValue();
                }
            }
        }

        return bestServlet;
    }

    /**
     * Returns the servlet map for the command.
     *
     * @param command HTTP command
     * @return command map, or null for unsupported commands
     */
    private Map<String, Servlet> getMap(String command) {
        if (command == null) {
            return null;
        }

        String c = command.trim().toUpperCase();
        if ("GET".equals(c)) {
            return getServlets;
        }
        if ("POST".equals(c)) {
            return postServlets;
        }
        if ("DELETE".equals(c)) {
            return deleteServlets;
        }
        return null;
    }

    /**
     * Normalizes URI prefixes so matching will be stable.
     *
     * @param uri URI to normalize
     * @return normalized URI
     */
    private String normalizeUri(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return "/";
        }

        String clean = removeQuery(uri.trim());
        if (!clean.startsWith("/")) {
            clean = "/" + clean;
        }
        while (clean.length() > 1 && clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    /**
     * Removes query parameters from a URI.
     *
     * @param uri full URI
     * @return URI path only
     */
    private String removeQuery(String uri) {
        if (uri == null) {
            return "/";
        }
        int q = uri.indexOf('?');
        return (q >= 0) ? uri.substring(0, q) : uri;
    }

    /**
     * Checks prefix matching for URI routing.
     *
     * @param uri request URI
     * @param prefix servlet prefix
     * @return true if the servlet should handle this URI
     */
    private boolean matchesPrefix(String uri, String prefix) {
        if ("/".equals(prefix)) {
            return true;
        }
        return uri.equals(prefix) || uri.startsWith(prefix + "/") || uri.startsWith(prefix);
    }

    /**
     * Writes a small HTTP 404 answer when no servlet was found.
     *
     * @param out output stream to client
     * @throws IOException if writing fails
     */
    private void writeNotFound(OutputStream out) throws IOException {
        String body = "404 Not Found";
        String response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" + body;
        out.write(response.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Closes every unique servlet in a map.
     *
     * @param map servlet map
     */
    private void closeServlets(Map<String, Servlet> map) {
        for (Servlet servlet : new java.util.HashSet<>(map.values())) {
            try {
                if (servlet != null) {
                    servlet.close();
                }
            } catch (IOException e) {
                // keep closing the other servlets
            }
        }
        map.clear();
    }

    /**
     * Stops the worker pool and waits a little for it.
     */
    private void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @return maximum number of worker threads
     */
    public int getNThreads() {
        return nThreads;
    }
}
