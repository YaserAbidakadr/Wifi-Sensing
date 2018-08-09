package ca.gc.crc.rnad.indoorwifi;

import java.sql.Timestamp;

/**
 * Created by brijesh on 20/4/17.
 */

public class Constants {

    public static final String MQTT_BROKER_URL = "tcp://iot.eclipse.org:1883";

    public static final String PUBLISH_TOPIC1 = "anchorId";
    public static final String PUBLISH_TOPIC2 = "WifiInfo";
    public static final String PUBLISH_TOPIC3 = "heartbeat";
    public static final String PUBLISH_TOPIC4 = "anchorIdAll";
    public static String t = new Timestamp(System.currentTimeMillis()).toString();
    public static String CLIENT_ID0= "androidkt" + t;
}

