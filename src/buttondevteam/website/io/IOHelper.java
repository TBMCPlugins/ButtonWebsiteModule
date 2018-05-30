package buttondevteam.website.io;

import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.website.WebUser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class IOHelper {
	public static void SendResponse(Response resp) throws IOException {
		if (resp == null)
			return; // Response is already sent
		SendResponse(resp.code, resp.content, resp.exchange);
	}

	public static void SendResponse(int code, String content, HttpExchange exchange) throws IOException {
		if (exchange.getRequestMethod().equalsIgnoreCase("HEAD")) {
            exchange.sendResponseHeaders(200, -1); // -1 indicates no data
            //exchange.getResponseBody().close(); - No stream is created for HEAD requests
			return;
		}
		try (BufferedOutputStream out = new BufferedOutputStream(exchange.getResponseBody())) {
			try (ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
				try {
					exchange.sendResponseHeaders(code, bis.available());
				} catch (IOException e) {
					if (!e.getMessage().equals("headers already sent"))
						throw e; // If an error occurs after sending the response headers send the error page even if the headers are for the original
				} // This code will send *some page* (most likely an error page) with the original headers instead of failing to do anything
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
            return IOUtils.toString(exchange.getRequestBody(), "UTF-8");
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
            return e.getAsJsonObject();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Sends login headers and sets the session id on the user
	 */
	public static void LoginUser(HttpExchange exchange, WebUser user) {
		Bukkit.getLogger().fine("Logging in user: " + user);
		user.sessionID().set(UUID.randomUUID());
		user.save();
		new Cookies(2).add(new Cookie("user_id", user.getUUID() + ""))
                .add(new Cookie("session_id", user.sessionID().get().toString())).AddHeaders(exchange);
		Bukkit.getLogger().fine("Logged in user.");
	}

	public static void LogoutUser(HttpExchange exchange, WebUser user) {
		user.sessionID().set(new UUID(0, 0));
		user.save();
		SendLogoutHeaders(exchange);
	}

	private static void SendLogoutHeaders(HttpExchange exchange) {
		String expiretime = "Sat, 19 Mar 2016 23:33:00 GMT";
		new Cookies(expiretime).add(new Cookie("user_id", "del")).add(new Cookie("session_id", "del"))
                .AddHeaders(exchange);
	}

	public static Response Redirect(String url, HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().add("Location", url);
		return new Response(303, "<a href=\"" + url + "\">If you can see this, click here to continue</a>", exchange);
	}

	public static Cookies GetCookies(HttpExchange exchange) {
		if (!exchange.getRequestHeaders().containsKey("Cookie"))
			return new Cookies();
		Map<String, String> map = new HashMap<>();
		for (String cheader : exchange.getRequestHeaders().get("Cookie")) {
			String[] spl = cheader.split("\\;\\s*");
			for (String s : spl) {
				String[] kv = s.split("\\=");
				if (kv.length < 2)
					continue;
				map.put(kv[0], kv[1]);
			}
		}
		if (!map.containsKey("expiretime"))
			return new Cookies();
		Cookies cookies = null;
		try {
			cookies = new Cookies(map.get("expiretime"));
			for (Entry<String, String> item : map.entrySet())
				if (!item.getKey().equalsIgnoreCase("expiretime"))
					cookies.put(item.getKey(), new Cookie(item.getKey(), item.getValue()));
		} catch (Exception e) {
			return new Cookies();
		}
		return cookies;
	}

	/**
	 * Get logged in user. It may also send logout headers if the cookies are invalid, or login headers to keep the user logged in. <b>Make sure to save the user data.</b>
	 * 
	 * @param exchange
	 * @return The logged in user or null if not logged in.
	 * @throws IOException
	 */
	public static WebUser GetLoggedInUser(HttpExchange exchange) throws IOException {
		Cookies cookies = GetCookies(exchange);
		if (!cookies.containsKey("user_id") || !cookies.containsKey("session_id"))
			return null;
		WebUser user = ChromaGamerBase.getUser(cookies.get("user_id").getValue(), WebUser.class);
		if (user != null && cookies.get("session_id") != null
				&& cookies.get("session_id").getValue().equals(user.sessionID().get().toString())) {
			if (cookies.getExpireTimeParsed().minusYears(1).isBefore(ZonedDateTime.now(ZoneId.of("GMT"))))
				LoginUser(exchange, user);
			return user;
		} else
			SendLogoutHeaders(exchange);
		return null;
	}

	public static Map<String, String> parseQueryString(HttpExchange exchange) {
		String qs = exchange.getRequestURI().getRawQuery();
		Map<String, String> result = new HashMap<>();
		if (qs == null)
			return result;

		int last = 0, next, l = qs.length();
		while (last < l) {
			next = qs.indexOf('&', last);
			if (next == -1)
				next = l;

			if (next > last) {
				int eqPos = qs.indexOf('=', last);
				try {
					if (eqPos < 0 || eqPos > next)
						result.put(URLDecoder.decode(qs.substring(last, next), "utf-8"), "");
					else
						result.put(URLDecoder.decode(qs.substring(last, eqPos), "utf-8"),
								URLDecoder.decode(qs.substring(eqPos + 1, next), "utf-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e); // will never happen, utf-8 support is mandatory for java
				}
			}
			last = next + 1;
		}
		return result;
	}

	public static HashMap<String, String> GetPOSTKeyValues(HttpExchange exchange) {
		try {
			String[] content = GetPOST(exchange).split("\\&");
			HashMap<String, String> vars = new HashMap<>();
			for (String var : content) {
				String[] spl = var.split("\\=");
				if (spl.length == 1)
					vars.put(spl[0], "");
				else
					vars.put(spl[0], URLDecoder.decode(spl[1], "utf-8"));
			}
			return vars;
		} catch (Exception e) {
			e.printStackTrace();
			return new HashMap<>();
		}
	}
}
