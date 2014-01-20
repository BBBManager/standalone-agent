package org.bbbmanager.agent.standalone.resource;

import org.bbbmanager.agent.common.servlet.Configuration;
import org.restlet.data.Status;
import org.restlet.resource.ServerResource;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class SecuredResource extends ServerResource {
	public Boolean checkKey() {
		String adminKeyReceived = (String) getRequest().getResourceRef().getQueryAsForm().getValues("adminKey");
		String adminKeyExpected = Configuration.getConfig("bbbmanager.adminKey");
		
		if(!adminKeyExpected.equals(adminKeyReceived)) {
			getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
			return false;
		}
		
		return true;
	}
}
