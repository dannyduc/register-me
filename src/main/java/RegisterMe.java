import com.sun.tools.doclets.internal.toolkit.util.links.LinkInfo;
import com.sun.xml.internal.ws.api.addressing.WSEndpointReference;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlParser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static org.apache.tika.sax.XHTMLContentHandler.XHTML;


public class RegisterMe {

    static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36";

    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            String[] parts = arg.split("/");
            final String username = parts[0];
            final String password = parts[2];
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        register(username, password);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private static void register(String username, String password) throws IOException, SAXException, TikaException, InstantiationException, IllegalAccessException {

        CloseableHttpClient httpclient = createHttpClient();

        initializeCookies(httpclient);

        login(username, password, httpclient);

        List<SignupLink> links = getClassSchedule(httpclient);
        for (SignupLink link : links) {
            String desc = link.desc;
            boolean requiresPayment = desc.endsWith("$");
            if (!requiresPayment) {
                if (desc.contains("Cardio Kickboxing")
                        || desc.contains("Body Sculpt")
                        || desc.contains("Power Fitness")
                        || desc.contains("Cardio Kickboxing & Sculpt")
                        || desc.contains("Cardio Kickboxing & Sculpt")
//                        || desc.contains("Indoor Cycling")
                        ) {
                    String confirmPage = signup(link.link, httpclient);
                    submitConfirmationPage(confirmPage, httpclient);
                    System.out.println(link);
                }
            }
        }
    }

    private static String getCurrentDateString() {
        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("MM/dd/yyyy");
        return dtfOut.print(new DateTime());
    }

    private static void submitConfirmationPage(String confirmPage, CloseableHttpClient client) throws IOException, SAXException, TikaException, InstantiationException, IllegalAccessException {
        ConfirmationPageHandler handler = new ConfirmationPageHandler();
        parse(confirmPage, handler);

        String response = post(handler.link, handler.params, client);
    }

    private static CloseableHttpClient createHttpClient() {
        // Create a local instance of cookie store
        CookieStore cookieStore = new BasicCookieStore();

        // Populate cookies if needed
        BasicClientCookie cookie = new BasicClientCookie("CMBMID", "41273");
        cookie.setDomain("clients.mindbodyonline.com");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);

        // Set the store
        return HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
    }

    static void initializeCookies(CloseableHttpClient httpclient) throws IOException {
        String homePage = "https://clients.mindbodyonline.com/classic/home?studioid=41273";
        get(homePage, httpclient);
    }

    private static void login(String username, String password, CloseableHttpClient httpclient) throws IOException {

        String loginUrl = String.format("https://clients.mindbodyonline.com/Login?studioID=41273&isLibAsync=true&isJson=true&libAsyncTimestamp=%s", System.currentTimeMillis());
        String dateString = getCurrentDateString();

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("catid", ""));
        formparams.add(new BasicNameValuePair("classid", ""));
        formparams.add(new BasicNameValuePair("date", dateString));
        formparams.add(new BasicNameValuePair("lvl", ""));
        formparams.add(new BasicNameValuePair("optForwardingLink", ""));
        formparams.add(new BasicNameValuePair("optRememberMe", "on"));
        formparams.add(new BasicNameValuePair("page", ""));
        formparams.add(new BasicNameValuePair("prodid", ""));
        formparams.add(new BasicNameValuePair("qParm", ""));
        formparams.add(new BasicNameValuePair("requiredtxtPassword", password));
        formparams.add(new BasicNameValuePair("requiredtxtUserName", username));
        formparams.add(new BasicNameValuePair("sSu", ""));
        formparams.add(new BasicNameValuePair("stype", ""));
        formparams.add(new BasicNameValuePair("tg", ""));
        formparams.add(new BasicNameValuePair("trn", "0"));
        formparams.add(new BasicNameValuePair("view", ""));
        formparams.add(new BasicNameValuePair("vt", ""));

        post(loginUrl, formparams, httpclient);
    }

    private static List<SignupLink> getClassSchedule(CloseableHttpClient httpclient) throws IOException, SAXException, TikaException, InstantiationException, IllegalAccessException {

        String classSchedule = "https://clients.mindbodyonline.com/classic/mainclass";
        String dateString = getCurrentDateString();

        List<NameValuePair> formparams;
        formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("pageNum", "1"));
        formparams.add(new BasicNameValuePair("requiredtxtUserName", ""));
        formparams.add(new BasicNameValuePair("requiredtxtPassword", ""));
        formparams.add(new BasicNameValuePair("optForwardingLink", ""));
        formparams.add(new BasicNameValuePair("optRememberMe", ""));
        formparams.add(new BasicNameValuePair("tabID", "7"));
        formparams.add(new BasicNameValuePair("optView", "week"));
        formparams.add(new BasicNameValuePair("useClassLogic", ""));
        formparams.add(new BasicNameValuePair("filterByClsSch", ""));
        formparams.add(new BasicNameValuePair("prevFilterByClsSch", "-1"));
        formparams.add(new BasicNameValuePair("prevFilterByClsSch2", "-2"));
        formparams.add(new BasicNameValuePair("txtDate", dateString));
        formparams.add(new BasicNameValuePair("optTG", "0"));
        formparams.add(new BasicNameValuePair("optVT", "0"));
        formparams.add(new BasicNameValuePair("optInstructor", "0"));
        String html = post(classSchedule, formparams, httpclient);
        //        FileUtils.writeStringToFile(new File("/tmp/a.html"), body);
        //        List<SignupLink> links = parseHtml(FileUtils.readFileToString(new File("/tmp/a.html")));
        return parseHtml(html);
    }

    static String post(String url, Collection<NameValuePair> params, CloseableHttpClient client) throws IOException {
        HttpPost post = new HttpPost(url);
        post.addHeader("User-Agent", USER_AGENT);
        post.addHeader("Origin", "https://clients.mindbodyonline.com");
        post.addHeader("Referer", "https://clients.mindbodyonline.com/classic/mainclass");
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params, Consts.UTF_8);
        post.setEntity(formEntity);
        CloseableHttpResponse response = client.execute(post);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("get non 200 ok status code");
        }
        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);
        EntityUtils.consume(entity);
        response.close();

        return body;
    }

    static String get(String url, CloseableHttpClient client) throws IOException {
        HttpGet get = new HttpGet(url);
        get.addHeader("User-Agent", USER_AGENT);
        CloseableHttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("get non 200 ok status code");
        }
        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);
        EntityUtils.consume(entity);
        response.close();
        return body;
    }

    static List<SignupLink> parseHtml(String html) throws TikaException, SAXException, IOException, IllegalAccessException, InstantiationException {
        PageHandler handler = new PageHandler();
        parse(html, handler);
        return handler.links;
    }

    static void parse(String content, DefaultHandler handler) throws IllegalAccessException, InstantiationException, TikaException, SAXException, IOException {
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        context.set(HtmlMapper.class, AllTagMapper.class.newInstance());

        Parser parser = new HtmlParser();

        parser.parse(new ByteArrayInputStream(content.getBytes()), handler, metadata, context);
    }

    static class PageHandler extends DefaultHandler {
        boolean inSignupButton;
        boolean inATag;
        StringBuilder sb = new StringBuilder();
        List<SignupLink> links = new ArrayList<SignupLink>();
        boolean inTd;
        boolean inReservedCount;
        String reservedCount = "";

        static final String host = "https://clients.mindbodyonline.com";


        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (XHTML.equals(uri)) {
                if (qName.equals("input")
                        && "button".equals(attributes.getValue("type"))
                        && "SignupButton".equals(attributes.getValue("class"))
                        && "Sign Up Now".equals(attributes.getValue("value"))
                        ) {
                    inSignupButton = true;
                    String jsBlock = attributes.getValue("onclick");
                    String[] stmts = jsBlock.split(";");
                    for (String stmt : stmts) {
                        String key = "document.location='";
                        if (stmt.startsWith(key)) {
                            links.add(new SignupLink(host + stmt.substring(key.length(), stmt.length()-1)));
                        }
                    }
                }
                if (inSignupButton && qName.equals("a") && "modalClassDesc".equals(attributes.getValue("class"))) {
                    inATag = true;
                }
                if (qName.equals("td")) {
                    inTd = true;
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inSignupButton && inATag) {
                String text = new String(ch, start, length);
                String[] fields = text.split(" ");
                List<String> words = new ArrayList<String>();
                for (String field : fields) {
                    field = field.trim();
                    if (StringUtils.isNotBlank(field)) {
                        words.add(field);
                    }
                }
                String nomalized = StringUtils.join(words, " ");
                links.get(links.size() - 1).desc = nomalized;
            }
            if (inTd && inSignupButton) {
                String s = new String(ch, start, length);
                if (s.length() > 1 && s.charAt(1) == '(') {
                    inReservedCount = true;
                }
                if (inReservedCount) {
                    reservedCount += s;
                    if (s.endsWith(")")) {
                        inReservedCount = false;
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (inSignupButton && qName.equals("tr")) {
                inSignupButton = false;
                if (reservedCount.endsWith("0 Open)")) {
                    links.remove(links.size() - 1);
                }
                reservedCount = "";
            }
            if (qName.equals("a")) {
                inATag = false;
            }
            if (qName.equals("td")) {
                inTd = false;
            }
        }
    }

    static class SignupLink {
        String link;
        String desc;

        public SignupLink(String link) {
            this.link = link;
        }

        @Override
        public String toString() {
            return "SignupLink{" +
                    "link='" + link + '\'' +
                    ", desc='" + desc + '\'' +
                    '}';
        }
    }

    static String signup(String link, CloseableHttpClient client) throws IOException {
        HttpGet get = new HttpGet(link);
        get.addHeader("User-Agent", USER_AGENT);
        CloseableHttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);
        EntityUtils.consume(entity);
        response.close();
        return body;
    }

    static class ConfirmationPageHandler extends DefaultHandler {
        String link;
        List<NameValuePair> params = new ArrayList<NameValuePair>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (XHTML.equals(uri)) {
                if (qName.equals("input")) {
                    String type = attributes.getValue("type");
                    String id = attributes.getValue("id");
                    if ("button".equals(type) && "SubmitEnroll2".equals(id)) {
                        String onclick = attributes.getValue("onclick");
                        String prefix = "submitResForm('";
                        String suffix = "',false, false);";
                        int start = onclick.indexOf(prefix) + prefix.length();
                        int end = onclick.indexOf(suffix);
                        link = "https://clients.mindbodyonline.com/ASP/" + onclick.substring(start, end);
                    } else {
                        String name = attributes.getValue("name");
                        String value = attributes.getValue("value");
                        if (name != null) {
                            params.add(new BasicNameValuePair(name, value));
                        }
                    }
                }
            }
        }
    }
}
