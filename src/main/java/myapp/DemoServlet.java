package myapp;

import java.io.IOException;
import javax.servlet.http.*;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;

public class DemoServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        //resp.getWriter().println("{ \"name\": \"World\"");
        try (final WebClient webClient = new WebClient()) {
            final HtmlPage page = webClient.getPage("http://catalog.cincinnatilibrary.org");
            final String pageAsXml = page.asXml();
            resp.getWriter().println(pageAsXml);
        }
        //resp.getWriter().println" }");
    }
}
