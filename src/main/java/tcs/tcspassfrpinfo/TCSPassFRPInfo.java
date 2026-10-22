package tcs.tcspassfrpinfo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class TCSPassFRPInfo extends JavaPlugin {

    private String token;
    private String checkinToken;
    private int refreshSeconds;

    private long totalTraffic = 0;
    private long usedTraffic = 0;
    private long remainingTraffic = 0;
    private double usagePercent = 0.0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TCSPAPI(this).register();
        }

        startAutoRefresh();

        getLogger().info("tcspassfrpinfo 插件已启用");
    }

    @Override
    public void onDisable() {
        getLogger().info("tcspassfrpinfo 插件已禁用");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        token = config.getString("token", "");
        checkinToken = config.getString("checkin_token", "");
        refreshSeconds = config.getInt("refresh_seconds", 300);

        if (token.isEmpty()) {
            getLogger().warning("配置文件中的 token 为空，请设置 token！");
        }
        if (checkinToken.isEmpty()) {
            getLogger().warning("配置文件中的 checkin_token 为空，请设置 checkin_token！");
        }

        totalTraffic = 0;
        usedTraffic = 0;
        remainingTraffic = 0;
        usagePercent = 0.0;

        getLogger().info("checkinToken 已更新为: " + checkinToken);
    }

    private void startAutoRefresh() {
        new BukkitRunnable() {
            @Override
            public void run() {
                fetchTrafficInfo(null);
            }
        }.runTaskTimerAsynchronously(this, 0L, refreshSeconds * 20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tcspassfrpinfo.admin")) {
            sender.sendMessage("§c你没有权限使用该命令！");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            if (sender instanceof Player) {
                fetchTrafficInfo((Player) sender);
            } else {
                sender.sendMessage("§a控制台无法使用 info 命令，但可以使用 reload 命令");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("reload")) {
            loadConfig();
            sender.sendMessage("§a配置已重新加载！");
            return true;
        } else if (args[0].equalsIgnoreCase("checkin")) {
            if (!getConfig().getBoolean("enable_manual_checkin", true)) {
                sender.sendMessage("§c手动签到功能未启用！");
                return true;
            }
            manualCheckIn(sender);
            return true;
        } else {
            sender.sendMessage("§c用法: /tcspassfrpinfo <info|reload|checkin>");
            return true;
        }
    }

    private void manualCheckIn(CommandSender sender) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.passnat.com/user/checkIn");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + checkinToken);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    conn.setRequestProperty("Referer", "https://dashboard.passnat.com/");
                    conn.setRequestProperty("Origin", "https://dashboard.passnat.com");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    int status = conn.getResponseCode();
                    if (status != 200) {
                        sender.sendMessage("§c签到失败: HTTP " + status);
                        return;
                    }

                    JsonObject json = JsonParser.parseReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                    ).getAsJsonObject();

                    String msg = json.has("msg") ? json.get("msg").getAsString() : "未知";
                    if (json.get("code").getAsInt() == 10000) {
                        sender.sendMessage("§a签到成功: " + msg);
                    } else {
                        sender.sendMessage("§c签到失败: " + msg);
                    }

                } catch (Exception e) {
                    sender.sendMessage("§c签到异常: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(this);
    }

    private void fetchTrafficInfo(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.passnat.com/user/userInfo");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    conn.setRequestProperty("Referer", "https://dashboard.passnat.com/");
                    conn.setRequestProperty("Origin", "https://dashboard.passnat.com");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    int status = conn.getResponseCode();
                    if (status != 200) {
                        if (player != null) player.sendMessage("§c请求失败: HTTP " + status);
                        return;
                    }

                    JsonObject json = JsonParser.parseReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                    ).getAsJsonObject();

                    if (json.get("code").getAsInt() != 10000) {
                        if (player != null) player.sendMessage("§c请求失败: " + json.get("msg").getAsString());
                        return;
                    }

                    JsonObject plan = json.getAsJsonObject("data").getAsJsonObject("plan_info");
                    long planTraffic = plan.get("plan_traffic").getAsLong();
                    long used = plan.get("used_traffic").getAsLong();
                    long addon = plan.has("addon_traffic") ? plan.get("addon_traffic").getAsLong() : 0;
                    long checkIn = plan.has("check_in_traffic") ? plan.get("check_in_traffic").getAsLong() : 0;

                    totalTraffic = planTraffic + addon + checkIn;
                    usedTraffic = used;
                    remainingTraffic = totalTraffic - usedTraffic;
                    usagePercent = (double) usedTraffic / totalTraffic * 100;

                    if (player != null) {
                        player.sendMessage("§a--- FRP 账户流量信息 ---");
                        player.sendMessage("§e套餐名称: §f" + plan.get("plan_name").getAsString());
                        player.sendMessage("§e总流量: §f" + String.format("%.2f", totalTraffic / 1024.0 / 1024 / 1024) + " GB");
                        player.sendMessage("§e已使用: §f" + String.format("%.2f", usedTraffic / 1024.0 / 1024 / 1024) + " GB");
                        player.sendMessage("§e剩余流量: §f" + String.format("%.2f", remainingTraffic / 1024.0 / 1024 / 1024) + " GB");
                        player.sendMessage("§e使用比例: §f" + String.format("%.2f", usagePercent) + "%");
                        player.sendMessage("§e当前隧道数量: §f" + plan.get("tunnel_count").getAsInt() + " / " + plan.get("tunnel_limit").getAsInt());
                    }

                } catch (Exception e) {
                    if (player != null) player.sendMessage("§c请求异常: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(this);
    }

    public class TCSPAPI extends PlaceholderExpansion {

        private final TCSPassFRPInfo plugin;

        public TCSPAPI(TCSPassFRPInfo plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String getIdentifier() {
            return "tcspassfrpinfo";
        }

        @Override
        public String getAuthor() {
            return "tcs";
        }

        @Override
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public String onPlaceholderRequest(Player player, String identifier) {
            switch (identifier.toLowerCase()) {
                case "total":
                    return String.format("%.2f", totalTraffic / 1024.0 / 1024 / 1024);
                case "used":
                    return String.format("%.2f", usedTraffic / 1024.0 / 1024 / 1024);
                case "remaining":
                    return String.format("%.2f", remainingTraffic / 1024.0 / 1024 / 1024);
                case "percent":
                    return String.format("%.2f", usagePercent);
                default:
                    return null;
            }
        }
    }
}
