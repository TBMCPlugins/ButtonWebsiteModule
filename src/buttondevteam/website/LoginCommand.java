package buttondevteam.website;

import buttondevteam.lib.chat.PlayerCommandBase;
import buttondevteam.website.page.LoginPage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LoginCommand extends PlayerCommandBase {
    @Override //TODO: Ask about linking already existing accounts, to prevent linking someone else's
    public boolean OnCommand(Player player, String s, String[] strings) {
        String state = LoginPage.generateState("minecraft", player.getUniqueId().toString()).toString();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " [\"\",{\"text\":\"Please \",\"color\":\"aqua\"},{\"text\":\"Click Here\",\"color\":\"aqua\",\"bold\":true,\"underlined\":true,\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://server.figytuna.com/login?type=minecraft&state=" + state + "\"}},{\"text\":\" to log in to our site using your Minecraft account.\",\"color\":\"aqua\",\"bold\":false,\"underlined\":false}]");
        return true;
    }

    @Override
    public String[] GetHelpText(String s) {
        return new String[]{//
                "§6---- Login ----", //
                "This command allows you to log in to our website using your Minecraft account.", //
                "If you are already logged in to the site, you can connect your MC account to it.", //
                "This is good for getting Minecraft rewards if you're a patreon for example." //
        };
    }
}
