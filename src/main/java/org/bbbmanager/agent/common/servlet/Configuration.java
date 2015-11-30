package org.bbbmanager.agent.common.servlet;

import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

/**
 * --
 *
 * @author BBBManager Team <team@bbbmanager.org>
 *
 */
public class Configuration extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(Configuration.class);

    private static Configuration _instance;
    private HashMap<String, String> config;

    @Override
    public void init() throws ServletException {
        log.info("Starting servlet: Configuration");
        config = new HashMap<>();

        config.put("bbbmanager.adminKeyFile", "/var/bbbmanager/parameters/agent_key");
        config.put("common.http.timeout.read", "3000");
        config.put("common.http.timeout.connect", "3000");
        config.put("bbb.properties", "/var/lib/tomcat7/webapps/bigbluebutton/WEB-INF/classes/bigbluebutton.properties");
        config.put("bbb.poll.interval", "3000");
        config.put("events.publish.interval", "5000");

        //TODO make the configuration read from an optional file, that if does not exist, don't break
//		String configFilePath = System.getProperty("bbbmanager.configfile");
//		if(configFilePath == null) {
//			configFilePath = System.getenv("BBBMANAGER_CONF");
//		}
//		if(configFilePath == null) {
//			configFilePath = "/var/bbbmanager/agent/conf/bbbmanager.properties";
//			String errorMessage = "Not possible to read config location from env var BBBMANAGER_CONF and no config file parsed to JVM (with -Dbbbmanager.configfile), using default location: " + configFilePath;
//			log.info(errorMessage);
//		}
//		File configFile = new File(configFilePath);
//		FileInputStream configFileIS;
//		
//		log.debug("Starting servlet: Configuration - Reading config from file: " + configFile);
//		
//		try {
//			configFileIS = new FileInputStream(configFile);
//			Properties props = new Properties() ;
//			try {
//				props.load(configFileIS);
//				configFileIS.close();
//				
//				for(Object propName : props.keySet()) {
//					String propNameString = (String) propName;
//					config.put(propNameString.toLowerCase(), (String) props.get(propName));
//				}
//				
//				_instance = this;
//			} catch (IOException e) {
//				log.error("Starting servlet: Configuration - Error reading config file:  " + e.getMessage(), e);
//			}
//		} catch (FileNotFoundException e) {
//			log.error("Starting servlet: Configuration - Error reading config file:  " + e.getMessage(), e);
//		}
    }

    @Override
    public void destroy() {
        _instance = null;
    }

    public static Configuration getInstance() {
        if (_instance == null) {
            log.error("getInstance - Servlet was not initialized yet.");
        }
        return _instance;
    }

    public static String getConfig(String string) {
        return getInstance().config.get(string.toLowerCase());
    }

    public static String getVersion() {
        return "v0.020";
    }
}
