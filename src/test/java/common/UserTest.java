package common;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * by abriand on 26/01/2017.
 */
public class UserTest {
    @Test
    public void addPosts() throws Exception {
        Posts posts = user.getPosts();
        ArrayList<Post> postsList = posts.getPosts();
        assertEquals(postsList.size(), 0);
        Posts posts2 = new Posts();
        posts2.addPost(new Post(""));
        user.addPosts(posts2);
        assertEquals(posts.getPosts().size(), 1);

    }

    private User user;

    @Before
    public void setUp() {
        user = new User("1234");
    }

    @Test
    public void getPosts() throws Exception {
        Posts posts = user.getPosts();
        ArrayList<Post> postsList = posts.getPosts();
        assertEquals(postsList.size(), 0);
    }

    @Test
    public void getId() throws Exception {
        assertEquals(user.getId(), "1234");
    }

    @Test
    public void setId() throws Exception {
        assertEquals(user.getId(), "1234");
        user.setCompleteId("9876");
        user.setId();
        assertEquals(user.getId(), "9876");
    }

    @Test
    public void getLabel(){
        assertEquals(user.getLabel(), User.Label.UNDECIDED);
    }

    @Test
    public void setLabel() throws Exception {
        assertEquals(user.getLabel(), User.Label.UNDECIDED);
        user.setLabel(User.Label.RISK);
        assertEquals(user.getLabel(), User.Label.RISK);
    }


}