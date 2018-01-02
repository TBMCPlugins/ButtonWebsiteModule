package buttondevteam.website.page;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;

import com.sun.net.httpserver.HttpExchange;

import buttondevteam.website.io.Response;

public class BridgePage extends Page {
	private Map<String, Socket> connections = new HashMap<>();

	@Override
	public String GetName() {
		return "bridge";
	}

	@Override
	public Response handlePage(HttpExchange exchange) {
		String method = exchange.getRequestMethod().toUpperCase();
		String id = getConnID(exchange);
		if (id == null)
			return new Response(400, "No ID", exchange);
		try {
			Socket s;
			switch (method) {
			case "POST":
				if (connections.containsKey(id))
					connections.get(id).close();
				Socket socket = new Socket("localhost", Bukkit.getPort());
				socket.setKeepAlive(true);
				socket.setTcpNoDelay(true);
				connections.put(id, socket);
				return new Response(201, "", exchange);
			case "PUT":
				s = getSocket(exchange);
				if (s == null)
					return new Response(400, "No connection", exchange);
				IOUtils.copy(exchange.getRequestBody(), s.getOutputStream());
				return new Response(200, "OK", exchange);
			case "GET":
				s = getSocket(exchange);
				if (s == null)
					return new Response(400, "No connection", exchange);
				IOUtils.copy(s.getInputStream(), exchange.getResponseBody());
				exchange.getResponseBody().close(); // TODO: Keep open?
				return null; // Response already sen
			case "DELETE":
				closeSocket(exchange);
				return new Response(200, "OK", exchange);
			default:
				return new Response(403, "Unknown request", exchange);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Socket getSocket(HttpExchange exchange) {
		String id = getConnID(exchange);
		if (id == null)
			return null;
		return connections.get(id);
	}

	private String getConnID(HttpExchange exchange) {
		String path = exchange.getRequestURI().getPath();
		if (path == null)
			return null;
		String[] spl = path.split("/");
		if (spl.length < 2)
			return null;
		return spl[spl.length - 1];
	}

	private void closeSocket(HttpExchange exchange) {
		Socket socket = getSocket(exchange);
		if (socket == null)
			return;
		try {
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		connections.values().remove(socket);
	}

	@Override
	public boolean exactPage() {
		return false;
	}
}
