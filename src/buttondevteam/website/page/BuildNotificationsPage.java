package buttondevteam.website.page;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.website.io.IOHelper;
import buttondevteam.website.io.Response;

public class BuildNotificationsPage extends Page {

	@Override
	public String GetName() {
		return "build_notifications";
	}

	private static final String signature = ((Supplier<String>) () -> {
		try {
			return fromString(TBMCCoreAPI.DownloadString("https://api.travis-ci.org/config"),
					"config.notifications.webhook.public_key").getAsString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}).get();

	@Override
	public Response handlePage(HttpExchange exchange) {
		HashMap<String, String> post = IOHelper.GetPOSTKeyValues(exchange);
		try {
			final List<String> signatures = exchange.getRequestHeaders().get("Signature");
			if (signatures.size() > 0 && post.containsKey("payload")
					&& verifySignature(Base64.getDecoder().decode(post.get("payload")),
							Base64.getDecoder().decode(signatures.get(0)), signature)) {
				// TODO: Send event
				return new Response(200, "All right", exchange);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return new Response(400, "Verification failed", exchange);
	}

	// Method for signature verification that initializes with the Public Key,
	// updates the data to be verified and then verifies them using the signature
	private boolean verifySignature(byte[] data, byte[] signature, String keystr) throws Exception {
		Signature sig = Signature.getInstance("SHA1withRSA");
		sig.initVerify(getPublic(keystr));
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
		JsonObject obj = new GsonBuilder().create().fromJson(json, JsonObject.class);
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
