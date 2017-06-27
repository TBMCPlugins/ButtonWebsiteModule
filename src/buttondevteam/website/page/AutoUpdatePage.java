package buttondevteam.website.page;

import java.io.IOException;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;

import buttondevteam.website.io.IOHelper;
import buttondevteam.website.io.Response;

public class AutoUpdatePage extends Page {

	@Override
	public String GetName() {
		return "autoupdate";
	}

	@Override
	public Response handlePage(HttpExchange exchange) {
		if (exchange.getRequestHeaders().containsKey("Expect")
				&& exchange.getRequestHeaders().get("Expect").contains("100-continue"))
			try {
				exchange.sendResponseHeaders(100, -1);
				exchange.getResponseBody().close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		return new Response(200,
				"Headers:\n" + exchange.getRequestHeaders().entrySet().stream()
						.map(e -> e.getKey() + ": " + e.getValue().stream().collect(Collectors.joining(" ")))
						.collect(Collectors.joining("\n")) + "\nPOST: " + IOHelper.GetPOST(exchange),
				exchange);
	}

}
