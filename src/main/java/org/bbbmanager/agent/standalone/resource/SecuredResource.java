package org.bbbmanager.agent.standalone.resource;

import org.bbbmanager.agent.common.servlet.Configuration;
import org.restlet.data.Status;
import org.restlet.resource.ServerResource;

/**
 * --
 *
 * @author BBBManager Team <team@bbbmanager.org>
 *
 */
public class SecuredResource extends ServerResource {

    public Boolean checkKey() {
        try {
            String adminKeyReceived = (String) getRequest().getResourceRef().getQueryAsForm().getValues("adminKey");
            String adminKeyExpected = Configuration.getConfig("bbbmanager.adminKey");
            
            if(adminKeyExpected == null) {
                System.err.println("Expected key is null. Access denied.");
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                return false;
            }

            if (!adminKeyExpected.equals(adminKeyReceived)) {
                System.err.println("Invalid key. Received: " + adminKeyReceived + ", expected: " + adminKeyExpected);
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                return false;
            }

            return true;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
