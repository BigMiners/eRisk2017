package vectorizer;

/**
 * Created by halmeida on 2/2/17.
 */
import common.DocUnit;
import common.IOUtil;
import org.apache.commons.lang.StringUtils;
import org.deeplearning4j.bagofwords.vectorizer.BagOfWordsVectorizer;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Class to generate word vectors from
 * plain text documents or strings.
 * It can:
 * - use Google word vectors (GoogleNews-vectors-negative300.bin), or
 * - train paragraph2vec models
 *
 * Created by halmeida on 12/7/16.
 */
public class ModelVectorizer extends Vectorizer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    TokenizerFactory tokenizerFactory;
    int numEpochs;
    String chunk, task, project;

    public ModelVectorizer() {
        super();

        tokenizerFactory = new DefaultTokenizerFactory();
        tokenizerFactory.setTokenPreProcessor(new CommonPreprocessor());
        project = props.getProperty("PROJECT");

        chunk = corpus_dir.substring(corpus_dir.length()-1);

        task = props.getProperty("TASK");

        corpus_dir += txt2vecFolder;
        if(!corpus_dir.contains("chunk")) corpus_dir += "/" + task;

        numEpochs = 3;
    }

    /**
     * Generates word2vecs for plain txt files
     * Goes over a directory of plain txt files,
     * generates a Doc2Vec (compressed) file for either:
     * each plain txt separately (TXT2VEC_LEVEL=doc)
     * each label (TXT2VEC_LEVEL=label)
     */
    public void extractTxt2Vec(List<DocUnit> docs) {

        logger.info("Starting to write doc2vec for {} docs.", docs.size());
        int count = 0;

        if (useGoogleWord2Vec)
            try {
                // use the word2vec from Google News model
                logger.info("Loading GoogleWord2Vec model...");
                googleWord2Vec = WordVectorSerializer.loadGoogleModel(new File(googleVecPath), true, false);
                logger.info("Done.");
            } catch (IOException e) {
                logger.error("Google word vectors not found: {}", googleVecPath);
            }

        for (DocUnit doc: docs) {
            String saveFile = "";
            if(task.contains("train"))  saveFile = corpus_dir + "/" + doc.getId() + "_"+chunk+"." + doc.getLabel().name();
            else saveFile = corpus_dir + "/" + doc.getId();

            if (useGoogleWord2Vec)
                googleTxt2Vec(doc, saveFile, true);
            else
                computeTxt2Vec(doc, saveFile, true);

            count++;
            if (count % 50 == 0) logger.info("Processed {} out of {} doc2vec.", count, docs.size());
        }
        logger.info("Done writing doc2vec for {} documents.", docs.size());
    }

    /**
     * Generates googleWord2Vecs for strings / plain txt files.
     * Looks for document words in the google set of vectors,
     * populates the document with google vectors.
     * Outputs files according to TXT2VEC_LEVEL
     * (word2Vec for docs, or labels)
     *
     * @param doc
     * @param savePath
     * @param export
     */
    public void googleTxt2Vec(DocUnit doc, String savePath, boolean export) {

        String label = String.valueOf(doc.getLabel().value());
        StringBuffer sb = new StringBuffer();

        // get all tokens of doc
        String content = doc.getContent();
        List<String> contentStrings = tokenizerFactory.create(content).getTokens();

        for (int i = 0; i < contentStrings.size(); i++) {
            //looks for document word in the google model
            if (googleWord2Vec.hasWord(contentStrings.get(i))) {
                //if word is found among google model,
                // retrieve the word matrix, and prepare its syn1 values.
                INDArray wordVector = googleWord2Vec.getWordVectorMatrix(contentStrings.get(i));
                sb.append(getSyn1(wordVector, label));
            }
        }
        exportDoc2Vec(savePath, sb.toString());
    }

    /**
     * Generates a word2vec with regards to the TXT2VEC_LEVEL.
     * For TXT2VEC_LEVEL=doc,vectors are generated WRT the document
     * (one output per doc)
     * For TXT2VEC_LEVEL=label, vectors are generated WRT the label
     * (one output per label)
     * Uses DL4J Paragraph2Vec algorithm (based on DBOW and Skipgram)
     *
     * @param doc
     * @param export
     */
    public void computeTxt2Vec(DocUnit doc, String savePath, boolean export) {

        ParagraphVectors vec;
        String label = String.valueOf(doc.getLabel().value());
        File file = null;

        String projectName = project.contains("-") ?  project.substring(0,project.indexOf("-")) : project;

        try {
            Path path = Files.createTempFile(projectName+"-vector", ".txt");
            file = path.toFile();
            Files.write(path, doc.getContent().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // writing sample data
        vec = getDoc2VecModel(file);
        vec.fit();
        file.delete();

        INDArray syn1 = ((InMemoryLookupTable<VocabWord>) vec.getLookupTable()).getSyn1();
        String result = getSyn1(syn1, label);
        exportDoc2Vec(savePath, result);

    }

    /**
     * Trains paragraph2vec for documents
     * iterating over each provided document
     * @param file
     * @return
     */
    private ParagraphVectors getDoc2VecModel(File file) {
        SentenceIterator iter = new LineSentenceIterator(file);
        ParagraphVectors vec = new ParagraphVectors.Builder()
                .learningRate(0.025)
                .layerSize(20)
                .minLearningRate(0.001)
                .batchSize(500)
                .stopWords(new ArrayList<String>(wordFilter.loadStopWords()))
                .epochs(numEpochs)
                .iterate(iter)
                .trainWordVectors(true)
                .tokenizerFactory(tokenizerFactory)
                .build();
        return vec;
    }

    /**
     * Trains bow2vec for documents
     * iterating over each provided document
     * @param file
     * @return
     */
    private BagOfWordsVectorizer getBoW2VecModel(File file) {
        SentenceIterator iter = new LineSentenceIterator(file);
        BagOfWordsVectorizer vec = new BagOfWordsVectorizer.Builder()
                .setMinWordFrequency(2)
                .setIterator(iter)
                .setStopWords(new ArrayList<String>(wordFilter.loadStopWords()))
                .setTokenizerFactory(tokenizerFactory)
                .build();
        return vec;
    }

    /**
     * Retrieves INDArray with result vector values
     * for a document, to be used for deeplearn
     *
     * @param array complete paragraph2vec array
     * @param label document label (must start from 0)
     * @return
     */
    private String getSyn1(INDArray array, String label) {
        StringBuffer sb = new StringBuffer();
        StringBuffer line = new StringBuffer();

        for (int i = 0; i < array.rows(); i++) {
            INDArray row = array.getRow(i);
            line.append(label + ",");

            for (int j = 0; j < row.length(); j++) {
                if (j < row.length() - 1)
                    line.append(row.getDouble(j) + ",");
                else
                    line.append(row.getDouble(j) + "\n");
            }
            //clean away lines 0.0
            if (StringUtils.countMatches(line.toString(), "0.0,") < 2)
                sb.append(line.toString());

            line.setLength(0);
        }
        return sb.toString();
    }

    /**
     * Generic method to output text2vec files
     * @param resultPath
     * @param content
     */
    public void exportDoc2Vec(String resultPath, String content) {
        resultPath += ".csv";
        IOUtil.getINSTANCE().writeOutput(resultPath, content);
    }

}