package sentiment;

import com.vader.SentimentAnalyzer;
import common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by halmeida on 3/10/17.
 *
 * Class to extract sentiments from user content, and use the
 * sentiment (negative, positive, neutral) and valence score
 * (provided by VADER -
 * https://github.com/cjhutto/vaderSentiment
 * https://github.com/apanimesh061/VaderSentimentJava )
 * to getPredictions user label.
 */
public class SentimentPredictor {

    Properties props;
    SentimentAnalyzer analyzer;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    String work_dir, resources_dir, results_dir, location, fileName, timeStamp, task, mode, project;

    boolean postGranularity, postMax, chunkWeight, userProgression;  //postAvg;
    double scorePerc;
    HashMap<String,String> results;

    public SentimentPredictor(){
        props = Configs.getInstance().getProps();
        project = props.getProperty("PROJECT");
        work_dir = props.getProperty("WORK_DIRECTORY");
        resources_dir = props.getProperty("RESOURCES_DIR");
        results_dir = props.getProperty("RESULTS_DIR");
        task = props.getProperty("TASK");
        mode = props.getProperty("MODE");

        timeStamp = new SimpleDateFormat("yyyyMMdd_hh:mm").format(new Date());

        location = work_dir + resources_dir + results_dir;
        results = new HashMap<>();

        this.postGranularity = Boolean.valueOf(props.getProperty("POST_GRAN"));
        this.postMax = Boolean.valueOf(props.getProperty("POST_MAX"));
        this.chunkWeight = Boolean.valueOf(props.getProperty("CHUNK_WEIGHT"));
        this.userProgression = Boolean.valueOf(props.getProperty("USER_PROG"));
        this.scorePerc = Double.parseDouble(props.getProperty("SCORE_PERC"));

        setFileName();
    }

    public void setFileName(){
        fileName = project + "_vader_";
        fileName += mode;
        if(mode.contains("prediction")) {
            if (postGranularity) fileName += "_perPost";
            if (postMax) fileName += "_postMax";
            if (chunkWeight) fileName += "_chunkW";
            if (scorePerc > 0) fileName += "_scPerc" + String.valueOf(scorePerc).replace(".", "");
        }
        fileName += "_" + task + "_" + timeStamp;
    }

    /**
     * ================================================================
     * From VADER https://github.com/cjhutto/vaderSentiment:
     * The compound score is computed by summing the valence scores of each word in the lexicon,
     * adjusted according to the rules, and then normalized to be between -1 (most extreme negative)
     * and +1 (most extreme positive). This is the most useful metric if you want a single
     * unidimensional measure of sentiment for a given sentence. Calling it a 'normalized,
     * weighted composite score' is accurate.
     * The pos, neu, and neg scores are ratios for proportions of text that fall in each category
     * (so these should all add up to be 1... or close to it with float operation).
     * These are the most useful metrics if you want multidimensional measures of sentiment for a given sentence.
     * ================================================================
     * Predicts user label according to
     * sentiment polarity provided by VADER.
     * Makes use of user content or post content,
     * user progression over time,
     * chunk weights,
     * and minimal negativity (score percentage)
     * for labeling a user.
     *
     * @param docs
     */
    public void predict(List<DocUnit> docs){
       if(mode.contains("prediction")) {
           getPredictions(docs);
       }
        else getScores(docs);
    }

    /**
     * Outputs the strongest sentiment for
     * a given content.
     * @param docs
     */
    public void getScores(List<DocUnit> docs){

        StringBuilder sbEval = new StringBuilder();

        for(int i = 0; i < docs.size(); i ++){
            DocUnit doc = docs.get(i);
            String content = doc.getContent();

            try {
                analyzer = new SentimentAnalyzer(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
            analyzer.analyse();

            float score = 0, compound = 0;
            String strongestSentiment = "";

            //get the sentiments identified for this content
            Iterator<String> iterator = analyzer.getPolarity().keySet().iterator();
            compound = analyzer.getPolarity().get("compound");

            //iterate over sentiments to find the strongest one
            while(iterator.hasNext()){
                String key = iterator.next();
                if(!key.contains("compound") && !key.contains("neutral")) {
                    float current = analyzer.getPolarity().get(key);
                    if (current > score) {
                        strongestSentiment = key;
                        score = current;
                    }
                    if(strongestSentiment.isEmpty())
                        strongestSentiment = "allNeutral";
                }
            }

            sbEval.append(doc.getId() + "\t" + strongestSentiment + "\t" + score + "\t" + compound +"\n");
            if((i+1) % 100 == 0) logger.info("Processed {} docs so far.", (i+1));
        }

        logger.info("Done.");

        IOUtil.getINSTANCE().writeOutput(location +"/"+ fileName+".eval", sbEval.toString());
    }


    /**
     * Outputs predictions based on threshold values
     * for users (that contain multiple posts)
     * @param docs
     */
    public void getPredictions(List<DocUnit> docs){

        for(int i = 0; i < docs.size(); i ++){
            try {
                DocUnit doc = docs.get(i);

                //evaluate post per post
                if(postGranularity) {

                    User user;
                    if (doc instanceof User) {
                        user = (User) doc;
                        List<Post> userPosts = user.getPosts().getPosts();
                        //float negative = 0, positive = 0, compound = 0, neutral = 0;

                        Map<Date, HashMap<String, Float>> userProgress = new TreeMap<>();
                        HashMap<String, Float> adjustedPolarity = new HashMap<>();

                        for (int j = 0; j < userPosts.size(); j++) {
                            String post = "";

                            post = userPosts.get(j).getContent();

                            String chunk = userPosts.get(j).getChunk();

                            if (!post.trim().isEmpty()) {
                                analyzer = new SentimentAnalyzer(post);
                                analyzer.analyse();

                                HashMap<String, Float> polarity = analyzer.getPolarity();

                                if (postMax) {
                                    // use only max value from all posts of a doc
                                    for (String feature : polarity.keySet()) {
                                        float currentValue = adjustedPolarity.get(feature);
                                        float newValue = polarity.get(feature);

                                        if (!feature.contains("compound")) {
                                            if (newValue > currentValue) {
                                                adjustedPolarity.put(feature, newValue);
                                            }
                                        } else if (newValue < currentValue) {
                                            adjustedPolarity.put(feature, newValue);
                                        }
                                    }
                                } else adjustedPolarity.clear();
                                //use the chunk to weight values
                                if (chunkWeight) {
                                    double weight = 1 + (Float.valueOf(chunk) * 0.1);
                                    for (String feature : polarity.keySet()) {
                                        float oldValue = polarity.get(feature);
                                        adjustedPolarity.put(feature, (float) (oldValue * weight));
                                    }
                                }

                                if (adjustedPolarity.isEmpty())
                                    adjustedPolarity.putAll(polarity);

                                userProgress.put(userPosts.get(j).getPostDate(), new HashMap<String, Float>() {{
                                    putAll(adjustedPolarity);
                                }});
                            }
                        }

                        String[] decision = null;

                        if (userProgression) decision = decideWithProgression(userProgress, scorePerc);
                        if (decision != null) results.put(doc.getId(), decision[0] + "\t\t" + decision[1]);

                    }
                }

                //evaluate per doc
                else {
                    String userContent = doc.getContent();

                    analyzer = new SentimentAnalyzer(userContent);
                    analyzer.analyse();

                    float negative = analyzer.getPolarity().get("negative");
                    float positive = analyzer.getPolarity().get("positive");

                    results.put(doc.getId(), decide(negative, positive));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            if((i+1) % 20 == 0) logger.info("Processed {} docs so far.", (i+1));
        }

        logger.info("Done.");

        outputResults();
    }

    /**
     * Makes use of user progression over time
     * to provide a label prediction.
     * @param userProgress predictions per post
     * @param scorePerc
     * @return
     */
    public String[] decideWithProgression(Map<Date, HashMap<String,Float>> userProgress, double scorePerc){

        int userNegAccScore = 0;
        int userCompAccScore = 0;

        float highestNeg = 0;
        float lowestComp = 1;

        for(Date date : userProgress.keySet()){
            float negative = userProgress.get(date).get("negative");
            float compound = userProgress.get(date).get("compound");

            //counting how many times negative feeling increased progressively
            if(negative > highestNeg) {
                highestNeg = negative;
                userNegAccScore++;
            }
            //counting how many times compound decreased progressively
            if(compound < lowestComp){
                lowestComp = compound;
                userCompAccScore++;
            }
        }

        //discounting the first ++ that replaces initialization values (0 and 1);
        userNegAccScore--;
        userCompAccScore--;

        //use number of posts as a parameter of comparison
        double scoreRatio = userProgress.size()*scorePerc;
        if(scoreRatio < 1) scoreRatio = userProgress.size()/2;
        double finalValue = (userNegAccScore + userCompAccScore);

        //if the accumulation of negative scores
        // summed with the accumulation of compound scores
        // is more than 10% of the posts, label as risk
        String[] result;
        if(finalValue >= scoreRatio)
            return new String[]{String.valueOf(User.Label.RISK.name()), String.valueOf(finalValue)};
        else return new String[]{String.valueOf(User.Label.NORISK.name()),String.valueOf(finalValue)};
    }

    /**
     * Provides adequate class according
     * to predicted values
     * @param neg negative predicted value
     * @param pos positive predicted value
     * @return label value
     */
    public String decide(float neg, float pos){
        if(neg > pos)
            return User.Label.RISK.name(); //label as risk
        else return User.Label.NORISK.name();
    }


    /**
     * Generic method to output prediction file
     */
    public void outputResults(){
        StringBuilder sbEval = new StringBuilder();
        for(String key : results.keySet()){
            sbEval.append(key + "\t" + results.get(key) + "\n");
        }
        IOUtil.getINSTANCE().writeOutput(location +"/"+ fileName+".eval", sbEval.toString());
    }



}
