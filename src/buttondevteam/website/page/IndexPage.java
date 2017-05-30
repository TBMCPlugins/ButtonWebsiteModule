package buttondevteam.website.page;

import com.sun.net.httpserver.HttpExchange;

import buttondevteam.website.io.Response;

public class IndexPage extends Page {

	@Override
	public Response handlePage(HttpExchange exchange) {
		return new Response(200, "Hello world!", exchange);
	}

	@Override
	public String GetName() {
		return "";
	}

}
