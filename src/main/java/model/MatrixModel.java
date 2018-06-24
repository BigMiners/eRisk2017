package model;

import common.Configs;
import common.DocUnit;
import common.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 *
 * Handles the model file generation and
 * workflow to build arff vectors.
 *
 * Created by halmeida on 1/27/17.
 */
public class MatrixModel {

    String expType, feature_set, arffFile, work_dir, resources_dir,
            model_dir, location, timeStamp, project, unit;
    Properties props;
    ArffVector vector;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    int sampling_perc;

    public MatrixModel(){

        props = Configs.getInstance().getProps();
        expType = props.getProperty("TASK");
        sampling_perc = Integer.parseInt(props.getProperty("SAMPLING_PERC"));
        project = props.getProperty("PROJECT");
        unit = props.getProperty("UNIT");

        work_dir = props.getProperty("WORK_DIRECTORY") ;
        resources_dir = props.getProperty("RESOURCES_DIR") ;
        model_dir = props.getProperty("MODEL_DIR") + "/";
        location = work_dir + resources_dir + model_dir;

        vector = new ArffVector();
        feature_set = informFeatureSet();

        timeStamp = new SimpleDateFormat("yyyyMMdd_hh:mm").format(new Date());
        arffFile = location + project + feature_set + "_" + expType +"_"+ timeStamp + ".arff";

    }


    /**
     * Iterates over a list of docs to generate
     * an ARFF file of docs vs. features.
     * @param docs
     */
    public void generateModel(List<DocUnit> docs){

        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(arffFile));

            String outHeaderArff = vector.getHeader(expType, feature_set, unit);
            writer.write(outHeaderArff + "\n");

            int count = 0;

            for(DocUnit doc: docs) {
                String arffLine = vector.getVectorLine(doc);

                count++;
                arffLine = arffLine + "\n";
                writer.write(arffLine);

                if(count%50 == 0){
                    logger.info(" -> " + count + " documents processed.");
                }
            }
            writer.flush();
            writer.close();

            logger.info("Done! " + expType + " model generated for " + docs.size() + " documents.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * List features used for task
     * used to name ARFF file with configurations
     * @return
     */
    private String informFeatureSet(){
        String value = "";
//        if(Boolean.valueOf(props.getProperty("USE_RULES")))
//            value += "_rules";
        if(vector.useCLPsych)
            value += "_withCLPsych";
        if(sampling_perc > 0)
            value += "_sampling"+sampling_perc;
        if(vector.useWritingsCount)
            value += "_writings";
        if(vector.usePOStags)
            value += "_POS";
        if(vector.useMedsCount)
            value += "_meds";
        if(vector.useDrugsCount)
            value += "_drugs";
        if(vector.useDiseasesCount)
            value += "_disease";
        if(vector.useMeds)
            value += "_medsDic";
        if(vector.useDrugs)
            value += "_drugsDic";
        if(vector.useDiseases)
            value += "_diseaseDic";
        if(vector.useSentic)
            value += "_sentic";
        if(vector.useFeelings)
            value += "_feelings";
        if(vector.useNgram)
            value += "_ngrams_s"+ vector.ngramSize;
        if(Boolean.valueOf(props.getProperty("USE_STEM")))
            value += "stem";
        if(Boolean.valueOf(props.getProperty("USE_STOPFILTER")))
            value += "_stopwords";
        if(Boolean.valueOf(props.getProperty("USE_VOCABULARY")))
            value += "_vocabulary";
        else if(Boolean.valueOf(props.getProperty("USE_USER_HIST")))
            value += "_userHist_s" + props.getProperty("N_PREVIOUS");

        return value;
    }

}
