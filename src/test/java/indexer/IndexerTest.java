package indexer;

import common.User;
import org.junit.Before;
import org.junit.Test;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class IndexerTest {
    private ArrayList<String> properties = new ArrayList<>();

    /**
     * Before each test, empty the system properties to avoid side effects
     */
    @Before
    public void setUp() {
        properties.add("indexer:extract:drugs");
        properties.add("indexer:core");
        properties.add("indexer:solr-url");

        properties.forEach(System::clearProperty);
    }


    @Test
    public void testSetUsers() throws Exception {

        Indexer indexer = new Indexer();
        assertNull("The indexer has no users. Return null object", indexer.getUsers());

        List<User> users = new ArrayList<>();
        users.add(new User("1"));
        indexer.setUsers(users);

        assertEquals("We added a list with one user, getUsers should return it", 1, indexer.getUsers().size());
    }



    @Test
    public void setDefaultSolrParameters() throws Exception {
        Indexer indexer = new Indexer();
        assertEquals("erisk", indexer.getCore());
        assertEquals("http://localhost:8983/solr", indexer.getSolrUrl());
    }

    @Test
    public void setCustomCoreSolrParameters() throws Exception {
        System.setProperty("indexer:core", "erisk2");
        Indexer indexer = new Indexer();
        assertEquals("erisk2", indexer.getCore());
        assertEquals("http://localhost:8983/solr", indexer.getSolrUrl());
    }

    @Test
    public void setCustomUrlSolrParameters() throws Exception {
        System.setProperty("indexer:solr-url", "http://mysolr.org:8983/solr");
        Indexer indexer = new Indexer();
        assertEquals("erisk", indexer.getCore());
        assertEquals("http://mysolr.org:8983/solr", indexer.getSolrUrl());
    }

    @Test
    public void setPathToCorpora() {
        Indexer indexer = new Indexer();
        indexer.setPathToCorpora("/fake/path/to/corpora");
        assertEquals("/fake/path/to/corpora", indexer.getPathToCorpora());
    }


    @Test
    public void parse() {
//        Indexer indexer = new Indexer();
//        indexer.setPathToCorpora("src/test/resources/corpus/train");
//        indexer.parseCorpora();
//        assertEquals(3, indexer.getUsers().size());
    }

    @Test
    public void testLoadDictionary() throws Exception {
        Extractors extractors = new Extractors();
        ArrayList<String> dictionary = extractors.loadDictionary("src/test/resources/dictionaries/drugs.txt");
        assertEquals(6, dictionary.size());
    }

    /**
     * This test only checks if the IOException is handled during the execution.
     *
     * @throws UncheckedIOException
     */
    @Test(expected = UncheckedIOException.class)
    public void testLoadDictionaryIOException() throws UncheckedIOException {
        Extractors extractors = new Extractors();
        extractors.loadDictionary("");
    }

//    @Test
//    public void dummy() throws Exception{
//        System.setProperty("indexer:extract:meds", "true");
//        System.setProperty("indexer:extract:drugs", "true");
//        System.setProperty("indexer:extract:diseases", "true");
//        System.setProperty("indexer:core", "erisk-with-dict");
//
//        Indexer indexer = new Indexer();
//        indexer.setPathToCorpora("/home/antoine/workspace/latece/erisk/src/test/resources/corpus/train");
////                indexer.setPathToCorpora("/home/antoine/workspace/latece/erisk/resources/corpora/train/negative_examples_anonymous_chunks");
//
//        indexer.parseCorpora();
//        indexer.handleExtracting();
//
//        indexer.getUsers().forEach(user -> {
//            user.getPosts().getPosts().forEach(post -> {
////                        indexer.pushDocument(indexer.attributesToFields(post, user));
//                System.out.println(indexer.attributesToFields(post,user));
//                System.out.println(user.getDiseasesCount());
//                System.out.println(user.getDrugsCount());
//                System.out.println(user.getMedsCount());
//            });
//        });
//    }
}