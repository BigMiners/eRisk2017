package extractor;

import common.DocUnit;
import common.IOUtil;
import common.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by halmeida on 1/27/17.
 */
public class VocabularyExtractor extends Extractor {

    String vocabFile;
    int writingsThreshold;
    int vocabOccurence;

    public VocabularyExtractor(){
        super();

        vocabFile = props.getProperty("VOCABULARY_FILE");
        vocabOccurence = Integer.parseInt(props.getProperty("VOCABULARY_OCC"));
        writingsThreshold = Integer.parseInt(props.getProperty("WRITINGS_THRESHOLD"));
    }


    /**
     * Extraction of features from vocabulary list:
     * list vocabulary words that appear in user
     * content.
     * @param docs list of docs to extract features from
     * @param step module type (extract, model, etc.)
     */
    @Override
    public int extractFeatures(List<DocUnit> docs, String step){

        HashMap<String,Integer> riskVocab = new HashMap<>(), noriskVocab = new HashMap<>();

        for(int i = 0; i < docs.size(); i++) {
            DocUnit docUnit = docs.get(i);
            int writings = 0;
            if(docUnit instanceof User){
                User user = (User) docUnit;
                writings = user.getWritings();
            }

            if(writings <= writingsThreshold){

                String label = docUnit.getLabel().name().toLowerCase();
                String text = normalizeContent(docs.get(i).getContent());
                text = normalizeSingleWord(text);
                ArrayList<String> words = tokenizeContent(text);

                for(String word : words) {
                    if(label.contains("risk"))
                        riskVocab = setOccPerFeature(word, riskVocab);
                    else noriskVocab = setOccPerFeature(word, noriskVocab);
                }
            }

            if( i > 0 && i % 100 == 0) logger.info("Processed {} out of {} docs.", i, docs.size());
        }

        for(String word : riskVocab.keySet()){
            if(riskVocab.get(word) >= vocabOccurence)
            featureList = setOccPerFeature(word, featureList);
        }
        logger.info("Exporting Vocabulary features...");
        IOUtil.getINSTANCE().exportFeatures(location + vocabFile+".writings.positive", featureList, docs.size(), step);
        featureList.clear();

        for(String word : noriskVocab.keySet()){
            if(riskVocab.get(word) >= vocabOccurence)
            featureList = setOccPerFeature(word, featureList);
        }
        logger.info("Exporting Vocabulary features...");
        IOUtil.getINSTANCE().exportFeatures(location + vocabFile+".writings.negative", featureList, docs.size(), step);
        featureList.clear();

        return featureList.size();
    }


    private HashMap<String,Integer> mergeVocab (HashMap<String,Integer> positive, HashMap<String,Integer> negative){
        HashMap<String,Integer> finalList = new HashMap<String,Integer>();
        return finalList;
    }

}
