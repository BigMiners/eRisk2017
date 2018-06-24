package model;

import common.Configs;
import common.DocUnit;
import common.IOUtil;
import common.User;
import extractor.DictionaryExtractor;
import extractor.Extractor;
import extractor.NgramExtractor;
import extractor.POSExtractor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.WebSocket;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to generate vector matrix from
 * training and test data, given a set of features
 * <p>
 * Created by halmeida on 1/27/17.
 */
public class ArffVector {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    boolean useNgram, useSentic, useFeelings, usePOStags, useVocabulary, useStopFilter,
            useWritingsCount, usePostFrequency, useCLPsych,
            useMedsCount, useMeds,
            useDrugsCount, useDrugs,
            useDiseasesCount, useDiseases;
    String ngramFeatures, senticFeatures, feelingsFeatures, POSFeatures, vocabFeatures,
            medsFeatures, drugsFeatures, diseasesFeatures,
            feature_dir, resources_dir, work_dir, ngramSize, project;
    Properties props;
    HashSet<String> ngrams, sentic, feelings, posTags, vocabulary, stopwords,
            meds, drugs, diseases;
    Set<String> features;
    String featureOccurence = "2";
    Extractor extractor;
    NgramExtractor ngramExt;
    POSExtractor posExt;
    DictionaryExtractor feelingsExt, medsExt, drugsExt, diseasesExt;
    ArrayList<String> featureListIndex;


    //main constructor
    public ArffVector() {

        props = Configs.getInstance().getProps();
        project = props.getProperty("PROJECT");
        work_dir = props.getProperty("WORK_DIRECTORY");
        resources_dir = props.getProperty("RESOURCES_DIR");
        feature_dir = props.getProperty("FEATURE_DIR") + "/";
        ngramFeatures = props.getProperty("NGRAM_FILE");
        ngramSize = props.getProperty("NGRAM_SIZE");

        if (!ngramFeatures.contains("size" + ngramSize))
            ngramFeatures += ngramSize;

        if (!ngramFeatures.contains("stemmed") && Boolean.valueOf(props.getProperty("USE_STEM")))
            ngramFeatures += ".stemmed";

        senticFeatures = work_dir + resources_dir + feature_dir + props.getProperty("SENTIC_FILE");
        feelingsFeatures = work_dir + resources_dir + feature_dir + props.getProperty("FEELINGS_FILE");
        medsFeatures = work_dir + resources_dir + feature_dir + props.getProperty("MEDS_FILE");
        drugsFeatures = work_dir + resources_dir + feature_dir + props.getProperty("DRUGS_FILE");
        diseasesFeatures = work_dir + resources_dir + feature_dir + props.getProperty("DISEASES_FILE");
        POSFeatures = work_dir + resources_dir + feature_dir + props.getProperty("POS_FILE");
        vocabFeatures = work_dir + resources_dir + props.getProperty("VOCABULARY_DIC_DIR") + "/" + props.getProperty("VOCABULARY_DIC");

        useMeds = Boolean.valueOf(props.getProperty("USE_MEDS"));
        useMedsCount = Boolean.valueOf(props.getProperty("USE_MEDS_COUNT"));
        useDrugs = Boolean.valueOf(props.getProperty("USE_DRUGS"));
        useDrugsCount = Boolean.valueOf(props.getProperty("USE_DRUGS_COUNT"));
        useDiseases = Boolean.valueOf(props.getProperty("USE_DISEASES"));
        useDiseasesCount = Boolean.valueOf(props.getProperty("USE_DISEASES_COUNT"));
        useSentic = Boolean.valueOf(props.getProperty("USE_SENTIC"));
        useFeelings = Boolean.valueOf(props.getProperty("USE_FEELINGS"));

        useWritingsCount = Boolean.valueOf(props.getProperty("USE_WRITINGS_COUNT"));
        usePostFrequency = Boolean.valueOf(props.getProperty("USE_POST_FREQ"));
        useStopFilter = Boolean.valueOf(props.getProperty("USE_STOPFILTER"));
        useNgram = Boolean.valueOf(props.getProperty("USE_NGRAM"));
        usePOStags = Boolean.valueOf(props.getProperty("USE_POS"));
        useVocabulary = Boolean.valueOf(props.getProperty("USE_VOCABULARY"));
//        useWritingsCount = Boolean.valueOf(props.getProperty("USE_WRITINGS_COUNT"));

        if ((useNgram && ngramFeatures.contains("clpsych")) ||
                (useSentic && senticFeatures.contains("clpsych")) ||
                (useFeelings && feelingsFeatures.contains("clpsych")) ||
                (useMeds && medsFeatures.contains("clpsych")) ||
                (useDrugs && drugsFeatures.contains("clpsych")) ||
                (useDiseases && diseasesFeatures.contains("clpsych")) ||
                (usePOStags && POSFeatures.contains("clpsych")) ||
                (useVocabulary && vocabFeatures.contains("clpsych")))
            useCLPsych = true;
        else useCLPsych = false;

        featureListIndex = new ArrayList<>();
        extractor = new Extractor();
        ngrams = new HashSet<>();
        loadResources();
    }

    /**
     * Loads lists of features to be
     * used for generating ARFF file
     */
    public void loadResources() {
        features = new HashSet<String>();

        if (useNgram) {
            ngramExt = new NgramExtractor();
            if (ngramFeatures.contains(",")) {
                String[] files = ngramFeatures.split(",");
                for (String file : files) {
                    file = work_dir + resources_dir + feature_dir + file;
                    ngrams.addAll(IOUtil.getINSTANCE().loadFilePerLineSeparator(file, featureOccurence));
                }
            } else {
                ngramFeatures = work_dir + resources_dir + feature_dir + ngramFeatures;
                ngrams = IOUtil.getINSTANCE().loadFilePerLineSeparator(ngramFeatures, featureOccurence);
            }
            features.addAll(ngrams);
        }
        if (useFeelings) {
            feelingsExt = new DictionaryExtractor("feelings");
            feelings = IOUtil.getINSTANCE().loadFilePerLineSeparator(feelingsFeatures, "");
            features.addAll(feelings);
        }
        if (usePOStags) {
            posExt = new POSExtractor();
            posTags = IOUtil.getINSTANCE().loadFilePerLineSeparator(POSFeatures, featureOccurence);
            features.addAll(posTags);
        }
        if (useMeds) {
            medsExt = new DictionaryExtractor("meds");
            meds = IOUtil.getINSTANCE().loadFilePerLineSeparator(medsFeatures, featureOccurence);
            features.addAll(meds);
        }
        if (useDrugs) {
            drugsExt = new DictionaryExtractor("drugs");
            drugs = IOUtil.getINSTANCE().loadFilePerLineSeparator(drugsFeatures, featureOccurence);
            features.addAll(drugs);
        }
        if (useDiseases) {
            diseasesExt = new DictionaryExtractor("diseases");
            diseases = IOUtil.getINSTANCE().loadFilePerLineSeparator(diseasesFeatures, featureOccurence);
            features.addAll(diseases);
        }

        featureListIndex.addAll(features);
    }

    /**
     * Loads features from file and generates
     * feature list for ARFF header
     * HA
     *
     * @param expType
     * @param featureSet
     * @return
     */
    public String getHeader(String expType, String featureSet, String unit) {
        StringBuilder header = new StringBuilder();
        int size = 0;

        String projectName = project.contains("-") ? project.substring(0, project.indexOf("-")) : project;
        DocUnit.Label label = DocUnit.Label.UNDECIDED;
        String labelList = label.informLabels(projectName);

        if (expType.contains("test")) header.append("% ARFF test file - " + projectName + " Task 2017\n\n");
        else header.append("% ARFF training file - " + projectName + "  Task 2017\n\n");

        header.append("@RELATION " + projectName + "\n");
        header.append(getHeaderValue("id", size++, "id"));

        if (unit.contains("user") && usePostFrequency) header.append(getHeaderValue("postfrequency", size++, "postfrequency"));
        if (useMedsCount) header.append(getHeaderValue("medsCount", size++, "medsCount"));
        if (useDrugsCount) header.append(getHeaderValue("drugsCount", size++, "drugsCount"));
        if (useDiseasesCount) header.append(getHeaderValue("diseasesCount", size++, "diseasesCount"));

        //if(features.size() > 0){
        if (featureListIndex.size() > 0) {
            logger.info("There are " + features.size() + " unique features for this setup.");

            for (int i = 0; i < featureListIndex.size(); i++) {
                String thisFeature = featureListIndex.get(i);
                if (useNgram && ngrams.contains(thisFeature)) {
                    header.append(getHeaderValue(thisFeature, size++, "Ngram"));
                } else if (useFeelings && feelings.contains(thisFeature)) {
                    header.append(getHeaderValue(thisFeature, size++, "Sentiment"));
                } else if (usePOStags && posTags.contains(thisFeature)) {
                    header.append(getHeaderValue(thisFeature, size++, "POSFeature"));
                } else if (useMeds && meds.contains(thisFeature)) {
                    header.append(getHeaderValue(thisFeature, size++, "Meds"));
                } else if (useDrugs && drugs.contains(thisFeature)) {
                    header.append(getHeaderValue(thisFeature, size++, "Drugs"));
                } else if (useDiseases && diseases.contains(thisFeature)) {
                    header.append(getHeaderValue(thisFeature, size++, "Diseases"));
                }
            }
        }


        header.append("@ATTRIBUTE class\t{" + labelList + "}\n");
        //"risk, " +
//                "norisk}\n");
        header.append("@DATA\n");

        return header.toString();
    }

    /**
     * Generates a vector line for model matrix
     * Accounts for each feature occurrence
     * in the doc
     * HA
     *
     * @return
     */
    public String getVectorLine(DocUnit doc) {
        StringBuilder vectorLine = new StringBuilder();

        vectorLine.append(doc.getId() + ",");

        if (doc instanceof User && usePostFrequency) {
            User user = (User) doc;
            vectorLine.append(user.getPostFrequency() + ",");
        }
        if (useMedsCount) {
            doc.setMedsCount(meds);
            vectorLine.append(doc.getMedsCount() + ",");
        }
        if (useDrugsCount) {
            doc.setDrugsCount(drugs);
            vectorLine.append(doc.getDrugsCount() + ",");
        }
        if (useDiseasesCount) {
            doc.setDiseasesCount(diseases);
            vectorLine.append(doc.getDiseasesCount() + ",");
        }

        int productionSize = doc.getContent().split(" ").length;

        if (features.size() > 0) {
            //check if it is worth to loop over all features
            if (features.size() < productionSize) {
                String content = extractor.normalizeContent(doc.getContent());
                content = extractor.normalizeSingleWord(content);
                for (String thisFeature : featureListIndex) {
                    vectorLine.append(getVectorValue(thisFeature, content));
                }
            } else {
                // if not, loop over only document content features instead
                // get all features for a single document
                HashMap<String, Integer> lineFeatures = getLineFeatures(doc);
                int[] thisLine = new int[featureListIndex.size()];

                if (lineFeatures.size() > 0) {
                    //if document feature is in the complete feature list
                    for (String feature : lineFeatures.keySet()) {
                        feature = feature.toLowerCase();
                        if (featureListIndex.contains(feature)) {
                            // retrieve the feature count for document
                            try {
                                int featureCount = lineFeatures.get(feature);
                                thisLine[featureListIndex.indexOf(feature)] = featureCount;
                            } catch (NullPointerException e) {
                                logger.info(" broken feature: {}", feature);
                            }
                        }
                    }
                }
                for (int i : thisLine) {
                    vectorLine.append(i + ",");
                }
            }
        }

        vectorLine.append(doc.getLabel().name().toLowerCase());
        return vectorLine.toString();
    }


    public HashMap<String, Integer> getLineFeatures(DocUnit doc) {
        HashMap<String, Integer> lineFeatures = new HashMap<String, Integer>();
        List<DocUnit> thisDoc = new ArrayList<>();
        thisDoc.add(doc);
        String step = "model";

        if (useNgram) {
            String[] sizes = ngramFeatures.split(",");
            for (String size : sizes) {
                int temp_ngramsize = extractNgramSize(size);

                ngramExt.setNgramSize(temp_ngramsize);
                ngramExt.extractFeatures(thisDoc, step);
                lineFeatures.putAll(ngramExt.getFeatureList());
            }
        }
        if (usePOStags) {
            posExt.extractFeatures(thisDoc, step);
            lineFeatures.putAll(posExt.getFeatureList());
        }
        if (useFeelings) {
            feelingsExt.extractFeatures(thisDoc, step);
            lineFeatures.putAll(feelingsExt.getFeatureList());
        }
        if (useMeds) {
            medsExt.extractFeatures(thisDoc, step);
            lineFeatures.putAll(medsExt.getFeatureList());
        }
        if (useDrugs) {
            drugsExt.extractFeatures(thisDoc, step);
            lineFeatures.putAll(drugsExt.getFeatureList());
        }
        if (useDiseases) {
            diseasesExt.extractFeatures(thisDoc, step);
            lineFeatures.putAll(diseasesExt.getFeatureList());
        }

        return lineFeatures;
    }


    int extractNgramSize(String size) {
        Pattern pattern = Pattern.compile("(size.)\\d+");
        Matcher matcher = pattern.matcher(size);
        if(matcher.find()){
            return Integer.parseInt(matcher.group(0).split("size.")[1]);
        }
        return 0;
    }


    /**
     * Determines the feature value (occurence)
     * to be written in the ARFF vector
     * HA
     *
     * @param feature
     * @param content
     * @return
     */
    private String getVectorValue(String feature, String content) {

        int featureCount = 0;
        feature = feature.toLowerCase();

        if (content.contains(feature)) {
            featureCount = StringUtils.countMatches(content, feature);
        }
        return (featureCount + ",");
    }

    /**
     * Determines the feature name and attribute
     * to be written in the ARFF head
     * HA
     *
     * @param feature
     * @param count
     * @param featureType
     * @return
     */
    private String getHeaderValue(String feature, int count, String featureType) {

        String namefeature = feature.replaceAll("\\s", "-");
        namefeature = namefeature.replaceAll("[,:=+']", "-");

        String ref = featureType + String.valueOf(count) + namefeature;

        return ("@ATTRIBUTE " + ref + "\tREAL \t\t%" + feature + "\n");
    }
}
