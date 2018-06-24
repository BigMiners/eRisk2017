package extractor;

import common.DocUnit;
import common.Post;
import common.Posts;
import common.User;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by halmeida on 5/5/17.
 */
public class ExtractorTest {

    private String oneText = " \"This sentence is wrapped in double quotes.\" " +
            "[square brackets] " +
            "{curly braces} " +
            "is this a question? " +
            "& (entity); " +
            " http://www.google.com";

    private String normalized = " this sentence is wrapped in double quotes . " +
            " square brackets curly braces is this a question ? " +
            "  entity ; " +
            " www . google . com";

    @Test
    public void testNormalizeContent() throws Exception {
        Extractor extractor = new Extractor();
        String extractNormalized = extractor.normalizeContent(oneText);
        assertEquals(extractNormalized, normalized);
    }
}