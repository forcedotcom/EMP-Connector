package com.salesforce.emp.connector;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;

public class LoggingExtension implements ClientSession.Extension {

    @Override
    public boolean rcv(ClientSession clientSession, Message.Mutable mutable) {
        System.out.println("<<<<");
        System.out.println(mutable.getJSON());
        return true;
    }

    @Override
    public boolean rcvMeta(ClientSession clientSession, Message.Mutable mutable) {
        System.out.println("<<<< Meta");
        System.out.println(mutable.getJSON());

        return true;
    }

    @Override
    public boolean send(ClientSession clientSession, Message.Mutable mutable) {
        System.out.println(">>>>");
        System.out.println(mutable.getJSON());
        return true;
    }

    @Override
    public boolean sendMeta(ClientSession clientSession, Message.Mutable mutable) {
        System.out.println(">>>> Meta");
        System.out.println(mutable.getJSON());
        return true;
    }
}
