package vectorizer;

import common.Configs;
import filter.WordFilter;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.util.Properties;

/**
 * Class for handling parameters for vectorizers
 * Created by halmeida on 2/2/17.
 */
public class Vectorizer {

    Properties props;
    String corpus_dir, txt2vecFolder, plaintxtFolder, task, googleVecPath;
    boolean useGoogleWord2Vec;

    WordFilter wordFilter;
    WordVectors googleWord2Vec;

    String level;

    public Vectorizer(){

        wordFilter = new WordFilter();
        props = Configs.getInstance().getProps();

        corpus_dir = props.getProperty("CORPUS_DIRECTORY");
        task = props.getProperty("TASK");
        txt2vecFolder = props.getProperty("TXT2VEC_DIR");
        plaintxtFolder = props.getProperty("PLAINTXT_DIR");
        level = props.getProperty("TXT2VEC_LEVEL");
        useGoogleWord2Vec = Boolean.parseBoolean(props.getProperty("USE_GOOGLEVEC"));
        googleVecPath = props.getProperty("GOOGLE_WORD2VEC");
    }

}
