package org.bbbmanager.agent.standalone.resource;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.meeting.Meeting;
import org.bbbmanager.agent.common.model.meeting.User;
import org.bbbmanager.agent.common.model.server.BBBServer;
import org.bbbmanager.agent.common.repository.MeetingRepository;
import org.bbbmanager.agent.standalone.repository.ServerRepository;
import org.bbbmanager.bigbluebutton.api.BigBlueButtonAPI;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Get;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class JoinResource extends SecuredResource {
	private static final Logger log = Logger.getLogger(JoinResource.class);
	
	//This semaphore ensure that only one meeting will be created at time
	static private Semaphore createNewMeeting = new Semaphore(1);
	
	//To avoid a user joining twice, we need a local cache between the time we generate the url
	//and the time the user enter into meeting
	static private HashMap<User, Long> userXTimestamp = new HashMap<User, Long>();
	@Get
	public Object joinMeeting() {
		if(checkKey()) {
			Form params = getRequest().getResourceRef().getQueryAsForm();
			String meetingID = (String) params.getValues("meetingId");
			String meetingName = (String) params.getValues("meetingName");
			String logoutUrl = (String) params.getValues("logoutUrl");
			Integer maxParticipants = Integer.parseInt(params.getValues("maxParticipants"));
			Boolean record = ((String)params.getValues("record")).trim().equals("true") || ((String)params.getValues("record")).trim().equals("1");
			Integer durationMinutes = Integer.parseInt(params.getValues("durationMinutes"));
			String userFullName = (String) params.getValues("userFullName");
			String userId = (String) params.getValues("userId");
			String userRoleInMeeting = (String) params.getValues("userRoleInMeeting");
			String welcomeMessage = (String) params.getValues("welcomeMessage");
			String callbackURL = (String) params.getValues("callbackURL");
			String userIpAddress = (String) params.getValues("userIpAddress");
			
			String strMeetingLockOnStart = (String) params.getValues("meetingLockOnStart");
			if(strMeetingLockOnStart == null) strMeetingLockOnStart = "0";
			
			String strMeetingMuteOnStart = (String) params.getValues("meetingMuteOnStart");
			if(strMeetingMuteOnStart == null) strMeetingMuteOnStart = "0";
			
			String strLockAllowModeratorLocking = (String) params.getValues("lockAllowModeratorLocking");
			if(strLockAllowModeratorLocking == null) strLockAllowModeratorLocking = "0";
			
			String strLockDisableMicForLockedUsers = (String) params.getValues("lockDisableMicForLockedUsers");
			if(strLockDisableMicForLockedUsers == null) strLockDisableMicForLockedUsers = "0";
			
			String strLockDisableCamForLockedUsers = (String) params.getValues("lockDisableCamForLockedUsers");
			if(strLockDisableCamForLockedUsers == null) strLockDisableCamForLockedUsers = "0";
			
			String strLockDisablePublicChatForLockedUsers = (String) params.getValues("lockDisablePublicChatForLockedUsers");
			if(strLockDisablePublicChatForLockedUsers == null) strLockDisablePublicChatForLockedUsers = "0";
			
			String strLockDisablePrivateChatForLockedUsers = (String) params.getValues("lockDisablePrivateChatForLockedUsers");
			if(strLockDisablePrivateChatForLockedUsers == null) strLockDisablePrivateChatForLockedUsers = "0";
			
			Boolean meetingLockOnStart = strMeetingLockOnStart.equals("1");
			Boolean meetingMuteOnStart = strMeetingMuteOnStart.equals("1");
			Boolean lockAllowModeratorLocking = strLockAllowModeratorLocking.equals("1");
			Boolean lockDisableMicForLockedUsers = strLockDisableMicForLockedUsers.equals("1");
			Boolean lockDisableCamForLockedUsers = strLockDisableCamForLockedUsers.equals("1");
			Boolean lockDisablePublicChatForLockedUsers = strLockDisablePublicChatForLockedUsers.equals("1");
			Boolean lockDisablePrivateChatForLockedUsers = strLockDisablePrivateChatForLockedUsers.equals("1");
			
			User.getUserIPS().put(userId, userIpAddress);
			
			MeetingRepository meetingRepo = MeetingRepository.getInstance();
			
			log.trace("Received request to join user into meeting " + meetingID);
			Meeting meeting = meetingRepo.getByID(meetingID);
			
			if(meeting == null){
				log.trace("Meeting " + meetingID + " does not exists");
				try {
					log.trace("Waiting for semaphore");
					//Meeting does not exists yet, create it.
					createNewMeeting.acquire();
					log.trace("Semaphore acquired");
					
					//Ensure that meeting still not existing (could be created by other thread holding the semaphore).
					meeting = meetingRepo.getByID(meetingID);
					
					//If it still not existing
					if(meeting == null) {
						BBBServer nextServer = ServerRepository.getInstance().getServerForNewMeeting();
						if(nextServer == null){
							createNewMeeting.release();
							
							getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
							log.error("No server available to place this meeting");
							
							return new Exception("No server available");
						}
						
						try {
							Meeting newMeeting = new Meeting(meetingName, meetingID, welcomeMessage, maxParticipants, logoutUrl, record, durationMinutes, callbackURL,
									meetingLockOnStart, meetingMuteOnStart, lockAllowModeratorLocking, lockDisableMicForLockedUsers, 
									lockDisableCamForLockedUsers, lockDisablePublicChatForLockedUsers, lockDisablePrivateChatForLockedUsers);
							BigBlueButtonAPI.createMeeting(nextServer, newMeeting, newMeeting.getMetadata());
						} catch (Exception e){
							log.trace("Error: " + e.getMessage(), e);
							createNewMeeting.release();
							getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
							return e;
						}
						
						//BigBlueButtonAPI.createMeeting(nextServer, );
						createNewMeeting.release();
					} else {
						log.trace("Meeting was created by another thread.");
						
						//If after the acquire meeting exists, do nothing
						createNewMeeting.release();
					}
				} catch (InterruptedException e) {
					createNewMeeting.release();
					log.error("Error creating meeting: " + e.getMessage(), e);
					return false;
				}
			}
			
			//We have meeting, get user data
			meeting = meetingRepo.getByID(meetingID);
			
			if(meeting == null) {
				log.error("Meeting didn't appear after creation");
				return false;
			}
			
			//Allow to change max participants on the fly
			if(maxParticipants != null) {
				meeting.setMaxParticipants(maxParticipants);
			}
			
			if(meeting.getUserCount() >= meeting.getMaxParticipants()) {
				getResponse().setStatus(Status.SUCCESS_OK);
				Properties prop = new Properties();
				prop.setProperty("error", "Maximum number of participants exceeded");
				return prop;
			}
			
			synchronized (userXTimestamp) {
				Set<User> usersToRemove = new HashSet<User>(); 
				for(User user : userXTimestamp.keySet()) {
					//Remove urls generated more than 10s
					if( System.currentTimeMillis() > ( userXTimestamp.get(user) + 10000L) ) {
						usersToRemove.add(user);
					}
				}
				
				for(User user : usersToRemove) {
					userXTimestamp.remove(user);
				}
				
				User user = new User(userFullName, userId, userRoleInMeeting);
				
				if(meeting.getUsers().contains(user) || userXTimestamp.containsKey(user)) {
					Properties prop = new Properties();
					prop.setProperty("error", "The user is already in meeting");
					return prop;
				}
				
				userXTimestamp.put(user, System.currentTimeMillis());
				
				//Join user into meeting
				String joinURL = BigBlueButtonAPI.joinUser(meeting, userId, userFullName, userRoleInMeeting);
				
				if(joinURL != null) {
					Properties prop = new Properties();
					prop.setProperty("joinURL", joinURL);
					return prop;
				} else {
					return false;
				}
			}
		} 
		
		return null;
	}
	
}
