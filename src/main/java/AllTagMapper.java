import org.apache.tika.parser.html.HtmlMapper;


public class AllTagMapper implements HtmlMapper {

    public String mapSafeElement(String name) {
        return name.toLowerCase();
    }

    public boolean isDiscardElement(String name) {
        return false;
    }

    public String mapSafeAttribute(String elementName, String attributeName) {
        return attributeName.toLowerCase();
    }

}