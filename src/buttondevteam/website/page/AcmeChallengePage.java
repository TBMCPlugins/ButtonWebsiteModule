package buttondevteam.website.page;

import com.sun.net.httpserver.HttpExchange;

import buttondevteam.website.io.Response;

public class AcmeChallengePage extends Page {

	public AcmeChallengePage(String token, String content) { // The page name needs to be known before server start
		AcmeChallengePage.token = token;
		AcmeChallengePage.content = content;
	}

	@Override
	public String GetName() {
		return ".well-known/acme-challenge/" + token;
	}

	@Override
	public Response handlePage(HttpExchange exchange) {
		if (content == null)
			return new Response(500, "500 No content", exchange);
		return new Response(200, content, exchange);
	}

	private static String token;
	private static String content;

}
