package buttondevteam.website.io;

import com.sun.net.httpserver.HttpExchange;

public class Response {
	public final int code;
	public final String content;
	public final HttpExchange exchange;

	public Response(int code, String content, HttpExchange exchange) {
		this.code = code;
		this.content = content;
		this.exchange = exchange;
	}
	public Response(int code, String[] content, HttpExchange exchange) {
		this.code = code;
		this.content = String.join("", content);
		this.exchange = exchange;
	}
}
