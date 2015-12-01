package org.bbbmanager.bigbluebutton.api;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.meeting.Meeting;
import org.bbbmanager.agent.common.model.meeting.User;
import org.bbbmanager.agent.common.model.server.BBBServer;
import org.bbbmanager.agent.common.repository.MeetingRepository;
import org.bbbmanager.agent.common.servlet.Configuration;
import org.bbbmanager.common.util.HTTPUtils;
import org.bbbmanager.common.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * API Integration with BigBlueButton server
 *
 * @author BBBManager Team <team@bbbmanager.org>
 *
 */
public abstract class BigBlueButtonAPI {

    /**
     * Logger
     */
    private static final Logger log = Logger.getLogger(BigBlueButtonAPI.class);

    /**
     * Path of API in server
     */
    private static final String apiPath = "bigbluebutton/api/";

    /**
     * Get the meeting list from the BigBlueButton server;
	 *
     */
    public static void pollMeetings(BBBServer server) {
        try {
            Semaphore pollSem = server.getPollSemaphore();
            pollSem.acquire();
            try {
                doPollMeetings(server);
            } catch (Exception e) {
                log.error("Error: " + e.getMessage(), e);
            } finally {
                pollSem.release();
            }
        } catch (InterruptedException e) {
            log.error("Error: " + e.getMessage(), e);
        }
    }

    /**
     * Get the meeting list from the BigBlueButton server;
	 *
     */
    private static void doPollMeetings(BBBServer server) {
        String method = "getMeetings";
        String url = server.getServerURL() + "/" + apiPath + method + "?checksum=" + generateChecksum(method, "", server.getServerSecuritySalt());

        try {
            log.trace("Calling getMeetings on server, URL: " + url);
            InputStream is = HTTPUtils.getUrlInputStream(url);
            Document doc = XMLUtils.parseXMLFromInputStream(is);
            doc.getElementById("response");

            String returnCode = doc.getElementsByTagName("returncode").item(0).getTextContent().trim();
            NodeList meetingsTag = doc.getElementsByTagName("meetings");

            if (returnCode.equals("SUCCESS") && meetingsTag != null || meetingsTag.getLength() != 0) {
                NodeList bbbMeetings = meetingsTag.item(0).getChildNodes();
                List<String> polledMeetings = new ArrayList<String>();

                for (int i = 0; i < bbbMeetings.getLength(); i++) {
                    Node xmlMeeting = bbbMeetings.item(i);

                    if ("meeting".equals(xmlMeeting.getNodeName())) {
                        Map<String, String> parsedProperties = new HashMap<String, String>();

                        NodeList xmlMeetingProperties = xmlMeeting.getChildNodes();
                        if (xmlMeetingProperties.getLength() == 0) {
                            log.error("Meeting has no property");
                        }

                        for (int j = 0; j < xmlMeetingProperties.getLength(); j++) {
                            Node xmlMeetingProperty = xmlMeetingProperties.item(j);
                            parsedProperties.put(xmlMeetingProperty.getNodeName(), xmlMeetingProperty.getTextContent().trim());
                        }

                        String meetingID = parsedProperties.get("meetingID").trim();
                        String meetingName = parsedProperties.get("meetingName").trim();
                        String attendeePW = parsedProperties.get("attendeePW").trim();
                        String moderatorPW = parsedProperties.get("moderatorPW").trim();
                        String running = parsedProperties.get("running").trim();
                        Long createTime = Long.parseLong(parsedProperties.get("createTime").trim());

                        log.debug("Meeting polled on BBB server " + server.getServerUUID() + ": " + meetingID + ", " + meetingName);

                        Meeting meeting = MeetingRepository.getInstance().getByID(meetingID);

                        HashMap<String, Object> meetingDetails = null;

                        if (meeting != null) {
                            BBBServer meetingServer = meeting.getMeetingServer();

							//The meeting is running in other server also, check for the createTime of meeting
                            //If the create time of this server is smaller, kill the room on this server and 
                            //keep the other one
                            if (meetingServer != null && !meetingServer.equals(server)) {
                                Long oldServerCreateTime = meeting.getCreateTime();
                                Long newServerCreateTime = createTime;

                                if (newServerCreateTime < oldServerCreateTime) {
                                    endMeetingInServer(meeting, server);

                                    //jump to next meeting
                                    continue;
                                }
                            }
                        } else {
                            meetingDetails = getMeetingDetailsFromServer(server, meetingID, moderatorPW);

                            String metadata = (String) meetingDetails.get("metadata");

                            if (metadata.contains("<bbbmanager-meeting>")) {
                                try {
                                    meeting = new Meeting(meetingID, metadata);
                                } catch (ParseException e) {
                                    log.error("Error creating meeting: " + e.getMessage(), e);

                                    //jump to next meeting
                                    continue;
                                }

                                MeetingRepository.getInstance().addMeeting(meeting);
                            } else {
                                log.error("BBB server " + server.getServerUUID() + " have a meeting that's not part of load balancer. MeetingID: " + meetingID);

                                //jump to next meeting
                                continue;
                            }
                        }

                        int attempts = 0;
                        while (meetingDetails == null) {
                            if (attempts > 5) {
                                break;
                            }
                            if (attempts > 0) {
                                log.trace("Last call to getMeetingDetailsFromServer failed. Waiting and trying again.");
                                try {
                                    Thread.sleep(250);
                                } catch (InterruptedException e) {
                                }
                            }
                            meetingDetails = getMeetingDetailsFromServer(server, meetingID, moderatorPW);
                            attempts++;
                        }

                        //Update basic information
                        meeting.setMeetingServer(server);
                        meeting.setModeratorPW(moderatorPW);
                        meeting.setAttendeePW(attendeePW);
                        meeting.setRunning(running.equals("true"));
                        meeting.setCreateTime(createTime);

                        @SuppressWarnings("unchecked")
                        List<User> userList = (List<User>) meetingDetails.get("attendees");

                        //Update detailed information
                        meeting.setRecording(meetingDetails.get("recording").equals("true"));
                        meeting.setParticipantCount(Integer.parseInt((String) meetingDetails.get("participantCount")));
                        meeting.setModeratorCount(Integer.parseInt((String) meetingDetails.get("moderatorCount")));
                        meeting.setMetadata((String) meetingDetails.get("metadata"));
                        meeting.setUsers(userList);

                        //Set the last time the meeting was seen on server
                        meeting.setLastTimeSeenOnServer(System.currentTimeMillis());

                        polledMeetings.add(meetingID);
                    } else {
                        log.error("Invalid tag found under meetings tag: " + xmlMeeting.getNodeName());
                    }
                }

                //Remove meetings of this server that was not polled
                Set<Meeting> meetingsOfServer = MeetingRepository.getInstance().getMeetingsOfServer(server);
                for (Meeting meeting : meetingsOfServer) {
                    if (!polledMeetings.contains(meeting.getId())) {
                        MeetingRepository.getInstance().removeMeeting(meeting);
                    }
                }
            } else {
                String messageKey = doc.getElementsByTagName("messageKey").item(0).getTextContent().trim();
                log.error("Error while getting meetings from server: xml returncode = " + returnCode + ", messageKey = " + messageKey);
            }
        } catch (IOException e) {
            log.error("Error while getting meetings from server: " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            log.error("Error while getting meetings from server: " + e.getMessage(), e);
        } catch (SAXException e) {
            log.error("Error while getting meetings from server: " + e.getMessage(), e);
        }
    }

    /**
     * End meeting in the server
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException 
	 *
     */
    private static boolean endMeetingInServer(Meeting meeting, BBBServer server) throws IOException, ParserConfigurationException, SAXException {
        String method = "end";
        String parameters = "meetingID=" + URLEncoder.encode(meeting.getId(), "UTF-8") + "&password=" + URLEncoder.encode(meeting.getModeratorPW(), "UTF-8");
        String url = server.getServerURL() + "/" + apiPath + method + "?" + parameters + "&checksum=" + generateChecksum(method, parameters, server.getServerSecuritySalt());

        log.debug("Ending meeting " + meeting.getId() + ", url: " + url);

        InputStream is = HTTPUtils.getUrlInputStream(url);
        Document doc = XMLUtils.parseXMLFromInputStream(is);
        doc.getElementById("response");

        String returnCode = doc.getElementsByTagName("returncode").item(0).getTextContent().trim();

        if (returnCode.equals("SUCCESS")) {
            log.debug("End meeting call returned SUCCESS.");
            is.close();
            return true;
        } else {
            log.debug("End meeting call returned " + returnCode + ".");
            is.close();
            return false;
        }
    }

    /**
     * End meeting in the server
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException 
	 *
     */
    private static HashMap<String, Object> getMeetingDetailsFromServer(BBBServer server, String meetingID, String moderatorPW) throws IOException, ParserConfigurationException, SAXException {
        String method = "getMeetingInfo";
        String parameters = "meetingID=" + URLEncoder.encode(meetingID, "UTF-8") + "&password=" + URLEncoder.encode(moderatorPW, "UTF-8");
        String url = server.getServerURL() + "/" + apiPath + method + "?" + parameters + "&checksum=" + generateChecksum(method, parameters, server.getServerSecuritySalt());

        log.debug("Getting meeting details " + meetingID + ", url: " + url);

        InputStream is = HTTPUtils.getUrlInputStream(url);
        Document doc = XMLUtils.parseXMLFromInputStream(is);
        Node response = doc.getElementsByTagName("response").item(0);

        String returnCode = doc.getElementsByTagName("returncode").item(0).getTextContent().trim();
        if (returnCode.equals("SUCCESS")) {
            HashMap<String, Object> meetingDetails = new HashMap<String, Object>();
            log.trace("Get meeting info returned SUCCESS.");

            NodeList xmlMeetingProperties = response.getChildNodes();
            if (xmlMeetingProperties.getLength() == 0) {
                log.error("Meeting has no property");
            }

            for (int j = 0; j < xmlMeetingProperties.getLength(); j++) {
                Node xmlMeetingProperty = xmlMeetingProperties.item(j);

                String name = xmlMeetingProperty.getNodeName();
                Object value;

                if (name.equals("attendees")) {
                    ArrayList<User> userList = new ArrayList<User>();

                    NodeList attendeesNodes = xmlMeetingProperty.getChildNodes();
                    for (int attendeeIdx = 0; attendeeIdx < attendeesNodes.getLength(); attendeeIdx++) {
                        Node attendeeNode = attendeesNodes.item(attendeeIdx);
                        NodeList attendeeNodeProps = attendeeNode.getChildNodes();
                        String userID = "", fullName = "", role = "";

                        for (int propIdx = 0; propIdx < attendeeNodeProps.getLength(); propIdx++) {
                            Node prop = attendeeNodeProps.item(propIdx);

                            if (prop.getNodeName() == "userID") {
                                userID = prop.getTextContent().trim();
                            } else if (prop.getNodeName() == "fullName") {
                                fullName = prop.getTextContent().trim();
                            } else if (prop.getNodeName() == "role") {
                                role = prop.getTextContent().trim();
                            }
                        }

                        userList.add(new User(fullName, userID, role));
                    }

                    value = userList;
                } else {
                    value = xmlMeetingProperty.getTextContent().trim();
                }

                meetingDetails.put(name, value);
            }

            //meetingDetails
            is.close();
            return meetingDetails;
        } else {
            log.debug("GetMeeting info call returned " + returnCode + ".");
            is.close();
            return null;
        }
    }

    /**
     * Generate the checksum based on BBB API specification:
     *
     * method + parameters + salt
	 *
     */
    public static String generateChecksum(String method, String parameters, String securitySalt) {
        return DigestUtils.shaHex(method + parameters + securitySalt);
    }

    /**
     * Create a meeting in bigbluebutton server
	 *
     */
    public static Boolean createMeeting(BBBServer server, Meeting newMeeting, String metadata) {
        String method = "create";
        String parameters;
        try {
            parameters = "meetingID=" + URLEncoder.encode(newMeeting.getId(), "UTF-8");
            parameters += "&name=" + URLEncoder.encode(newMeeting.getName(), "UTF-8");
            parameters += "&welcome=" + URLEncoder.encode(newMeeting.getWelcomeMessage(), "UTF-8");
            parameters += "&logoutURL=" + URLEncoder.encode(newMeeting.getLogoutURL(), "UTF-8");
            parameters += "&maxParticipants=" + newMeeting.getMaxParticipants();
            parameters += "&record=" + newMeeting.getRecord();
            parameters += "&duration=" + newMeeting.getDurationMinutes();
            parameters += "&meta_xml=" + URLEncoder.encode(metadata, "UTF-8");

            String url = server.getServerURL() + "/" + apiPath + method + "?" + parameters + "&checksum=" + generateChecksum(method, parameters, server.getServerSecuritySalt());

            log.debug("Creating meeting on BBB server " + newMeeting.getId() + ", url: " + url);

            InputStream is = HTTPUtils.getUrlInputStream(url);
            Document doc = XMLUtils.parseXMLFromInputStream(is);

            String returnCode = doc.getElementsByTagName("returncode").item(0).getTextContent().trim();
            if (returnCode.equals("SUCCESS")) {
                pollMeetings(server);

                return true;
            } else {
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Error: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Error: " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            log.error("Error: " + e.getMessage(), e);
        } catch (SAXException e) {
            log.error("Error: " + e.getMessage(), e);
        }

        return false;
    }

    /**
     *
     *
     */
    public static String joinUser(Meeting meeting, String userID,
            String userName, String userRole) {
        BBBServer server = meeting.getMeetingServer();
        if (server == null) {
            return null;
        }

        String method = "join";
        String parameters;
        try {
            String configToken = modifyConfigXML(meeting, server);

            parameters = "meetingID=" + URLEncoder.encode(meeting.getId(), "UTF-8");
            parameters += "&fullName=" + URLEncoder.encode(userName, "UTF-8");
            parameters += "&password=" + URLEncoder.encode(("M".equals(userRole) ? meeting.getModeratorPW() : meeting.getAttendeePW()), "UTF-8");

            if (meeting.getCreateTime() != null) {
                parameters += "&createTime=" + meeting.getCreateTime();
            }

            parameters += "&userID=" + URLEncoder.encode(userID, "UTF-8");

            parameters += "&configToken=" + URLEncoder.encode(configToken, "UTF-8");

            String url = server.getServerURL() + "/" + apiPath + method + "?" + parameters + "&checksum=" + generateChecksum(method, parameters, server.getServerSecuritySalt());
            log.trace("joinUser: API call URL: " + url);

            URL urlObj = new URL(url);

            Integer readTimeout = Integer.parseInt(Configuration.getConfig("common.http.timeout.read").trim());
            Integer connectTimeout = Integer.parseInt(Configuration.getConfig("common.http.timeout.connect").trim());

            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Request-Method", "GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setReadTimeout(readTimeout);
            connection.setConnectTimeout(connectTimeout);
            connection.setUseCaches(false);

            if (connection.getResponseCode() == 302) {
                String cookie = connection.getHeaderField("Set-Cookie");
                if (cookie != null) {
                    cookie = cookie.substring(0, cookie.indexOf(';'));
                }

                //String location = connection.getHeaderField("Location");
                String location = "/index-join.php";

                if (location.contains("?")) {
                    location += "&";
                } else {
                    location += "?";
                }
                location += cookie;

                meeting.incrementUserCount();

                return location;
            } else {
                connection.connect();

                InputStream is = connection.getInputStream();
                Document doc = XMLUtils.parseXMLFromInputStream(is);

                String message = doc.getElementsByTagName("message").item(0).getTextContent().trim();
                log.error("Failed to join the user: " + message);
                return null;
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Error: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Error: " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            log.error("Error: " + e.getMessage(), e);
        } catch (SAXException e) {
            log.error("Error: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * End meeting in the server
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException 
	 *
     */
    public static Set<String> getRecordings(BBBServer server, Boolean onlyPublished) throws IOException, ParserConfigurationException, SAXException {
        String method = "getRecordings";
        String parameters = "";
        String url = server.getServerURL() + "/" + apiPath + method + "?" + parameters + "&checksum=" + generateChecksum(method, parameters, server.getServerSecuritySalt());
        log.trace("getRecordings: API call URL: " + url);

        InputStream is = HTTPUtils.getUrlInputStream(url);
        Document doc = XMLUtils.parseXMLFromInputStream(is);

        String returnCode = doc.getElementsByTagName("returncode").item(0).getTextContent().trim();
        log.trace("getRecordings: API call returned: " + returnCode);

        if (returnCode.equals("SUCCESS")) {
            HashSet<String> recordingList = new HashSet<String>();
            is.close();
            NodeList recordings = doc.getElementsByTagName("recording");

            log.trace("getRecordings: API returned " + recordings.getLength() + " recordings");

            for (int recordIndex = 0; recordIndex < recordings.getLength(); recordIndex++) {
                Node recordingNode = recordings.item(recordIndex);

                NodeList recordingProperties = recordingNode.getChildNodes();
                Map<String, String> parsedProperties = new HashMap<String, String>();
                if (recordingProperties.getLength() == 0) {
                    log.error("Meeting has no property");
                }

                for (int j = 0; j < recordingProperties.getLength(); j++) {
                    Node xmlMeetingProperty = recordingProperties.item(j);
                    parsedProperties.put(xmlMeetingProperty.getNodeName(), xmlMeetingProperty.getTextContent().trim());
                }

                String recordID = parsedProperties.get("recordID").trim();
                String published = parsedProperties.get("published").trim();
                log.trace("getRecordings: API result:\trecordings[" + recordIndex + "] = {ID:" + recordID + ", published:" + published + "}");

                if (!onlyPublished || "true".equals(published)) {
                    log.trace("getRecordings: will return recording " + recordID);
                    recordingList.add(recordID);
                } else {
                    log.trace("getRecordings: will not return recording " + recordID);
                }
            }

            return recordingList;
        } else {
            log.debug("Get recordings call returned " + returnCode + ".");
            is.close();
            return null;
        }
    }

    public static boolean deleteRecording(BBBServer server, String recordingID) throws IOException, ParserConfigurationException, SAXException {
        String method = "deleteRecordings";
        String parameters = "recordID=" + recordingID;
        String url = server.getServerURL() + "/" + apiPath + method + "?" + parameters + "&checksum=" + generateChecksum(method, parameters, server.getServerSecuritySalt());
        log.trace("deleteRecording: API call URL: " + url);

        InputStream is = HTTPUtils.getUrlInputStream(url);
        Document doc = XMLUtils.parseXMLFromInputStream(is);
        String returnCode = doc.getElementsByTagName("returncode").item(0).getTextContent().trim();

        log.trace("deleteRecording: API call returned: " + returnCode);

        if (returnCode.equals("SUCCESS")) {
            is.close();
            return true;
        } else {
            is.close();
            return false;
        }
    }

    public static boolean endMeeting(Meeting meeting) throws IOException, ParserConfigurationException, SAXException {

        BBBServer server = meeting.getMeetingServer();

        if (server == null) {
            log.trace("endMeeting: server of meeting " + meeting.getId() + " is null, can't proceed");
            return false;
        }

        Semaphore pollSem = server.getPollSemaphore();
        try {
            pollSem.acquire();
        } catch (InterruptedException e) {
        }

        String method = "end";
        String parameters = "meetingID=" + meeting.getId();
        parameters += "&password=" + URLEncoder.encode(meeting.getModeratorPW(), "UTF-8");

        String url = server.getServerURL() + "/" + apiPath + method + "?" + parameters + "&checksum=" + generateChecksum(method, parameters, server.getServerSecuritySalt());
        log.trace("endMeeting: API call URL: " + url);

        InputStream is = HTTPUtils.getUrlInputStream(url);
        Document doc = XMLUtils.parseXMLFromInputStream(is);
        String returnCode = doc.getElementsByTagName("returncode").item(0).getTextContent().trim();

        log.trace("endMeeting: API call returned: " + returnCode);

        pollSem.release();

        if (returnCode.equals("SUCCESS")) {
            is.close();
            return true;
        } else {
            is.close();
            return false;
        }
    }

    /**
     * Modify the config xml on server and return the token.
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException 
	 *
     */
    private static String modifyConfigXML(Meeting meeting, BBBServer server) throws IOException, ParserConfigurationException, SAXException {
        String method = "getDefaultConfigXML";
        String parameters = "meetingID=" + URLEncoder.encode(meeting.getId(), "UTF-8") + "&password=" + URLEncoder.encode(meeting.getModeratorPW(), "UTF-8");
        String url = server.getServerURL() + "/" + apiPath + method + "?" + parameters + "&checksum=" + generateChecksum(method, parameters, server.getServerSecuritySalt());

        log.debug("Geting default configXML from meeting " + meeting.getId() + ", url: " + url);

        InputStream is = HTTPUtils.getUrlInputStream(url);
        Document doc = XMLUtils.parseXMLFromInputStream(is);
        doc.getElementById("response");

        boolean success = doc.getElementsByTagName("config").getLength() > 0;

        if (success) {

            String newConfigXML = null;

            log.debug("Get Default Config XML call returned SUCCESS.");

            Node configNode = doc.getElementsByTagName("config").item(0);

            Node oldLockNode = null, oldMeetingNode = null;
            if (doc.getElementsByTagName("lock").getLength() > 0) {
                oldLockNode = doc.getElementsByTagName("lock").item(0);
            }

            if (doc.getElementsByTagName("meeting").getLength() > 0) {
                oldMeetingNode = doc.getElementsByTagName("meeting").item(0);
            }

            Element lockNode = doc.createElement("lock");
            lockNode.setAttribute("disableMicForLockedUsers", meeting.getLockDisableMicForLockedUsers() ? "true" : "false");
            lockNode.setAttribute("disableCamForLockedUsers", meeting.getLockDisableCamForLockedUsers() ? "true" : "false");
            lockNode.setAttribute("disablePublicChatForLockedUsers", meeting.getLockDisablePublicChatForLockedUsers() ? "true" : "false");
            lockNode.setAttribute("disablePrivateChatForLockedUsers", meeting.getLockDisablePrivateChatForLockedUsers() ? "true" : "false");
            lockNode.setAttribute("lockOnJoin", meeting.getLockLockOnJoin() ? "true" : "false");
            lockNode.setAttribute("lockLayoutForLockedUsers", meeting.getLockLockLayoutForLockedUsers() ? "true" : "false");

            Element meetingNode = doc.createElement("meeting");
            meetingNode.setAttribute("muteOnStart", meeting.getMeetingMuteOnStart() ? "true" : "false");

            if (oldLockNode != null) {
                configNode.replaceChild(lockNode, oldLockNode);
            } else {
                configNode.appendChild(lockNode);
            }

            if (oldMeetingNode != null) {
                configNode.replaceChild(meetingNode, oldMeetingNode);
            } else {
                configNode.appendChild(meetingNode);
            }

            try {
                DOMSource domSource = new DOMSource(doc);
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.transform(domSource, result);
                newConfigXML = writer.toString();
            } catch (TransformerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            is.close();

            if (newConfigXML != null) {
                return setConfigXML(meeting, server, newConfigXML);
            }
            return null;
        } else {
            log.error("Get Default Config XML call didn't returned a config node.");
            is.close();
            return null;
        }
    }

    private static String setConfigXML(Meeting meeting, BBBServer server,
            String newConfigXML) throws ParserConfigurationException, SAXException, IOException {
        log.debug("Setting config.xml for meeting " + meeting.getName());
        String method = "setConfigXML";
        String parameters = "configXML=" + URLEncoder.encode(newConfigXML, "UTF-8") + "&" + "meetingID=" + URLEncoder.encode(meeting.getId(), "UTF-8");
        String url = server.getServerURL() + "/" + apiPath + method + ".xml";
        String fullParamList = parameters + "&checksum=" + generateChecksum(method, parameters, server.getServerSecuritySalt());

        String response = postURL(url, fullParamList, "application/x-www-form-urlencoded");
        Document doc = XMLUtils.parseXMLFromString(response);
        doc.getElementById("response");

        String returnCode = doc.getElementsByTagName("returncode").item(0).getTextContent().trim();

        if (returnCode.equals("SUCCESS")) {
            String configToken = doc.getElementsByTagName("configToken").item(0).getTextContent().trim();

            log.debug("SUCCESS on setting config.xml for meeting " + meeting.getName());
            log.debug("Config token is: " + configToken);
            return configToken;
        }
        log.error("FAIL on setting config.xml for meeting " + response);
        return null;
    }

    //TODO refactor this code (it was pasted here to use in the post)
    public static String postURL(String targetURL, String urlParameters) {
        return postURL(targetURL, urlParameters, "text/xml");
    }

    public static String postURL(String targetURL, String urlParameters, String contentType) {
        URL url;
        HttpURLConnection connection = null;
        int responseCode = 0;
        try {
            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", contentType);

            connection.setRequestProperty("Content-Length", ""
                    + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            //Get Response        
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();

        } catch (Exception e) {

            e.printStackTrace();
            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
