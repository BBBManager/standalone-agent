package org.bbbmanager.agent.common.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class Configuration extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(Configuration.class);
	
	private static Configuration _instance;
	private HashMap<String, String> config;
	
	@Override
	public void init() throws ServletException {
		log.info("Starting servlet: Configuration");
		config = new HashMap<String, String>();
		String configFilePath = System.getProperty("bbbmanager.configfile");
		if(configFilePath == null) {
			configFilePath = System.getenv("BBBMANAGER_CONF");
		}
		if(configFilePath == null) {
			String errorMessage = "Environment variable not set: BBBMANAGER_CONF (you can use java -Dbbbmanager.configfile also)";
			log.error(errorMessage);
			throw new ServletException(errorMessage);
		}
		File configFile = new File(configFilePath);
		FileInputStream configFileIS;
		
		log.debug("Starting servlet: Configuration - Reading config from file: " + configFile);
		
		try {
			configFileIS = new FileInputStream(configFile);
			Properties props = new Properties() ;
			try {
				props.load(configFileIS);
				configFileIS.close();
				
				for(Object propName : props.keySet()) {
					String propNameString = (String) propName;
					config.put(propNameString.toLowerCase(), (String) props.get(propName));
				}
				
				_instance = this;
			} catch (IOException e) {
				log.error("Starting servlet: Configuration - Error reading config file:  " + e.getMessage(), e);
			}
		} catch (FileNotFoundException e) {
			log.error("Starting servlet: Configuration - Error reading config file:  " + e.getMessage(), e);
		}
		
	}
	
	@Override
	public void destroy() {
		_instance = null;
	}
	
	public static Configuration getInstance() {
		if(_instance == null) {
			log.error("getInstance - Servlet was not initialized yet.");
		}
		return _instance;
	}

	public static String getConfig(String string) {
		return getInstance().config.get(string.toLowerCase());
	}

	public static String getVersion() {
		return "v0.019";
	}
}
