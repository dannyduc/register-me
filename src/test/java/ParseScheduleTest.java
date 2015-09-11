import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ParseScheduleTest {

    public static void main(String[] args) throws Exception {
        List<RegisterMe.SignupLink> signupLinks = RegisterMe.parseHtml(FileUtils.readFileToString(new File("src/test/class-schedule-with-ampersand.html")));
        System.out.println(signupLinks);
    }
}
