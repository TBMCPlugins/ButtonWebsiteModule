package buttondevteam.website.page;

import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import buttondevteam.website.io.IOHelper;
import buttondevteam.website.io.Response;

public class LoginPage extends Page {

	@Override
	public String GetName() {
		return "login";
	}

	@Override
	public Response handlePage(HttpExchange exchange) {
		Map<String, String> q = IOHelper.parseQueryString(exchange);
		if (q == null || !q.containsKey("type"))
			return new Response(400, "400 Bad request", exchange);
		String type = q.get("type");
		/*if (type.equalsIgnoreCase("getstate"))
			return new Response(200, "TODO", exchange); // TO!DO: Store and return a random state and check on other types
		String state = q.get("state"), code = q.get("code");*/
		return new Response(200, "", exchange);
	}

}
