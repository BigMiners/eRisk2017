package predictor;

import common.Configs;
import common.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.output.prediction.PlainText;
import weka.core.Instances;

import java.util.*;

/**
 * Class for handling and
 * outputting predictions
 *
 * Created by halmeida on 1/26/17.
 */
public class Evaluator {

    String work_dir, modelFile, testFile, resources_dir, results_dir, resultFile, goldFile, project;
    boolean CV, CFS, useRules, overwriteRules;
    Properties props;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Evaluator(String classifier){
        props = Configs.getInstance().getProps();
        work_dir = props.getProperty("WORK_DIRECTORY");
        resources_dir = props.getProperty("RESOURCES_DIR");
        results_dir = work_dir + resources_dir + props.getProperty("RESULTS_DIR");
        CV = Boolean.valueOf(props.getProperty("CV"));
        CFS = Boolean.valueOf(props.getProperty("CFS"));
        useRules = Boolean.valueOf(props.getProperty("USE_RULES"));
        overwriteRules = Boolean.valueOf(props.getProperty("OVERWRITE_RULES"));

        project = props.getProperty("PROJECT");

        goldFile = props.getProperty("GOLD_DATA");

        if(classifier.contains("ensemble")){
            String ensemble_dir = props.getProperty("ENSEMBLE_DIR");
            String ensemble_type = ensemble_dir.replace("ensemble","").replaceAll("/","");
            classifier += "_" + ensemble_type;
        }
        modelFile = props.getProperty("MODEL_FILE");
        testFile = props.getProperty("TEST_FILE");

        resultFile = results_dir + "/" + testFile.replace("arff", classifier);

        if(CFS) resultFile = resultFile + ".cfs";

        if (useRules) {
            if (!overwriteRules) resultFile = resultFile + ".rulesBefore";
            else if(overwriteRules) resultFile = resultFile + ".rulesAfter";
        }
    }


    /**
     * Recovers predictions and complies to output format
     * Handles the merge between automatic predictions and rules
     * write to a file
     * @param output
     * @param data
     */
    public TreeMap<String,String> getAllPredictions(ArrayList<Prediction> output, Instances data, HashMap<String,String> rulePredictions, boolean overwriteRules) {



        TreeMap<String,String> results = new TreeMap<String,String>();
        StringBuilder sb = new StringBuilder();

        String id, predicted = "", actual = "";
        double act, pred, weight;

       // if(!CV) {

            for (int i = 0; i < output.size(); i++) {

                NominalPrediction np = (NominalPrediction) output.get(i);

                double confidence = getConfidence(np.distribution());
                act = np.actual();
                pred = np.predicted();
                id = data.get(i).toString(0);

                //adjust user id name for train/test data
                if(project.contains("clpsych")){
                    predicted = getLabel(project, pred);
                    predicted += "\t" + confidence;
                    results.put(id, predicted);
                }
                else if(project.contains("erisk")) {
                    if (testFile.contains("train"))
                        id = "train_subject" + id;
                    else id = "test_subject" + id;

                    if (overwriteRules && rulePredictions != null && rulePredictions.keySet().contains(id)) {
                        predicted = rulePredictions.get(id).split(",")[1];
                    } else {
                        predicted = getLabel(project, pred);
                    }

                    //not write a final decision if handling test set
                    if (testFile.contains("test")) {
                        if (predicted.contains("2")) predicted = predicted.replace("2", "0");
                    }
                    predicted += "\t\t" + confidence;
                    results.put(id, predicted);
                }
            }
            outputPredictions(project, results);
//        }

        return results;
    }

    private double getConfidence(double[] values){

        double prediction = 0;

        for(int i = 0; i < values.length; i++){
            if(values[i] > prediction)
                prediction = values[i];
        }
        return prediction;
    }

    private String getLabel(String project, double value){

        String label = "";
        if(project.contains("clpsych")){
            if (value == 0.0) label = "green";
            else if (value == 1.0) label = "amber";
            else if (value == 2.0) label = "red";
            else if (value == 3.0) label = "crisis";
        }
        else if(project.contains("erisk")){
            if (value == 0.0) label = "1"; //risk
            else if (value == 1.0) label = "2"; //norisk
        }
        return label;
    }

    /**
     * Concatenate predictions to
     * be written in an output file
     * @param predicitons map of predictions
     */
    private void outputPredictions(String project,TreeMap<String,String> predicitons){
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = predicitons.keySet().iterator();

        while(iterator.hasNext()){
            String id = iterator.next();
            String predicted = predicitons.get(id);

            if(project.contains("erisk"))
                sb.append(id).append("\t\t");
            else
                sb.append(id).append("\t");

            sb.append(predicted).append("\n");
        }
       IOUtil.getINSTANCE().writeOutput(resultFile,sb.toString());
    }


}
