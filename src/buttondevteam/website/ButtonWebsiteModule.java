package buttondevteam.website;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;

import javax.net.ssl.*;
import java.security.cert.Certificate;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.website.page.*;

public class ButtonWebsiteModule extends JavaPlugin {
	public static final int PORT = 8080;
	private static HttpsServer server;

	public ButtonWebsiteModule() {
		try {
			server = HttpsServer.create(new InetSocketAddress((InetAddress) null, PORT), 10);
			SSLContext sslContext = SSLContext.getInstance("TLS");

			// initialise the keystore
			char[] password = "password".toCharArray();
			KeyStore ks = KeyStore.getInstance("JKS");
			String certfile = "domain-chain.crt"; /* your cert path */
			File keystoreFile = new File("keystore.keystore");
			FileInputStream is = new FileInputStream(keystoreFile);

			ks.load(is, "somepass".toCharArray());

			String alias = "chroma";

			//////

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			InputStream certstream = fullStream(certfile);
			Certificate[] certs = cf.generateCertificates(certstream).stream().toArray(Certificate[]::new);

			byte[] keyBytes = Files.readAllBytes(new File("domain.key").toPath());

			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PrivateKey pk = kf.generatePrivate(spec);

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
			final Calendar calendar = Calendar.getInstance();
			if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) { // Only update every week
				Thread t = new Thread(() -> AcmeClient.main("server.figytuna.com"));
				t.setContextClassLoader(getClass().getClassLoader());
				t.start();
			}
		});
	}

	/**
	 * Adds a new page/endpoint to the website. This method needs to be called before the server finishes loading (onEnable).
	 */
	public static void addPage(Page page) {
		server.createContext("/" + page.GetName(), page);
	}

	private static InputStream fullStream(String fname) throws IOException {
		FileInputStream fis = new FileInputStream(fname);
		DataInputStream dis = new DataInputStream(fis);
		byte[] bytes = new byte[dis.available()];
		dis.readFully(bytes);
		dis.close();
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		return bais;
	}
}
