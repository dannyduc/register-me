import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

public class ParseScheduleTest {

    public static void main(String[] args) throws Exception {
        List<RegisterMe.SignupLink> signupLinks = RegisterMe.parseHtml(FileUtils.readFileToString(new File("src/test/class-schedule-with-ampersand.html")));
        System.out.println(signupLinks);
    }
}
