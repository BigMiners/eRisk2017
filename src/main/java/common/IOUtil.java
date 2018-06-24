package common;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Class to handle I/O of files
 * Created by halmeida on 1/26/17.
 */
public class IOUtil {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static IOUtil INSTANCE = new IOUtil();
    public static IOUtil getINSTANCE(){
        return INSTANCE;
    }

     /**
     * Loads a list of content from a list of files
      * (e.g., content of list of dictionary files)
     * HA
     * @param path dictionary file names (separated by ",")
     * @return
     */
    public HashMap<String, String> loadDictionary(String path, String file){

        String[] files = file.split(",");
        HashMap<String, String> dictionary = new HashMap<>();

        for(int i = 0; i < files.length; i++){
            String filename = files[i];
            dictionary.putAll(loadResourceList(path+filename));
        }

        return dictionary;
    }


    /**
     * Loads a list (of resources) from a file.
     * Resources in a file are mapped as <concept,value>
     * @param file
     * @return
     */
    public HashMap<String,String> loadResourceList(String file){

        HashMap<String,String> list = new HashMap<String,String>();
        try{
            String featureLine = "";

            //listing features
            BufferedReader reader = new BufferedReader(new FileReader(file));

            int featureCount = 0;
            while (( featureLine = reader.readLine()) != null) {

                String[] content = StringUtils.split(featureLine,"\n");

                for(int i = 0; i < content.length; i++){

                    String[] featurename = null;

                    if(!content[i].contains("#")) featurename = StringUtils.split(content[i],"\t");
                    String key = "", value = "";

                    if(featurename != null) {
                        if (!file.contains("mapping")) {
                            // invert insertion in map, so resources
                            // are unique per value instead of per concept
                            key = featurename[0];
                            if(featurename.length > 1) value = featurename[1];

                        } else{
                            // invert insertion in map, so resources
                            // are unique per value instead of per concept
                            key = featurename[1];
                            value = featurename[0];
                        }
                        //check for duplicate features
                        if (!list.keySet().contains(key))
                            list.put(key.toLowerCase(), value.toLowerCase());
                    }
                }
            }
            reader.close();
        }
        catch (FileNotFoundException e) {
            logger.error("File not found: {}", file);
        }
        catch (IOException e) {
            logger.error("Error handling file {}", file);
        }
        return list;
    }


    /**
     * Loads a feature list found in a file.
     * Feature lists are single item lists (no mapping)
     * (e.g. stopword list, ngram list, sentiment list...)
     * HA
     * @param file
     * @param separator defaults to tab
     * @return
     */

    public HashSet<String> loadFilePerLineSeparator(String file, String separator){
        return loadFilePerLineSeparator(new File(file), separator);
    }
    public HashSet<String> loadFilePerLineSeparator(File file, String separator){

        int filter = 0;
        boolean filterOcc = false;
        if(separator.isEmpty()) separator = "\t";

        if(StringUtils.isNumeric(separator)) {
            filterOcc = true;
            filter = Integer.parseInt(separator);
            separator = "\t";
        }

        HashSet<String> list = new HashSet<String>();
        try{
            String featureLine = "";

            //listing features
            BufferedReader reader = new BufferedReader(new FileReader(file));

            int featureCount = 0;
            while (( featureLine = reader.readLine()) != null) {

                String[] content = StringUtils.split(featureLine,"\n");

                for(int i = 0; i < content.length; i++){
                    String[] featurename = StringUtils.split(content[i],separator);
                    int val = 0;
                    if(featurename.length > 1) val = Integer.parseInt(featurename[1]);
                    //check for duplicate features
                    //if(!(list.contains(featurename[0]))){
                        if(val > 0 && filterOcc){
                            if(val >= filter){
                                list.add(featurename[0]);
                            }
                        }
                        else list.add(featurename[0]);
                   // }
                }
            }
            reader.close();
        }
        catch (FileNotFoundException e) {
            //e.printStackTrace();
            logger.info("File not found {}", file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }


    /**
     * Exports extracted feature list to a file
     * (ngrams, sentiments...)
     *
     * HA
     * @param location
     * @param list
     */
    public void exportFeatures(String location, HashMap<String,Integer> list, int numberDocs, String step){
        String SEPARATOR = "\n";
        StringBuffer line = new StringBuffer();

        if(!step.contains("UniTest")) {
            try {

                for (Map.Entry<String, Integer> entry : list.entrySet()) {
                    if (entry != null) {
                        String str = entry.getKey() + "\t" + entry.getValue();
                        line.append(str).append(SEPARATOR);
                    }
                }

                BufferedWriter writer = new BufferedWriter(new FileWriter(location, false));

                writer.write((line.toString()));
                writer.flush();
                writer.close();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            logger.info("Done! " + list.size() + " features extracted to file from " + numberDocs + " files.\n");
        }
        else logger.info("Done! " + list.size() + " features extracted for testing from " + numberDocs + " files.\n");
    }



    /**
     *
     * Reads a (word2vec, doc2vec) vector file to generate a
     * training/testing instance for deep learners.
     *
     * @param file txt file with (w2vec, doc2vec) vector representation of document
     * @return
     */
    public HashMap<double[],String> loadVectorMap(File file){
        HashSet<String> lines = loadFilePerLineSeparator(file,"");

        HashMap<double[],String> list = new HashMap<>();


        for(String line : lines){

            String thisline = line;
            String label = thisline.substring(0,1);
            String[] vector = thisline.substring(2).split(",");

            double[] doubleValues = Arrays.stream(vector)
                    .mapToDouble(Double::parseDouble)
                    .toArray();

            list.put(doubleValues,label);
        }

        if(list.size() == 0 || list == null)logger.info("file: {} \t with NO vectors.", file.getName(), list.size());

        return list;

    }

    /**
     * Lists all files from a given
     * folder recursively, according to a level
     * @param dir directory from where start reading recursively
     * @param level default (read all files recursively): pass empty string ""
     * @return
     */
    public HashSet<File> listAllFiles(String dir, String level){

        HashSet<File> filesResult = new HashSet<>();
        File path = new File(dir);
        if(path.exists()) {

            if (path.exists() && path.isFile()) {
                filesResult.add(path);
            } else if (level.contains("all") || level.contains("label")) {
                File directory = new File(dir);
                filesResult.add(directory);
            } else {
                File[] files = new File(dir).listFiles();

                if (level.contains("doc") || level.isEmpty()) {
                    for (File file : files) {
                        getFilesRecursively(file, filesResult);
                    }
                }
            }
        }
        else logger.error("Provided path does not exist: {}", dir);

        return filesResult;
    }

    private void getFilesRecursively(File entry, HashSet<File> result){

        if(entry.isDirectory()) {
            File[] dir = entry.listFiles();
            for (int i = 0; i < dir.length; i++) {
                getFilesRecursively(dir[i], result);
            }
        }
        else result.add(entry);

    }

    /**
     * Generic function to write string to a file
     */
    public void writeOutput(String path, String content) {

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(content);
            writer.flush();
            writer.close();
           // logger.info("File outputed in " + path);
        }catch(IOException e){
            logger.info("Error writing file in " + path);
        }
    }

}


