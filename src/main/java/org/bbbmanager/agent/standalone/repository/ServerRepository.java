package org.bbbmanager.agent.standalone.repository;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.server.BBBServer;

/**
 * Repository of servers, on the standalone implementation, it contains only one
 * server.
 *
 * @author BBBManager Team <team@bbbmanager.org>
 *
 */
public class ServerRepository {

    /**
     * Logger
     */
    private static final Logger log = Logger.getLogger(ServerRepository.class);

    /**
     * Instance to this object.
     */
    private static ServerRepository _instance;

    /**
     * Map server UUID x BBBServer instance
     *
     */
    private BBBServer bbbServer;

    /**
     * Start a thread that check for died servers
     *
     */
    public ServerRepository() {

    }

    /**
     * Get the server with smallest number of meetings
     *
     * @return
     */
    public BBBServer getServerForNewMeeting() {
        return getServer();
    }

    /**
     * Get instance of ServersDAO
     *
     * @return
     */
    public static ServerRepository getInstance() {
        if (_instance == null) {
            _instance = new ServerRepository();
        }
        return _instance;
    }

    /**
     * Add server to memory and send notification about this to subscribed
     * objects.
     *
     * @param bbbServer
     */
    public void setServer(BBBServer bbbServer) {
        this.bbbServer = bbbServer;
    }

    public BBBServer getServer() {
        return bbbServer;
    }
}
