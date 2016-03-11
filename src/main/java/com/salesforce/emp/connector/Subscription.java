package com.salesforce.emp.connector;

/**
 * A subscription to a topic
 * 
 * @author hal.hildebrand
 * @since 138
 */
public interface Subscription {

    /**
     * Cancel the subscription
     */
    void cancel();

    /**
     * @return the current replayFrom event id of the subscription
     */
    long getReplayFrom();

    /**
     * @return the topic subscribed to
     */
    String getTopic();

}