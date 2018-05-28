package buttondevteam.website.page;

import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.website.WebUser;
import buttondevteam.website.io.IOHelper;
import buttondevteam.website.io.Response;
import com.google.common.collect.HashBiMap;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class LoginPage extends Page {

	@Override
	public String GetName() {
		return "login";
	}

	@Override
	public Response handlePage(HttpExchange exchange) {
		Map<String, String> q = IOHelper.parseQueryString(exchange);
		if (q == null || !q.containsKey("type"))
			return new Response(400, "400 Bad request", exchange);
		String type = q.get("type");
		/*if (type.equalsIgnoreCase("getstate"))
			return new Response(200, "TODO", exchange); // TO!DO: Store and return a random state and check on other types
		String state = q.get("state"), code = q.get("code");*/
		Response nope = new Response(401, "401 Nope", exchange);
		if (type.equalsIgnoreCase("minecraft")) {
			//In case of Minecraft, we don't need the full OAuth2 flow, we only need to ensure the state matches
			if (q.containsKey("state")) {
				UUID state = UUID.fromString(q.get("state"));
				if (!states.containsKey(state))
					return nope;
                String[] folder_id = states.get(state).split(" ");
				if (!folder_id[0].equalsIgnoreCase(type)) //TODO: Use for other OAuth stuff as well
					return nope;
				TBMCPlayer cp = TBMCPlayer.getPlayer(UUID.fromString(folder_id[1]), TBMCPlayer.class);
				WebUser wu = cp.getAs(WebUser.class);
				if (wu == null) //getAs return Optional?
					cp.connectWith(wu = WebUser.getUser(UUID.randomUUID().toString(), WebUser.class)); //Create new user with random UUID
				IOHelper.LoginUser(exchange, wu);
				states.remove(state);
				try {
                    return IOHelper.Redirect("https://chromagaming.figytuna.com/", exchange);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else return new Response(418, "Now what", exchange); //Minecraft doesn't have full OAuth
        }
        return new Response(400, "Wut", exchange);
	}

	/**
     * Value: Folder ID (don't use dashes as a separator... UUIDs contain them)
	 */
	private static final HashBiMap<UUID, String> states = HashBiMap.create();

	/**
	 * Generates a temporary state data that can be used to authenticate a user.
	 *
	 * @param type The service type. Only used to separate in temporary storage.
	 * @param id   The user id in the service. Only used to separate in temporary storage.
	 * @return A unique state that can be used to authenticate a user.
	 */
	public static UUID generateState(String type, String id) {
		UUID state = UUID.randomUUID();
        states.forcePut(state, type + " " + id); //Replace existing for an user
		return state;
	}
}
