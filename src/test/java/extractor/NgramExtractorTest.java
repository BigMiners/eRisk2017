package extractor;

import common.DocUnit;
import common.Post;
import common.Posts;
import common.User;
import org.junit.Test;
import sun.security.krb5.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by halmeida on 5/11/17.
 */
public class NgramExtractorTest {

    private String oneTitle = "One title";
    private String twoTitle = "Two title";

    private String oneText = " \"Sentence wrap in double quote.\" " +
            "[square bracket] " +
            "{curly brace} " +
            "is this a question? " +
            "& (entity); " +
            " http://www.google.com";

    private String twoText = "This is a second post text.";

    Post post1 = new Post("pid1");
    Post post2 = new Post("pid2");

    User user1 = new User("uid1");
    List<DocUnit> docs = new ArrayList<>();

    NgramExtractor extractor = new NgramExtractor();

    @Test
    public void testExtractFeaturesFromPosts() throws Exception {

        post1.setTitle(oneTitle);
        post1.setText(oneText);
        post2.setTitle(twoTitle);
        post2.setText(twoText);

        List<DocUnit> docs = new ArrayList<>();
        docs.add(post1);
        docs.add(post2);

        extractor.setNgramSize(1);
        extractor.extractFeatures(docs,"model");
        assertEquals(extractor.getFeatureList().size(),18);
        extractor.getFeatureList().clear();

        extractor.setNgramSize(2);
        extractor.extractFeatures(docs,"model");
        assertEquals(extractor.getFeatureList().size(),20);
        extractor.getFeatureList().clear();

        docs.clear();
    }

    @Test
    public void testExtractFeaturesFromUsers() throws Exception{
        post1.setTitle(oneTitle);
        post1.setText(oneText);
        post2.setTitle(twoTitle);
        post2.setText(twoText);

        Posts postsUser = new Posts();
        postsUser.addPost(post1);
        postsUser.addPost(post2);

        user1.addPosts(postsUser);
        docs.add(user1);

        extractor.setNgramSize(1);
        extractor.extractFeatures(docs,"model");

        assertEquals(extractor.getFeatureList().size(),19);
        extractor.getFeatureList().clear();

        extractor.setNgramSize(2);
        extractor.extractFeatures(docs,"model");
        assertEquals(extractor.getFeatureList().size(),21);
        extractor.getFeatureList().clear();

        docs.clear();
    }
}