package io.Yomicer.magicExpansion.specialActions.Command;

import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.Yomicer.magicExpansion.utils.aiManager.AIManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;



public class AIChat implements CommandExecutor,TabCompleter {

    private final AIManager aiManager;

    public AIChat(AIManager aiManager) {
        this.aiManager = aiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        // ✅ 1. Check the basic mxai.use permission
        if (!player.hasPermission("mxai.use")) {
            player.sendMessage("You do not have permission to use AI commands!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("chaton")) {
            aiManager.enableAI(player);
            player.sendMessage("§a✅ AI chat mode is now on (private chat).");
        } else if (sub.equals("chatoff")) {
            aiManager.disableAI(player);
            player.sendMessage("§b❌ AI chat mode is now off.");
        } else if (sub.equals("public") && args.length > 1) {
            // Example: /mxai public hello
            if (aiManager.getPublicMode()) {
                String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                aiManager.askAIPublic(player, msg);
            }else{
                Bukkit.broadcastMessage("Player " + player.getName() + " wants to ask AI: \"" + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) + "\"");
                player.sendMessage("❌ Public AI chat is off. Ask an admin to turn it on with /mxai publicmode on.");
            }
        }else if (sub.equals("publicmode")) {
            // ✅ Check OP permission
            if (!player.hasPermission("mxai.op") && !player.isOp()) {
                player.sendMessage("§cYou do not have permission to use this command!");
                return true;
            }

            if (args.length == 1) {
                // Check the current status
                boolean isPublic = aiManager.getPublicMode();
                player.sendMessage("§6[AI Admin] Public mode: " + (isPublic ? "§aOn" : "§cOff"));
                return true;
            }

            String mode = args[1].toLowerCase();
            if (mode.equals("on")) {
                aiManager.setPublicMode(true);
                Bukkit.broadcastMessage("📢 §aPublic AI chat is now on! Everyone can join with §6/mxai public§a.");
            } else if (mode.equals("off")) {
                aiManager.setPublicMode(false);
                Bukkit.broadcastMessage("📢 §cPublic AI chat is now off. AI chat is back to private mode.");
            } else {
                player.sendMessage("§cUsage: /mxai publicmode <on|off>");
            }

        } else {
            sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== §bAI Chat Helper §6==========");
        player.sendMessage("§7/mxai chaton §f- Turn on AI chat");
        player.sendMessage("§7/mxai chatoff §f- Turn off AI chat");
        player.sendMessage("§7/mxai public §a message §f- Chat with the public AI");
        if (player.hasPermission("mxai.op") || player.isOp()) {
            player.sendMessage("§c/mxai publicmode <on|off> §7- §c[OP] Manage public AI chat");
        }
        player.sendMessage("§6==================================");
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("chaton", "chatoff", "public", "publicmode");
        }
        return Collections.emptyList();
    }


}
