package buttondevteam.website.page;

import java.io.PrintStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.sun.net.httpserver.*;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.website.io.IOHelper;
import buttondevteam.website.io.Response;

/**
 * Add to {@link Main}.Pages
 */
public abstract class Page implements HttpHandler {
	public abstract String GetName();

	@Override
	public final void handle(HttpExchange exchange) {
		try {
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "https://tbmcplugins.github.io");
			if (exactPage() ? exchange.getRequestURI().getPath().equals("/" + GetName()) : true)
				IOHelper.SendResponse(handlePage(exchange));
			else {
				IOHelper.SendResponse(404, "404 Not found: " + exchange.getRequestURI().getPath(), exchange);
			}
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Internal Server Error in ButtonWebsiteModule!", e);
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream str = new PrintStream(baos);
				str.print("<h1>500 Internal Server Error</h1><pre>");
				e.printStackTrace(str);
				str.print("</pre>");
				IOHelper.SendResponse(500, baos.toString("UTF-8"), exchange);
			} catch (Exception e1) {
				TBMCCoreAPI.SendException("Exception while sending Internal Server Error in ButtonWebsiteModule!", e1);
			}
		}
	}

	/**
	 * The main logic of the endpoint. Use IOHelper to retrieve the message sent and other things.
	 */
	public abstract Response handlePage(HttpExchange exchange);

	/**
	 * Whether to return 404 when the URL doesn't match the exact path
	 * 
	 * @return
	 */
	public boolean exactPage() {
		return true;
	}
}
