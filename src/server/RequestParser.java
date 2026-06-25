package server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small parser for simple HTTP requests.
 *
 * The parser reads the request line, the headers, query parameters from the URI,
 * optional form/file parameters after the headers, and then the content bytes.
 * It is not a full HTTP parser, only what we need for the project.
 */
public class RequestParser {

    /**
     * Holds the important data that was taken from the HTTP request.
     */
    public static class RequestInfo {
        private final String httpCommand;
        private final String uri;
        private final String[] uriSegments;
        private final Map<String, String> parameters;
        private final byte[] content;

        /**
         * Creates a new parsed request object.
         *
         * @param httpCommand HTTP command, for example GET or POST
         * @param uri full URI from the first request line
         * @param uriSegments URI path split by '/'
         * @param parameters parameters from the query and from the request body header
         * @param content request content bytes
         */
        public RequestInfo(String httpCommand, String uri, String[] uriSegments,
                           Map<String, String> parameters, byte[] content) {
            this.httpCommand = (httpCommand == null) ? "" : httpCommand;
            this.uri = (uri == null) ? "" : uri;
            this.uriSegments = (uriSegments == null) ? new String[0] : uriSegments.clone();
            this.parameters = new LinkedHashMap<>();
            if (parameters != null) {
                this.parameters.putAll(parameters);
            }
            this.content = (content == null) ? new byte[0] : content.clone();
        }

        /**
         * @return the HTTP command
         */
        public String getHttpCommand() {
            return httpCommand;
        }

        /**
         * @return the URI from the request line
         */
        public String getUri() {
            return uri;
        }

        /**
         * @return the URI path split into segments
         */
        public String[] getUriSegments() {
            return uriSegments.clone();
        }

        /**
         * @return map of all parameters
         */
        public Map<String, String> getParameters() {
            return Collections.unmodifiableMap(parameters);
        }

        /**
         * @return request content bytes
         */
        public byte[] getContent() {
            return content.clone();
        }
    }

    /**
     * Parses a simple HTTP request from a BufferedReader.
     *
     * @param reader source of the request
     * @return parsed request information
     * @throws IOException if reading fails
     */
    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {
        if (reader == null) {
            return new RequestInfo("", "", new String[0], new LinkedHashMap<>(), new byte[0]);
        }

        String requestLine = reader.readLine();
        while (requestLine != null && requestLine.trim().isEmpty()) {
            requestLine = reader.readLine();
        }

        if (requestLine == null) {
            return new RequestInfo("", "", new String[0], new LinkedHashMap<>(), new byte[0]);
        }

        String[] firstLineParts = requestLine.trim().split("\\s+");
        String command = firstLineParts.length > 0 ? firstLineParts[0].toUpperCase() : "";
        String uri = firstLineParts.length > 1 ? firstLineParts[1] : "";

        Map<String, String> parameters = new LinkedHashMap<>();
        String path = uri;
        int queryIndex = uri.indexOf('?');
        if (queryIndex >= 0) {
            path = uri.substring(0, queryIndex);
            parseParameterString(uri.substring(queryIndex + 1), parameters);
        }

        int contentLength = -1;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.trim().isEmpty()) {
                break;
            }

            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Content-Length")) {
                try {
                    contentLength = Integer.parseInt(line.substring(colon + 1).trim());
                } catch (NumberFormatException e) {
                    contentLength = -1;
                }
            }
        }

        ByteArrayOutputStream content = new ByteArrayOutputStream();
        readAfterHeaders(reader, parameters, content, contentLength);

        return new RequestInfo(command, uri, splitPath(path), parameters, content.toByteArray());
    }

    /**
     * Reads the part that appears after the regular HTTP headers.
     *
     * @param reader request reader
     * @param parameters parameters map to update
     * @param content content output
     * @param contentLength Content-Length value, or -1 if missing/bad
     * @throws IOException if reading fails
     */
    private static void readAfterHeaders(BufferedReader reader, Map<String, String> parameters,
                                         ByteArrayOutputStream content, int contentLength) throws IOException {
        if (!reader.ready()) {
            return;
        }

        reader.mark(8192);
        String first = reader.readLine();
        if (first == null) {
            return;
        }

        boolean hasExtraParameters = looksLikeParameter(first);
        if (hasExtraParameters) {
            parseLooseParameter(first, parameters);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.trim().isEmpty()) {
                    break;
                }
                parseLooseParameter(line, parameters);
            }
            readContentUntilEnd(reader, content, contentLength);
        } else {
            reader.reset();
            if (contentLength > 0) {
                readExactChars(reader, content, contentLength);
            } else {
                readContentUntilEnd(reader, content, contentLength);
            }
        }
    }

    /**
     * Reads content until an empty line, EOF, or reader not ready.
     *
     * @param reader request reader
     * @param content content output
     * @param contentLength optional maximum length
     * @throws IOException if reading fails
     */
    private static void readContentUntilEnd(BufferedReader reader, ByteArrayOutputStream content,
                                            int contentLength) throws IOException {
        int read = 0;
        String line;
        while ((contentLength < 0 || read < contentLength) && reader.ready() && (line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }

            byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
            int allowed = (contentLength < 0) ? bytes.length : Math.min(bytes.length, contentLength - read);
            content.write(bytes, 0, allowed);
            read += allowed;

            if (contentLength < 0 || read < contentLength) {
                content.write('\n');
                read++;
            }
        }
    }

    /**
     * Reads exactly len characters when possible.
     *
     * @param reader request reader
     * @param content content output
     * @param len wanted length
     * @throws IOException if reading fails
     */
    private static void readExactChars(BufferedReader reader, ByteArrayOutputStream content, int len) throws IOException {
        if (len <= 0) {
            return;
        }

        char[] buffer = new char[len];
        int total = 0;
        while (total < len) {
            int count = reader.read(buffer, total, len - total);
            if (count == -1) {
                break;
            }
            total += count;
        }

        content.write(new String(buffer, 0, total).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Splits a URI path into clean parts.
     *
     * @param path URI path without query
     * @return path segments
     */
    private static String[] splitPath(String path) {
        if (path == null || path.trim().isEmpty() || path.equals("/")) {
            return new String[0];
        }

        String clean = path;
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }

        if (clean.isEmpty()) {
            return new String[0];
        }

        String[] raw = clean.split("/");
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        for (String part : raw) {
            if (part != null && !part.trim().isEmpty()) {
                parts.add(decode(part.trim()));
            }
        }
        return parts.toArray(new String[0]);
    }

    /**
     * Parses a query string like a=1&b=2.
     *
     * @param query query string
     * @param parameters parameters map to update
     */
    private static void parseParameterString(String query, Map<String, String> parameters) {
        if (query == null || query.isEmpty()) {
            return;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            parseLooseParameter(pair, parameters);
        }
    }

    /**
     * Parses one key=value parameter. Missing value becomes empty string.
     *
     * @param text parameter text
     * @param parameters parameters map to update
     */
    private static void parseLooseParameter(String text, Map<String, String> parameters) {
        if (text == null) {
            return;
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        int eq = trimmed.indexOf('=');
        String key = (eq >= 0) ? trimmed.substring(0, eq).trim() : trimmed;
        String value = (eq >= 0) ? trimmed.substring(eq + 1).trim() : "";

        if (key.isEmpty()) {
            return;
        }

        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }

        parameters.put(decode(key), decode(value));
    }

    /**
     * Checks if a line probably contains a parameter and not raw content.
     *
     * @param line line to check
     * @return true if the line looks like key=value
     */
    private static boolean looksLikeParameter(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        int eq = trimmed.indexOf('=');
        return eq > 0 && !trimmed.substring(0, eq).contains(" ");
    }

    /**
     * URL-decodes a string safely.
     *
     * @param s string to decode
     * @return decoded string, or original string if decoding fails
     */
    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }
}
