package extractor;

import common.DocUnit;
import common.IOUtil;
import common.User;
import edu.stanford.nlp.process.Morphology;

import java.util.List;

/**
 * Created by halmeida on 1/27/17.
 */
public class POSExtractor extends Extractor{

    String posFile ="";
    Boolean stemPOS;
        public POSExtractor(){
        super();
        posFile = props.getProperty("POS_FILE");
            stemPOS = Boolean.valueOf(props.getProperty("USE_STEM"));
        }

    /**
     * Extraction of features for POS tags
     * Tags words using POS tagger, and
     * selects only words belonging to
     * specific POS tag set
     *
     * @param docs list of docs to extract features from
     * @param step module type (extract, model, etc.)
     */
    @Override
    public int extractFeatures(List<DocUnit> docs, String step){

        int docsProcessed = 0;

        if(featureList.size() > 0 ) featureList.clear();

        for(int i = 0; i < docs.size(); i++) {
            docsProcessed = i;

            //normalize and get POS tags
            String thisContent = normalizeContent(docs.get(i).getContent());
            // handle extra punctuation on a single token
            thisContent = normalizeSingleWord(thisContent);

            String[] tagged = getPOSTags(thisContent, tagger);

            for(int j = 0; j < tagged.length; j++){
                //retrieve each tag, and (word_tag)
                String taggedWord =  getPOSWord(tagged[j]);
                String POS = getPOSTag(tagged[j]);
                String word = taggedWord;
                if(stemPOS && !word.isEmpty()) word = Morphology.stemStatic(taggedWord, POS).word();

                //check if word has a relevant POS tag
                if(isPOSValid(POS))
                    //add to features list
                    featureList = setOccPerFeature(taggedWord, featureList);
            }
            if( i > 0 && i % 100 == 0 && !step.contains("model")) logger.info("Processed {} out of {} docs and a total of {} posts.", i, docs.size(), docsProcessed);        }

        if(!step.contains("model") && featureList.size() > 0) {
            logger.info("Exporting POS features...");
            IOUtil.getINSTANCE().exportFeatures(location + posFile, featureList, docs.size(), step);
            featureList.clear();
        }



        return featureList.size();
    }


   /**
     * Validate POS according to
     * POS tag set experimentally chosen:
     * JJ, NN, PDT, RP, VB
     * @param text pos tag
     * @return true if tag belongs to set
     */
    private boolean isPOSValid(String text){
        if( text.contains("JJ") ||
            text.contains("NN") ||
            text.contains("PDT") ||
            text.contains( "RP") ||
            text.contains("VB") )
            return true;
        else return false;
    }
}
