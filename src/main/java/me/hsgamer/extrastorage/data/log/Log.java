package me.hsgamer.extrastorage.data.log;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.configs.SettingConfig;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public final class Log {

    private final SettingConfig setting;
    private final File logFolder;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private volatile Calendar cal;
    private volatile File logFile;

    public Log(ExtraStorage instance) {
        this.setting = instance.getSetting();

        this.logFolder = new File(instance.getDataFolder(), "logs");
        if (!logFolder.exists()) logFolder.mkdirs();

        this.initLogFile();
    }

    public synchronized boolean initLogFile() {
        if ((!setting.log().transfer()) && (!setting.log().withdraw()) && (!setting.log().sales())) return false;

        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        String dateKey = dateFormat.format(now.getTime());
        File newFile = new File(logFolder, dateKey + ".txt");

        if (logFile != null && logFile.equals(newFile)) return true;

        this.cal = now;
        this.logFile = newFile;

        try {
            if (!logFile.exists()) logFile.createNewFile();
            return true;
        } catch (IOException error) {
            error.printStackTrace();
        }
        return false;
    }

    public void log(Player player, OfflinePlayer partner, Action action, String key, int amount, double price) {
        if (!this.initLogFile()) return;

        String text, time = timeFormat.format(cal.getTime()), itemName = setting.getNameFormatted(key, true);
        switch (action) {
            case SELL:
                text = String.format("[%s] %s sold x%d %s for: %.2f", time, player.getName(), amount, itemName, price);
                break;
            case TRANSFER:
                text = String.format("[%s] %s transfered x%d %s to %s's storage", time, player.getName(), amount, itemName, partner.getName());
                break;
            case WITHDRAW:
                text = String.format("[%s] %s withdrew x%d %s from %s's storage", time, player.getName(), amount, itemName, partner.getName());
                break;
            default:
                return;
        }

        File currentFile = this.logFile;
        if (currentFile == null) return;

        try (FileWriter writer = new FileWriter(currentFile, true)) {
            writer.write(text + '\n');
            writer.flush();
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    public enum Action {
        WITHDRAW, TRANSFER, SELL
    }

}
