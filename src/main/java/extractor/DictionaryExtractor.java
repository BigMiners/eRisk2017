package extractor;

import common.DocUnit;
import common.IOUtil;
import common.User;
import edu.stanford.nlp.process.Morphology;

import java.util.HashMap;
import java.util.List;

/**
 * Created by halmeida on 1/27/17.
 */
public class DictionaryExtractor extends Extractor{

    String file, dicFile, dicDir, dicName;
    HashMap<String, String> dictionary;

    public DictionaryExtractor(String dicName) {
        super();
        this.dicName = dicName;
        loadDicVars(dicName);

        dicDir = work_dir + resources_dir +props.getProperty("DICTIONARIES_DIR") + "/";
        dictionary = IOUtil.getINSTANCE().loadDictionary(dicDir, dicFile);
    }

    public DictionaryExtractor() {
    }

    /**
     * Extraction of features from dictionaries:
     * list dictionary words that appear in user
     * content.
     * @param docs list of docs to extract features from
     * @param step module type (extract, model, etc.)
     * @return
     */
    @Override
    public int extractFeatures(List<DocUnit> docs, String step) {

        if(featureList.size() > 0 ) featureList.clear();

        normalizeDictionary();

        int docsProcessed = 0;

            for (int i = 0; i < docs.size(); i++) {
                docsProcessed = i;

                //normalize post content and get POS tags
                String text = normalizeContent(docs.get(i).getContent());
                // handle extra punctuation on a single token
                text = normalizeSingleWord(text);

                //POS tagger is used to provide stemmed format of words
                String[] tagged = getPOSTags(text, tagger);

                for (int j = 0; j < tagged.length; j++) {
                    //retrieve each word and each tag
                    String taggedWord = getPOSWord(tagged[j]);
                    String POS = getPOSTag(tagged[j]);

                    //get stemmed word using POS tag
                    String word = taggedWord;
                    if(!word.isEmpty()) word = Morphology.stemStatic(taggedWord, POS).word();

                    //if stemmed word is in the sentiment dictionary
                    if (dictionary.keySet().contains(word)
                            || dictionary.values().contains(word)) {
                        //add to feature list
                        featureList = setOccPerFeature(word, featureList);
                    }
                }

                if( i > 0 && i % 100 == 0 && !step.contains("model")) logger.info("Processed {} out of {} docs and a total of {} posts.", i, docs.size(), docsProcessed);            }

            if(!step.contains("model") && featureList.size() > 0) {
                logger.info("Exporting {} dictionary features...", dicName);
                IOUtil.getINSTANCE().exportFeatures(location + file, featureList, docs.size(), step);
                featureList.clear();
            }

        return featureList.size();
    }


    /**
     * Normalization of single words in dictionaries
     * The normalization must be executed so the normalized
     * parsed text and the dictionary words will match
     */
    public void normalizeDictionary(){
        HashMap<String, String> normalizedDic = new HashMap<>();

        for(String key : dictionary.keySet()){
            String value = dictionary.get(key);
            key = normalizeSingleWord(key);
            if(!value.isEmpty()) value = normalizeSingleWord(value);
            normalizedDic.put(key,value);
        }

        dictionary.clear();
        dictionary.putAll(normalizedDic);
    }

    /**
     * Adjust variable values according to dictionary used
     * @param dicName dictionary name
     */
    public void loadDicVars(String dicName) {
        if (dicName.contains("sentic")) {
            file = props.getProperty("SENTIC_FILE");
            dicFile = props.getProperty("SENTIC_DIC");
        }
        else if  (dicName.contains("feelings")) {
            file = props.getProperty("FEELINGS_FILE");
            dicFile = props.getProperty("FEELINGS_DIC");
        }
        else if (dicName.contains("meds")) {
            file = props.getProperty("MEDS_FILE");
            dicFile = props.getProperty("MEDS_DIC");
        }
        else if (dicName.contains("drugs")) {
            file = props.getProperty("DRUGS_FILE");
            dicFile = props.getProperty("DRUGS_DIC");
        }
        else if (dicName.contains("diseases")) {
            file = props.getProperty("DISEASES_FILE");
            dicFile = props.getProperty("DISEASES_DIC");
        }
    }
}
