package buttondevteam.website;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.sun.net.httpserver.HttpServer;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.website.page.*;

public class ButtonWebsiteModule extends JavaPlugin {
	public static final int PORT = 8080;
	private static HttpServer server;

	public ButtonWebsiteModule() {
		try {
			server = HttpServer.create(new InetSocketAddress((InetAddress) null, PORT), 10);
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while starting the webserver!", e);
		}
	}

	@Override
	public void onEnable() {
		addPage(new IndexPage());
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			this.getLogger().info("Starting webserver...");
			((Runnable) server::start).run(); // Totally normal way of calling a method
			this.getLogger().info("Webserver started");
			Thread t = new Thread(() -> AcmeClient.main("server.figytuna.com"));
			t.setContextClassLoader(getClass().getClassLoader());
			t.start();
		});
	}

	/**
	 * Adds a new page/endpoint to the website. This method needs to be called before the server finishes loading (onEnable).
	 */
	public static void addPage(Page page) {
		server.createContext("/" + page.GetName(), page);
	}
}
