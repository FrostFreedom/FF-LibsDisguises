package me.libraryaddict.disguise.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import me.libraryaddict.disguise.DisallowedDisguises;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.libraryaddict.disguise.DisguiseConfig;
import me.libraryaddict.disguise.LibsDisguises;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.utilities.DisguiseParser;
import me.libraryaddict.disguise.utilities.DisguiseParser.DisguiseParseException;
import me.libraryaddict.disguise.utilities.DisguiseParser.DisguisePerm;
import me.libraryaddict.disguise.utilities.ReflectionFlagWatchers;
import me.libraryaddict.disguise.utilities.ReflectionFlagWatchers.ParamInfo;
import org.bukkit.entity.Entity;

public class DisguiseEntityCommand extends DisguiseBaseCommand implements TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You may not use this command from the console!");
            return true;
        }

        if (args.length >= 2) {
            if (args[1].contains(":")) {
                sender.sendMessage("That disguise is forbidden.");
                return true;
            }
        }
        
        Disguise disguise;

        try {
            disguise = DisguiseParser.parseDisguise(sender, getPermNode(), args, getPermissions(sender));
        }
        catch (DisguiseParseException ex) {
            if (ex.getMessage() != null) {
                sender.sendMessage(ex.getMessage());
            }

            return true;
        }
        catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            return true;
        }
        
        if (Arrays.toString(args).toLowerCase().contains("item_frame")) {
            sender.sendMessage(ChatColor.RED + "That disguise is forbidden.");
            return true;
        }

        if (Arrays.toString(args).toLowerCase().contains("itemframe")) {
            sender.sendMessage(ChatColor.RED + "That disguise is forbidden.");
            return true;

        }

        if (Arrays.toString(args).toLowerCase().contains("portal")) {
            sender.sendMessage(ChatColor.RED + "That disguise is forbidden.");
            return true;
        }

        if (Arrays.toString(args).toLowerCase().contains("hay_block")) {
            sender.sendMessage(ChatColor.RED + "That disguise is forbidden.");
            return true;
        }

        if (Arrays.toString(args).contains("fire")) {
            sender.sendMessage(ChatColor.RED + "That disguise is forbidden.");
        }

        if (Arrays.toString(args).contains("carrot")) {
            sender.sendMessage(ChatColor.RED + "That disguise is forbidden.");
            return true;
        }

        if (!DisallowedDisguises.disabled) {
            if (DisallowedDisguises.isAllowed(disguise)) {
                LibsDisguises.getInstance().getListener().setDisguiseEntity(sender.getName(), disguise);
            } else {
                sender.sendMessage(ChatColor.RED + "That disguise is forbidden.");
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Disguises are disabled.");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Right click a entity in the next " + DisguiseConfig.getDisguiseEntityExpire()
                + " seconds to disguise it as a " + disguise.getType().toReadable() + "!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] origArgs) {
        ArrayList<String> tabs = new ArrayList<String>();

        if (!(sender instanceof Player)) {
            return tabs;
        }

        String[] args = getArgs(origArgs);

        HashMap<DisguisePerm, HashMap<ArrayList<String>, Boolean>> perms = getPermissions(sender);

        if (args.length == 0) {
            for (String type : getAllowedDisguises(perms)) {
                tabs.add(type);
            }
        } else {
            DisguisePerm disguiseType = DisguiseParser.getDisguisePerm(args[0]);

            if (disguiseType == null) {
                return filterTabs(tabs, origArgs);
            }

            if (args.length == 1 && disguiseType.getType() == DisguiseType.PLAYER) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    tabs.add(player.getName());
                }
            } else {
                ArrayList<String> usedOptions = new ArrayList<String>();

                for (Method method : ReflectionFlagWatchers.getDisguiseWatcherMethods(disguiseType.getWatcherClass())) {
                    for (int i = disguiseType.getType() == DisguiseType.PLAYER ? 2 : 1; i < args.length; i++) {
                        String arg = args[i];

                        if (!method.getName().equalsIgnoreCase(arg)) {
                            continue;
                        }

                        usedOptions.add(arg);
                    }
                }

                if (passesCheck(sender, perms.get(disguiseType), usedOptions)) {
                    boolean addMethods = true;

                    if (args.length > 1) {
                        String prevArg = args[args.length - 1];

                        ParamInfo info = ReflectionFlagWatchers.getParamInfo(disguiseType, prevArg);

                        if (info != null) {
                            if (info.getParamClass() != boolean.class) {
                                addMethods = false;
                            }

                            if (info.isEnums()) {
                                for (String e : info.getEnums(origArgs[origArgs.length - 1])) {
                                    tabs.add(e);
                                }
                            } else if (info.getParamClass() == String.class) {
                                for (Player player : Bukkit.getOnlinePlayers()) {
                                    tabs.add(player.getName());
                                }
                            }
                        }
                    }

                    if (addMethods) {
                        // If this is a method, add. Else if it can be a param of the previous argument, add.
                        for (Method method : ReflectionFlagWatchers.getDisguiseWatcherMethods(disguiseType.getWatcherClass())) {
                            tabs.add(method.getName());
                        }
                    }
                }
            }
        }

        return filterTabs(tabs, origArgs);
    }

    /**
     * Send the player the information
     *
     * @param sender
     * @param map
     */
    @Override
    protected void sendCommandUsage(CommandSender sender, HashMap<DisguisePerm, HashMap<ArrayList<String>, Boolean>> map) {
        ArrayList<String> allowedDisguises = getAllowedDisguises(map);

        sender.sendMessage(ChatColor.DARK_GREEN + "Choose a disguise then right click a entity to disguise it!");
        sender.sendMessage(ChatColor.DARK_GREEN + "You can use the disguises: " + ChatColor.GREEN
                + StringUtils.join(allowedDisguises, ChatColor.RED + ", " + ChatColor.GREEN));

        if (allowedDisguises.contains("player")) {
            sender.sendMessage(ChatColor.DARK_GREEN + "/disguiseentity player <Name>");
        }

        sender.sendMessage(ChatColor.DARK_GREEN + "/disguiseentity <DisguiseType> <Baby>");

        if (allowedDisguises.contains("dropped_item") || allowedDisguises.contains("falling_block")) {
            sender.sendMessage(ChatColor.DARK_GREEN + "/disguiseentity <Dropped_Item/Falling_Block> <Id> <Durability>");
        }
    }

}
