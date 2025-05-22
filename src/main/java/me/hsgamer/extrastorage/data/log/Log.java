package me.hsgamer.extrastorage.data.log;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.configs.Setting;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

public final class Log {

    private final Setting setting;
    private final File logFolder;
    private Calendar cal;
    private File logFile;

    public Log(ExtraStorage instance) {
        this.setting = instance.getSetting();

        this.logFolder = new File(instance.getDataFolder(), "logs");
        if (!logFolder.exists()) logFolder.mkdirs();

        this.initLogFile();
    }

    public boolean initLogFile() {
        if ((!setting.isLogTransfer()) && (!setting.isLogWithdraw()) && (!setting.isLogSales())) return false;

        this.cal = Calendar.getInstance(TimeZone.getDefault());
        this.logFile = new File(logFolder, DateFormatUtils.format(cal, "dd-MM-yyyy") + ".txt");

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

        try {
            FileWriter writer = new FileWriter(logFile, true);
            String text, time = DateFormatUtils.format(cal, "HH:mm:ss"), itemName = setting.getNameFormatted(key, true);
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
            writer.write(text + '\n');
            writer.flush();
            writer.close();
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    public enum Action {
        WITHDRAW, TRANSFER, SELL
    }

}
