package parser;

import common.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Parsing of the dataset
 * Organizes documents as:
 * group -> list of users
 * user -> list of posts
 */
public class Parser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    Properties props;
    String corpus_dir, usersWritings_dir, dictionaries_dir, unit, project;
    Boolean useWritingsCount;
    int writingsThreshold, sampling_perc;
    HashMap<String, String> usersWritings;
    String task;
    Boolean useMedsCount, useDrugsCount, useDiseasesCount;
    String meds_dir, drugs_dir, diseases_dir;
    HashSet<String> meds, drugs, diseases;

    //corpus statistics
    int totalNumPosts, numDocuments, numDocs, numRisk, numNoRisk, numRiskPost, numNoRiskPost;
    Date maxDate, minDate;


    public Parser() {

        props = Configs.getInstance().getProps();
        task = props.getProperty("TASK");
        corpus_dir = props.getProperty("CORPUS_DIRECTORY");
        if (!corpus_dir.contains("chunk")) corpus_dir += "/" + task;

        project = props.getProperty("PROJECT");
        project = project.contains("-") ?  project.substring(0,project.indexOf("-")) : project;

        unit = props.getProperty("UNIT");

        useWritingsCount = Boolean.valueOf(props.getProperty("USE_WRITINGS_COUNT"));
        writingsThreshold = Integer.parseInt(props.getProperty("WRITINGS_THRESHOLD"));

        usersWritings_dir = props.getProperty("WORK_DIRECTORY") + props.getProperty("RESOURCES_DIR") + "/" + props.getProperty("USER_WRITINGS");
        usersWritings = IOUtil.getINSTANCE().loadResourceList(usersWritings_dir);

        sampling_perc = Integer.parseInt(props.getProperty("SAMPLING_PERC"));
        if(sampling_perc > 0 && task.contains("train")) {
            corpus_dir += "_sampling" + sampling_perc;
        }

        dictionaries_dir = props.getProperty("WORK_DIRECTORY") + props.getProperty("RESOURCES_DIR") + props.getProperty("DICTIONARIES_DIR") + "/";
        meds_dir =  dictionaries_dir + props.getProperty("MEDS_DIC");
        drugs_dir = dictionaries_dir + props.getProperty("DRUGS_DIC");
        diseases_dir = dictionaries_dir + props.getProperty("DISEASES_DIC");

        useMedsCount = Boolean.valueOf(props.getProperty("USE_MEDS_COUNT"));
        useDrugsCount = Boolean.valueOf(props.getProperty("USE_DRUGS_COUNT"));
        useDiseasesCount = Boolean.valueOf(props.getProperty("USE_DISEASES_COUNT"));

        //load dictionaries to count keywords in user production
        if(useMedsCount) meds = IOUtil.getINSTANCE().loadFilePerLineSeparator(meds_dir,"");
        if(useDrugsCount) drugs = IOUtil.getINSTANCE().loadFilePerLineSeparator(drugs_dir, "");
        if(useDiseasesCount) diseases = IOUtil.getINSTANCE().loadFilePerLineSeparator(diseases_dir, "");

        //corpus statistics
        totalNumPosts = 0;

        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.ENGLISH);

        try {
            maxDate = sf.parse(sf.format(new Date(0)));
            minDate = sf.parse(sf.format(new Date()));
        }catch (ParseException e){
            logger.info("Error parsing date. {}",e.getMessage());
        }
    }

    public Group parse(Group group, String dir, String level) {

        if (dir.isEmpty()) dir = corpus_dir;

        List<Document> docs = new ArrayList<>();
        HashSet<File> files = IOUtil.getINSTANCE().listAllFiles(dir, level);


        logger.info("Loading data... ");
        try {
            for (File file : files)
                if (file.getName().endsWith("xml"))
                    docs.add(getXMLDocument(file.getPath()));
        } catch (SAXException e) {
            logger.error("Error parsing files from directory {}", dir);
        } catch (ParserConfigurationException e) {
            logger.error("Error parsing files from directory {}", dir);
        } catch (IOException e) {
            logger.error("Error reading files from directory {}", dir);
        }
        logger.info("Done.");
        return addChunks(group, docs);
    }

    public Group parse(String dir, String level) {
        return parse(new Group(), dir, level);
    }

    public Group parse(String dir) {
        return parse(dir, "doc");
    }

    /**
     * This method is used to read an XML file and parse it using DocumentBuilderFactory and DocumentBuilder from javax xml.
     * It returns a fully parsed Document. It should be used as an input for parseFile() method.
     *
     * @param filePath : This is the absolute path to the XML file to read.
     * @return Document : This is a W3C Dom Document object used by the getPostList() method of the parser
     * @throws ParserConfigurationException : This error is triggered when DocumentBuilderFactory fail to build the document
     * @throws IOException                  : This error is triggered when the file can't be read
     * @throws SAXException                 : This error is triggered when DocumentBuilder is not able to parse the Document
     */
    Document getXMLDocument(String filePath) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File fileXML = new File(filePath);
        return builder.parse(fileXML);
    }


    /**
     * Parses the given documents and integrates them to the given group, assigning them to their author.
     * Each document constitutes a chunk.
     *
     * @param group     the working group of users. May be expanded upon.
     * @param documents The list of xml documents to parse
     * @return the (possibly updated) group of users with their updated chunks
     * @throws XPathExpressionException In case the path can't be evaluated by Javax/Xpath, it will throw an error
     */
    public Group addChunks(Group group, List<Document> documents) {
        String subjectIDPath = "/INDIVIDUAL/ID[1]";
        String writingPath = "/INDIVIDUAL/WRITING";
        String messageIDPath = "//response";

        for (Document document : documents) {
            String URI = document.getDocumentURI();
            String subjectID = "";
            NodeList nodes = null;
            String chunk = informChunk(URI);

            int writings = 0;

            Element root = document.getDocumentElement();
            XPathFactory xpf = XPathFactory.newInstance();
            XPath path = xpf.newXPath();

            //determine which type of parsing should be used
            String parserType = root.getElementsByTagName("WRITING").getLength() > 0 ? "user" : "post";

            //parses "post" files (CLPsych)
            if(parserType.contains("post")){
                try {
                    nodes = (NodeList) path.evaluate(messageIDPath, root, XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    logger.error("Error when reading post.");
                }
            }
            //parses "user" files (eRisk)
            else{
                try {
                    subjectID = path.evaluate(subjectIDPath, root);
                    nodes = (NodeList) path.evaluate(writingPath, root, XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    logger.error("Error when reading user.");
                }
                if (usersWritings.containsKey(subjectID)) writings = Integer.parseInt(usersWritings.get(subjectID));
            }

            if (!useWritingsCount || (useWritingsCount && writings <= writingsThreshold)) {
                //parses all posts contained in a file
                Posts posts = getPosts(nodes, chunk, parserType, subjectID);

                //adjust user label, and add to statistics
                DocUnit.Label label = DocUnit.Label.UNDECIDED;
                label = label.getLabel(project, URI);

                //generates a group of users
                if(unit.contains("user")){
                    //in cases of creating users from single posts
                    //the userID has to be assigned
                    if(subjectID.isEmpty() && posts.size() < 2) {
                        subjectID = posts.getPosts().get(0).getAuthor();
                        writings++;
                    }

                    //create a new user in the group, if already does not exist
                    User user = group.getUser(subjectID);
                    //set user values accordingly
                    user.setLabel(label);
                    user.addPosts(posts);
                    user.setWritings(writings);

                    //adjust user min and max date for posts
                    if(minDate.after(user.getMinDate()))
                        minDate = user.getMinDate();
                    if(maxDate.before(user.getMaxDate()))
                        maxDate = user.getMaxDate();
                }
                //generates a group of posts
                if(unit.contains("post")){
                    //iterates over post list parsed (if
                    for(int i = 0; i < posts.size(); i++) {
                        Post post = posts.getPosts().get(i);
                        //if it is a file with multiple posts,
                        //adjust the post ID (format: userID-[counter])
                        if(!parserType.contains("post")) {
                            post.setCompleteId(subjectID + "-" + i);
                        }
                        //if it is not a file with multiple posts,
                        //just add userID as is
                        else post.setCompleteId(subjectID);
                        post.setLabel(label);
                        group.getPost(post);
                    }
                }

                totalNumPosts += posts.size();
                numDocuments++;
            }
        }
        numDocs += group.getDocsAsList().size();

        return group;
    }


    /**
     * Populates an user object
     * with values
     * @param user a new user object
     * @param posts a list of posts of an user
     * @param label a docunit label
     * @param writings number of writings
     * @return
     */
    private User parseUser(User user, Posts posts, DocUnit.Label label, int writings){
        user.setLabel(label);
        user.addPosts(posts);
        user.setWritings(writings);

        if(minDate.after(user.getMinDate()))
            minDate = user.getMinDate();
        if(maxDate.before(user.getMaxDate()))
            maxDate = user.getMaxDate();

        return user;
    }

    /**
     * Retrieves dataset chunk
     * from document name (URI)
     * @param docURI
     * @return
     */
    private String informChunk(String docURI){
        String chunk = "0";

        if (docURI.contains("/chunk")) {
            chunk = docURI.substring(docURI.indexOf("/chunk")).replaceFirst("/", "");
            chunk = chunk.substring(0, chunk.indexOf("/")).replace("chunk", "");
        }

        return chunk;
    }


    /**
     * Provides statistics
     * on the parsed corpus
     */
    public void getCorpusStats(){
        logger.info("\n " +
                    "# of users: {} \n " +
                    "# of docs: {} \n " +
                    "# of posts: {} \n " +
                numDocs, numDocuments, totalNumPosts, minDate, maxDate);
    }

    /**
     * This method is used to retrieve every post contained in one xml file and ouput them as a common.Posts object.
     *
     * @param nodes : Takes a NodeList of nodes
     * @return : common.Posts (An object that contains a list of common.Post)
     */
    private Posts getPosts(NodeList nodes, String chunk, String parserType, String subjectId) {
        Posts posts = new Posts();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            posts.addPost(getPost(element, chunk, parserType, subjectId));
        }

        return posts;
    }


    /**
     * This method takes an Element and output a Post object.
     *
     * @param element : Takes an Element element
     * @return : common.Post with the writings informations (title, date, info and text)
     */
    /**
     * This method takes an Element and outputs a Post object.
     * @param element w3c element
     * @param chunk if existent, chunk number for post (if not, 0)
     * @param parserType "user" if file with user+multiple posts, "post" if file has single post
     * @param subjectId ID of user (if user) or author (if post)
     * @return
     */
    Post getPost(Element element, String chunk, String parserType, String subjectId) {
        String postId = "", title = "", date = "",
               info = "", text = "", author = "";

        if(parserType.contains("post")){
            title = element.getElementsByTagName("subject").item(0).getTextContent();
            date = element.getElementsByTagName("post_time").item(0).getTextContent();
            info = "clpsychpost";
            text = element.getElementsByTagName("body").item(0).getTextContent();

            postId = element.getElementsByTagName("message").item(0).getAttributes().getNamedItem("href").toString();
            author = element.getElementsByTagName("author").item(0).getAttributes().getNamedItem("href").toString();

            postId = cleanHrefTag(postId);
            author = cleanHrefTag(author);
        }
        else if(parserType.contains("user")){
            title = element.getElementsByTagName("TITLE").item(0).getTextContent();
            date = element.getElementsByTagName("DATE").item(0).getTextContent();
            info = element.getElementsByTagName("INFO").item(0).getTextContent();
            text = element.getElementsByTagName("TEXT").item(0).getTextContent();
            postId = subjectId;
            author = subjectId;
        }

        Post aPost = new Post(postId);
        aPost.setAuthor(author);
        aPost.setTitle(title);
        aPost.setPostDate(date);
        aPost.setInfo(info);
        aPost.setText(text);
        aPost.setChunk(chunk);

        return aPost;
    }

    /**
     * Parses a href tag from HTML
     * and returns only the ID
     * @param tag html href tag
     * @return
     */
    public String cleanHrefTag(String tag){
        return tag.substring(tag.lastIndexOf("/")).replace("/","").replace("\"","");
    }
}
