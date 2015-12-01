package org.bbbmanager.agent.standalone.resource;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.recording.Recording;
import org.bbbmanager.agent.standalone.repository.RecordingRepository;
import org.restlet.resource.Get;

public class RecordingResource extends SecuredResource {

    private static final Logger log = Logger.getLogger(RecordingResource.class);

    @Get
    public Recording getRecording() {
        if (checkKey()) {
            String idString = (String) getRequest().getAttributes().get("recordingID");
            return RecordingRepository.getInstance().getByID(idString);
        } else {
            return null;
        }
    }

}
