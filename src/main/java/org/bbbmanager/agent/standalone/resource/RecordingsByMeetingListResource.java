package org.bbbmanager.agent.standalone.resource;

import java.util.List;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.recording.Recording;
import org.bbbmanager.agent.standalone.repository.RecordingRepository;
import org.restlet.resource.Get;

/**
 * This resource provides a listing of recordings on server
 *
 *
 */
public class RecordingsByMeetingListResource extends SecuredResource {

    private static final Logger log = Logger.getLogger(RecordingsByMeetingListResource.class);

    @Get
    public List<Recording> getRecordings() {
        if (checkKey()) {
            String idString = (String) getRequest().getAttributes().get("meetingID");
            return RecordingRepository.getInstance().getByMeeting(idString);
        } else {
            return null;
        }
    }

}
