package indexer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import common.Post;
import common.User;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IndexManager {

    private static final MetricRegistry metrics = new MetricRegistry();
    private final SolrClient solrClient;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Meter docsMeter = metrics.meter("Num. indexed documents");
    private IndexerStatus indexerStats;

    public IndexManager() {
        String serverPath = "http://localhost:8983/solr/eriskRun2/";
        logger.info("Using Solr server {}", serverPath);
        this.solrClient = new HttpSolrClient(serverPath);

        getIndexingRate();
        indexerStats = new IndexerStatus();
    }

    public void pushData(List<User> users) {

        ArrayList<SolrInputDocument> solrDocs = getUsers(users);
        for (SolrInputDocument singleDoc : solrDocs) {
            sendDocToIndex(singleDoc, singleDoc.getField(FIELDS.title));
            indexerStats.incrementNbProcessedDocs();

            if(indexerStats.getNbProcessed() % 100 == 0){
                logger.info("{} docs processed so far.", indexerStats.getNbProcessed());
            }
        }
    }

    private ArrayList<SolrInputDocument> getUsers(List<User> users) {
        ArrayList<SolrInputDocument> solrDocs = new ArrayList<>();
        for (User user : users) {
                for (Post post : user.getPosts().getPosts()) {
                SolrInputDocument solrDoc = postToSolrDoc(post, user);
                solrDocs.add(solrDoc);
            }
        }
        return solrDocs;
    }


    private void sendDocToIndex(SolrInputDocument doc, SolrInputField postTitle) {
        if (doc != null) {
            try {
                docsMeter.mark();
                pushDocument(doc);
            } catch (Exception e) {
                logger.error("Could not push document for file {}", postTitle, e);
            }
        } else {
            logger.warn("Error processing file {}", postTitle);
        }
    }

    private SolrInputDocument postToSolrDoc(Post post, User user) {
        SolrInputDocument doc = new SolrInputDocument();

        doc.setField(FIELDS.title, post.getTitle());
        doc.setField(FIELDS.content, post.getContent());
        doc.setField(FIELDS.date, post.getDate());
        doc.setField(FIELDS.label, user.getLabel().toString());
        doc.setField(FIELDS.chunk, post.getChunk());
        doc.setField(FIELDS.user, user.getId());

        return doc;
    }


    private void pushDocument(SolrInputDocument solrDoc) {
            try {
                int commitWithinMs = 10;
                solrClient.add(solrDoc, commitWithinMs);
            } catch (SolrServerException ex) {
                logger.error("SolrServerException: could not push document {}", ex);
            } catch (IOException ex) {
                logger.error("IOException: could not push document {}" + ex.getMessage());
            }
    }

    private void getIndexingRate() {
        //reporter indexing rate
        Slf4jReporter reporter = Slf4jReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        //outputs the indexing rate every minute
        reporter.start(1, TimeUnit.MINUTES);
    }
}
