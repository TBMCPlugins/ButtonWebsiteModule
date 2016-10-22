package buttondevteam.website.page;

import java.io.IOException;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import buttondevteam.website.data.Stats;
import buttondevteam.website.io.IOHelper;

public class IndexPage extends Page {

	@Override
	public void handlePage(HttpExchange exchange) throws IOException {
		Gson gson = new Gson();
		Stats request = gson.fromJson(IOHelper.GetPOSTJSON(exchange), Stats.class); // TODO: Change to a request class
		Stats response = new Stats();
		IOHelper.SendResponse(200, gson.toJson(response), exchange);
	}

	@Override
	public String GetName() {
		return "";
	}

}
