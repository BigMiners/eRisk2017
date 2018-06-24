package common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class PostTest {
    private String completeId = "123";
    private String title = "The post title";
    private String date = "2012-10-10 11:00:55";
    private String info = "Reddit post";
    private String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ...";
    private String chunk = "10";

    @Test
    public void testSetupPost() {
        Post post = new Post(completeId);

        post.setTitle(title);
        post.setDate(date);
        post.setInfo(info);
        post.setText(text);
        post.setChunk(chunk);


        assertEquals(post.getId(), completeId);
        assertEquals(post.getTitle(), title);
        assertEquals(post.getDate(), date);
        assertEquals(post.getInfo(), info);
        assertEquals(post.getText(), text);
        assertEquals(post.getChunk(), chunk);
    }

    @Test
    public void testCreateEmptyPost() {
        Post post = new Post("id");
        assertEquals(post.getText(), "");
        assertEquals(post.getTitle(), "");
        assertEquals(post.getDate(), "");
        assertEquals(post.getInfo(), "");
        assertEquals(post.getChunk(), "");
    }

    @Test
    public void testGetTotalText(){
        Post post = new Post(completeId);
        post.setTitle(title);
        post.setText(text);

        assertEquals(post.getContent(),title + " " + text);
    }

    //    @Test
//    public void testCreateCompletePost() {
//        Post post = new Post(title, date, info, text, chunk);
//
//        assertEquals(post.getTitle(), title);
//        assertEquals(post.getDate(), date);
//        assertEquals(post.getInfo(), info);
//        assertEquals(post.getContent(), text);
//        assertEquals(post.getChunk(), chunk);
//    }
}