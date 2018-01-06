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
				System.out.println("[" + id + "] Created a bridge: " + id);
				return new Response(201, "You know what you created. A bridge.", exchange);
			case "PUT":
				s = getSocket(exchange);
				if (s == null)
					return new Response(400, "No connection", exchange);
				System.out.println("[" + id + "] PUT " + IOUtils.copy(exchange.getRequestBody(), s.getOutputStream())
						+ " bytes into the server");
				s.getOutputStream().flush();
				return new Response(200, "OK", exchange);
			case "GET":
				s = getSocket(exchange);
				if (s == null)
					return new Response(400, "No connection", exchange);
				exchange.sendResponseHeaders(200, 0); // Chunked transfer, any amount of data
				System.out.println("[" + id + "] Sending to GET");
				System.out.println("[" + id + "] Sent to GET "
						+ IOUtils.copy(s.getInputStream(), exchange.getResponseBody()) + " bytes");
				exchange.getResponseBody().flush();
				// exchange.getResponseBody().close(); // TO!DO: Keep open? - YES
				return null; // Response already sent
			case "DELETE":
				System.out.println("[" + id + "] delet this");
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
