package predictor;

import common.IOUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * CustomIterator to aggregate a set of rows belonging to a file
 * to be processed together
 * (results per document, as opposed to per row)
 *
 * Created by halmeida on 1/26/17.
 */
 public class CustomIterator implements DataSetIterator {

    String path;
    HashSet<File> dataset;
    int vectorSize;
    int nbClasses;
    int cursor;
    int batchSize;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public CustomIterator(String path, int batchSize, int nbClasses, int vectorSize){
        this.path = path;
        cursor = 0;
        this.batchSize = batchSize;
        this.nbClasses = nbClasses;
        this.vectorSize = vectorSize;

        dataset = IOUtil.getINSTANCE().listAllFiles(path,"");
    }

    /**
     * Iterates over Dataset objects
     * @param num
     * @return
     */
    @Override
    public DataSet next(int num) {
        try{
            return nextDataSet(num);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Iterates over a list of Datasets
     *
     * @param num
     * @return
     * @throws IOException
     */
    private DataSet nextDataSet(int num) throws IOException {

        // list of docs
        cursor = dataset.size();

        int maxLength = 14000; //max number of rows in a file - depends on the dataset

        //Here: we have dataset.size() examples of varying lengths
        //each is a doc
        INDArray features = Nd4j.create(dataset.size(), vectorSize, maxLength);
        INDArray labels = Nd4j.create(dataset.size(), nbClasses, maxLength);


        //starts off all arrays with zeros
        INDArray featuresMask = Nd4j.zeros(dataset.size(), maxLength);
        INDArray labelsMask = Nd4j.zeros(dataset.size(), maxLength);

        int[] temp = new int[2];
        int i = 0;
        for(File file : dataset) {

            //retrieve all vectors in a file (txt2vec file)
            HashMap<double[],String> vectors = IOUtil.getINSTANCE().loadVectorMap(file);

            String label = "";
            temp[0] = i;
            int j = 0;
            for (Map.Entry set : vectors.entrySet() ) {
                //for each vector in a file retrieve data
                double[] vector = (double[]) set.getKey();
                label = (String)set.getValue();

                INDArray ndvector = Nd4j.create(vector);
                features.put(new INDArrayIndex[]{NDArrayIndex.point(i), NDArrayIndex.all(), NDArrayIndex.point(j)}, ndvector);

                temp[1] = j;
                featuresMask.putScalar(temp, 1.0);  //Word is present (not padding) for this example + time step -> 1.0 in features mask
                j++;
            }

            int idx = Integer.parseInt(label); //classValue;
            int lastIdx = Math.min(vectors.size(),maxLength);

            labels.putScalar(new int[]{i,idx,lastIdx-1},1.0);   //Set label: [0,1] for negative, [1,0] for positive
            labelsMask.putScalar(new int[]{i,lastIdx-1},1.0);   //Specify that an output exists at the final time step for this example
            i++;
        }

        return new DataSet(features,labels,featuresMask,labelsMask);
    }


    @Override
    public int totalExamples() {
        return dataset.size();
    }

    @Override
    public int inputColumns() {
        return vectorSize;
    }

    @Override
    public int totalOutcomes() {
        return nbClasses;
    }

    @Override
    public boolean resetSupported() {
        return true;
    }

    @Override
    public boolean asyncSupported() {
        return true;
    }

    @Override
    public void reset() {
        cursor = 0;
    }

    @Override
    public int batch() {
        return 0;
    }

    @Override
    public int cursor() {
        return cursor;
    }

    @Override
    public int numExamples() {
        return totalExamples();
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        return null;
    }

    @Override
    public List<String> getLabels() {
        return Arrays.asList("norisk","risk");
    }

    @Override
    public boolean hasNext() {
        return cursor < numExamples();
    }

    @Override
    public DataSet next() {
        return next(batchSize);
    }
}