package buttondevteam.website.page;

import buttondevteam.core.component.updater.PluginUpdater;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.website.io.IOHelper;
import buttondevteam.website.io.Response;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.Bukkit;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class BuildNotificationsPage extends Page {

	@Override
	public String GetName() {
		return "build_notifications";
	}

	private static final Gson gson = new Gson();

	private static final String publickey = ((Supplier<String>) () -> {
		try {
			JsonElement pubkey = fromString(TBMCCoreAPI.DownloadString("https://api.travis-ci.org/config"),
					"config.notifications.webhook.public_key");
			if (pubkey == null)
				return null;
			return pubkey.getAsString().replace("-----BEGIN PUBLIC KEY-----", "")
							.replaceAll("\n", "").replace("-----END PUBLIC KEY-----", "");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}).get();

	@Override
	public Response handlePage(HttpExchange exchange) {
		HashMap<String, String> post = IOHelper.GetPOSTKeyValues(exchange);
		try {
			final List<String> signatures = exchange.getRequestHeaders().get("Signature");
			final String payload = post.get("payload");
			if (signatures != null && signatures.size() > 0 && post.containsKey("payload")
					&& verifySignature(payload.getBytes(StandardCharsets.UTF_8),
					Base64.getDecoder().decode(signatures.get(0)))) {
				Bukkit.getPluginManager()
						.callEvent(new PluginUpdater.UpdatedEvent(gson.fromJson(payload, JsonObject.class)));
				return new Response(200, "All right", exchange);
			}
		} catch (Exception e) {
			return new Response(400,
					"Invalid data, error: " + e + " If you're messing with this, stop messing with this.", exchange); // Blame the user
		}
		return new Response(400, "Verification failed", exchange);
	}

	// Method for signature verification that initializes with the Public Key,
	// updates the data to be verified and then verifies them using the signature
	private boolean verifySignature(byte[] data, byte[] signature) throws Exception {
		Signature sig = Signature.getInstance("SHA1withRSA");
		sig.initVerify(getPublic(BuildNotificationsPage.publickey));
		sig.update(data);

		return sig.verify(signature);
	}

	// Method to retrieve the Public Key from a file
	public PublicKey getPublic(String keystr) throws Exception {
		byte[] keyBytes = Base64.getDecoder().decode(keystr);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}

	public static JsonElement fromString(String json, String path) throws JsonSyntaxException {
		JsonObject obj = gson.fromJson(json, JsonObject.class);
		String[] seg = path.split("\\.");
		for (String element : seg) {
			if (obj != null) {
				JsonElement ele = obj.get(element);
				if (!ele.isJsonObject())
					return ele;
				else
					obj = ele.getAsJsonObject();
			} else {
				return null;
			}
		}
		return obj;
	}
}
