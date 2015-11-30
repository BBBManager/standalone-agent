package org.bbbmanager.agent.standalone.resource;

import org.apache.log4j.Logger;
import org.restlet.resource.Get;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class PingResource extends SecuredResource {
	private static final Logger log = Logger.getLogger(JoinResource.class);
        
	@Get
	public Object ping() {
            return "pong";
	}
	
}
