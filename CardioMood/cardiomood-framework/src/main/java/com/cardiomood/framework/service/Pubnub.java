package com.cardiomood.framework.service;

import com.pubnub.api.Callback;

import org.json.JSONObject;

import java.util.Hashtable;

/**
 * Created by antondanhsin on 01/11/14.
 */
public class Pubnub extends com.pubnub.api.Pubnub {

    public Pubnub(String publish_key, String subscribe_key) {
        super(publish_key, subscribe_key);
    }


    /**
     * Send a message to a channel.
     *
     * @param channel
     *            Channel name
     * @param message
     *            JSONObject to be published
     * @param callback
     *            object of sub class of Callback class
     */
    @Override
    public void publish(String channel, JSONObject message, boolean storeInHistory, Callback callback) {
        Hashtable args = new Hashtable();
        args.put("channel", channel);
        args.put("message", message);
        args.put("callback", callback);
        args.put("storeInHistory", (storeInHistory)?"":"0");
        publish(args);
    }


}
