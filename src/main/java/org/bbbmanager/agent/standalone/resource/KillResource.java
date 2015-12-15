package org.bbbmanager.agent.standalone.resource;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.apache.log4j.Logger;
import org.bbbmanager.agent.common.model.meeting.Meeting;
import org.bbbmanager.agent.common.model.meeting.User;
import org.bbbmanager.agent.common.model.server.BBBServer;
import org.bbbmanager.agent.common.repository.MeetingRepository;
import org.bbbmanager.agent.standalone.repository.ServerRepository;
import org.bbbmanager.bigbluebutton.api.BigBlueButtonAPI;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 * --
 *
 * @author BBBManager Team <team@bbbmanager.org>
 *
 */
public class KillResource extends SecuredResource {

    private static final Logger log = Logger.getLogger(KillResource.class);

    @Get
    public Object killUserInMeeting() {
        if (checkKey()) {
            Form params = getRequest().getResourceRef().getQueryAsForm();

            //Meting info
            String userId = (String) params.getValues("userId");

            if (killUser(userId)) {
                Properties prop = new Properties();
                prop.setProperty("success", "1");
                return prop;
            } else {
                Properties prop = new Properties();
                prop.setProperty("success", "0");
                return prop;
            }
        }

        return null;
    }

    public class Subscriber extends JedisPubSub {
        private ConcurrentHashMap<String, String> dataHolder = new ConcurrentHashMap<>();
        private JsonParser jp = new JsonParser();
        private Long last_message_ts = 0L;

        @Override
        public void onMessage(String channel, String message) {
            //log.info(message);
            System.out.println("KillResource: " + message);
            if (message.contains("get_users_reply")) {
                JsonObject jsobj = jp.parse(message).getAsJsonObject().get("payload").getAsJsonObject();
                JsonArray users = jsobj.get("users").getAsJsonArray();

                for (JsonElement user : users) {
                    String extern_userid = user.getAsJsonObject().get("extern_userid").getAsString();
                    String userid = user.getAsJsonObject().get("userid").getAsString();
                    String meeting_id = jsobj.get("meeting_id").getAsString();
                    dataHolder.put(extern_userid + "_userid", userid);
                    dataHolder.put(extern_userid + "_meeting_id", meeting_id);
                    last_message_ts = System.currentTimeMillis();
                }
            }
        }

        @Override
        public void onPMessage(String pattern, String channel, String message) {

        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            System.out.println("KillResource: subscribed to channel " + channel);
            dataHolder.put("subscribed", "true");
        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {

        }

        @Override
        public void onPUnsubscribe(String pattern, int subscribedChannels) {

        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {

        }

        private void setDataHolder(ConcurrentHashMap<String, String> dataHolder) {
            this.dataHolder = dataHolder;
        }

        public Long getLastMessageTS() {
            return last_message_ts;
        }
    }

    private boolean killUser(final String externalUserID) {
        final ConcurrentHashMap<String, String> dataHolder = new ConcurrentHashMap<>();
        final AtomicBoolean retVal = new AtomicBoolean();
        dataHolder.put("subscribed", "false");
        
        retVal.set(false);
        final Thread thSub = new Thread(new Runnable() {
            @Override
            public void run() {
                JedisPool jedisPool = null;
                try {
                    jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379, 0);
                    Jedis subscriberJedis = jedisPool.getResource();
                    final Subscriber subscriber = new Subscriber();
                    subscriber.setDataHolder(dataHolder);

                    Thread timeoutThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Long threadStart = System.currentTimeMillis();
                            while (true) {
                                //If we received a message, and no other message has been received in last 1s, no more messages will be received
                                if (subscriber.getLastMessageTS() != null && subscriber.getLastMessageTS() > 0) {
                                    if (System.currentTimeMillis() - subscriber.getLastMessageTS() > 1000) {
                                        //log.info("Unsubscribing due to inter message timeout (1s)");
                                        System.out.println("KillResource: " + "Unsubscribing due to inter message timeout (1s)");
                                        subscriber.unsubscribe();
                                        break;
                                    }
                                } else if (System.currentTimeMillis() - threadStart > 20000) {
                                    //If no message was received after 10s, stop waiting
                                    //log.info("Unsubscribing due to no message timeout (20s)");
                                    System.out.println("KillResource: " + "Unsubscribing due to no message timeout (20s)");
                                    subscriber.unsubscribe();
                                    break;
                                }

                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException ex) {
                                }
                            }
                        }
                    });
                    timeoutThread.start();

                    subscriberJedis.subscribe(subscriber, "bigbluebutton:from-bbb-apps:users");
                } catch (Exception e) {
                    //log.error("Error: " + e.getMessage(), e);
                    System.out.println("KillResource: " + "Error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (jedisPool != null) {
                        jedisPool.close();
                    }
                }
            }
        });
        thSub.start();

        Thread thPub = new Thread(new Runnable() {

            @Override
            public void run() {
                JedisPool jedisPool = null;
                try {
                    jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379, 0);
                    Jedis publisherJedis = jedisPool.getResource();
                    publisherJedis.publish("bigbluebutton:to-bbb-apps:meeting", "{\"header\":{\"name\":\"get_all_meetings_request\"},\"payload\":{}}");

                    //Wait until Subscribe exits
                    thSub.join();

                    if (dataHolder.containsKey(externalUserID + "_userid")) {
                        String meetingId = dataHolder.get(externalUserID + "_meeting_id");
                        String userId = dataHolder.get(externalUserID + "_userid");
                        System.out.println("Ejecting user " + userId + " from meeting " + meetingId);
                        publisherJedis.publish("bigbluebutton:to-bbb-apps:users", "{\"payload\": {\"userid\": \"" + userId + "\", \"ejected_by\": \"" + userId + "\", \"meeting_id\": \"" + meetingId + "\"}, \"header\": { \"name\": \"eject_user_from_meeting_request_message\" }}");
                        retVal.set(true);
                        Thread.sleep(5000);
                    } else {
                        String availableUsers = "";
                        for (String key : dataHolder.keySet()) {
                            availableUsers += key + ",";
                        }
                        //log.info("User " + externalUserID + " not found, available users are: " + availableUsers);
                        System.out.println("KillResource: " + "User " + externalUserID + " not found, available users are: " + availableUsers);
                    }
                } catch (Exception e) {
                    //log.error("Error: " + e.getMessage(), e);
                    System.out.println("KillResource: " + "Error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (jedisPool != null) {
                        jedisPool.close();
                    }
                }
            }
        });
        
        while(true) {
            if(dataHolder.get("subscribed").equals("true")) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {}
        }
        thPub.start();
        try {
            thPub.join();
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(KillResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        return retVal.get();
    }
    
    public static void main (String args[]) {
        KillResource kr = new KillResource();
        kr.killUser("1_1");
    }
}
