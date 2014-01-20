package org.bbbmanager.agent.standalone.resource;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.meeting.Meeting;
import org.bbbmanager.agent.common.repository.MeetingRepository;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class MeetingResource extends SecuredResource {
	private static final Logger log = Logger.getLogger(MeetingResource.class);
	
	@Post
	public void insertMeeting(Meeting  newMeeting) throws IOException  {		if(checkKey()) {
			String idString = (String) getRequest().getAttributes().get("meetingID");
			
			if(idString == null || newMeeting == null){
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return;
			}
			
			MeetingRepository dao = MeetingRepository.getInstance();
			
			Meeting oldMeeting = dao.getByID(idString);
			if(oldMeeting != null) {
				getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT);
				return;
			} else {
				if(idString.equals(newMeeting.getId())) {
					dao.addMeeting(newMeeting);
				} else {
					getResponse().setStatus(Status.CLIENT_ERROR_FAILED_DEPENDENCY);
					return;
				}
			}
		}
	}
	
	@Put
	public void storeMeeting(Meeting  newMeeting) throws IOException  {
		if(checkKey()) {
			String idString = (String) getRequest().getAttributes().get("meetingID");
			
			if(idString == null || newMeeting == null){
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return;
			}
			
			MeetingRepository dao = MeetingRepository.getInstance();
			
			Meeting oldMeeting = dao.getByID(idString);
			if(oldMeeting != null) {
				dao.removeMeeting(oldMeeting);
			} 
			
			if(idString.equals(newMeeting.getId())) {
				dao.addMeeting(newMeeting);
			} else {
				getResponse().setStatus(Status.CLIENT_ERROR_FAILED_DEPENDENCY);
				return;
			}
		}
	}
	
	@Get
	public Object getMeetingById() {
		if(checkKey()) {
			String idString = (String) getRequest().getAttributes().get("meetingID");
			Meeting meeting = MeetingRepository.getInstance().getByID(idString);
			if(meeting == null){
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				
				return new Error("Meeting not found");
			}
			return meeting;
		} else return null;
	}
}
