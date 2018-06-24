package common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class represents a subject's post.
 * It has the following attributes :
 */
public class Post extends DocUnit{

    String title, text;
    String date;
    Date postDate;
    String info;
    String chunk;
    String author;
    ArrayList<String> drugs;
    ArrayList<String> meds;
    ArrayList<String> diseases;

    public Post(String id) {
        super(id);
        this.text = "";
        this.title = "";
        this.date = "";
        this.info = "";
        this.chunk = "";
        this.author = "";
    }



    public String getAuthor(){ return author; }

    public void setAuthor(String author) { this.author = author; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }


    public void setText(String text){
        this.text = text;
    }
    public String getText(){
        return text;
    }

    @Override
    public String getContent() {
        return title + " " + text;
    }

    public String getChunk() {
        return chunk;
    }

    public void setChunk(String chunk) {
        this.chunk = chunk;
    }

    public Date getPostDate() {
        return postDate;
    }

    public void setPostDate(String date){
        SimpleDateFormat sdf;
        try {
            if(date.contains("T")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm", Locale.ENGLISH);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                this.postDate = sdf.parse(date);
            }
            else
                sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.ENGLISH);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                this.postDate = sdf.parse(date);
        }catch (ParseException e) {
            e.printStackTrace();
        }
        setDate(postDate.toString());
    }

    public ArrayList<String> getDrugs() {
        return drugs;
    }

    public void setDrugs(ArrayList<String> drugs) {
        this.drugs = drugs;
    }

    public ArrayList<String> getMeds() {
        return meds;
    }

    public void setMeds(ArrayList<String> meds) {
        this.meds = meds;
    }

    public ArrayList<String> getDiseases() {
        return diseases;
    }

    public void setDiseases(ArrayList<String> diseases) {
        this.diseases = diseases;
    }

    @Override
    public String toString(){

        return  "Post id: " + getId() + "\n" +
                "Label: " + getLabel().value() + "\n" +
                "Author: " + getAuthor() + "\n" +
                "Title: " + getTitle() + "\n" +
                "Date: " + getDate()+ "\n\n";
    }

}