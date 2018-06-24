package common;

import bm25.Labels;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class SolrQueryLabels {
    private Labels labels = new Labels();
    @Nullable
    public String addLabel(SolrClient solr, org.apache.solr.client.solrj.SolrQuery query) {
        QueryResponse response;
        try {
            response = solr.query(query);
            StringBuilder labels = this.labels.getLabels(response);
            return labels.toString();
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
