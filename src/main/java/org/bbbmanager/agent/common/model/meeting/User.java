package org.bbbmanager.agent.common.model.meeting;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class User implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** List of User IPs. Used in event API.*/
	private static ConcurrentHashMap<String, String> userIPS = new ConcurrentHashMap<String, String>();
	
	private String name;
	
	@XStreamAsAttribute
	private String id;
	private String role; 
	private String ip;
	
	public User(String name, String id, String role) {
		this.setName(name);
		this.setId(id);
		this.setRole(role);
		String userIp=getUserIPS().get(id);
		if(userIp == null)
			userIp = "0.0.0.0";
		this.setIp(userIp);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof User)
			return (this.getId().equals(((User)obj).getId()));
		else
			return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public static ConcurrentHashMap<String, String> getUserIPS() {
		return userIPS;
	}
	
}
