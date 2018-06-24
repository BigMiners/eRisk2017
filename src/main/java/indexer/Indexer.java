package indexer;

import common.Configs;
import common.Post;
import common.User;
import extractor.Extractor;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class Indexer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Properties props = Configs.getInstance().getProps();

    private final HttpSolrClient solrClient;
    private String core;

    private String solrUrl;
    private String pathToCorpora;

    private List<User> users;

    public Indexer() {
        setPathToCorpora("");
        setSolrParameters();
        this.solrClient = new HttpSolrClient(getSolrUrl() + "/" + getCore());

    }

    public void parseCorpora() {
        logger.info("Parsing started");
        Parser parser = new Parser();
        //-------------------
        //TODO: from user to docUnit
        this.users = new ArrayList<>();
        //this.users = parser.parse(this.pathToCorpora, "doc").getUsersAsList();
        //-------------------
        logger.info("Parsing ended");
    }

    private void setSolrParameters() {
        String solrCore = System.getProperty("indexer:core");
        this.core = solrCore;
        if (solrCore == null) {
            this.core = "erisk";
        }


        String solrUrl = System.getProperty("indexer:solr-url");
        this.solrUrl = solrUrl;
        if (solrUrl == null) {
            this.solrUrl = "http://localhost:8983/solr";
        }
    }



    public String getCore() {
        return core;
    }

    String getSolrUrl() {
        return solrUrl;
    }


    String getPathToCorpora() {
        return pathToCorpora;
    }

    void setPathToCorpora(String path) {
        if (Objects.equals(path, "")) {
            this.pathToCorpora = props.getProperty("CORPUS_DIRECTORY");
        } else {
            this.pathToCorpora = path;
        }
    }

    public void pushDocuments(){
        Extractor extractor = new Extractor();
        this.getUsers().forEach(user -> {
            user.getPosts().getPosts().forEach(post -> {
                post.setContent(extractor.normalizeContent(post.getContent()));
                post.setTitle(extractor.normalizeContent(post.getTitle()));
                logger.info("Pushing document for user " + user.getId());
                logger.info("This post contains " + post.getDrugs().size() + " drugs " + post.getMeds().size() + " meds and " + post.getDiseases().size() + " diseases");
                this.pushDocument(this.attributesToFields(post, user));
            });
        });
    }

    private void pushDocument(SolrInputDocument solrDoc) {
        try {
            solrClient.add(solrDoc, 10);
        } catch (SolrServerException ex) {
            logger.error("SolrServerException: could not push document {}", ex);
        } catch (IOException ex) {
            logger.error("IOException: could not push document {}" + ex.getMessage());
        }
    }

    private SolrInputDocument attributesToFields(Post post, User user) {
        SolrInputDocument doc = new SolrInputDocument();

        doc.setField(FIELDS.title, post.getTitle());
        doc.setField(FIELDS.content, post.getContent());
        doc.setField(FIELDS.date, post.getDate());
        doc.setField(FIELDS.label, user.getLabel().toString());
        doc.setField(FIELDS.chunk, post.getChunk());
        doc.setField(FIELDS.user, user.getId());
        doc.setField(FIELDS.drugs, post.getDiseases());
        doc.setField(FIELDS.meds, post.getDrugs());
        doc.setField(FIELDS.diseases, post.getMeds());

        return doc;
    }


    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> posts) {
        this.users = posts;
    }

}
