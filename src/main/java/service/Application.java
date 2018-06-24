package service;

import common.DocUnit;
import informationretrieval.AdHocDocumentRetrieval;
import bm25.PredictOneAgainstOthers;
import common.Configs;
import extractor.*;
import indexer.Extractors;
import indexer.Indexer;
import model.MatrixModel;
import parser.Parser;
import predictor.*;
import preprocess.CorpusSampler;
import sentiment.SentimentPredictor;
import vectorizer.ModelVectorizer;
import weka.classifiers.Evaluation;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Application {

    public static void main(String[] args) {

        String module = System.getProperty("module");
        String classifier = System.getProperty("classifier");
        String deepLearner = System.getProperty("learner");
        Properties props = Configs.getInstance().getProps();
        boolean verbose = false;

        boolean useNgrams = Boolean.valueOf(props.getProperty("USE_NGRAM"));
        boolean usePOS = Boolean.valueOf(props.getProperty("USE_POS"));
        boolean useSenctic = Boolean.valueOf(props.getProperty("USE_SENTIC"));
        boolean useFeelings = Boolean.valueOf(props.getProperty("USE_FEELINGS"));
        boolean useMeds = Boolean.valueOf(props.getProperty("USE_MEDS"));
        boolean useDrugs = Boolean.valueOf(props.getProperty("USE_DRUGS"));
        boolean useDiseases = Boolean.valueOf(props.getProperty("USE_DISEASES"));
        boolean useVocabulary = Boolean.valueOf(props.getProperty("USE_VOCABULARY"));
        boolean useRules = Boolean.valueOf(props.getProperty("USE_RULES"));
        boolean overwriteRule = Boolean.valueOf(props.getProperty("OVERWRITE_RULES"));


        String task = props.getProperty("TASK");

        Parser parser = new Parser();

        List<DocUnit> docUnits = new ArrayList<>();

        RulePredictor rulePredictor = new RulePredictor();
        // TODO: from user to docUnit
        //  if (useRules) rulePredictor.loadPredictionByRule(users);
        //-------------

        //if we are not classifying, we want to parse data
        if (!module.contains("classify")
                && !module.contains("deeplearn")
                && !module.contains("bm25oneagainstothers")
                && !module.contains("indexer")
                && !module.contains("sample")) {
            //parse labeled data and list all IDs in it
            docUnits = parser.parse("").getDocsAsList();
        }

        switch (module) {

            //module to get statistics on the corpus
            case ("stats"): {
                parser.getCorpusStats();
                break;
            }

            //module to perform sentiment analysis using VADER
            case ("vader"): {
                SentimentPredictor predictor = new SentimentPredictor();
                predictor.predict(docUnits);
                break;
            }

            //module to perform corpus sampling
            case ("sample"): {
                CorpusSampler sampler = new CorpusSampler();
                sampler.sampleCorpus();
                break;
            }

            //module to generate plain text representation of files
            case ("plaintext"): {
                Extractor extractor = new Extractor();
                extractor.createPlainText(docUnits);
                break;
            }

            //module to generate text2vec representation of
            //document or string objects
            case ("vectorize"): {
                ModelVectorizer extractor = new ModelVectorizer();
                extractor.extractTxt2Vec(docUnits);
                break;
            }

            case ("bm25oneagainstothers"): {
                new PredictOneAgainstOthers();
                break;
            }

            case ("bm25oneagainstothersTest"): {
                //new PredictOneAgainstOthersTests();
                new AdHocDocumentRetrieval();
                break;
            }

            //module to extract features from dataset
            //for each feature type named below
            case ("extract"): {

                if (useNgrams) {
                    NgramExtractor extractor = new NgramExtractor();
                    extractor.extractFeatures(docUnits, module);
                }
                if (usePOS) {
                    POSExtractor extractor = new POSExtractor();
                    extractor.extractFeatures(docUnits, module);
                }
                if (useSenctic) {
                    DictionaryExtractor extractor = new DictionaryExtractor("sentic");
                    extractor.extractFeatures(docUnits, module);
                }
                if (useFeelings) {
                    DictionaryExtractor extractor = new DictionaryExtractor("feelings");
                    extractor.extractFeatures(docUnits, module);
                }
                if (useMeds) {
                    DictionaryExtractor extractor = new DictionaryExtractor("meds");
                    extractor.extractFeatures(docUnits, module);
                }
                if (useDrugs) {
                    DictionaryExtractor extractor = new DictionaryExtractor("drugs");
                    extractor.extractFeatures(docUnits, module);
                }
                if (useDiseases) {
                    DictionaryExtractor extractor = new DictionaryExtractor("diseases");
                    extractor.extractFeatures(docUnits, module);
                }
                if (useVocabulary) {
                    VocabularyExtractor extractor = new VocabularyExtractor();
                    extractor.extractFeatures(docUnits, module);
                }
                break;
            }

            case ("indexer"): {
                System.setProperty("indexer:extract:meds", "true");
                System.setProperty("indexer:extract:drugs", "true");
                System.setProperty("indexer:extract:diseases", "true");
                System.setProperty("indexer:core", "erisk-with-dict-and-clpsych");

                Indexer indexer = new Indexer();

                indexer.parseCorpora();

                Extractors extractors = new Extractors(indexer.getUsers());
                extractors.setExtractors();
                extractors.handleExtracting();

                indexer.setUsers(extractors.getUsers());
                indexer.pushDocuments();

                break;

            }

            //module to perform deep learning classification
            case ("deeplearn"): {

                DeepPredictor predictor = new DeepPredictor();
                predictor.trainModel(deepLearner);
                predictor.evaluateModel();
                break;
            }

            //module to generate matrices (ARFF)
            case ("model"): {
                MatrixModel model = new MatrixModel();
                model.generateModel(docUnits);
                break;
            }

            //module to perform standard classification
            case ("classify"): {
                Predictor predictor = new Predictor(classifier);
                Evaluation supervResults = predictor.predict();
                Instances rawData = predictor.getRawInstances();
                Evaluator evaluator = new Evaluator(classifier);
                evaluator.getAllPredictions(supervResults.predictions(), rawData, rulePredictor.getRuleUsers(), overwriteRule);
                break;
            }
        }
        System.exit(1);
    }
}