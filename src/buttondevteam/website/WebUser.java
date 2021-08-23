package buttondevteam.website;

import java.util.UUID;

import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.UserClass;

@UserClass(foldername = "web")
public class WebUser extends ChromaGamerBase {
	private UUID uuid;

	public UUID getUUID() {
		if (uuid == null)
			uuid = UUID.fromString(getFileName());
		return uuid;
	}

	//It's used with toString() directly, so can't be null
	public ConfigData<UUID> sessionID = getConfig().getData("sessionID", new UUID(0, 0));
}
