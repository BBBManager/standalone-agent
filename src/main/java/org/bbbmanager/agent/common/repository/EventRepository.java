package org.bbbmanager.agent.common.repository;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.event.Event;
import org.bbbmanager.agent.common.servlet.Configuration;
import org.bbbmanager.common.util.HTTPUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * --
 * 
 * @author BBBManager Team <team@bbbmanager.org>
 * */
public class EventRepository implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final Logger log = Logger.getLogger(EventRepository.class);
	private static EventRepository _instance;
	
	private ConcurrentHashMap<String, ConcurrentLinkedQueue<Event> > eventQueuePerURL = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Event>>();
	
	public static EventRepository getInstance() {
		if(_instance == null) {
			_instance = new EventRepository();
		}
		return _instance;
	}
	
	public EventRepository() {
		Thread thPublishEvents = new Thread(new Runnable() {
			public void run() {
				Integer publishEventsInterval;
				
				try{
					publishEventsInterval = Integer.parseInt(Configuration.getConfig("events.publish.interval"));
				} catch(Exception e){
					publishEventsInterval = 2000;
				}
				
				while(true) {
					for(String callbackUrl : eventQueuePerURL.keySet()) {
						List<Event> events = EventRepository.getInstance().getAndRemoveAll(callbackUrl);
						
						if(events.size() == 0){
							events = null;
							continue;
						}
						
						XStream xstream = new XStream(new DomDriver());
						xstream.alias("event", Event.class);
						
						String xmlString = xstream.toXML(events);
						
						Hashtable<String, String> params = new Hashtable<String, String>();
						params.put("xml", xmlString);
						xmlString = null;
						xstream = null;
						
						while( true ){
							try {
								if(HTTPUtils.doPost(callbackUrl, params)) {
									log.debug("Success sending events to url " + callbackUrl);
									break;
								}
							} catch (IOException e) {
								log.error("Error sending events to url " + callbackUrl + ": " + e.getMessage(), e);
							}
							try {
								Thread.sleep(60000);
							} catch (InterruptedException e) {}
							log.debug("Trying to send events again after failure.");
						}
					}
					
					try {
						Thread.sleep(publishEventsInterval);
					} catch (InterruptedException e) {}
				}
			}
		});
		thPublishEvents.setName("thPublishEvents");
		thPublishEvents.start();
	}
	
	public void add(String callbackURL, Event event) {
		if(!eventQueuePerURL.containsKey(callbackURL)) {
			eventQueuePerURL.put(callbackURL, new ConcurrentLinkedQueue<Event>());
		}
		eventQueuePerURL.get(callbackURL).add(event);
	}
	
	private ArrayList<Event> getAndRemoveAll(String url){
		ArrayList<Event> eventList = new ArrayList<Event>();
		
		Event event = null;
		while((event = eventQueuePerURL.get(url).poll()) != null) {
			eventList.add(event);
		}
		
		return eventList;
	}
}
