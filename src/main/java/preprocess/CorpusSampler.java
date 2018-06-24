package preprocess;

import common.Configs;
import common.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Performs sampling in a given dataset.
 * Created by halmeida on 2/23/17.
 */
public class CorpusSampler {

    String corpus_dir, task;
    File location;
    int sampling_perc;
    Properties props;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    HashSet<File> positives;
    HashSet<File> negatives;
    double percNeg = 0;
    double percPos = 0;

    public CorpusSampler(){

        props = Configs.getInstance().getProps();
        corpus_dir = props.getProperty("CORPUS_DIRECTORY");

        task = props.getProperty("TASK");
        if (!corpus_dir.contains("chunk")) corpus_dir += "/" + task;

        sampling_perc = Integer.parseInt(props.getProperty("SAMPLING_PERC"));
        location = new File(corpus_dir + "_sampling"+sampling_perc);

        if(!location.exists()) location.mkdirs();

        positives = new HashSet<>();
        negatives = new HashSet<>();

    }

    /**
     * Retrieve simple statistics on
     * the dataset for sanity check
     * @param files dataset files
     */
    public void getStatistics(HashSet<File> files){

        for(File file : files){
            if(file.getName().contains("norisk"))
                negatives.add(file);
            else if(file.getName().contains("risk"))
                positives.add(file);
        }
        percNeg = (double) (negatives.size() * 100) / files.size();
        percPos = (double) (positives.size() * 100) / files.size();

        logger.info("Corpus contains {}% risk and {}% norisk users.", String.format("%.2f",percPos), String.format("%.2f",percNeg));
    }


    /**
     * Performs corpus sampling according
     * to percentage provided in the config file.
     * @return
     */
    public int sampleCorpus(){

        //load files and get statistics
        HashSet<File> files = IOUtil.getINSTANCE().listAllFiles(corpus_dir, "");
        getStatistics(files);

        //compute new percetage of negative (majority) instances
        int sampleNeg = 100 - sampling_perc;
        int newNegSize = ((positives.size() * 100) / sampleNeg) - positives.size();

        files.clear();

        int i = 0;
        //select negative files up until the size requested
        for(File file: negatives){
            if(i < newNegSize)
                files.add(file);
            i++;
        }

        //include all positives
        files.addAll(positives);
        negatives.clear();
        positives.clear();

        logger.info("Done sampling.");
        getStatistics(files);

        //copy sampled dataset to a new directory
        for(File file : files){
            try {
                File newFile = new File(location.getAbsolutePath() + "/" + file.getName());
                Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error("Error copying file {}", file.getAbsolutePath());
            }
        }
        return files.size();
    }

}
