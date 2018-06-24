package common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A group of subjects
 */
public class Group {

    //private Map<String, User> documents;
    private Map<String, DocUnit> documents;

    public Group() {
        documents = new HashMap<>();
    }

    /**
     * Returns a user matching an ID
     *
     * @param id the id of the user
     * @return the user with the given id or a new user if no user is found.
     */
    public User getUser(String id) {
  //  public DocUnit getUser(String id) {
        documents.computeIfAbsent(id, k -> new User(id));
        return (User) documents.get(id);
    }

    /**
     * Returns a post matching an post ID
     * If not, adds parsed post to list
     * @param post
     * @return
     */
    public Post getPost(Post post){
        documents.computeIfAbsent(post.getId(), k -> post);
        return (Post) documents.get(post.getId());
    }

    //public List<User> getDocsAsList(){
    public List<DocUnit> getDocsAsList(){
        //List<User> list = new ArrayList<>();
        List<DocUnit> list = new ArrayList<>();
        list.addAll(documents.values());
        return list;
    }


}
