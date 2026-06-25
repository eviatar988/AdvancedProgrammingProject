import server.HTTPServer;
import server.MyHTTPServer;
import servlets.TopicDisplayer;
import servlets.ConfLoader;
import servlets.HtmlLoader;     /**

 * Main class that starts the local server and registers the project servlets.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        HTTPServer server = new MyHTTPServer(8080, 5);

        server.addServlet("GET", "/publish", new TopicDisplayer());
        server.addServlet("POST", "/upload", new ConfLoader());
        server.addServlet("GET", "/app/", new HtmlLoader("files_html"));

        server.start();
        System.out.println("Server started on http://localhost:8080/app/index.html");
        System.in.read();
        server.close();
        System.out.println("done");
    }
}
