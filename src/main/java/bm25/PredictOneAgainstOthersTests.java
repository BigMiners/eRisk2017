package bm25;

import common.*;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.Parser;

import java.util.*;


public class PredictOneAgainstOthersTests extends Thread {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private StringBuilder results;
    private ArrayList<String> cores = new ArrayList<>();
    private String baseUrl;
    private SolrQueryLabels solrQueryLabels = new SolrQueryLabels();


    public PredictOneAgainstOthersTests() {
        Properties props = Configs.getInstance().getProps();
        logger.info("Solr base URL is {}", props.getProperty("SOLR_BASE_URL"));
        logger.info("Test copora path {}", props.getProperty("TEST_CORPORA_DIRECTORY"));


        String test_corpora_directory = props.getProperty("TEST_CORPORA_DIRECTORY");
        logger.debug("Corpora directpry = ", test_corpora_directory);
        int chunkid = extractChunkId(test_corpora_directory);
        logger.debug("Processing chunk ", chunkid);
        String exampleType = "Test";

        Parser parser = new Parser();
        results = new StringBuilder();
        //--------------------
        //TODO: from user to docUnit
        List<User> users = new ArrayList<>();
        //List<User> users = parser.parse(test_corpora_directory, "doc").getDocsAsList();
        //--------------------

        baseUrl = props.getProperty("SOLR_BASE_URL");
        String[] _cores = getCoresFromProps(props.getProperty("SOLR_CORES"));

        System.out.println(Arrays.toString(_cores));



//        for (String value : _cores) {
//            this.cores.add(value + "/");
//        }
//        process(chunkid, users, exampleType, _cores[1]);

    }

    private int extractChunkId(String corporaRootPath) {
        try {
            return Integer.parseInt(corporaRootPath.split(" ")[1]);
        } catch (Exception e) {
            logger.error("The path to the corpora does not contain a chunk id. Error is ", e);
        }
        return 0;
    }

    private String[] getCoresFromProps(String solr_cores) {
        return solr_cores.split(",");
    }


    private void process(int chunkid, List<User> users, String exampleType, String core) {
        logger.info("Start processing core {}", core);
        String urlString = this.baseUrl + core;
        logger.info("Solr full url (base + core) {}" + urlString);
        SolrClient solr = new HttpSolrClient.Builder(urlString).build();
        processUsersWrtings(chunkid, users, solr);
        writeResultFile(String.valueOf(chunkid), results.toString(), exampleType, core);
    }

    private void processUsersWrtings(int chunkid, List<User> users, SolrClient solr) {
        int i = 0;
        String queryString;
        for (User user : users) {
            i++;
            logger.info("Processing user {}", user.getId());
            logger.info("Done {} % of users for the chunk {} ( {} of {} )", String.valueOf(i / users.size() * 100), chunkid, i, users.size());
            queryString = user.getContent();
            logger.info("QueryString length is {}", queryString.length());

            appendResultString(user, getSolrResults(solr, queryString));

        }
    }

    private String getSolrResults(SolrClient solr, String queryString) {
        SolrQuery query = new SolrQuery();
        query.set("fl", "label");
        query.set("qt", "mlt");
        query.set("rows", "20");
        query.setQuery(ClientUtils.escapeQueryChars(queryString));

        return solrQueryLabels.addLabel(solr, query);
    }


    private void appendResultString(User user, String labels) {
        String line = "test_subject" + user.getId() +
                " " +
                labels +
                "\n";
        logger.info("Adding {} to the results", line);
        results.append(line);
    }

    private void writeResultFile(String chunkid, String content, String exampleType, String core) {
        StringBuilder path = new StringBuilder();
        path.append("/home/antoine/workspace/latece/erisk/results/");
        path.append("week0").append(chunkid);
        path.append("/IR/tests-chunk-").append(chunkid);
        path.append("/chunk_").append(exampleType).append("_").append(chunkid);
        path.append("_");
        if (Objects.equals(core, "eriskRun2/")) {
            path.append("mtl2.txt");
        } else {
            path.append("mtl1.txt");
        }
        IOUtil ioUtil = new IOUtil();
        ioUtil.writeOutput(path.toString(), content);
    }

}
