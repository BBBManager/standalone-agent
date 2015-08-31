package org.bbbmanager.agent.standalone.resource;

import java.util.List;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.meeting.Meeting;
import org.bbbmanager.agent.common.repository.MeetingRepository;
import org.restlet.resource.Get;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class MeetingListResource extends SecuredResource {
	private static final Logger log = Logger.getLogger(MeetingListResource.class);
	
	@Get
	public List<Meeting> getMeetings() {
		if(checkKey()) {
			return MeetingRepository.getInstance().getAll();
		} else {
			return null;
		}
	}
	
}
