package utils;

import main.ArdentKt;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private static Config config;
    private final Map<String, String> keys;
    public static String ip = "158.69.214.251";

    public Config(String url) {
        keys = new HashMap<>();
        try {
            List<String> keysTemp = IOUtils.readLines(new FileReader(new File(url)));
            keysTemp.forEach(pair -> {
                String[] keyPair = pair.split(" :: ");
                if (keyPair.length == 2) keys.put(keyPair[0], keyPair[1]);
            });
        } catch (IOException e) {
            System.out.println("Unable to load Config....");
            e.printStackTrace();
            System.exit(1);
        }
        config = this;
        ArdentKt.setConn(ArdentKt.getR().connection().db("ardent").hostname(ip).port(28015).connect());
    }

    public String getValue(String keyName) {
        return keys.getOrDefault(keyName, "not_available");
    }

    public static Config getConfig() {
        return config;
    }
}
