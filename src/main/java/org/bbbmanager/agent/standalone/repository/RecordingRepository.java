package org.bbbmanager.agent.standalone.repository;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.recording.Recording;
import org.xml.sax.SAXException;

/**
 * --
 * 
 * */
public class RecordingRepository implements Serializable {
	private static final Logger log = Logger.getLogger(RecordingRepository.class);
	
	private static final long serialVersionUID = -859913537136051557L;
	private static RecordingRepository _instance;
	private ArrayList<Recording> recordings;
	private HashMap<String, Recording> recordingMap;
	private static Long lastUpdate = 0L;
	private HashMap<String, Set<Recording>> recordingsByMeetingIDMap;
	
	/**
	 * Get / create an instance of this object
	 * */
	public static RecordingRepository getInstance() {
		if(_instance == null) {
			_instance = new RecordingRepository();
		}
		return _instance;
	}
	
	/**
	 * Constructor, must be called from getInstance 
	 * */
	public RecordingRepository() {
		recordings = new ArrayList<Recording>();
		recordingMap = new HashMap<String, Recording>();
		recordingsByMeetingIDMap = new HashMap<String, Set<Recording>>();
		update();
	}

	/**
	 * Update recording list from server
	 * */
	public void update() {
		synchronized (lastUpdate) {
			lastUpdate = System.currentTimeMillis();
		
			File recordingFolder = new File(Recording.getBaseFolder());
			
			File [] recordingDirList = recordingFolder.listFiles();
			HashSet<String> listedRecordings = new HashSet<String>();
			
			for(File recordingDir : recordingDirList) {
				try {
					String recordingID = recordingDir.getName();
					listedRecordings.add(recordingID);
					
					if(!recordingMap.containsKey(recordingID)) {
						Recording recording = new Recording(recordingDir);
						
						if(recordingID.equals(recording.getRecordingID())) {
							log.trace("update: adding recording " + recordingID);
							recordings.add(recording);
							recordingMap.put(recordingID, recording);
							
							String meetingID = recording.getMeetingID();
							if(!recordingsByMeetingIDMap.containsKey(meetingID)){
								recordingsByMeetingIDMap.put(meetingID, new HashSet<Recording>());
							}
							recordingsByMeetingIDMap.get(meetingID).add(recording);
						} else {
							log.error("Invalid recording object. Parsed ID is different from folder name. Parsed: " + recording.getRecordingID() + ", Folder: " + recordingID);
						}
					}
				} catch (IOException e) {
					log.error("Error: " + e.getMessage(), e);
				} catch (ParserConfigurationException e) {
					log.error("Error: " + e.getMessage(), e);
				} catch (SAXException e) {
					log.error("Error: " + e.getMessage(), e);
				}
			}
			
			synchronized (recordingMap) {
				HashSet<String> recordingsToDelete = new HashSet<String>();
				for(String recordingID : recordingMap.keySet()) {
					if(!listedRecordings.contains(recordingID)){
						recordingsToDelete.add(recordingID);
					}
				}
				
				for(String recordingID : recordingsToDelete) {
					Recording recording = recordingMap.get(recordingID);
					String meetingID = recording.getMeetingID();
					
					recordingsByMeetingIDMap.get(meetingID).remove(recording);
					
					if(recordingsByMeetingIDMap.get(meetingID).isEmpty()) {
						synchronized (recordingsByMeetingIDMap) {
							recordingsByMeetingIDMap.remove(meetingID);
						}
					}
					recordings.remove(recordings.indexOf(recording));
					recordingMap.remove(recordingID);
					
				}
			}
		}
	}
	
	/**
	 * Get all recordings of this server
	 * */
	public ArrayList<Recording> getAll() {
		synchronized (lastUpdate) {
			if(System.currentTimeMillis() - lastUpdate > 60000){
				update();
			}
		}
		return recordings;
	}
	
	/**
	 * Get all recordings of this server
	 * */
	public ArrayList<Recording> getByMeeting(String meetingID) {
		synchronized (lastUpdate) {
			if(System.currentTimeMillis() - lastUpdate > 60000){
				update();
			}
		}
		ArrayList<Recording> filteredRecordings = new ArrayList<Recording>();
		for(Recording rec : recordings){
			if(rec.getMeetingID().equals(meetingID))
				filteredRecordings.add(rec);
		}
		return filteredRecordings;
	}
	
	/**
	 * Get recording by ID
	 * */
	public Recording getByID(String id) {
		if(id==null) return null;
		return recordingMap.get(id.toLowerCase());
	}
}
