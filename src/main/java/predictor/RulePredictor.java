package predictor;

import common.Configs;
import common.IOUtil;
import common.User;
import parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created by halmeida on 1/26/17.
 */
public class RulePredictor {

    Properties props;
    String work_dir, resources_dir, rulesFile, rulesPath;
    ArrayList<String> rules;
    List<User> users;
    Parser parser;
    HashMap<String,String> ruleUsers;


    public RulePredictor(){

        props = Configs.getInstance().getProps();
        work_dir = props.getProperty("WORK_DIRECTORY");
        resources_dir = props.getProperty("RESOURCES_DIR") + "/vocabulary/";
        rulesFile = "";
        rulesPath = work_dir + resources_dir;
        ruleUsers = new HashMap<String,String>();
    }

    public HashMap<String,String> getRuleUsers(){
        return ruleUsers;
    }

    public List<User> filterRulePredictions(List<User> users){
        int count = 0;

        if(!ruleUsers.isEmpty()) {
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                String id = user.getId();
                if (ruleUsers.containsKey(id)) {
                    users.remove(i);
                    count++;
                }
            }
        }
        return users;
    }


    public void loadPredictionByRule(List<User> users){

        HashMap<String,String> rules = IOUtil.getINSTANCE().loadDictionary(rulesPath, rulesFile);

        for(int i = 0; i < users.size(); i++){

            User user = users.get(i);
            String content = user.getContent();

            for(String rule : rules.keySet()){
                String ruleClass = rules.get(rule);

                if (content.contains(rule)) {
                    String userLabel = user.getLabel().name();
                    String userId = user.getId();

                    if(ruleUsers.keySet().contains(userId)){
                        String existentClass = ruleUsers.get(userId);

                        //adapt for new rules + labels
//                        if(ruleClass.contains("amber") && !existentClass.contains("crisis")
//                                && !existentClass.contains("red"))
//                            ruleUsers.put(userId, userLabel + "," + ruleClass);
                    }
                    else ruleUsers.put(userId, userLabel + "," + ruleClass);
                    System.out.println("post id: " + userId + " label: " + ruleClass);
                }
            }
        }

        System.out.println(ruleUsers.size() + " users labeled based on rules.");

    }
}
