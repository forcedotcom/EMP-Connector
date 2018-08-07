package com.salesforce.emp.connector.example;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.LoginHelper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.URL;
import java.util.UUID;

public class EventsGenerator {

    private static String SOBJECT_URI = "services/data/v42.0/sobjects/";
    private static String[] EVENTS_URI = new String[] {
            SOBJECT_URI+"dbTest__e",
//            SOBJECT_URI+"dbTest1__e",
//            SOBJECT_URI+"dbTest2__e"
    };

    private static String PAYLOAD_TEMPLATE =
            "{\"status__c\" : \" %s \"}";

    public static void main(String[] argv) throws Throwable {
        if (argv.length < 4 || argv.length > 5) {
            System.err.println("Usage: EventGenerator url username password delayInSecs howManyToCreate");
            System.exit(1);
        }

        String serverUrl = argv[0];
        String username = argv[1];
        String password = argv[2];
        Long delaySecs = Long.parseLong(argv[3]);
        Long count = Long.parseLong(argv[4]);

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                return LoginHelper.login(new URL(serverUrl), username, password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        BayeuxParameters params = tokenProvider.login();

        HttpClient client = new HttpClient(new SslContextFactory(true));
        client.setFollowRedirects(true);
        client.start();
        try {
            for (int i = 0; i < count; i += 1) {
                for (String eventURI : EVENTS_URI) {
                    Request request = client.POST(serverUrl + eventURI)
                            .header(HttpHeader.AUTHORIZATION, "OAuth " + params.bearerToken())
                            .header(HttpHeader.ACCEPT, "application/json")
                            .content(jsonPayload(), "application/json");

                    System.out.println(">>> " + request);
                    ContentResponse response = request.send();
                    System.out.println("<<< " + response.getContentAsString());
                    Thread.sleep(delaySecs * 1000);
                }
            }
        } finally {
            client.stop();
        }
    }

    private static ContentProvider jsonPayload() {
        return new StringContentProvider(String.format(PAYLOAD_TEMPLATE, UUID.randomUUID(), UUID.randomUUID()));
    }
}
