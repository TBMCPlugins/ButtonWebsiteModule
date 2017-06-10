package buttondevteam.website.io;

import com.sun.net.httpserver.HttpExchange;

public class Response {
	public int code;
	public String content;
	public HttpExchange exchange;

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
