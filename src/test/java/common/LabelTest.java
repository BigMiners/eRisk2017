package common;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hayda on 12/05/17.
 */
public class LabelTest {

    @Test
    public void testInformLabels() throws Exception {

        String project = "clpsych";
        DocUnit.Label label = DocUnit.Label.UNDECIDED;
        assertEquals(label.informLabels(project), "green, amber, red, crisis");

        project = "erisk";
        assertEquals(label.informLabels(project), "risk, norisk");
    }
}