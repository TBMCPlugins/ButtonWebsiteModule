package buttondevteam.website;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.ButtonPlugin;
import buttondevteam.website.io.IOHelper;
import buttondevteam.website.page.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bukkit.Bukkit;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ButtonWebsiteModule extends ButtonPlugin {
	private static HttpsServer server;
	/**
	 * For ACME validation and user redirection
	 */
	private static HttpServer httpserver;
	private static boolean enabled;

	public ButtonWebsiteModule() {
		try {
			int ps = getConfig().getInt("https-port", 443);
			int p = getConfig().getInt("http-port", 80);
			server = HttpsServer.create(new InetSocketAddress((InetAddress) null, ps), 10);
			httpserver = HttpServer.create(new InetSocketAddress((InetAddress) null, p), 10);
			SSLContext sslContext = SSLContext.getInstance("TLS");

			// initialise the keystore
			char[] password = "password".toCharArray();
			KeyStore ks = KeyStore.getInstance("JKS");
            File certfile = AcmeClient.DOMAIN_CHAIN_FILE; /* your cert path */
			File keystoreFile = new File("keystore.keystore");

			ks.load(keystoreFile.exists() ? new FileInputStream(keystoreFile) : null, password);

			String alias = "chroma";

			//////

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			InputStream certstream = fullStream(certfile);
            Certificate[] certs = cf.generateCertificates(certstream).toArray(new Certificate[0]);

            BufferedReader br = new BufferedReader(new FileReader(AcmeClient.DOMAIN_KEY_FILE));

			Security.addProvider(new BouncyCastleProvider());

			PEMParser pp = new PEMParser(br);
			PEMKeyPair pemKeyPair = (PEMKeyPair) pp.readObject();
			KeyPair kp = new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
			pp.close();
			PrivateKey pk = kp.getPrivate();

			// Add the certificate
			ks.setKeyEntry(alias, pk, password, certs); // TODO: Only set if updated

			// Save the new keystore contents
			FileOutputStream out = new FileOutputStream(keystoreFile);
			ks.store(out, password);
			out.close();

			// setup the key manager factory
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, password);

			// setup the trust manager factory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);

			// setup the HTTPS context and parameters
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
				public void configure(HttpsParameters params) {
					try {
						// initialise the SSL context
						SSLContext c = SSLContext.getDefault();
						SSLEngine engine = c.createSSLEngine();
						params.setNeedClientAuth(false);
						params.setCipherSuites(engine.getEnabledCipherSuites());
						params.setProtocols(engine.getEnabledProtocols());

						// get the default parameters
						SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
						params.setSSLParameters(defaultSSLParameters);

					} catch (Exception ex) {
						System.out.println("Failed to create HTTPS port");
					}
				}
			});
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
		TBMCCoreAPI.RegisterUserClass(WebUser.class, WebUser::new);
		getCommand2MC().registerCommand(new LoginCommand());
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			this.getLogger().info("Starting webserver...");
			server.setExecutor(
                    new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100)));
            httpserver.createContext("/", exchange -> IOHelper.SendResponse(IOHelper.Redirect("https://server.figytuna.com/", exchange)));
			final Calendar calendar = Calendar.getInstance();
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY && !TBMCCoreAPI.IsTestServer()) { // Only update every week
				Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
				AcmeClient.main("server.figytuna.com"); // Task is running async so we don't need an extra thread
			}
			((Runnable) server::start).run(); // Totally normal way of calling a method
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
		if (!enabled)
			return;
		server.createContext("/" + page.GetName(), page);
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
