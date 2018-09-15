package buttondevteam.website.page;

import buttondevteam.website.io.Response;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class BridgePage extends Page {
	private final Map<String, Socket> connections = new HashMap<>();

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
				try {
					exchange.sendResponseHeaders(200, 0); // Chunked transfer, any amount of data
					copyStream(s.getInputStream(), exchange.getResponseBody());
					exchange.getResponseBody().close(); // It'll only get here when the communication is already done
				} catch (IOException ex) { //Failed to send it over HTTP, GET connection closed
					closeSocket(exchange); //We only have one GET, connection over
					System.out.println("[BWM] [" + id + "] over (GET): " + ex.toString());
				}
				return null; // Response already sent
			case "DELETE":
				System.out.println("[BWM] [" + id + "] delet this");
				closeSocket(exchange);
				return new Response(200, "OK", exchange);
			default:
				return new Response(403, "Unknown request", exchange);
			}
		} catch (IOException e) {
			if (e instanceof SocketException) {
				closeSocket(exchange);
				System.out.println("[BWM] [" + id + "] closed: " + e.toString());
				return new Response(410, "Socket Gone because of error: " + e, exchange);
			}
			e.printStackTrace();
			return new Response(500, "Internal Server Error: " + e, exchange);
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
		} catch (IOException ignored) {
		}
		connections.values().remove(socket);
	}

	private void copyStream(InputStream is, OutputStream os) throws IOException { // Based on IOUtils.copy()
		byte[] buffer = new byte[4096];
		int n;
		try {
			while (-1 != (n = is.read(buffer))) { // Read is blocking
				os.write(buffer, 0, n);
				os.flush();
			}
		} catch (SocketException e) { // Conection closed
			os.flush();
		}
	}

	@Override
	public boolean exactPage() {
		return false;
	}
}
