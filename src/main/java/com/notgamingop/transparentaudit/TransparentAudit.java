package com.notgamingop.transparentaudit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * TransparentAudit - command transparency/audit plugin for Minecraft 1.8.8.
 *
 * Designed against the Spigot 1.8.8 API so it can run on typical 1.8.8
 * Spigot/Paper/Bukkit-compatible servers.
 */
public final class TransparentAudit extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final String OWNER_NAME = "NOTGAMINGOP";
    private static final String PREFIX = "§8[§bAudit§8] §r";
    private static final int DEFAULT_RECENT_LIMIT = 50;

    private final Set<String> sensitiveCommands = new HashSet<String>();
    private final Deque<String> recentCommands = new ArrayDeque<String>();
    private long commandCount = 0L;
    private long sensitiveCount = 0L;
    private File auditFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSensitiveCommands();

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder: " + getDataFolder().getAbsolutePath());
        }

        auditFile = new File(getDataFolder(), "audit.log");
        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("ta") != null) {
            getCommand("ta").setExecutor(this);
            getCommand("ta").setTabCompleter(this);
        }

        writeLifecycleLog("PLUGIN_ENABLED");
        getLogger().info("TransparentAudit enabled. Player + non-player command dispatches are audited.");
        getLogger().info("Sensitive command alerts are enabled for " + sensitiveCommands.size() + " command labels.");
    }

    @Override
    public void onDisable() {
        writeLifecycleLog("PLUGIN_DISABLED");
        getLogger().info("TransparentAudit disabled.");
    }

    private void loadSensitiveCommands() {
        sensitiveCommands.clear();
        List<String> configured = getConfig().getStringList("sensitive-commands");
        for (String value : configured) {
            String normalized = normalizeLabel(value);
            if (!normalized.isEmpty()) {
                sensitiveCommands.add(normalized);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        boolean sensitive = isSensitiveCommand(raw);
        String state = event.isCancelled() ? "§c[CANCELLED]" : "§a[DISPATCHED]";
        String format = getConfig().getString(
                "chat.player-command-format",
                "&8[&bAudit&8] &bPLAYER &f%player% &7→ &f%command% &7%state%"
        );
        String line = color(format
                .replace("%player%", player.getName())
                .replace("%command%", sanitizeForDisplay(raw))
                .replace("%state%", color(state)));

        recordAndBroadcast(line, raw, player.getName(), "PLAYER", sensitive, event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onServerCommand(ServerCommandEvent event) {
        String raw = event.getCommand();
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }

        boolean sensitive = isSensitiveCommand(raw);
        String state = event.isCancelled() ? "§c[CANCELLED]" : "§a[DISPATCHED]";
        String senderType = getSenderType(event.getSender());
        String sender = getSenderName(event.getSender());
        String format = getConfig().getString(
                "chat.server-command-format",
                "&8[&bAudit&8] &6%source% &f%sender% &7→ &f%command% &7%state%"
        );
        String line = color(format
                .replace("%source%", senderType)
                .replace("%sender%", sender)
                .replace("%command%", sanitizeForDisplay(raw))
                .replace("%state%", color(state)));

        recordAndBroadcast(line, raw, sender, senderType, sensitive, event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!getConfig().getBoolean("events.join-leave.enabled", true)) {
            return;
        }
        String message = color(getConfig().getString(
                "events.join-leave.join-format",
                "&8[&bAudit&8] &a+ &f%player% &7joined the server"
        ).replace("%player%", event.getPlayer().getName()));
        Bukkit.broadcastMessage(message);
        writePlainLog("JOIN", event.getPlayer().getName() + " joined the server");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        if (!getConfig().getBoolean("events.join-leave.enabled", true)) {
            return;
        }
        String message = color(getConfig().getString(
                "events.join-leave.quit-format",
                "&8[&bAudit&8] &c- &f%player% &7left the server"
        ).replace("%player%", event.getPlayer().getName()));
        Bukkit.broadcastMessage(message);
        writePlainLog("QUIT", event.getPlayer().getName() + " left the server");
    }

    private void recordAndBroadcast(String line, String rawCommand, String senderName,
                                    String sourceType, boolean sensitive, boolean cancelled) {
        commandCount++;
        if (sensitive) {
            sensitiveCount++;
        }

        String compact = timeNow() + " " + sourceType + " " + senderName + " -> "
                + sanitizeForDisplay(rawCommand)
                + (sensitive ? " [SENSITIVE]" : "")
                + (cancelled ? " [CANCELLED]" : "");
        addRecent(compact);

        if (getConfig().getBoolean("chat.broadcast-all-commands", true)) {
            Bukkit.broadcastMessage(line);
        }

        if (getConfig().getBoolean("file-log.enabled", true)) {
            writePlainLog(sensitive ? "SENSITIVE_COMMAND" : "COMMAND", sourceType + " " + senderName
                    + " -> " + sanitizeForDisplay(rawCommand)
                    + (cancelled ? " [CANCELLED]" : " [DISPATCHED]"));
        }

        if (sensitive && getConfig().getBoolean("alert.enabled", true)) {
            sendSensitiveAlert(sourceType, senderName, rawCommand, cancelled);
        }
    }

    private void sendSensitiveAlert(String sourceType, String senderName, String rawCommand, boolean cancelled) {
        String title = color(getConfig().getString("alert.title", "&c&l⚠ AUDIT ALERT"));
        String subtitleTemplate = getConfig().getString(
                "alert.subtitle", "&f%source% %player% used &e%command%"
        );
        String commandDisplay = sanitizeForDisplay(rawCommand);
        String subtitle = color(subtitleTemplate
                .replace("%source%", sourceType)
                .replace("%player%", senderName)
                .replace("%command%", commandDisplay)
                .replace("%state%", cancelled ? "CANCELLED" : "DISPATCHED"));

        int fadeIn = getConfig().getInt("alert.fade-in-ticks", 5);
        int stay = getConfig().getInt("alert.stay-ticks", 100);
        int fadeOut = getConfig().getInt("alert.fade-out-ticks", 10);

        Sound sound = readAlertSound();
        float volume = (float) getConfig().getDouble("alert.volume", 10.0D);
        float pitch = (float) getConfig().getDouble("alert.pitch", 2.0D);

        for (Player target : Bukkit.getOnlinePlayers()) {
            try {
                target.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                target.playSound(target.getLocation(), sound, volume, pitch);
            } catch (Throwable ignored) {
                // Keep the command audit working even if a fork has a sound/title quirk.
            }
        }

        if (getConfig().getBoolean("alert.chat-message", true)) {
            String alertChat = getConfig().getString(
                    "alert.chat-format",
                    "&c&l⚠ AUDIT ALERT &7| &f%source% &f%player% &7used &e%command% &7[%state%]"
            );
            Bukkit.broadcastMessage(color(alertChat
                    .replace("%source%", sourceType)
                    .replace("%player%", senderName)
                    .replace("%command%", commandDisplay)
                    .replace("%state%", cancelled ? "CANCELLED" : "DISPATCHED")));
        }
    }

    private Sound readAlertSound() {
        String configured = getConfig().getString("alert.sound", "NOTE_PLING");
        try {
            return Sound.valueOf(configured.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Unknown alert sound '" + configured + "'. Falling back to NOTE_PLING.");
            return Sound.NOTE_PLING;
        }
    }

    private void writePlainLog(String type, String message) {
        if (auditFile == null) {
            return;
        }
        String line = timeNow() + " [" + type + "] " + stripColor(message) + System.lineSeparator();
        try {
            if (!auditFile.exists()) {
                File parent = auditFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                auditFile.createNewFile();
            }
            FileWriter writer = new FileWriter(auditFile, true);
            try {
                writer.write(line);
            } finally {
                writer.close();
            }
        } catch (IOException ex) {
            getLogger().warning("Could not write audit.log: " + ex.getMessage());
        }
    }

    private void writeLifecycleLog(String event) {
        if (getConfig().getBoolean("file-log.enabled", true)) {
            writePlainLog("LIFECYCLE", event);
        }
    }

    private String timeNow() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date());
    }

    private void addRecent(String line) {
        recentCommands.addFirst(line);
        int limit = getConfig().getInt("file-log.recent-memory-limit", DEFAULT_RECENT_LIMIT);
        if (limit < 1) {
            limit = DEFAULT_RECENT_LIMIT;
        }
        while (recentCommands.size() > limit) {
            recentCommands.removeLast();
        }
    }

    private boolean isSensitiveCommand(String rawCommand) {
        String label = extractLabel(rawCommand);
        return sensitiveCommands.contains(label);
    }

    private String extractLabel(String rawCommand) {
        String command = rawCommand.trim();
        while (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isEmpty()) {
            return "";
        }

        String label = command.split("\\s+", 2)[0].toLowerCase(Locale.ENGLISH);
        int colon = label.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < label.length()) {
            label = label.substring(colon + 1);
        }
        return normalizeLabel(label);
    }

    private String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int colon = normalized.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) {
            normalized = normalized.substring(colon + 1);
        }
        return normalized;
    }

    private String sanitizeForDisplay(String command) {
        String text = command == null ? "" : command.trim();
        if (!text.startsWith("/")) {
            text = "/" + text;
        }
        // Prevent command arguments from being interpreted as Minecraft color codes in chat.
        return text.replace("§", "?").replace("&", "?");
    }

    private String getSenderName(CommandSender sender) {
        if (sender == null) {
            return "UNKNOWN";
        }
        return sender.getName();
    }

    private String getSenderType(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return "CONSOLE";
        }
        if (sender instanceof BlockCommandSender) {
            return "COMMAND_BLOCK";
        }
        return "SERVER";
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    private String stripColor(String value) {
        return ChatColor.stripColor(value == null ? "" : value);
    }

    private boolean isOwner(CommandSender sender) {
        // Deliberately case-sensitive, exactly as requested.
        return sender instanceof Player && OWNER_NAME.equals(((Player) sender).getName());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"ta".equalsIgnoreCase(command.getName())) {
            return false;
        }

        if (!isOwner(sender)) {
            sender.sendMessage(PREFIX + "§cUnknown command or insufficient access.");
            return true;
        }

        if (args.length == 0 || "status".equalsIgnoreCase(args[0]) || "stats".equalsIgnoreCase(args[0])) {
            sendStatus(sender);
            return true;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            sender.sendMessage(PREFIX + "§bSensitive command labels:");
            sender.sendMessage("§f" + joinSensitiveCommands());
            return true;
        }

        if ("recent".equalsIgnoreCase(args[0])) {
            int requested = 10;
            if (args.length >= 2) {
                try {
                    requested = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {
                    requested = 10;
                }
            }
            requested = Math.max(1, Math.min(requested, 25));
            sendRecent(sender, requested);
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            reloadConfig();
            loadSensitiveCommands();
            writeLifecycleLog("CONFIG_RELOADED_BY_" + OWNER_NAME);
            sender.sendMessage(PREFIX + "§aConfiguration reloaded. Auditing remains enabled.");
            sender.sendMessage(PREFIX + "§7Sensitive labels loaded: §f" + sensitiveCommands.size());
            return true;
        }

        if ("verify".equalsIgnoreCase(args[0])) {
            Bukkit.broadcastMessage(color(
                    "&8[&bAudit&8] &a✔ &fAudit system is online. "
                            + "&7Owner: &e" + OWNER_NAME + " &7| Commands tracked: &f" + commandCount
            ));
            writePlainLog("VERIFY", OWNER_NAME + " ran /ta verify");
            return true;
        }

        if ("help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(PREFIX + "§bTransparentAudit status");
        sender.sendMessage("§7Owner lock: §f" + OWNER_NAME + " §8(case-sensitive)");
        sender.sendMessage("§7Commands audited this session: §f" + commandCount);
        sender.sendMessage("§7Sensitive commands: §c" + sensitiveCount);
        sender.sendMessage("§7Online players: §f" + Bukkit.getOnlinePlayers().size());
        sender.sendMessage("§7Sensitive labels loaded: §f" + sensitiveCommands.size());
        sender.sendMessage("§7Broadcast all commands: §f" + getConfig().getBoolean("chat.broadcast-all-commands", true));
        sender.sendMessage("§7File log: §f" + (getConfig().getBoolean("file-log.enabled", true) ? auditFile.getPath() : "disabled"));
    }

    private void sendRecent(CommandSender sender, int requested) {
        sender.sendMessage(PREFIX + "§bRecent audited commands §7(last " + requested + "):");
        if (recentCommands.isEmpty()) {
            sender.sendMessage("§7(none yet)");
            return;
        }
        int index = 0;
        for (String line : recentCommands) {
            sender.sendMessage("§8" + line);
            index++;
            if (index >= requested) {
                break;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(PREFIX + "§bOwner audit commands:");
        sender.sendMessage("§f/ta status §7- session counters and configuration");
        sender.sendMessage("§f/ta recent [1-25] §7- recent commands kept in memory");
        sender.sendMessage("§f/ta list §7- sensitive command labels");
        sender.sendMessage("§f/ta reload §7- reload config without disabling the audit engine");
        sender.sendMessage("§f/ta verify §7- broadcast an audit-system verification");
    }

    private String joinSensitiveCommands() {
        List<String> values = new ArrayList<String>(sensitiveCommands);
        Collections.sort(values);
        return values.isEmpty() ? "(none)" : String.join(", ", values);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!"ta".equalsIgnoreCase(command.getName()) || !isOwner(sender)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return Arrays.asList("status", "stats", "recent", "list", "reload", "verify", "help");
        }
        if (args.length == 2 && "recent".equalsIgnoreCase(args[0])) {
            return Arrays.asList("5", "10", "15", "25");
        }
        return Collections.emptyList();
    }
}
