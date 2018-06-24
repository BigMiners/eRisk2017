package bm25;

import common.SolrQueryLabels;
import common.User;
import normalizer.SpecialCharNormalizer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.Parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PredictOneAgainstOthers extends Thread {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private StringBuilder results;
    private String currentType;
    private int toChunk;


    public PredictOneAgainstOthers() {
        Parser parser = new Parser();

        ArrayList<String> exampleTypes = new ArrayList<>();
        exampleTypes.add("negative");
        exampleTypes.add("positive");
        int fromChunk = 1;
        toChunk = 1;


        for (int chunkid = fromChunk; chunkid <= toChunk; chunkid++) {
            for (String exampleType : exampleTypes) {
                this.currentType = exampleType;
                results = new StringBuilder();
                 //--------------------
                 //TODO: from user to docUnit
                List<User> users = new ArrayList<>();
                //List<User> users = parser.parse("/home/antoine/workspace/erisk/resources/corpora/train/" + exampleType + "_examples_anonymous_chunks/chunk" + chunkid, "doc").getDocsAsList();
                //--------------------
                String urlString = "http://localhost:8983/solr/eriskRun2/";
                SolrClient solr = new HttpSolrClient.Builder(urlString).build();
                processUsersWrtings(chunkid, users, solr);
                writeResultFile(String.valueOf(chunkid), results.toString(), exampleType);

            }
        }
    }

    private void processUsersWrtings(int chunkid, List<User> users, SolrClient solr) {
        int i = 0;
        String queryString;
        for (User user : users) {
            i++;
            logger.info("Processing {} user {} from chunk {} out of {}", user.getId(), this.currentType, chunkid, toChunk);
            logger.info("Done {} % of users for the chunk {} ( {} of {} )", ((float)i/(float)users.size())*100, chunkid, i, users.size());
            SpecialCharNormalizer specialCharNormalizer = new SpecialCharNormalizer();
            queryString = specialCharNormalizer.handlePunctuation(user.getContent());
            logger.info("QueryString length is {}", queryString.length());

            appendResultString(user, getSolrResults(chunkid, solr, queryString));

        }
    }

    private String getSolrResults(int chunkid, SolrClient solr, String queryString) {
        SolrQuery query = new SolrQuery();
        query.set("fl", "label");
        query.set("qt", "mlt");
        query.set("rows", "20");
        query.set("fq", "!chunk:" + chunkid);
        query.setQuery(ClientUtils.escapeQueryChars(queryString));
        SolrQueryLabels solrQueryLabels = new SolrQueryLabels();

        return solrQueryLabels.addLabel(solr, query);
    }



    private void appendResultString(User user, String labels) {
        String line = "train_subject" + user.getId() +
                " " +
                labels +
                "\n";
        logger.info("Adding {} to the results", line);
        results.append(line);
    }



    private void writeResultFile(String chunkid, String content, String exampleType) {
        writeOutput("/home/antoine/workspace/erisk/resources/output/chunk_" + exampleType + "_" + chunkid + ".txt", content);
    }

    private void writeOutput(String path, String content) {

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, true));
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            logger.info("Error writing file in " + path);
        }
    }
}