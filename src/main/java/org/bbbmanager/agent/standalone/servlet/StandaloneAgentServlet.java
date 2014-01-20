package org.bbbmanager.agent.standalone.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.server.BBBServer;
import org.bbbmanager.agent.common.repository.MeetingRepository;
import org.bbbmanager.agent.common.servlet.Configuration;
import org.bbbmanager.agent.standalone.repository.ServerRepository;
import org.bbbmanager.bigbluebutton.api.BigBlueButtonAPI;

/**
 *  
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class StandaloneAgentServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(StandaloneAgentServlet.class);
	
	private static StandaloneAgentServlet _instance;
	private HashMap<String, String> config;
	
	/** Flag that's used to stop threads*/
	private Boolean destroyed = false;
	
	@Override
	public void destroy() {
		super.destroy();
		synchronized (destroyed) {
			destroyed=true;
		}
		_instance = null;
	}
	
	@Override
	public void init() throws ServletException {
		log.info("Starting servlet: " + this.getClass().getSimpleName());
		File bbbPropertiesFile = new File(Configuration.getConfig("bbb.properties"));
		if(!bbbPropertiesFile.exists()) {
			log.error("Property bbb.properties is invalid, the file does not exists");
			return;
		} else {
			FileInputStream bbbPropertiesFileStream;
			try {
				bbbPropertiesFileStream = new FileInputStream(bbbPropertiesFile);
				Properties bbbConf = new Properties();
				bbbConf.load(bbbPropertiesFileStream);
				bbbPropertiesFileStream.close();
				
				String securitySalt = bbbConf.getProperty("securitySalt");
				String serverURL = bbbConf.getProperty("bigbluebutton.web.serverURL");
				String serverUUID = UUID.randomUUID().toString();
				String defaultClientUrl = bbbConf.getProperty("defaultClientUrl");
				String serverIP = getServerIp();
				
				defaultClientUrl = defaultClientUrl.replaceAll(Pattern.quote("/client/BigBlueButton.html"), "/bbbmanager-standalone-agent/index.jsp");
				defaultClientUrl = defaultClientUrl.replaceAll(Pattern.quote("${bigbluebutton.web.serverURL}"), serverURL);
				
				
				ServerRepository.getInstance().setServer(new BBBServer(serverUUID, serverURL, securitySalt, defaultClientUrl, serverIP));
			} catch (IOException e) {
				String errorMessage = "Error reading configurations from file " + bbbPropertiesFile.getName() + ", error was: " + e.getMessage();
				log.error(errorMessage, e);
				throw new ServletException(errorMessage);
			}
		}
		
		Thread thPollBBBServers = new Thread(
				new Runnable() {
					public void run() {
						final AtomicReference<Boolean> forcePoll = new AtomicReference<Boolean>();
						Long sleepTime = 100L;
						Long sleepTotalTime = 0L;
						
						forcePoll.set(true);
						
						Long pollInterval;
						
						try{
							pollInterval = Long.parseLong(Configuration.getConfig("bbb.poll.interval"));
						} catch (Exception e) {
							log.error("Error parsing configuration lb.poll.interval. Default will be used.");
							pollInterval = 10000L;
						}
						while(true) {
							try {
								if(destroyed) return;
								
								if(forcePoll.getAndSet(false) || sleepTotalTime > pollInterval) {
									sleepTotalTime = 0L;
									BBBServer server = ServerRepository.getInstance().getServer();
									BigBlueButtonAPI.pollMeetings(server);
								}
								
								sleepTotalTime += sleepTime;
								Thread.sleep(sleepTime);
							} catch (InterruptedException e) {
							}
						}
					}
				}
			);
		thPollBBBServers.setName("PollBBBServers");
		thPollBBBServers.start();
	}
	
	private String getServerIp() throws ServletException {
		String ipCommand = Configuration.getConfig("common.detect-ip-command");
		String ipAddress = null;
		
		try {
			ProcessBuilder pb = new ProcessBuilder(ipCommand);
			Process p = pb.start();
			
			InputStream procInput = p.getInputStream();
			InputStreamReader ir = new InputStreamReader(procInput);
			BufferedReader br = new BufferedReader(ir);
			
			
			
			String line = null;
			
			while((line = br.readLine()) != null){
				line = line.trim();
				
				if(line.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
					ipAddress = line;
				}
			}
			
			if(ipAddress == null){
				log.error("init: Unable to detect ip address");
				throw new ServletException("init: Unable to detect ip address.");
			} else {
				log.info("init: Server IP address: " + ipAddress);
			}
			
		} catch (IOException e1) {
			log.error("Error executing IP detection command: " + e1.getMessage(), e1);
			throw new ServletException("Error executing IP detection command: " + e1.getMessage());
		}
		
		return ipAddress;
	}
	
	public static StandaloneAgentServlet getInstance() {
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
