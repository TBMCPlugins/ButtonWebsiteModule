package buttondevteam.website;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.ButtonPlugin;
import buttondevteam.website.io.IOHelper;
import buttondevteam.website.page.*;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.*;

public class ButtonWebsiteModule extends ButtonPlugin {
	/**
	 * For ACME validation and user redirection
	 */
	private static HttpServer httpserver;
	private static boolean enabled;

	public ButtonWebsiteModule() {
		try {
			int p = getConfig().getInt("http-port", 80);
			httpserver = HttpServer.create(new InetSocketAddress((InetAddress) null, p), 10);
			enabled = true;
		} catch (Exception e) {
            TBMCCoreAPI.SendException("An error occurred while starting the webserver!", e, this);
			enabled = false; //It's not even enabled yet, so we need a variable
		}
	}

	@Override
	public void pluginEnable() {
		if (!enabled) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		addPage(new IndexPage());
		addPage(new LoginPage());
		addPage(new ProfilePage());
		addPage(new BuildNotificationsPage());
		addPage(new BridgePage());
		addHttpPage(new BridgePage());
		TBMCCoreAPI.RegisterUserClass(WebUser.class, WebUser::new);
		getCommand2MC().registerCommand(new LoginCommand());
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			this.getLogger().info("Starting webserver...");
			httpserver.createContext("/", exchange -> IOHelper.SendResponse(IOHelper.Redirect("https://www.youtube.com/watch?v=dQw4w9WgXcQ", exchange)));
			if (!httpstarted)
				httpserver.start();
			this.getLogger().info("Webserver started");
		});
	}

	@Override
	protected void pluginDisable() {
	}

	private static boolean httpstarted = false;

	/**
	 * Used to start the server when the ACME client needs it
	 */
	static void startHttp() {
		httpserver.start();
		httpstarted = true;
	}

	/**
	 * Adds a new page/endpoint to the website. This method needs to be called before the server finishes loading (onEnable).
	 */
	public static void addPage(Page page) {
		// No HTTPS support for now but these pages should be secured (and updated)
	}

	/**
	 * Adds an <b>insecure</b> endpoint to the website. This should be avoided when possible.
	 */
	public static void addHttpPage(Page page) {
		if (!enabled)
			return;
		httpserver.createContext("/" + page.GetName(), page);
	}

    static void storeRegistration(URL location) {
		final ButtonWebsiteModule plugin = getPlugin(ButtonWebsiteModule.class);
		plugin.getConfig().set("registration", location.toString());
		plugin.saveConfig();
	}

	static URI getRegistration() {
		try {
			String str = getPlugin(ButtonWebsiteModule.class).getConfig().getString("registration");
			return str == null ? null : new URI(str);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

    private static InputStream fullStream(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		byte[] bytes = new byte[dis.available()];
		dis.readFully(bytes);
		dis.close();
        return new ByteArrayInputStream(bytes);
	}
}
