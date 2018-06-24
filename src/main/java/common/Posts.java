package common;

import java.util.ArrayList;

/**
 * A group of posts contained in a single file.
 */
public class Posts {

    private ArrayList<Post> posts;


    void setPosts(ArrayList<Post> posts) {
        this.posts = posts;
    }

    public void addPost(Post post) {
        this.posts.add(post);
    }

    public Posts(){
        posts = new ArrayList<>();
    }

    public ArrayList<Post> getPosts() {
        return posts;
    }

    public int size() { return posts.size();}
}
