package informationretrieval;

import common.Post;
import common.User;

import java.util.HashMap;
import java.util.HashSet;

public interface Scenario {
    HashSet<User> users = new HashSet<User>();
    HashMap<String, String> resultsPerUsers = new HashMap<>();

    void run();

    HashSet<User> getUsers();

    void setUsers(HashSet<User> users);

    HashMap<String, String> processByUsers(HashSet<User> Users);

    void processByPosts();

    HashMap<String, String> getResultsPerUsers();

    void setResultsPerUsers(HashMap<String, String> resultsPerUsers);

    Post preProcessPost(Post post);

    String preProcessTotalProduction(String totalProduction);
}
