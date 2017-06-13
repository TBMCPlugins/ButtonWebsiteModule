package buttondevteam.website.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.website.WebUser;

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

	/**
	 * Sends login headers and sets the session id on the user
	 * 
	 * @param exchange
	 * @param user
	 */
	public static void LoginUser(HttpExchange exchange, WebUser user) {
		Bukkit.getLogger().fine("Logging in user: " + user);
		// provider.SetValues(() ->
		// user.setSessionid(UUID.randomUUID().toString()));
		user.sessionID().set(UUID.randomUUID());
		new Cookies(2).add(new Cookie("user_id", user.getUUID() + ""))
				.add(new Cookie("session_id", user.sessionID().get().toString())).SendHeaders(exchange);
		Bukkit.getLogger().fine("Logged in user.");
	}

	public static void LogoutUser(HttpExchange exchange, WebUser user) {
		user.sessionID().set(new UUID(0, 0));
		SendLogoutHeaders(exchange);
	}

	private static void SendLogoutHeaders(HttpExchange exchange) {
		String expiretime = "Sat, 19 Mar 2016 23:33:00 GMT";
		new Cookies(expiretime).add(new Cookie("user_id", "del")).add(new Cookie("session_id", "del"))
				.SendHeaders(exchange);
	}

	public static void Redirect(String url, HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().add("Location", url);
		IOHelper.SendResponse(303, "<a href=\"" + url + "\">If you can see this, click here to continue</a>", exchange);
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
	 * Get logged in user. It may also send logout headers if the cookies are invalid, or login headers to keep the user logged in.
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
				&& cookies.get("session_id").getValue().equals(user.sessionID().get())) {
			if (cookies.getExpireTimeParsed().minusYears(1).isBefore(ZonedDateTime.now(ZoneId.of("GMT"))))
				LoginUser(exchange, user);
			return user;
		} else
			SendLogoutHeaders(exchange);
		return null;
	}
}
