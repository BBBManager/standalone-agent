package org.bbbmanager.agent.common.servlet;

import java.io.File;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.bbbmanager.common.util.FileUtils;

/**
 * --
 *
 * @author BBBManager Team <team@bbbmanager.org>
 *
 */
public class Configuration extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(Configuration.class);

    private static Configuration _instance;
    private HashMap<String, String> config;

    @Override
    public void init() throws ServletException {
        log.info("Starting servlet: Configuration");
        config = new HashMap<>();

        config.put("bbbmanager.adminKey".toLowerCase(), FileUtils.getFileContents(new File("/var/bbbmanager/parameters/agent_key")));
        config.put("common.http.timeout.read".toLowerCase(), "3000");
        config.put("common.http.timeout.connect".toLowerCase(), "3000");
        config.put("bbb.properties".toLowerCase(), "/var/lib/tomcat7/webapps/bigbluebutton/WEB-INF/classes/bigbluebutton.properties");
        config.put("bbb.poll.interval".toLowerCase(), "3000");
        config.put("events.publish.interval".toLowerCase(), "5000");

        _instance = this;

        //TODO read from a file
    }

    @Override
    public void destroy() {
        _instance = null;
    }

    public static Configuration getInstance() {
        while (_instance == null) {
            log.info("getInstance - Servlet was not initialized yet, waiting 1 sec");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
        }
        return _instance;
    }

    public static String getConfig(String string) {
        return getInstance().config.get(string.toLowerCase());
    }

    public static String getVersion() {
        return "v0.020";
    }
}
