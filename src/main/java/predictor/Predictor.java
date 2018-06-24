package predictor;

import common.Configs;
import common.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.functions.*;
import weka.classifiers.meta.EnsembleSelection;
import weka.classifiers.meta.ensembleSelection.EnsembleSelectionLibrary;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.output.prediction.PlainText;

import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.File;
import java.util.Properties;
import java.util.Random;

/**
 * Class to train and test classification models.
 * Handles single and ensemble classifiers
 *
 * Created by halmeida on 1/26/17.
 */
public class Predictor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static int SEED = 1; //the seed for randomizing the data

    String work_dir, resources_dir, model_dir,
            ensemble_dir, ensemble_task, ensemble_lib, ensemble_type,
            trainingData, testingData, classifier,
            results_dir, resultFile, folds;
    Boolean CV, CFS, Cluster;
    Classifier cls;
    Instances trainData, testData, filtTrainData, filtTestData;
    Properties props;
    AttributeSelection selector;

    public Predictor(String classif){

        props = Configs.getInstance().getProps();

        work_dir = props.getProperty("WORK_DIRECTORY");
        resources_dir = props.getProperty("RESOURCES_DIR");
        model_dir = props.getProperty("MODEL_DIR") + "/";

        CV = Boolean.valueOf(props.getProperty("CV"));
        folds = props.getProperty("FOLDS");
        CFS = Boolean.valueOf(props.getProperty("CFS"));

        classifier = classif;
        selector = initializeCSF();

        ensemble_task = props.getProperty("ENSEMBLE_TASK");
        ensemble_dir = work_dir + resources_dir + props.getProperty("ENSEMBLE_DIR");
        ensemble_lib = ensemble_dir +"/"+ props.getProperty("ENSEMBLE_LIB");


        results_dir = work_dir + resources_dir + props.getProperty("RESULTS_DIR");
        trainingData = work_dir +resources_dir + model_dir + props.getProperty("MODEL_FILE");
        testingData = work_dir +resources_dir + model_dir  + props.getProperty("TEST_FILE");

        if(CV) resultFile = results_dir + "/" + "CV_" + props.getProperty("MODEL_FILE").replace("arff", classifier);
        else resultFile = results_dir + "/" + props.getProperty("TEST_FILE").replace("arff", classifier);

        if(CFS) resultFile = resultFile + ".cfs";

    }

    /**
     * Reads training and test ARFFs,
     * generates training model and evaluates test set.
     *
     * @return
     */
    public Evaluation predict(){

        Evaluation eval = null;

        cls = getClassifier(classifier);

        try {
            //Loading train data
            if(!trainingData.isEmpty()) trainData = new ConverterUtils.DataSource(trainingData).getDataSet();
            //Flagging the class index on data
            trainData.setClassIndex(trainData.numAttributes()-1);
            logger.info("Training data loaded. Number of instances: " + trainData.numInstances() + "\n");

            //filter the file IDs, consider the new training set
            filtTrainData = filteredIDs(trainData);

            //CSF filtering
            if(CFS) filtTrainData = selectAttributes(filtTrainData, "train");

            if(CV) {
                //perform cross-validation
                eval = crossFold(filtTrainData, trainData, cls);
            }
            else {
                //perform train vs. test classification
                if(!testingData.isEmpty()) testData = loadData(testingData);
                testData.setClassIndex(testData.numAttributes()-1);
                logger.info("Test data loaded. Number of instances: " + testData.numInstances() + "\n");

                filtTestData = filteredIDs(testData);
                if(CFS) filtTestData = selectAttributes(filtTestData, "test");

                logger.info("Classifier: {}",cls.getClass());
                eval = classify(filtTrainData, filtTestData, cls, testData);

            }

        }catch(NullPointerException | IllegalArgumentException e){
            logger.info("MODEL_FILE or TEST_FILE names missing. Please check the config file.");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return eval;
    }

    /**
     * Trains and tests a classifier when two separated
     * datasets are provided.
     *
     * @param filteredTrain training data to build classifier
     * @param filteredTest  test data to evaluate classifier
     * @param classif  type of classifier applied
     * @throws Exception
     */
    public Evaluation classify(Instances filteredTrain, Instances filteredTest, Classifier classif, Instances test) throws Exception{

        StringBuffer sb = new StringBuffer();
        PlainText prediction = initPredictions(filteredTest);

        classif.buildClassifier(filteredTrain);
        Evaluation evaluateClassifier = new Evaluation(filteredTrain);
        evaluateClassifier.evaluateModel(classif, filteredTest, prediction, true);

        stats(evaluateClassifier);
        //outputSystemPredictions(evaluateClassifier.predictions(), test);
        return evaluateClassifier;
    }


    /**
     * Creates classifier according to user's choice
     * @param name
     * @return
     */
    private Classifier getClassifier(String name){
        if (name.contains("lmt"))
            return new LMT();
        else if (name.contains("perceptron"))
            return new MultilayerPerceptron();
        else if (name.contains("forest"))
            return new RandomForest();
        else if (name.contains("j48"))
            return new J48();
        else if(name.contains("bnet"))
            return new BayesNet();
        else if (name.contains("svm"))
            return new LibSVM();
        else if (name.contains("smo")) {
            try {
                SMO smo = new SMO();
                //fits logistic models to SVM outputs
                smo.setOptions(new String[]{"-M"});
                return smo;
            } catch (Exception e) {
                return new SMO();
            }
        }
        else if (name.contains("logistic")) {
            try {
                SimpleLogistic logistic = new SimpleLogistic();
                logistic.setOptions(new String[]{"-I 0 -M 500 -H 50 -W 0.0"});
            } catch (Exception e) {
                return new Logistic();
            }
        }
        else if (name.contains("ensemble"))
            return configEnsemble();

        return new NaiveBayes();
    }

    /**
     * Configurates an ensemble classifier
     * @return
     */
    private EnsembleSelection configEnsemble(){
        EnsembleSelection classifier = new EnsembleSelection();

        EnsembleSelectionLibrary lib = new EnsembleSelectionLibrary();
        lib.setWorkingDirectory(new File(ensemble_dir));
        lib.setModelListFile(ensemble_lib);

        try {
            lib.loadLibrary(new File(ensemble_lib), lib);

        }catch (Exception e){
            logger.info("Error loading ensemble library.");
        }
        classifier.setLibrary(lib);
        classifier.setWorkingDirectory(new File(ensemble_dir));
        if(ensemble_task.contains("build"))
            classifier.setAlgorithm(new SelectedTag(EnsembleSelection.ALGORITHM_BUILD_LIBRARY, new Tag[]{(new Tag(EnsembleSelection.ALGORITHM_BUILD_LIBRARY, "Build Library Only"))}));
        else
            classifier.setAlgorithm(new SelectedTag(EnsembleSelection.ALGORITHM_FORWARD, new Tag[]{(new Tag(EnsembleSelection.ALGORITHM_FORWARD, "Forward selection"))}));

        return classifier;
    }

    /**
     * Outputs classifier results.
     *
     * @param eval  Evaluation model built by a classifier
     * @throws Exception
     */
    public void stats(Evaluation eval) throws Exception{

        logger.info("Number of attributes: {}", eval.getHeader().numAttributes());
        logger.info(eval.toSummaryString("\n======== RESULTS ========\n", false));
        logger.info(eval.toClassDetailsString("\n\n======== Detailed accuracy by class ========\n"));
        logger.info(eval.toMatrixString("\n\n======== Confusion Matrix ========\n"));
        IOUtil.getINSTANCE().writeOutput(resultFile+".metrics",
                eval.toClassDetailsString( "\n\n======== Detailed accuracy by class ========\n") +
                        eval.toMatrixString("\n\n======== Confusion Matrix ========\n"));


    }

    /**
     * Configures an output object (PlainText)
     * to handle model output predictions
     * @param data
     * @return
     * @throws Exception
     */
    public PlainText initPredictions(Instances data) throws Exception{
        PlainText prediction = new PlainText();
        StringBuffer forPredictionsPrint = new StringBuffer();
        prediction.setNumDecimals(3);
        prediction.setHeader(data);
        prediction.setBuffer(forPredictionsPrint);
        prediction.setOutputDistribution(true);

        return prediction;
    }

    /**
     * Provides the (dataset) instance list
     * according to classification type
     * (if CV or not)
     * @return
     */
    public Instances getRawInstances(){
        if (CV) return trainData;
        else return testData;
    }

    /**
     * Loads dataset instances
     * from a file
     * @param path
     * @return
     * @throws Exception
     */
    public Instances loadData(String path) throws Exception {
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(path);
        return source.getDataSet();
    }


    /**
     * Removes the ID attribute (index 1)
     * from a given dataset
     *
     * @param data instances
     * @return filtered dataset
     * @throws Exception
     */
    private Instances filteredIDs(Instances data) throws Exception {
        Remove remove = new Remove();
        //setting index to be removed
        remove.setAttributeIndices("1");
        remove.setInvertSelection(false);
        remove.setInputFormat(data);

        Instances dataSubset = Filter.useFilter(data, remove);
        return dataSubset;
    }

    /**
     * Configures a Correlation Selection Feature filter
     * @return
     */
    private AttributeSelection initializeCSF(){
        CfsSubsetEval cfsEval = new CfsSubsetEval();
        cfsEval.setPreComputeCorrelationMatrix(true);
        BestFirst bestSearch = new BestFirst();
        String[] searchOpts = {"-D 1","-N 5"};
        AttributeSelection selector = new AttributeSelection();

        try {
            bestSearch.setOptions(searchOpts);

            selector.setSearch(bestSearch);
            selector.setEvaluator(cfsEval);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return selector;
    }

    /**
     * Filter out attributes from dataset
     * instances according to a given filter
     * @param data
     * @param task
     * @return
     */
    private Instances selectAttributes(Instances data, String task){

        Instances selectedData = null;

        try {
            if(task.contains("train")) {
                selector.SelectAttributes(data);
                selector.toResultsString();

                logger.info("Attributes reduced from {} to {}", data.numAttributes(), selector.selectedAttributes().length);
                logger.info(selector.toResultsString());
            }

            selectedData = selector.reduceDimensionality(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return selectedData;
    }

    /**
     * Executes k-fold cross validation
     * on a given dataset
     * @param data training data provided
     * @param classif type of classifier usedsearch
     * @throws Exception
     */
    public Evaluation crossFold(Instances filteredData, Instances data, Classifier classif) throws Exception{

        Random random = new Random(SEED); //creating seed number generator
        Evaluation evaluateClassifier = new Evaluation(filteredData);

        PlainText prediction = initPredictions(filteredData);

        logger.info("Classifier: {}", classif.getClass());
        logger.info("Method: {}-fold cross-validation", folds);
        logger.info("Evaluation in process...\n\n");
        Integer numFolds = Integer.parseInt(folds);

        try {
            //Predictor should not be trained when cross-validation is executed.
            //because subsequent calls to buildClassifier method will return the same results always.
            evaluateClassifier.crossValidateModel(classif, filteredData, numFolds, random, prediction);
            stats(evaluateClassifier);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return evaluateClassifier;
    }

}
