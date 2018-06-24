package model;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class ArffVectorTest {
    @Test
    public void extractNgramSize() throws Exception {
        ArffVector arffVector = new ArffVector();
        String fileName = "./resources/features/features.ngrams.size.3.stemmed3";
        int size = arffVector.extractNgramSize(fileName);

        assertEquals(3, size);
    }

}