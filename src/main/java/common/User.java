package common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Class represents a single subject. Does not contain posts directly, these are held by posts.
 */
public class User extends DocUnit{

    Posts posts;
    int writings;
    StringBuffer totalProduction;
    Date minDate;
    Date maxDate;
    int postFrequency;


    SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.ENGLISH);

    public User(String id) {
        super(id);

        this.posts = new Posts();
        totalProduction = new StringBuffer();
        setId();
        try {
            maxDate = sf.parse(sf.format(new Date(0)));
            minDate = sf.parse(sf.format(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        postFrequency = 0;
        writings = 0;
    }

    public User(String id, Label label){
        super(id, label);

        this.posts = new Posts();
        totalProduction = new StringBuffer();
        setId();
        try {
            maxDate = sf.parse(sf.format(new Date()));
            minDate = sf.parse(sf.format(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        postFrequency = 0;
        writings = 0;
    }

    public Posts getPosts() {
        return this.posts;
    }

    public String getId(){
        return id;
    }

    public void addPosts(Posts posts) {
        for (int i = 0; i < posts.getPosts().size(); i++) {
            Post onePost = posts.getPosts().get(i);
            Date postDate = onePost.getPostDate();

            if(postDate != null) {
                if (maxDate.before(postDate))
                    maxDate = postDate;
                if (minDate.after(postDate))
                    minDate = postDate;
            }
            this.posts.addPost(onePost);
            if(i < posts.getPosts().size()-1)
                addTotalProduction(onePost.getContent() + " ");
            else addTotalProduction(onePost.getContent());
            addOnlyText(onePost.getContent());
        }
        setPostFrequency(posts.getPosts().size());
    }

    public int getPostFrequency(){
        return this.postFrequency;
    }

    public void setPostFrequency(int numPosts){
        int temp = (int)((maxDate.getTime() - minDate.getTime()) / 86400000 );
        if(temp > 0){
            int val = numPosts / temp;
            if (val > this.postFrequency) this.postFrequency = val;
        }
    }

    @Override
    public String getContent(){
        return totalProduction.toString();
    }

   public void addTotalProduction(String production){
        totalProduction.append(production);
    }


    public void addOnlyText(String text) {
        if(!(text==null) && !(text.trim().isEmpty()))
            content += text + " ";
    }

    public int getWritings() {
        return writings;
    }

    public void setWritings(int writings) {
        this.writings = writings;
    }

    public void setPosts(Posts posts) {
        this.posts = posts;
    }

    public Date getMinDate(){
        return minDate;
    }

    public Date getMaxDate(){
        return maxDate;
    }

@Override
    public String toString(){

        return  "User id: " + getId() + "\n" +
                "Label: " + getLabel().value() + "\n" +
                "Writings: " + getWritings() + "\n" +
                "Posts: " + getPosts().size() + "\n" +
                "Post freq.: " + getPostFrequency()+ "\n\n";
    }
}
