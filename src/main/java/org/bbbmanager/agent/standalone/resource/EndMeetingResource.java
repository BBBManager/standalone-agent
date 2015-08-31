package org.bbbmanager.agent.standalone.resource;


import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.meeting.Meeting;
import org.bbbmanager.agent.common.repository.MeetingRepository;
import org.bbbmanager.bigbluebutton.api.BigBlueButtonAPI;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.xml.sax.SAXException;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class EndMeetingResource extends SecuredResource {
	private static final Logger log = Logger.getLogger(EndMeetingResource.class);
	
	@Get
	public void endMeetingById() {
		if(checkKey()) {
			String idString = (String) getRequest().getAttributes().get("meetingID");
			MeetingRepository dao = MeetingRepository.getInstance();
			
			Meeting meeting = dao.getByID(idString);
			if(meeting == null){
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				return ;
			}
			try {
				BigBlueButtonAPI.endMeeting(meeting);
				getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
				return;
			} catch (IOException e) {
				log.error("Error calling endMeetingAPI: " + e.getMessage(), e);
			} catch (ParserConfigurationException e) {
				log.error("Error calling endMeetingAPI: " + e.getMessage(), e);
			} catch (SAXException e) {
				log.error("Error calling endMeetingAPI: " + e.getMessage(), e);
			}
			
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return ;
		}
	}
}
