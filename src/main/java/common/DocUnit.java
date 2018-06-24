package common;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/**
 * Created by halmeida on 5/10/17.
 */
public class DocUnit {

    //from user
    public enum Label {
        RISK(1),
        NORISK(2),
        UNDECIDED(0),
        GREEN(0),
        AMBER(1),
        RED(2),
        CRISIS(3);

        private final int value;
        Label(int value){
            this.value = value;
        }
        public int value(){
            return this.value;
        }

        public Label getLabel(String project, String URI){
            if(project.toLowerCase().contains("erisk")){
                if (URI.contains(NORISK.name().toLowerCase())
                || URI.contains(GREEN.name().toLowerCase())
                || URI.contains(AMBER.name().toLowerCase()))
                    return NORISK;
                else
                    return RISK;
            }
            else {
                if(URI.contains(GREEN.name().toLowerCase())
                || URI.contains(NORISK.name().toLowerCase()))
                    return GREEN;
                if(URI.contains(AMBER.name().toLowerCase()))
                    return AMBER;
                if(URI.contains(RED.name().toLowerCase())
                || URI.contains(RISK.name().toLowerCase()))
                    return RED;
                if(URI.contains(CRISIS.name().toLowerCase()))
                    return CRISIS;
            }
            return UNDECIDED;
        }

        public String informLabels(String project){
            String list = "";
            if(project.toLowerCase().contains("erisk"))
                list =  RISK.name().toLowerCase() + ", "+
                        NORISK.name().toLowerCase();
            else list = GREEN.name().toLowerCase() + ", "+
                        AMBER.name().toLowerCase() + ", "+
                        RED.name().toLowerCase() + ", "+
                        CRISIS.name().toLowerCase();
            return list;
        }
    }

    Label label;
    String id, completeId;
    String content;
    int medsCount;
    int drugsCount;
    int diseasesCount;

    public DocUnit(){
    }


    public DocUnit(String completeId){
        this.completeId = completeId;
        setId();
        this.label = Label.UNDECIDED;
        this.content = "";
        medsCount = 0;
        drugsCount = 0;
        diseasesCount = 0;
    }

    public DocUnit(String completeId, Label label){
        this.completeId = completeId;
        setId();
        this.label = label;
        this.content = "";
        medsCount = 0;
        drugsCount = 0;
        diseasesCount = 0;
    }

    public DocUnit(String completeId, Label label, String content){
        this.completeId = completeId;
        setId();
        this.label = label;
        this.content = content;
        medsCount = 0;
        drugsCount = 0;
        diseasesCount = 0;
    }



    public void setId() {
        if(completeId.contains("subject")){
            this.id = (completeId.substring(completeId.indexOf("subject"), completeId.length())).replace("subject", "");
        }
        else this.id = completeId;
    }
    public String getId() {
        return this.id;
    }

    public void setCompleteId(String completeId) {
        this.completeId = completeId;
    }
    public String getCompleteId() { return this.completeId; }

    public String getContent() { return content; }
    public void setContent(String content){
        this.content = content;
    }

    public Label getLabel() {
        return label;
    }
    public void setLabel(Label label) {
        this.label = label;
    }

    public int getDrugsCount() { return drugsCount; }

    public void setDrugsCount(HashSet<String> drugsList) {
        this.drugsCount = countOccurrences(content, drugsList);
    }

    public int getMedsCount(){
        return medsCount;
    }
    public void setMedsCount(HashSet<String> medsList) {
        this.medsCount = countOccurrences(content, medsList);
    }

    public int getDiseasesCount() {return diseasesCount;}
    public void setDiseasesCount(HashSet<String> diseasesList) {
        this.diseasesCount = countOccurrences(content, diseasesList);
    }


    /**
     * Counts the number of occurrences
     * of keywords in a string
     *
     * @param content string
     * @param list words that should be counted in string
     * @return
     */
    private int countOccurrences(String content, HashSet<String> list){
        int allAtributes = 0;
        content = content.toLowerCase();

        for(String word : list){
            word = word.toLowerCase();
            if(content.contains(word)) {
                allAtributes += StringUtils.countMatches(content, word);
            }
        }
        return allAtributes;
    }
}
