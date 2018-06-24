package sentiment;

import com.vader.SentimentAnalyzer;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created by hayda on 25/05/17.
 */
public class SentimentPredictorTest {

    SentimentAnalyzer analyzer;
    String content = "I am exhausted, sad, and unmotivated.";

    @Test
    public void testAnalyzeSentiment() throws Exception {

        analyzer = new SentimentAnalyzer(content);
        analyzer.analyse();

        float score = 0;
        String strongestSentiment = "";

        Iterator<String> iterator = analyzer.getPolarity().keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            float current = analyzer.getPolarity().get(key);
            if (current > score) {
                strongestSentiment = key;
                score = current;
            }
        }

        assertEquals(strongestSentiment, "negative");
        assertNotEquals(score,0);
    }

}