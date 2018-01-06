package buttondevteam.website.page;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
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
				System.out.println("[BWM] Created a bridge: " + id);
				return new Response(201, "You know what you created. A bridge.", exchange);
			case "PUT":
				s = getSocket(exchange);
				if (s == null)
					return new Response(400, "No connection", exchange);
				if (s.isClosed())
					return new Response(410, "Socket Gone", exchange);
				copyStream(exchange.getRequestBody(), s.getOutputStream());
				// Don't close the socket, PUT messages are sent individually
				return new Response(200, "OK", exchange);
			case "GET":
				s = getSocket(exchange);
				if (s == null)
					return new Response(400, "No connection", exchange);
				if (s.isClosed())
					return new Response(410, "Socket Gone", exchange);
				exchange.sendResponseHeaders(200, 0); // Chunked transfer, any amount of data
				copyStream(s.getInputStream(), exchange.getResponseBody());
				exchange.getResponseBody().close(); // It'll only get here when the communication is already done
				return null; // Response already sent
			case "DELETE":
				System.out.println("[BWM] [" + id + "] delet this");
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

	private int copyStream(InputStream is, OutputStream os) throws IOException { // Based on IOUtils.copy()
		byte[] buffer = new byte[4096];
		long count = 0;
		int n = 0;
		try {
			while (-1 != (n = is.read(buffer))) { // Read is blocking
				os.write(buffer, 0, n);
				count += n;
				os.flush();
			}
		} catch (SocketException e) { // Conection closed
			os.flush();
		}
		return (int) count;
	}

	@Override
	public boolean exactPage() {
		return false;
	}
}
