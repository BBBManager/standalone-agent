package org.bbbmanager.agent.common.model.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
@XStreamAlias("server")
public class BBBServer {
	private static final Logger log = Logger.getLogger(BBBServer.class);
	
	private String	serverUUID;
	private String	serverURL;
	private String	serverHost;
	private String	serverIP;
	
	@XStreamOmitField
	private String	serverSecuritySalt;
	
	private String	defaultClientUrl;
	private Long 	tsLastAnnounce;
	
	@XStreamOmitField
	private Semaphore pollSemaphore = new Semaphore(1);
	
	public BBBServer(String serverUUID, String serverURL, String securitySalt, String defaultClientUrl, String serverIP) throws MalformedURLException {
		URL url;
		url = new URL(serverURL);
		
		this.setServerUUID(serverUUID);
		this.setServerURL(serverURL);
		this.setServerSecuritySalt(securitySalt);
		this.setDefaultClientUrl(defaultClientUrl);
		this.setTsLastAnnounce(System.currentTimeMillis());
		this.setServerHost(url.getHost());
		this.setServerIP(serverIP);
	}

	public String getServerUUID() {
		return serverUUID;
	}

	public void setServerUUID(String serverUUID) {
		this.serverUUID = serverUUID;
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public String getServerSecuritySalt() {
		return serverSecuritySalt;
	}

	public void setServerSecuritySalt(String serverSecuritySalt) {
		this.serverSecuritySalt = serverSecuritySalt;
	}

	public String getDefaultClientUrl() {
		return defaultClientUrl;
	}

	public void setDefaultClientUrl(String defaultClientUrl) {
		this.defaultClientUrl = defaultClientUrl;
	}

	public Long getTsLastAnnounce() {
		return tsLastAnnounce;
	}

	public void setTsLastAnnounce(Long tsLastAnnounce) {
		this.tsLastAnnounce = tsLastAnnounce;
	}

	public Semaphore getPollSemaphore() {
		return pollSemaphore;
	}

	public String getServerHost() {
		return serverHost;
	}

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public String getServerIP() {
		return serverIP;
	}

	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}
}
