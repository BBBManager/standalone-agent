package org.bbbmanager.agent.common.model.event;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class Event {
	public static final String USER_JOIN="user_join";
	public static final String USER_LEAVE="user_leave";
	
	private String eventName;
	private String userID;
	private String userName;
	private String meetingID;
	private String meetingName;
	private Long eventTimestamp;
	private String eventIPAddress;
	
	public Event(String eventName, String userID, String userName, String meetingID, String meetingName, String eventIPAddress) {
		this.setEventName(eventName);
		this.setUserID(userID);
		this.setUserName(userName);
		this.setMeetingID(meetingID);
		this.setMeetingName(meetingName);
		this.setEventTimestamp(System.currentTimeMillis()/1000);
		this.setEventIPAddress(eventIPAddress);
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getMeetingID() {
		return meetingID;
	}

	public void setMeetingID(String meetingID) {
		this.meetingID = meetingID;
	}

	public String getMeetingName() {
		return meetingName;
	}

	public void setMeetingName(String meetingName) {
		this.meetingName = meetingName;
	}

	public Long getEventTimestamp() {
		return eventTimestamp;
	}

	public void setEventTimestamp(Long eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getEventIPAddress() {
		return eventIPAddress;
	}

	public void setEventIPAddress(String eventIPAddress) {
		this.eventIPAddress = eventIPAddress;
	}
}
