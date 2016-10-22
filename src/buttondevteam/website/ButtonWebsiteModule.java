package buttondevteam.website;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.bukkit.plugin.java.JavaPlugin;
import com.sun.net.httpserver.HttpServer;

import buttondevteam.website.page.*;

public class ButtonWebsiteModule extends JavaPlugin {
	@Override
	public void onEnable() {
		try {
			this.getLogger().info("Starting webserver...");
			HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLocalHost(), 8080), 10);
			/*
			 * Reflections rf = new Reflections( new ConfigurationBuilder().setUrls(ClasspathHelper.forClassLoader(Page.class.getClassLoader()))
			 * .addClassLoader(Page.class.getClassLoader()).addScanners(new SubTypesScanner()) .filterInputsBy((String pkg) -> pkg.contains(Page.class.getPackage().getName()))); Set<Class<? extends
			 * Page>> pages = rf.getSubTypesOf(Page.class); for (Class<? extends Page> page : pages) { try { if (Modifier.isAbstract(page.getModifiers())) continue; Page p = page.newInstance();
			 * addPage(server, p); } catch (InstantiationException e) { e.printStackTrace(); } catch (IllegalAccessException e) { e.printStackTrace(); } }
			 */ //^^ This code would load the pages dynamically - But we'll only have like, one page...
			addPage(server, new IndexPage());
			server.start();
			this.getLogger().info("Webserver started");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void addPage(HttpServer server, Page page) {
		server.createContext("/" + page.GetName(), page);
	}
}
