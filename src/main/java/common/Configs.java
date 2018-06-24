package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Handles the system configurations,
 * loaded from a config.properties file.
 *
 * Created by halmeida on 1/26/17.
 */
public class Configs {

    private Properties props;
    private final String configFileParam = "config.properties";
    private static Configs INSTANCE = new Configs();

    public static Configs getInstance() {
        return INSTANCE;
    }

    private Configs(){
        this.props = new Properties();
        setConfigFileParam();
        String configFile = System.getProperty(configFileParam);
        try {
            this.props.load(new FileInputStream(new File(configFile)));
        } catch (IOException ex) {
            System.err.print("Could not load resources " + configFile +  ex);
        }
    }

    private void setConfigFileParam() {
        if (System.getProperty(configFileParam) == null) {
            System.setProperty(configFileParam, "./config.properties");
        }
    }

    public Properties getProps() {
        return this.props;
    }
}
