package buttondevteam.website.page;

import java.io.IOException;
import java.io.PrintStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.sun.net.httpserver.*;

import buttondevteam.website.io.IOHelper;

/**
 * Add to {@link Main}.Pages
 */
public abstract class Page implements HttpHandler {
	public abstract String GetName();
	
	@Override
	public void handle(HttpExchange exchange) {
		try {
			if (exchange.getRequestURI().getPath().equals("/" + GetName()))
				handlePage(exchange);
			else {
				IOHelper.SendResponse(404, "404 Not found", exchange);
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream str = new PrintStream(baos);
				str.print("<h1>500 Internal Server Error</h1><pre>");
				e.printStackTrace(str);
				str.print("</pre>");
				IOHelper.SendResponse(500, baos.toString("UTF-8"), exchange);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public abstract void handlePage(HttpExchange exchange) throws IOException;
}
