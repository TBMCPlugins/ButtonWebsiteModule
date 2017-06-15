package buttondevteam.website.page;

import com.sun.net.httpserver.HttpExchange;

import buttondevteam.website.io.Response;

public class ProfilePage extends Page {

	@Override
	public String GetName() {
		return "profile";
	}

	@Override
	public Response handlePage(HttpExchange exchange) {
		return new Response(200, "Under construction", exchange);
	}

}
