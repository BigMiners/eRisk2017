package extractor;

import common.Configs;
import common.DocUnit;
import common.IOUtil;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import filter.WordFilter;
import normalizer.SpecialCharNormalizer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Main class to manage feature extractor.
 * Has common attributes and methods for
 * all extractor types
 *
 * Created by halmeida on 1/27/17.
 */
public class Extractor {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    HashMap<String,Integer> featureList = new HashMap<String,Integer>();
    SpecialCharNormalizer charNormalizer = new SpecialCharNormalizer();
    boolean useStopList,useZipfList, useDiscVocabulary;
    WordFilter wordFilter;
    String work_dir, corpus_dir, resources_dir, features_dir, sentimentDir, tagger_dir, location, plaintxtFolder, sentimentDicFile;
    Properties props;
    String taggerModel ="";
    MaxentTagger tagger;
    int sampling_perc;


    // HA: main constructor
    public Extractor(){
        props = Configs.getInstance().getProps();
        corpus_dir = props.getProperty("CORPUS_DIRECTORY");
        work_dir = props.getProperty("WORK_DIRECTORY");

        resources_dir = props.getProperty("RESOURCES_DIR");
        features_dir = props.getProperty("FEATURE_DIR")+"/";
        tagger_dir = props.getProperty("TAGGER_DIR") + "/";
        plaintxtFolder = props.getProperty("PLAINTXT_DIR");

        location = work_dir + resources_dir + features_dir;
        taggerModel= work_dir + resources_dir + tagger_dir + props.getProperty("TAGGER_FILE");

        useStopList = Boolean.valueOf(props.getProperty("USE_STOPFILTER"));
        useZipfList = Boolean.valueOf(props.getProperty("USE_ZIPFFILTER"));
        useDiscVocabulary = Boolean.valueOf(props.getProperty("USE_DISC_VOCAB"));
        wordFilter = new WordFilter();
        if(useStopList) wordFilter.loadStopWords();

        tagger = new MaxentTagger(taggerModel);

        sampling_perc = Integer.parseInt(props.getProperty("SAMPLING_PERC"));
        if(sampling_perc > 0)
            location += "sampling"+sampling_perc+".";
    }


    /**
     * General method for feature extraction
     * @param users
     * @return
     */
//    public int extractFeatures(List<User> users, String step){
    public int extractFeatures(List<DocUnit> docs, String step){

        for(int i = 0; i < docs.size(); i++){

            //String post = normalizeContent(users.get(i).getTotalProduction());
            String post = normalizeContent(docs.get(i).getContent());
            post = normalizeSingleWord(post);

            ArrayList<String> tokens = tokenizeContent(post);
            // handle extra punctuation on a single token

            for(int j = 0; j < tokens.size(); j++){
                featureList = setOccPerFeature(tokens.get(j), featureList);
            }
        }
        featureList.clear();

        return featureList.size();
    }


    public Set<String> removeStopWords(Set<String> list){
        return wordFilter.filterWordByList(list,"stop");
    }



    /**
     * Performs specific normalization steps in a
     * given text, before feature extraction
     * HA
     * @param text
     * @return
     */

    public String normalizeContent(String text){

        /* HA: the order of normalization steps is important,
        and must not be changed without performing tests first */

        text = text.toLowerCase();

        text = charNormalizer.handleLinks(text);
        text = charNormalizer.replaceSpecialHTML(text);
        text = charNormalizer.replaceSmileyChars(text);

        text = text.replace("</", "<");
        text = text.replace("/>", ">");
        text = text.replace("\\", "");
        text = text.replaceAll("\\s+", " ");

        text = charNormalizer.handleXMLTags(text);
        text = charNormalizer.handlePunctuation(text);

        return text;
    }

    /**
     * Handle normalization of single words applying
     * norms that shouldnt be valid to entire post msg
     * (deal with cases like "friends/family"
     * or "kind-of-thing")
     * HA
     * @param text
     * @return
     */
    public String normalizeSingleWord(String text){
        StringBuilder sb = new StringBuilder();
        ArrayList<String> words = tokenizeContent(text);

        for(int j = 0; j < words.size(); j++){
            String w = words.get(j);
            w = charNormalizer.handleWordPunctuation(w);

            String[] splitted = StringUtils.split(w, " ");
            for(int i = 0; i < splitted.length; i++){
                if(isTokenValid(splitted[i])) {
                    if(j < words.size()-1)
                        sb.append(splitted[i] + " ");
                    else sb.append(splitted[i]);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Generates plain text version of user
     * production and writes it to new files
     *
     * @param docs list of docs to generate plain text for
     */
    public void createPlainText(List<DocUnit> docs) {

        for (int i =0; i < docs.size(); i++) {
            DocUnit doc = docs.get(i);
            String content = doc.getContent();
            content = normalizeContent(content);
            content = tokenizeContentBySentence(content);

            String chunk = corpus_dir.substring(corpus_dir.length()-1);
            String path = corpus_dir + plaintxtFolder + "/" + doc.getId() + "_"+chunk+"." + doc.getLabel().name() + ".txt";
            IOUtil.getINSTANCE().writeOutput(path, content);
            if(i%100 == 0) logger.info("Processed " + i +" docs.");
        }
        logger.info("Done processing plain text docs.");
    }

    /**
     * Informs features and their occurence
     * (i.e., counts all times a feature
     * was seen in a given list)
     * HA
     * @param feature
     * @param list
     * @return
     */
    public HashMap<String,Integer> setOccPerFeature(String feature, HashMap<String,Integer> list){

        if(!list.containsKey(feature)){
            list.put(feature,1);
        }
        else{
            int count = list.get(feature);
            list.put(feature, count+1);
        }
        return list;
    }

    /**
     * Returns a POS tagged text,
     * spplited by a white space
     * HA
     * @param normText
     * @param tagger
     * @return
     *
     */
    @SuppressWarnings("untokenizable")
    public String[] getPOSTags(String normText, MaxentTagger tagger){
        return tagger.tagString(normText).split(" ");
    }

    /**
     * Returns the word of a (word_POS) string
     * @param taggedWord
     * @return
     */
    public String getPOSWord(String taggedWord){
        if(taggedWord.length() > 0 && taggedWord.contains("_"))
        return taggedWord.substring(0, taggedWord.indexOf("_"));
        else return taggedWord;
    }

    /**
     * Returns the POS of a (word_POS) string
     * @param taggedWord
     * @return
     */
    public String getPOSTag(String taggedWord){
        return taggedWord.substring(taggedWord.indexOf("_")+1);
    }



    /**
     * Splits a text into tokens, using
     * single white space as criterion
     * HA
     * @param text
     * @return
     */
    public ArrayList<String> tokenizeContent(String text){
        text = text.replace(" ", "*");
        String[] tokenized = StringUtils.split(text, "*");

        //filter tokens by length
        //get rid of tokens having single char
        ArrayList<String> cleaned = new ArrayList<String>();

        for(int i = 0; i < tokenized.length; i++){

            if(isTokenValid(tokenized[i])){
                cleaned.add(tokenized[i]);
            }
        }

        return cleaned;
    }

    /**
     * Splits a text into tokens, using
     * single white space as criterion
     * HA
     * @param text
     * @return
     */
    public String tokenizeContentBySentence(String text){
        text = text.replace(". ", ".\n");
        text = text.replace("? ", ".\n");
        text = text.replace("; ", ".\n");
        text = text.replace("! ", ".\n");

        return text;
    }

    /**
     * Sanity check for a ngram (token)
     * tokens must have:
     * at least one letter
     * at least three chars
     * HA
     * @param word
     * @return
     */
    public boolean isTokenValid(String word){
        if((word.length() >= 3 || word.equalsIgnoreCase("no"))
                && word.matches(".*[a-zA-Z]+.*"))
            return true;
        else return false;
    }

    /**
     * Provides only list of features (w/o count)
     * @return
     */
    public HashMap<String,Integer> getFeatureList(){
        return featureList;
    }


}
