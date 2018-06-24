package predictor;

import common.Configs;
import common.DocUnit;
import common.IOUtil;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.AsyncDataSetIterator;
import org.deeplearning4j.eval.ConfusionMatrix;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.eval.meta.Prediction;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Class to train and test classification
 * models with deep learners.
 *
 * Created by halmeida on 1/26/17.
 */
public class DeepPredictor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static int seed = 123;
    public static int numEpochs;
    public static int batchSize;
    public static int numInputs;
    public static int numOutputs; // The number of possible outcomes
    public static int scoreListener = 100; //Print score every 1000 parameter updates

    public static double learningRate = 0.1;
    public static int lstmLayerSize;                    //Number of units in each GravesLSTM layer
    public static int tbpttLength;
    public static int iterations;

    public static int numHiddenNodes;

    //to be changed according to data
    public static int labelIndex = 0;
    public static int numPossibleLabels;

    Properties props;
    String work_dir, corpus_dir, txt2vecLevel, txt2vecDir, trainPath, testPath, resultOutputPath, project;
    DataSetIterator trainIterator, testIterator;
    boolean loadNNGraph, writeFile;
    DocUnit.Label label;
    int outcomes;
    HashSet<File> testSet;
    HashMap<String, DataSetIterator> testIteratorAll;

    MultiLayerNetwork model;

    public DeepPredictor() {
        props = Configs.getInstance().getProps();
        corpus_dir = props.getProperty("CORPUS_DIRECTORY");
        work_dir = props.getProperty("WORK_DIRECTORY");

        loadNNGraph = Boolean.valueOf(props.getProperty("loadNNGraph"));
        writeFile = Boolean.valueOf(props.getProperty("writeFile"));

        txt2vecLevel = props.getProperty("TXT2VEC_LEVEL");
        txt2vecDir = props.getProperty("TXT2VEC_DIR");

        resultOutputPath = work_dir + props.getProperty("RESOURCES_DIR") + props.getProperty("RESULTS_DIR") + "/";

        trainPath = corpus_dir + txt2vecDir + "/train";
        testPath = corpus_dir + txt2vecDir + "/test";

        logger.info("Training data: {} \n Test data: {}", trainPath, testPath);

        testIteratorAll = new HashMap<>();
        testSet = IOUtil.getINSTANCE().listAllFiles(testPath, "");

        project = props.getProperty("PROJECT");
        label = DocUnit.Label.UNDECIDED;
        outcomes = label.informLabels(project).split(",").length;

    }

    /**
     * Load neural network parameters,
     * according to network type.
     *
     * @param networkType
     */
    public void loadParameters(String networkType) {

        numEpochs = Integer.parseInt(props.getProperty("numEpochs"));
        batchSize = Integer.parseInt(props.getProperty("batchSize"));
        iterations = Integer.parseInt(props.getProperty("iterations"));

        numPossibleLabels= outcomes; numOutputs = outcomes;

        //mlp
        numHiddenNodes = Integer.parseInt(props.getProperty("numHiddenNodes"));

        //rnn
        lstmLayerSize = Integer.parseInt(props.getProperty("lstmLayerSize"));
        tbpttLength = Integer.parseInt(props.getProperty("tbpttLength"));
    }

    /**
     * Load training and test data
     * to their respective iterators
     */
    private void loadData() {
        logger.info("Loading training and test data...");

        if(trainPath.contains("google"))   numInputs = 300;
        else numInputs = 20;

        trainIterator = new AsyncDataSetIterator(new CustomIterator(trainPath, batchSize, numPossibleLabels, numInputs));
       // testIterator = new AsyncDataSetIterator(new CustomIterator(testPath, batchSize, numPossibleLabels, numInputs));

        for (File file : testSet) {
            testIteratorAll.put(file.getName(), new AsyncDataSetIterator(new CustomIterator(file.getAbsolutePath(), batchSize, numPossibleLabels, numInputs)));
        }
    }

    // loads network configuration
    // depending on the type
    private MultiLayerConfiguration prepareNetwork(String network) {

        logger.info("Preparing {} network configuration...", network);
        MultiLayerConfiguration.Builder nnBuilder = new MultiLayerConfiguration.Builder();

        if (network.contains("mlp")) nnBuilder = buildMLP();
        else if (network.contains("rnn")) nnBuilder = buildRNN();
        else if (network.contains("convolution")) nnBuilder = buildCNN();

        MultiLayerConfiguration nnConfig = nnBuilder.build();

        return nnConfig;

    }

    // builds model using network
    // configuration and training data
    public void trainModel(String networkType) {

        loadParameters(networkType);

        loadData();

        MultiLayerConfiguration conf = prepareNetwork(networkType); // load configuration
        model = new MultiLayerNetwork(conf);
        model.setListeners(new ScoreIterationListener(scoreListener));

        if(loadNNGraph) {
            //Initialize the user interface backend
            UIServer uiServer = UIServer.getInstance();
            //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
            StatsStorage statsStorage = new InMemoryStatsStorage();        //Alternative: new FileStatsStorage(File), for saving and loading later
            //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
            uiServer.attach(statsStorage);
            //Then add the StatsListener to collect this information from the network, as it trains
            model.setListeners(new StatsListener(statsStorage));
        }

        model.init();

        logger.info("Building {} model...", networkType);

        for (int n = 0; n < numEpochs; n++) {
            model.fit(trainIterator);
            logger.info("Epoch: {}", n);
        }

        logger.info("Done all epochs.");
        getResultOutputFileName(networkType);
    }


    public void evaluateModel() {

        logger.info("Evaluating model...");
        //per doc eval:
        StringBuilder sb = new StringBuilder();
        logger.info("Preparing predictions per ID...");

        //eval object to output the general evaluation
        // precision, recall, f-measure for all docs
        Evaluation evalLoop = new Evaluation(numOutputs);

        //each test document is a testIterator.
        //by running a testIterator at a time,
        // we can output predictions separately:
        // [document - predicted class]
        for (Map.Entry entry : testIteratorAll.entrySet()) {

            //eval object to retrieve evaluation for
            // a single test document
            Evaluation evalLocal = new Evaluation(numOutputs);
            String fileName="";


            fileName = (String) entry.getKey();

            if(fileName.contains("_"))
                fileName = fileName.substring(0, fileName.lastIndexOf("_"));
            else
                fileName = fileName.substring(0, fileName.indexOf("."));

            //get current iterator, and request its results
            // according to the features and labels used
            DataSetIterator iter = (DataSetIterator) entry.getValue();
            while (iter.hasNext()) {
                DataSet set = iter.next();
                INDArray features = set.getFeatureMatrix();
                INDArray inmask = set.getFeaturesMaskArray();
                INDArray outmask = set.getLabelsMaskArray();
                INDArray labels = set.getLabels();
                INDArray prediction = model.output(features, false, inmask, outmask);

                String conf = "";
                if(writeFile) {
                    evalLocal.evalTimeSeries(labels, prediction, outmask);
                    conf = getConfidenceValue(prediction.data().toString());
                }
                evalLoop.evalTimeSeries(labels, prediction, outmask);
                String docClass = "";

                docClass = processConfusionMatrix(evalLocal.getConfusionMatrix().toCSV());

                if(writeFile) sb.append(fileName + "\t\t" + docClass + "\t\t" + conf + "\n");
            }
            iter.reset();
        }
        logger.info("Done.");

        logger.info("========= RESULTS ========= {}\n", evalLoop.stats());
        logger.info(evalLoop.confusionToString());

        logger.info("Model result : {}", resultOutputPath);
        //logger.info("Results: {}",sb.toString());
        if(writeFile) {
            IOUtil.getINSTANCE().writeOutput(resultOutputPath, evalLoop.stats() + "\n" + evalLoop.confusionToString());
            IOUtil.getINSTANCE().writeOutput(resultOutputPath + "_IDs", sb.toString());


        }
        // testIterator.reset();
    }

    /**
     * Retrieve the prediction confidence value
     * @param predictionData string containing prediction
     * @return confidence value
     */
    private String getConfidenceValue(String predictionData){
        predictionData = predictionData.replaceAll("0.0,","");
        predictionData = predictionData.replaceAll(", 0.0","");
        predictionData = predictionData.replace("[","");
        predictionData = predictionData.replace("]","");
        predictionData = predictionData.replace(",","\t");

        return predictionData;
    }

    /**
     * Retrieve the predicted class from
     * the evaluation confusion matrix
     * @param matrix
     * @return
     */
    private String processConfusionMatrix(String matrix) {
        String predictedClass = "";

        String metrics = matrix.substring(matrix.indexOf("Actual Class"));
        metrics = metrics.substring(0, metrics.indexOf("Total"));
        metrics = metrics.replace("Actual Class", "");

        String[] lines = metrics.split("\n");
        int docClass = 0;
        for (int i = 0; i < lines.length; i++) {

            String[] oneLine = lines[i].split(",");
            for (int j = 2; j < oneLine.length - 1; j++) {
                if (oneLine[j].contains("1")) {
                    docClass = j - 2;
                    break;
                }
            }
        }
        if (docClass == 1) predictedClass = "1";  //risk
        if (docClass == 0) predictedClass = "2"; //norisk

        return predictedClass;
    }

    /**
     * MultiLayer Perceptron configuration
     * as from DL4J example
     * @return
     */
    private MultiLayerConfiguration.Builder buildMLP() {
        MultiLayerConfiguration.Builder nnBuilder =
                new NeuralNetConfiguration.Builder()
                        .seed(seed)
                        .iterations(iterations)
                        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                        .learningRate(learningRate)
                        .updater(Updater.NESTEROVS).momentum(0.9).weightInit(WeightInit.XAVIER)
                        .list()
                        .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                                .weightInit(WeightInit.XAVIER)
                                .activation("relu")
                                .build())
                        .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                                .weightInit(WeightInit.XAVIER)
                                .activation("softmax").weightInit(WeightInit.XAVIER)
                                .nIn(numHiddenNodes).nOut(numOutputs).build())
                        .pretrain(false).backprop(true);

        return nnBuilder;
    }

    /**
     * Recurrent Neural Network configuration
     * as from DL4J example
     * @return
     */
    private MultiLayerConfiguration.Builder buildRNN() {
        MultiLayerConfiguration.Builder nnBuilder =
                new NeuralNetConfiguration.Builder()
                        .seed(seed)
                        .iterations(iterations)
                        .regularization(true)
                        .l2(0.001)
                        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                        //.learningRate(0.00008)
                        .rmsDecay(0.95)
                        .weightInit(WeightInit.XAVIER)
                        .updater(Updater.RMSPROP)
                        .list()
                        .layer(0, new GravesLSTM.Builder().nIn(numInputs).nOut(lstmLayerSize).learningRate(0.00008)
                                .activation("softsign").build())
                        .layer(1, new GravesLSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize).learningRate(0.00008)
                                .activation("softsign").build())
                        .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT).activation("softmax").learningRate(0.0001)        //MCXENT + softmax for classification
                                .nIn(lstmLayerSize).nOut(numOutputs).build())
                        .backpropType(BackpropType.TruncatedBPTT).tBPTTForwardLength(tbpttLength).tBPTTBackwardLength(tbpttLength)
                        .pretrain(false)
                        .backprop(true);

        return nnBuilder;
    }

    /**
     * Convolutional Neural Network configuration
     * (requires fixed size inputs)
     * as from DL4J example
     * @return
     */
    private MultiLayerConfiguration.Builder buildCNN() {
        // as is from https://github.com/deeplearning4j/dl4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/convolution/LenetMnistExample.java

        int numIterations = 20;
        int nChannels = 3; // Number of input channels
        int height = 500, width = 20;

        MultiLayerConfiguration.Builder nnBuilder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations) // Training iterations as above
                .regularization(true).l2(0.0005)
                /*
                    Uncomment the following for learning decay and bias
                 */
                .learningRate(.01)//.biasLearningRate(0.02)
                //.learningRateDecayPolicy(LearningRatePolicy.Inverse).lrPolicyDecayRate(0.001).lrPolicyPower(0.75)
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                        //nIn and nOut specify depth. nIn here is the nChannels and nOut is the number of filters to be applied
                        .nIn(nChannels)
                        .stride(1, 1)
                        .nOut(20)
                        .activation("identity")
                        .build())
                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(2, new ConvolutionLayer.Builder(5, 5)
                        //Note that nIn need not be specified in later layers
                        .stride(1, 1)
                        .nOut(50)
                        .activation("identity")
                        .build())
                .layer(3, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(4, new DenseLayer.Builder().activation("relu")
                        .nOut(500).build())
                .layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(numOutputs)
                        .activation("softmax")
                        .build())
//                    .setInputType(InputType.convolutionalFlat(height, width, nChannels))
                .backprop(true).pretrain(false);

        return nnBuilder;
    }

    public void getResultOutputFileName(String networkType) {


        String timeStamp = new SimpleDateFormat("yyyyMMdd_hh:mm").format(new Date());
        resultOutputPath +=
                          networkType
                        + "_epochs" + numEpochs
                        + "_batch" + batchSize
                        + "_iter" + iterations;

        if (networkType.contains("rnn")) {
            resultOutputPath += "_lstmLayer" + lstmLayerSize
                    + "_tbptt" + tbpttLength;
        }
        if (networkType.contains("mlp")) {
            resultOutputPath += "_hiddenNodes" + numHiddenNodes;
        }
        resultOutputPath += "_" + timeStamp + ".txt";
    }

}
