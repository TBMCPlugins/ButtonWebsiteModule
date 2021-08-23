package buttondevteam.website;

import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.chat.ICommand2;
import buttondevteam.lib.chat.ICommand2MC;
import buttondevteam.website.page.LoginPage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandClass(helpText = {//
        "§6---- Login ----", //
        "This command allows you to log in to our website using your Minecraft account.", //
        "If you are already logged in to the site, you can connect your MC account to it.", //
        "This is good for getting Minecraft rewards if you're a patron for example." //
})
public class LoginCommand extends ICommand2MC {
    //TODO: Ask about linking already existing accounts, to prevent linking someone else's
    public boolean def(Player player) {
        String state = LoginPage.generateState("minecraft", player.getUniqueId().toString()).toString();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " [\"\",{\"text\":\"Please \",\"color\":\"aqua\"},{\"text\":\"Click Here\",\"color\":\"aqua\",\"bold\":true,\"underlined\":true,\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://server.figytuna.com/login?type=minecraft&state=" + state + "\"}},{\"text\":\" to log in to our site using your Minecraft account.\",\"color\":\"aqua\",\"bold\":false,\"underlined\":false}]");
        return true;
    }
}
