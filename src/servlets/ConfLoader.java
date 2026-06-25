package servlets;

import configs.Config;
import configs.GenericConfig;
import graph.Graph;
import graph.TopicManagerSingleton;
import server.RequestParser.RequestInfo;
import views.HtmlGraphWriter;
import views.HtmlGraphWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servlet for /upload.
 *
 * It saves the uploaded configuration file, creates the GenericConfig,
 * builds a graph from the created topics and returns an HTML drawing of it.
 */
public class ConfLoader implements Servlet {
    private static final Path UPLOAD_DIR = Paths.get("configs");
    private Config currentConfig;

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        try {
            UploadedConfig upload = extractUpload(ri);
            if (upload.content.trim().isEmpty()) {
                HttpUtil.writeError(toClient, "uploaded configuration is empty");
                return;
            }

            Files.createDirectories(UPLOAD_DIR);
            Path saved = UPLOAD_DIR.resolve(cleanFileName(upload.fileName)).normalize();
            if (!saved.startsWith(UPLOAD_DIR.toAbsolutePath().normalize()) && saved.isAbsolute()) {
                saved = UPLOAD_DIR.resolve("config.conf");
            }
            Files.writeString(saved, upload.content, StandardCharsets.UTF_8);

            if (currentConfig != null) {
                currentConfig.close();
            }
            TopicManagerSingleton.get().clear();

            GenericConfig config = new GenericConfig();
            config.setConfFile(saved.toString());
            config.create();
            currentConfig = config;

            Graph graph = new Graph();
            graph.createFromTopics();
            HttpUtil.writeHtml(toClient, String.join("\n", HtmlGraphWriter.getGraphHTML(graph)));
        } catch (Exception e) {
            HttpUtil.writeError(toClient, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /** Holds uploaded filename and content. */
    private static class UploadedConfig {
        final String fileName;
        final String content;

        UploadedConfig(String fileName, String content) {
            this.fileName = fileName;
            this.content = content;
        }
    }

    /** Extracts the uploaded file from multipart, text/plain, or normal body. */
    private UploadedConfig extractUpload(RequestInfo ri) {
        String body = new String(ri.getContent(), StandardCharsets.UTF_8);
        String fileName = firstNonEmpty(ri.getParameters().get("filename"), ri.getParameters().get("file"), "config.conf");

        if (body.contains("Content-Disposition:") && body.contains("filename=")) {
            fileName = extractBetween(body, "filename=\"", "\"");
            int headerEnd = body.indexOf("\r\n\r\n");
            int sepLen = 4;
            if (headerEnd < 0) {
                headerEnd = body.indexOf("\n\n");
                sepLen = 2;
            }
            if (headerEnd >= 0) {
                String content = body.substring(headerEnd + sepLen);
                int boundary = content.lastIndexOf("\n--");
                if (boundary >= 0) {
                    content = content.substring(0, boundary);
                }
                return new UploadedConfig(fileName, content.trim());
            }
        }

        String paramContent = firstNonEmpty(ri.getParameters().get("config"), ri.getParameters().get("content"));
        if (paramContent != null) {
            return new UploadedConfig(fileName, paramContent);
        }

        return new UploadedConfig(fileName, body);
    }

    private String extractBetween(String s, String start, String end) {
        int a = s.indexOf(start);
        if (a < 0) return "config.conf";
        a += start.length();
        int b = s.indexOf(end, a);
        return b < 0 ? s.substring(a) : s.substring(a, b);
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private String cleanFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "config.conf";
        String clean = name.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        return clean.isEmpty() ? "config.conf" : clean;
    }

    @Override
    public void close() {
        if (currentConfig != null) {
            currentConfig.close();
            currentConfig = null;
        }
    }
}
