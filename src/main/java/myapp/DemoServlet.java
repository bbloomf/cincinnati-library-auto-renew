package myapp;

import java.io.IOException;
import javax.servlet.http.*;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;

public class DemoServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/html");
        //resp.getWriter().println("{ \"name\": \"World\"");
        final WebClient webClient = new WebClient();
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        try {
            final HtmlPage page = webClient.getPage("https://catalog.cincinnatilibrary.org/iii/encore/myaccount");
            final String pageAsXml = page.asXml();
            resp.getWriter().println(pageAsXml);
        } catch(Exception e) {
          resp.getWriter().println(e.toString());
        }
        //resp.getWriter().println" }");
    }
}
