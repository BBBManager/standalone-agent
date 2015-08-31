package org.bbbmanager.agent.common.model.recording;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.bbbmanager.agent.common.servlet.Configuration;
import org.bbbmanager.common.util.XMLUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.annotations.XStreamAlias;


/**
 * Recording object
 * */
@XStreamAlias("recording")
public class Recording {
	@XStreamAlias("id")
	private String recordingID;
	
	@XStreamAlias("startTime")
	private Long recordingStartTime;
	
	@XStreamAlias("endTime")
	private Long recordingEndTime;
	
	@XStreamAlias("meetingID")
	private String meetingID;
	
	@XStreamAlias("meetingName")
	private String meetingName;
	
	public Recording(File recordingFolder) throws IOException, ParserConfigurationException, SAXException {
		Document doc = XMLUtils.parseXMLFromInputStream(new FileInputStream(recordingFolder + File.separator +  "metadata.xml"));
		
		recordingID = doc.getElementsByTagName("id").item(0).getTextContent().trim();
		recordingStartTime = Long.parseLong(doc.getElementsByTagName("start_time").item(0).getTextContent().trim());
		recordingEndTime = Long.parseLong(doc.getElementsByTagName("end_time").item(0).getTextContent().trim());
		meetingName = doc.getElementsByTagName("meetingName").item(0).getTextContent().trim();
		meetingID = doc.getElementsByTagName("meetingId").item(0).getTextContent().trim();
	}

	public String getRecordingID() {
		return recordingID;
	}

	public void setRecordingID(String recordingID) {
		this.recordingID = recordingID;
	}

	public Long getRecordingStartTime() {
		return recordingStartTime;
	}

	public void setRecordingStartTime(Long recordingStartTime) {
		this.recordingStartTime = recordingStartTime;
	}

	public Long getRecordingEndTime() {
		return recordingEndTime;
	}

	public void setRecordingEndTime(Long recordingEndTime) {
		this.recordingEndTime = recordingEndTime;
	}

	public String getMeetingID() {
		return meetingID;
	}

	public void setMeetingID(String meetingID) {
		this.meetingID = meetingID;
	}

	public String getMeetingName() {
		return meetingName;
	}

	public void setMeetingName(String meetingName) {
		this.meetingName = meetingName;
	}
	
	/**
	 * Get base recording folder
	 * */
	public static String getBaseFolder() {
		String presentationFolder = Configuration.getConfig("recording.folder");
		
		if(presentationFolder == null ){
			return null;
		}
		
		if(! presentationFolder.substring(presentationFolder.length()-1).equals("/")) {
			presentationFolder = presentationFolder + "/";
		}
		
		return presentationFolder;
	}
}
