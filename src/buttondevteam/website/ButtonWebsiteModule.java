package buttondevteam.website;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.sun.net.httpserver.HttpServer;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.website.page.*;

public class ButtonWebsiteModule extends JavaPlugin {
	private static HttpServer server;

	@Override
	public void onEnable() {
		try {
			server = HttpServer.create(new InetSocketAddress(InetAddress.getLocalHost(), 8080), 10);
			addPage(new IndexPage());
			Bukkit.getScheduler().runTaskAsynchronously(this, () -> this.getLogger().info("Starting webserver..."));
			Bukkit.getScheduler().runTaskAsynchronously(this, server::start);
			Bukkit.getScheduler().runTaskAsynchronously(this, () -> this.getLogger().info("Webserver started"));
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while starting the webserver!", e);
		}
	}

	/**
	 * Adds a new page/endpoint to the website. This method needs to be called before the server finishes loading (onEnable).
	 */
	public static void addPage(Page page) {
		server.createContext("/" + page.GetName(), page);
	}
}
