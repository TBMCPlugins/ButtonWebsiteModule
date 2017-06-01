package buttondevteam.website.page;

import com.sun.net.httpserver.HttpExchange;

import buttondevteam.website.io.Response;

public class AcmeChallengePage extends Page {

	@Override
	public String GetName() {
		return ".well-known/acme-challenge";
	}

	@Override
	public Response handlePage(HttpExchange exchange) {
		// TODO Auto-generated method stub
		return null;
	}

}
