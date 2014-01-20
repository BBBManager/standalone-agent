package org.bbbmanager.agent.common.model.meeting;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.event.Event;
import org.bbbmanager.agent.common.model.server.BBBServer;
import org.bbbmanager.agent.common.repository.EventRepository;
import org.bbbmanager.common.util.XMLUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class Meeting implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(Meeting.class);
	
	//Information used to create a meeting
	@XStreamAsAttribute
	private String id; 
	@XStreamAsAttribute
	private String name;
	@XStreamAsAttribute
	private String welcomeMessage;
	@XStreamAsAttribute
	private Integer maxParticipants;
	@XStreamAsAttribute
	private Integer durationMinutes;
	@XStreamAsAttribute
	private Boolean record;
	@XStreamAsAttribute
	private String logoutURL;
	@XStreamAsAttribute
	private String callbackURL;
	
	//Information polled from server (getMeetingInfo)
	private Boolean recording;
	private Integer participantCount;
	private Integer moderatorCount;
	private String  metadata;
	private List<User> users;
	
	//Information polled from server (getMeetings)
	private String 	attendeePW;
	private String 	moderatorPW;
	private Boolean running;
	private Long createTime;
	
	//Reference to the BBB server that's running this meeting
	private BBBServer meetingServer;
	
	//This value is updated every time a meeting is saw in a server
	private Long lastTimeSeenOnServer = 0L;
	
	private Boolean meetingLockOnStart;
	private Boolean meetingMuteOnStart;
	private Boolean lockAllowModeratorLocking;
	private Boolean lockDisableMicForLockedUsers; 
	private Boolean lockDisableCamForLockedUsers;
	private Boolean lockDisablePublicChatForLockedUsers;
	private Boolean lockDisablePrivateChatForLockedUsers;
	
	//Users in meeting (updated from polling method and from join api)
	@XStreamOmitField
	private Integer userCount = 0;
	
	private void construct(String name, String id, String welcomeMessage, Integer maxParticipants, String logoutURL, Boolean record, Integer durationMinutes, String callbackURL,
			Boolean meetingLockOnStart, Boolean meetingMuteOnStart, Boolean lockAllowModeratorLocking, Boolean lockDisableMicForLockedUsers, 
			Boolean lockDisableCamForLockedUsers, Boolean lockDisablePublicChatForLockedUsers, Boolean lockDisablePrivateChatForLockedUsers) {
		setName(name);
		setId(id);
		setWelcomeMessage(welcomeMessage);
		setMaxParticipants(maxParticipants);
		setLogoutURL(logoutURL);
		setRecord(record);
		setDurationMinutes(durationMinutes);
		setCallbackURL(callbackURL);
		
		setMeetingLockOnStart(meetingLockOnStart);
		setMeetingMuteOnStart(meetingMuteOnStart);
		setLockAllowModeratorLocking(lockAllowModeratorLocking);
		setLockDisableCamForLockedUsers(lockDisableCamForLockedUsers);
		setLockDisableMicForLockedUsers(lockDisableMicForLockedUsers);
		setLockDisablePrivateChatForLockedUsers(lockDisablePrivateChatForLockedUsers);
		setLockDisablePublicChatForLockedUsers(lockDisablePublicChatForLockedUsers);
		
		users = new ArrayList<User>();
	}

	
	public Meeting(String name, String id, String welcomeMessage, Integer maxParticipants, String logoutURL, Boolean record, Integer durationMinutes, String callbackURL, Boolean meetingLockOnStart, Boolean meetingMuteOnStart, Boolean lockAllowModeratorLocking, Boolean lockDisableMicForLockedUsers, 
			Boolean lockDisableCamForLockedUsers, Boolean lockDisablePublicChatForLockedUsers, Boolean lockDisablePrivateChatForLockedUsers) {
		construct(name, id, welcomeMessage, maxParticipants, logoutURL, record, durationMinutes, callbackURL, 
				meetingLockOnStart, meetingMuteOnStart, lockAllowModeratorLocking, lockDisableMicForLockedUsers, 
				lockDisableCamForLockedUsers, lockDisablePublicChatForLockedUsers, lockDisablePrivateChatForLockedUsers);
	}
	
	public Meeting(String meetingID, String metadata) throws ParserConfigurationException, SAXException, IOException, ParseException {
		
		metadata = metadata.replace("&lt;", "<");
		metadata = metadata.replace("&gt;", ">");
		Document doc = XMLUtils.parseXMLFromString(metadata );
		
		meetingID = meetingID.toLowerCase();
		String _meetingID = doc.getElementsByTagName("meetingID").item(0).getTextContent().trim().toLowerCase();
		String _meetingName = doc.getElementsByTagName("meetingName").item(0).getTextContent().trim();
		
		
		if(!meetingID.equals(_meetingID)){
			throw new ParseException("Meeting ID received from URL is not equal to requested meetingID. Received: " + _meetingID + ", requested: " + meetingID, 1);
		}
		
		String _welcomeMessage = doc.getElementsByTagName("welcomeMessage").item(0).getTextContent().trim();
		Integer _maxParticipants = Integer.parseInt(doc.getElementsByTagName("maxParticipants").item(0).getTextContent().trim());
		Integer _durationMinutes = Integer.parseInt(doc.getElementsByTagName("durationMinutes").item(0).getTextContent().trim());
		Boolean _record = doc.getElementsByTagName("record").item(0).getTextContent().trim().equals("true");
		String _logoutURL = doc.getElementsByTagName("logoutURL").item(0).getTextContent().trim();
		String _callbackURL = doc.getElementsByTagName("callbackURL").item(0).getTextContent().trim();
		
		Boolean meetingLockOnStart = doc.getElementsByTagName("meetingLockOnStart").item(0).getTextContent().trim().equals("true");
		Boolean meetingMuteOnStart = doc.getElementsByTagName("meetingMuteOnStart").item(0).getTextContent().trim().equals("true");
		Boolean lockAllowModeratorLocking = doc.getElementsByTagName("lockAllowModeratorLocking").item(0).getTextContent().trim().equals("true");
		Boolean lockDisableMicForLockedUsers = doc.getElementsByTagName("lockDisableMicForLockedUsers").item(0).getTextContent().trim().equals("true");
		Boolean lockDisableCamForLockedUsers = doc.getElementsByTagName("lockDisableCamForLockedUsers").item(0).getTextContent().trim().equals("true");
		Boolean lockDisablePublicChatForLockedUsers = doc.getElementsByTagName("lockDisablePublicChatForLockedUsers").item(0).getTextContent().trim().equals("true");
		Boolean lockDisablePrivateChatForLockedUsers = doc.getElementsByTagName("lockDisablePrivateChatForLockedUsers").item(0).getTextContent().trim().equals("true");
		
		construct(_meetingName, _meetingID, _welcomeMessage, _maxParticipants, _logoutURL, _record, _durationMinutes, _callbackURL,
				meetingLockOnStart, meetingMuteOnStart, lockAllowModeratorLocking, lockDisableMicForLockedUsers, 
				lockDisableCamForLockedUsers, lockDisablePublicChatForLockedUsers, lockDisablePrivateChatForLockedUsers);
	}
	
	/**
	 * Return a copy of list of users currently in meeting
	 * */
	public List<User> getUsers() {
		ArrayList<User> userList;
		synchronized (users) {
			userList = new ArrayList<User>(users);
		}
		return userList;
	}
	
	/**
	 * This method was originally created to replace users list.
	 * We received a requirement to have events when user join a room, and when user leave a room.
	 * */
	public void setUsers(List<User> newUserList) {
		synchronized (users) {
			HashSet<User> removedUsers = new HashSet<User>();
			
			//Check for users that leaved the session
			for(User existingUser : users) {
				if(!newUserList.contains(existingUser)){
					removedUsers.add(existingUser);
				}
			}
			
			for(User removedUser : removedUsers) {
				EventRepository.getInstance().add(getCallbackURL(), new Event(Event.USER_LEAVE, removedUser.getId(), removedUser.getName(), this.getId(), this.getName(), removedUser.getIp()));
				users.remove(users.indexOf(removedUser));
				log.info(removedUser.getName() + " left meeting " + this.getName());
			}
			
			//Check for new users
			for(User newUser : newUserList) {
				if(!this.users.contains(newUser)){
					this.users.add(newUser);
					EventRepository.getInstance().add(getCallbackURL(), new Event(Event.USER_JOIN, newUser.getId(), newUser.getName(), this.getId(), this.getName(), newUser.getIp()));
					log.info(newUser.getName() + " joined meeting " + this.getName());
				}
			}
			this.userCount = users.size();
		}
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getWelcomeMessage() {
		return welcomeMessage;
	}
	public void setWelcomeMessage(String welcomeMessage) {
		this.welcomeMessage = welcomeMessage;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLogoutURL() {
		return logoutURL;
	}
	public void setLogoutURL(String logoutURL) {
		this.logoutURL = logoutURL;
	}
	public Integer getMaxParticipants() {
		return maxParticipants;
	}
	public void setMaxParticipants(Integer maxParticipants) {
		this.maxParticipants = maxParticipants;
	}
	public Boolean getRecord() {
		return record;
	}
	public void setRecord(Boolean record) {
		this.record = record;
	}
	public Integer getDurationMinutes() {
		return durationMinutes;
	}
	public void setDurationMinutes(Integer durationMinutes) {
		this.durationMinutes = durationMinutes;
	}
	
	public Boolean getRunning() {
		return running;
	}

	public void setRunning(Boolean running) {
		this.running = running;
	}

	public String getAttendeePW() {
		return attendeePW;
	}

	public void setAttendeePW(String attendeePW) {
		this.attendeePW = attendeePW;
	}

	public String getModeratorPW() {
		return moderatorPW;
	}

	public void setModeratorPW(String moderatorPW) {
		this.moderatorPW = moderatorPW;
	}

	public Boolean getRecording() {
		return recording;
	}

	public void setRecording(Boolean recording) {
		this.recording = recording;
	}

	public Integer getParticipantCount() {
		return participantCount;
	}

	public void setParticipantCount(Integer participantCount) {
		this.participantCount = participantCount;
	}

	public Integer getModeratorCount() {
		return moderatorCount;
	}

	public void setModeratorCount(Integer moderatorCount) {
		this.moderatorCount = moderatorCount;
	}

	public String getMetadata() {
		//If metadata is null, it was just created
		if(metadata == null){
			metadata = "<bbb-lb-meeting>" +
					"\t<meetingID>" + getId() + "</meetingID>" +
					"\t<meetingName>" + getName() + "</meetingName>" +
					"\t<welcomeMessage>" + getWelcomeMessage() + "</welcomeMessage>" +
					"\t<maxParticipants>" + getMaxParticipants() + "</maxParticipants>" +
					"\t<durationMinutes>" + getDurationMinutes() + "</durationMinutes>" +
					"\t<record>" + getRecord() + "</record>" +
					"\t<logoutURL>" + getLogoutURL() + "</logoutURL>" +
					"\t<callbackURL>" + getCallbackURL() + "</callbackURL>" +
					"\t<meetingLockOnStart>" + getMeetingLockOnStart() + "</meetingLockOnStart>" +
					"\t<meetingMuteOnStart>" + getMeetingMuteOnStart() + "</meetingMuteOnStart>" +
					"\t<lockAllowModeratorLocking>" + getLockAllowModeratorLocking() + "</lockAllowModeratorLocking>" +
					"\t<lockDisableMicForLockedUsers>" + getLockDisableMicForLockedUsers() + "</lockDisableMicForLockedUsers>" +
					"\t<lockDisableCamForLockedUsers>" + getLockDisableCamForLockedUsers() + "</lockDisableCamForLockedUsers>" +
					"\t<lockDisablePublicChatForLockedUsers>" + getLockDisablePublicChatForLockedUsers() + "</lockDisablePublicChatForLockedUsers>" +
					"\t<lockDisablePrivateChatForLockedUsers>" + getLockDisablePrivateChatForLockedUsers() + "</lockDisablePrivateChatForLockedUsers>" +
					"</bbb-lb-meeting>";
		}
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	public BBBServer getMeetingServer() {
		return meetingServer;
	}

	public void setMeetingServer(BBBServer meetingServer) {
		this.meetingServer = meetingServer;
	}

	public Long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Long createTime) {
		this.createTime = createTime;
	}


	public Long getLastTimeSeenOnServer() {
		return lastTimeSeenOnServer;
	}


	public void setLastTimeSeenOnServer(Long lastTimeSeenOnServer) {
		this.lastTimeSeenOnServer = lastTimeSeenOnServer;
	}

	public Integer getUserCount() {
		return userCount;
	}
	
	public void incrementUserCount() {
		synchronized (userCount) {
			userCount++;
		}
	}


	public String getCallbackURL() {
		return callbackURL;
	}


	public void setCallbackURL(String callbackURL) {
		this.callbackURL = callbackURL;
	}


	public Boolean getMeetingLockOnStart() {
		return meetingLockOnStart;
	}


	public void setMeetingLockOnStart(Boolean meetingLockOnStart) {
		this.meetingLockOnStart = meetingLockOnStart;
	}


	public Boolean getMeetingMuteOnStart() {
		return meetingMuteOnStart;
	}


	public void setMeetingMuteOnStart(Boolean meetingMuteOnStart) {
		this.meetingMuteOnStart = meetingMuteOnStart;
	}


	public Boolean getLockAllowModeratorLocking() {
		return lockAllowModeratorLocking;
	}


	public void setLockAllowModeratorLocking(Boolean lockAllowModeratorLocking) {
		this.lockAllowModeratorLocking = lockAllowModeratorLocking;
	}


	public Boolean getLockDisableMicForLockedUsers() {
		return lockDisableMicForLockedUsers;
	}


	public void setLockDisableMicForLockedUsers(
			Boolean lockDisableMicForLockedUsers) {
		this.lockDisableMicForLockedUsers = lockDisableMicForLockedUsers;
	}


	public Boolean getLockDisableCamForLockedUsers() {
		return lockDisableCamForLockedUsers;
	}


	public void setLockDisableCamForLockedUsers(
			Boolean lockDisableCamForLockedUsers) {
		this.lockDisableCamForLockedUsers = lockDisableCamForLockedUsers;
	}


	public Boolean getLockDisablePublicChatForLockedUsers() {
		return lockDisablePublicChatForLockedUsers;
	}


	public void setLockDisablePublicChatForLockedUsers(
			Boolean lockDisablePublicChatForLockedUsers) {
		this.lockDisablePublicChatForLockedUsers = lockDisablePublicChatForLockedUsers;
	}


	public Boolean getLockDisablePrivateChatForLockedUsers() {
		return lockDisablePrivateChatForLockedUsers;
	}


	public void setLockDisablePrivateChatForLockedUsers(
			Boolean lockDisablePrivateChatForLockedUsers) {
		this.lockDisablePrivateChatForLockedUsers = lockDisablePrivateChatForLockedUsers;
	}
}
