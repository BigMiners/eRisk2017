package common;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * by abriand on 26/01/2017.
 */
public class GroupTest {
    @Test
    public void getUser() throws Exception {
        Group group = new Group();
        assertEquals(group.getUser("1").getClass(), User.class);
    }

}