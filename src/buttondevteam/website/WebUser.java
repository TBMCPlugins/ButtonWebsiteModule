package buttondevteam.website;

import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.PlayerData;
import buttondevteam.lib.player.UserClass;

import java.util.UUID;

@UserClass(foldername = "web")
public class WebUser extends ChromaGamerBase {
	private UUID uuid;

	public UUID getUUID() {
		if (uuid == null)
			uuid = UUID.fromString(getFileName());
		return uuid;
	}

    public PlayerData<String> sessionID() {
        return data(null);
	}
}
