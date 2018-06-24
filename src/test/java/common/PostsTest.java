package common;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * by abriand on 26/01/2017.
 */
public class PostsTest {
    @Test
    public void setUpChunk() throws Exception {
        ArrayList<Post> posts = new ArrayList<>();
        posts.add(new Post(""));
        Posts chunk = new Posts();
        chunk.setPosts(posts);
        assertEquals(posts.size(), 1);
    }

    @Test
    public void testAddPostToChunk() throws  Exception{
        Posts posts = new Posts();
        posts.addPost(new Post(""));
        assertEquals(posts.getPosts().size(), 1);
    }

}