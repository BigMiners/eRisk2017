package bm25;


import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Labels {
private final Logger logger = LoggerFactory.getLogger(this.getClass());

    String getClassFromLabel(String label) {
        String classFound;
        switch (label) {
            case "NORISK":
                classFound = "2";
                break;
            case "RISK":
                classFound = "1";
                break;
            default:
                classFound = "0";
                break;
        }
        return classFound;
    }

    public StringBuilder getLabels(QueryResponse response) {
        StringBuilder labels = new StringBuilder();
        logger.debug("Got {} results from this query", response.getResults().size());
        for (int i = 0; i < 20; i++) {
            if (!response.getResults().isEmpty() && i <= response.getResults().size()) {
                String label = getClassFromLabel(response.getResults().get(i).get("label").toString());
                labels.append(label);
            } else {
                labels.append("0");
            }
            labels.append(" ");

        }
        return labels;
    }
}
