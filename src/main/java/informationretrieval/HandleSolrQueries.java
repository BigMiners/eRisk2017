package informationretrieval;

import common.SolrQueryLabels;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.util.ClientUtils;

public class HandleSolrQueries {
    private SolrClient solr;
    private String solrUrl;
    private String solrCore;
    private String queryString;
    private SolrQueryLabels solrQueryLabels = new SolrQueryLabels();

    HandleSolrQueries(String queryString, String solrUrl, String solrCore) {
        this.setQueryString(queryString);
        this.setSolrUrl(solrUrl);
        this.setSolrCore(solrCore);
        String solrFullUrl = getSolrUrl() + getSolrCore();
        this.solr = new HttpSolrClient.Builder(solrFullUrl).build();
    }

    String getCorrespondingLabels() {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.set("fl", "label");
        solrQuery.set("qt", "mlt");
        solrQuery.set("rows", "20");
        solrQuery.setQuery(ClientUtils.escapeQueryChars(queryString));
        return solrQueryLabels.addLabel(this.solr, solrQuery);
    }

    public String getQueryString() {
        return queryString;
    }

    private void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    private String getSolrUrl() {
        return solrUrl;
    }

    private void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    private String getSolrCore() {
        return solrCore;
    }

    private void setSolrCore(String solrCore) {
        this.solrCore = solrCore;
    }
}
