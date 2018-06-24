package informationretrieval;

import common.Post;
import common.User;
import me.tongfei.progressbar.ProgressBar;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.HashSet;

public class MoreLikeThisWithStopWordsRemoval implements Scenario {
    private HashSet<User> Users = new HashSet<>();
    private String solrUrl;
    private String solrCore;
    private HashMap<String, String> resultsPerUsers;

    MoreLikeThisWithStopWordsRemoval(HashSet<User> users, String solrUrl, String solrCore) {
        this.setUsers(users);
        this.solrUrl = solrUrl;
        this.solrCore = solrCore;
        this.resultsPerUsers = new HashMap<>();
        this.run();
    }

    @Override
    public void run() {
        processByUsers(this.getUsers());
    }

    @Override
    public HashSet<User> getUsers() {
        return this.Users;
    }

    @Override
    public void setUsers(HashSet<User> users) {
        this.Users = users;
    }

    @Override
    public HashMap<String, String> processByUsers(HashSet<User> Users) {
        ProgressBar progressBar = new ProgressBar("Processing Users for eriskRun2 (mlt2) core", Users.size());
        progressBar.start();
        for (User user : Users) {
            progressBar.step();
            String _production = this.preProcessTotalProduction(user.getContent());
            this.resultsPerUsers.put(user.getId(), new HandleSolrQueries(_production, this.solrUrl, this.solrCore).getCorrespondingLabels());
        }
        progressBar.stop();
        return this.resultsPerUsers;
    }

    @Override
    public void processByPosts() {
        throw new NotImplementedException();
    }

    @Override
    public HashMap<String, String> getResultsPerUsers() {
        return this.resultsPerUsers;
    }

    @Override
    public void setResultsPerUsers(HashMap<String, String> resultsPerUsers) {
        this.resultsPerUsers = resultsPerUsers;
    }

    @Override
    public Post preProcessPost(Post post) {
        return post;
    }

    @Override
    public String preProcessTotalProduction(String totalProduction) {
        return totalProduction;
    }
}
