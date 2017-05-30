package buttondevteam.website.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

public class IOHelper {
	public static void SendResponse(Response resp) throws IOException {
		SendResponse(resp.code, resp.content, resp.exchange);
	}

	public static void SendResponse(int code, String content, HttpExchange exchange) throws IOException {
		try (BufferedOutputStream out = new BufferedOutputStream(exchange.getResponseBody())) {
			try (ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
				exchange.sendResponseHeaders(code, bis.available());
				byte[] buffer = new byte[512];
				int count;
				while ((count = bis.read(buffer)) != -1) {
					out.write(buffer, 0, count);
				}
			}
		}
		exchange.getResponseBody().close();
	}

	public static String GetPOST(HttpExchange exchange) {
		try {
			if (exchange.getRequestBody().available() == 0)
				return "";
			String content = IOUtils.toString(exchange.getRequestBody(), "UTF-8");
			return content;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public static JsonObject GetPOSTJSON(HttpExchange exchange) {
		try {
			String content = GetPOST(exchange);
			if (content.length() == 0)
				return null;
			JsonElement e = new JsonParser().parse(content);
			if (e == null)
				return null;
			JsonObject obj = e.getAsJsonObject();
			return obj;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
