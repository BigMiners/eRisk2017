package informationretrieval;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

class ResultsOutput {
    private HashMap<String, String> results;

    ResultsOutput(HashMap<String, String> results, String outputPath) {
        this.results = results;
        this.resultsToFile(outputPath);
    }

    private void resultsToFile(String outputPath) {
        FileWriter fw;
        try {
            fw = new FileWriter(outputPath);
            for (String userID : this.results.keySet()) {
                fw.write("test_subject" + userID + " " + this.results.get(userID) + "\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
