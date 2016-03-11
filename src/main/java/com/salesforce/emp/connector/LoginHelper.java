package com.salesforce.emp.connector;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.ByteBuffer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.ByteBufferContentProvider;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A helper to obtain the Authentication bearer token via login
 *
 * @author hal.hildebrand
 * @since 202
 */
public class LoginHelper {

    private static class LoginResponseParser extends DefaultHandler {

        private boolean inServerUrl;
        private boolean inSessionId;

        private String serverUrl;
        private String sessionId;

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inSessionId) sessionId = new String(ch, start, length);
            if (inServerUrl) serverUrl = new String(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (localName != null) {
                if (localName.equals("sessionId")) {
                    inSessionId = false;
                }

                if (localName.equals("serverUrl")) {
                    inServerUrl = false;
                }
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (localName != null) {
                if (localName.equals("sessionId")) {
                    inSessionId = true;
                }

                if (localName.equals("serverUrl")) {
                    inServerUrl = true;
                }
            }
        }
    }

    static final String LOGIN_ENDPOINT = "https://login.salesforce.com";
    private static final String COMETD_REPLAY = "/cometd/replay";

    private static final String ENV_END = "</soapenv:Body></soapenv:Envelope>";

    private static final String ENV_START = "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' "
            + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
            + "xmlns:urn='urn:partner.soap.sforce.com'><soapenv:Body>";

    // The enterprise SOAP API endpoint used for the login call in this example.
    private static final String SERVICES_SOAP_PARTNER_ENDPOINT = "/services/Soap/u/22.0/";

    public static BayeuxParameters login(String username, String password) throws Exception {
        return login(new URL(LOGIN_ENDPOINT), username, password);
    }

    public static BayeuxParameters login(String username, String password, BayeuxParameters params) throws Exception {
        return login(new URL(LOGIN_ENDPOINT), username, password, params);
    }

    public static BayeuxParameters login(URL loginEndpoint, String username, String password) throws Exception {
        return login(loginEndpoint, username, password, new BayeuxParameters() {
            @Override
            public String bearerToken() {
                throw new IllegalStateException("Have not authenticated");
            }

            @Override
            public URL endpoint() {
                throw new IllegalStateException("Have not established replay endpoint");
            }
        });
    }

    public static BayeuxParameters login(URL loginEndpoint, String username, String password, BayeuxParameters in)
            throws Exception {
        HttpClient client = new HttpClient(in.sslContextFactory());
        client.getProxyConfiguration().getProxies().addAll(in.proxies());
        client.start();
        URL endpoint = new URL(loginEndpoint, getSoapUri());
        Request post = client.POST(endpoint.toURI());
        post.content(new ByteBufferContentProvider("text/xml", ByteBuffer.wrap(soapXmlForLogin(username, password))));
        post.header("SOAPAction", "''");
        post.header("PrettyPrint", "Yes");
        ContentResponse response = post.send();
        if (response.getStatus() != 200) { throw new IllegalStateException(
                String.format("Unable to login, response: %s", response.getStatus())); }

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();

        LoginResponseParser parser = new LoginResponseParser();
        saxParser.parse(response.getContentAsString(), parser);

        if (parser.sessionId == null || parser.serverUrl == null) { throw new IllegalStateException(
                String.format("Login Failed to %s\n%s", endpoint, response)); }

        URL soapEndpoint = new URL(parser.serverUrl);
        URL replayEndpoint = new URL(soapEndpoint.getProtocol(), soapEndpoint.getHost(), soapEndpoint.getPort(),
                new StringBuilder().append(COMETD_REPLAY).append(1).toString());
        return new BayeuxParameters() {
            @Override
            public String bearerToken() {
                return parser.sessionId;
            }

            @Override
            public URL endpoint() {
                return replayEndpoint;
            }

            @Override
            public int maxBufferSize() {
                return in.maxBufferSize();
            }

            @Override
            public int maxNetworkDelay() {
                return in.maxNetworkDelay();
            }
        };
    }

    private static String getSoapUri() {
        return SERVICES_SOAP_PARTNER_ENDPOINT;
    }

    private static byte[] soapXmlForLogin(String username, String password) throws UnsupportedEncodingException {
        return (ENV_START + "  <urn:login>" + "    <urn:username>" + username + "</urn:username>" + "    <urn:password>"
                + password + "</urn:password>" + "  </urn:login>" + ENV_END).getBytes("UTF-8");
    }
}
