package extractor;

import common.DocUnit;
import common.IOUtil;
import common.User;
import edu.stanford.nlp.process.Morphology;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by halmeida on 1/27/17.
 */
public class NgramExtractor extends Extractor{

    String ngramFile;
    int ngramSize ;
    Boolean stemNgram;


    // HA: main constructor
    public NgramExtractor() {
        super();
        ngramSize = Integer.parseInt(props.getProperty("NGRAM_SIZE"));
        ngramFile = props.getProperty("NGRAM_FILE") + ngramSize;
        stemNgram = Boolean.valueOf(props.getProperty("USE_STEM"));
        if(useStopList)ngramFile += ".stop";
        if(stemNgram) ngramFile += ".stemmed";
    }


    public void setNgramSize(int size){
        ngramSize = size;
    }

    /**
     * Extracts n-grams from a given text
     * and exports it to a file
     * HA
     * @param docs list of docs to extract features from
     * @param step module type (extract, model, etc.)
     */
    @Override
    public int extractFeatures(List<DocUnit> docs, String step) {

        if(featureList.size() > 0 ) featureList.clear();

        int ngramLength = ((ngramSize > 1) ? ngramSize - 1 : ngramSize);
        int docsProcessed = 0;
//        loadWordFilterLists(docs, step);

        for(int i = 0; i < docs.size(); i++) {

            ArrayList<String> words = new ArrayList<>();

            docsProcessed = i;
            String text = normalizeContent(docs.get(i).getContent());

            // handle extra punctuation on a single token
            text = normalizeSingleWord(text);

            if(stemNgram) {
                //get stemmed words using the POS tagger
                String[] tagged = getPOSTags(text, tagger);

                for (int j = 0; j < tagged.length; j++) {
                    //retrieve each word and each tag
                    String taggedWord = getPOSWord(tagged[j]);
                    String POS = getPOSTag(tagged[j]);

                    //get stemmed word using POS tag
                    String word = taggedWord;
                    if(!word.isEmpty()) word = Morphology.stemStatic(taggedWord, POS).word();

                    if(word.length() > 2) words.add(word);
                }
            }

            if(words.isEmpty() || words == null) words =  tokenizeContent(text);
            if(useStopList) words = wordFilter.filterWordByList(words, "stop");

            //generate a ngram according to size of "n"
            for (int j = 0; j < words.size() - ngramLength; j++) {
                String ngram = "";
                int size = 0;

                do {
                    if (ngram.isEmpty())
                        ngram = words.get(j + size).toLowerCase();
                    else
                        ngram += " " + words.get(j + size).toLowerCase();
                    size++;
                } while (size < ngramSize);

                //export feature list
                featureList = setOccPerFeature(ngram, featureList);
            }
            if( i > 0 && i % 50 == 0 && !step.contains("model")) logger.info("Processed {} out of {} docs and a total of {} posts.", i, docs.size(), docsProcessed);
        }

        if(!step.contains("model") && featureList.size() > 0) {
            logger.info("Exporting Ngram features...");
            IOUtil.getINSTANCE().exportFeatures((location + ngramFile), featureList, docs.size(), step);
            featureList.clear();
        }

        return featureList.size();
    }




}
