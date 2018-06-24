package parser;

import common.DocUnit;
import common.Post;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class ParserTest {

    private Parser parser;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Before
    public void setUp() {
        parser = new Parser();
    }

    @Test
    public void parseClpsychFile() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Document document = parser.getXMLDocument("src/test/resources/clpsych_sample.xml");
        Element element = document.getDocumentElement();
        Post post = parser.getPost(element, "0", "post", "");

        assertEquals("The title of the post is \"This is a subject\"",
                post.getTitle(), "This is a subject");
        assertEquals("The date of the post is \"Fri Apr 24 04:46:00 UTC 2015\"",
                post.getDate(),  "Fri Apr 24 04:46:00 UTC 2015");
        assertEquals("The info value of the post correspond to \" clpsychpost \"",
                post.getInfo(), "clpsychpost");
        assertEquals("The text of this post is \"going back to America in less than a week\"",
                post.getText(), "going back to America in less than a week");
        assertEquals("The post id is \"666\"",
                post.getId(), "666");
        assertEquals("The user id is \"6687\"",
                post.getAuthor(), "6687");
    }

    @Test
    public void parseFile() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Document document = parser.getXMLDocument("src/test/resources/single_post_file.xml");
        Element element = document.getDocumentElement();
        Post post = parser.getPost(element, "chunk", "user", "");

        assertEquals("The title of the post is \" This is the title of my post \"",
                post.getTitle(), " This is the title of my post ");
        assertEquals("The date of the post is \"Mon Jul 06 00:05:00 UTC 2015\"",
                post.getDate(), "Mon Jul 06 00:05:00 UTC 2015");
        assertEquals("The info value of the post correspond to \" reddit post \"",
                post.getInfo(), " reddit post ");
        assertEquals("The text of this post is \" Hello World \"",
                post.getText(), " Hello World ");
    }

    @Test
    public void testGetPost() throws Exception {
        Document document = parser.getXMLDocument("src/test/resources/single_post_file.xml");
        Element element = document.getDocumentElement();
        Post post = parser.getPost(element, "chunk", "user", "");
        assertEquals(post.getTitle(), " This is the title of my post ");
    }

    @Test
    public void testGetPostWithEmptyTitle() throws Exception {
        Document document = parser.getXMLDocument("src/test/resources/single_post_file_empty_title.xml");
        Element element = document.getDocumentElement();
        Post post = parser.getPost(element, "chunk", "user", "");
        assertEquals(post.getTitle(), "");
    }

    @Test
    public void testParse() throws Exception {
        String dir = "src/test/resources/corpus/train";
        Parser parser = new Parser();
        List<DocUnit> docs = parser.parse(dir).getDocsAsList();
    }
}