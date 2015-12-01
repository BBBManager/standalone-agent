package org.bbbmanager.agent.common.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.meeting.Meeting;
import org.bbbmanager.agent.common.model.meeting.User;
import org.bbbmanager.agent.common.model.server.BBBServer;

/**
 * --
 *
 * @author BBBManager Team <team@bbbmanager.org>
 *
 */
public class MeetingRepository implements Serializable {

    private static final Logger log = Logger.getLogger(MeetingRepository.class);

    private static final long serialVersionUID = -859913537136051557L;
    private static MeetingRepository _instance;
    private ArrayList<Meeting> meetings;
    private HashMap<String, Meeting> meetingMap;

    public static MeetingRepository getInstance() {
        if (_instance == null) {
            _instance = new MeetingRepository();
        }
        return _instance;
    }

    public void addMeeting(Meeting meeting) {
        synchronized (meetings) {
            meetings.add(meeting);
            meetingMap.put(meeting.getId().toLowerCase(), meeting);
        }
    }

    public void removeMeeting(Meeting meeting) {
        synchronized (meetings) {
            if (meetings.contains(meeting)) {
                synchronized (meeting) {
                    //Set the user list to an empty list, so the events of all users exiting are called
                    meeting.setUsers(new ArrayList<User>());
                }

                meetings.remove(meeting);
                meetingMap.remove(meeting.getId().toLowerCase());
            }
        }
    }

    private void init() {
        meetings = new ArrayList<Meeting>();
        meetingMap = new HashMap<String, Meeting>();
    }

    public MeetingRepository() {
        init();
    }

    public ArrayList<Meeting> getAll() {
        return meetings;
    }

    public Meeting getByID(String id) {
        if (id == null) {
            return null;
        }
        return meetingMap.get(id.toLowerCase());
    }

    /**
     * Remove all meetings of a server from the memory
	 *
     */
    public void removeMeetingsByServer(BBBServer bbbServer) {
        synchronized (meetings) {
            Set<Meeting> meetingsToRemove = new HashSet<Meeting>();

            for (Meeting meeting : meetings) {
                BBBServer bbbServerOfMeeting = meeting.getMeetingServer();
                if (bbbServerOfMeeting != null && bbbServerOfMeeting.equals(bbbServer)) {
                    meetingsToRemove.add(meeting);
                }
            }

            for (Meeting meeting : meetingsToRemove) {
                removeMeeting(meeting);
            }
        }
    }

    public HashMap<BBBServer, Integer> getMeetingCountByServer() {
        HashMap<BBBServer, Integer> serverMeetingCount = new HashMap<BBBServer, Integer>();

        synchronized (meetings) {
            for (Meeting meeting : meetings) {
                BBBServer meetingServer = meeting.getMeetingServer();

                if (meetingServer != null) {
                    if (!serverMeetingCount.containsKey(meetingServer)) {
                        serverMeetingCount.put(meetingServer, 0);
                    }
                    serverMeetingCount.put(meetingServer, serverMeetingCount.get(meetingServer) + 1);
                }
            }
        }

        return serverMeetingCount;
    }

    /**
     * Remove all meetings of a server from the memory
	 *
     */
    public Set<Meeting> getMeetingsOfServer(BBBServer bbbServer) {
        Set<Meeting> meetingsOfServer = new HashSet<Meeting>();

        synchronized (meetings) {
            for (Meeting meeting : meetings) {
                BBBServer bbbServerOfMeeting = meeting.getMeetingServer();

                if (bbbServerOfMeeting != null && bbbServerOfMeeting.equals(bbbServer)) {
                    meetingsOfServer.add(meeting);
                }
            }
        }

        return meetingsOfServer;
    }
}
