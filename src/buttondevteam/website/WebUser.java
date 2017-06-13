package buttondevteam.website;

import java.util.UUID;

import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.PlayerData;
import buttondevteam.lib.player.UserClass;

@UserClass(foldername = "web")
public class WebUser extends ChromaGamerBase {
	private UUID uuid;

	public UUID getUUID() {
		if (uuid == null)
			uuid = UUID.fromString(getFileName());
		return uuid;
	}

	public PlayerData<UUID> sessionID() {
		return data();
	}
}
