package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.tinylog.Logger;

import models.CreatureTemplate;

public class YoupHead {

	private static CommandLine YoupHeadCli;
	private static Connection MySQLConnection;
	public static final int CLIENT_BUILD=36949;

	public static void main(String[] args) {
		//System.setProperty("javax.net.debug", "all");
		System.setProperty("https.protocols", "TLSv1.2");
		
		Logger.info("---");
		Logger.info("YoupHead (1.0.0) By Galathil");
		Logger.info("---");
		if(parsingOptions(args) && mysqlSetup()) {
			
			Logger.debug("Retrieve creatures from database...");
			try {
				
				// Retrieve creatures templates
				Statement statement = MySQLConnection.createStatement();
				ResultSet rs = statement.executeQuery( "SELECT entry, minlevel, maxlevel FROM creature_template");
				ArrayList<CreatureTemplate> creatures = new ArrayList<CreatureTemplate>();
				HashMap<String, Boolean> creaturesModelInfo = new HashMap<String, Boolean>();
				int i=0;
				while (rs.next()) {
					CreatureTemplate tmpCreature = new CreatureTemplate();
					tmpCreature.entry=rs.getInt("entry");
					tmpCreature.minlevel=rs.getInt("minlevel");
					tmpCreature.maxlevel=rs.getInt("maxlevel");
					creatures.add(tmpCreature);
					i++;
					//break;
				}
				Logger.debug("Found {} creatures",creatures.size());
				statement.close();
				
				// Retrieve creatures model infos
				Statement statement2 = MySQLConnection.createStatement();
				ResultSet rs2 = statement2.executeQuery("SELECT DisplayID FROM creature_model_info");
				
				int j=0;
				while (rs2.next()) {
					creaturesModelInfo.put(Integer.toString(rs2.getInt("DisplayID")), true);
					j++;
				}
				Logger.debug("Found {} creature_model_info",creaturesModelInfo.size());
				statement2.close();
				int k=1;
				for(CreatureTemplate tpl : creatures) {
					try {
						Logger.info("-------------------------------------------------------------");
						Logger.info("("+k+"/"+creatures.size()+") https://www.wowhead.com/npc="+tpl.entry);
						k++;
						//Document wowheadDoc = Jsoup.connect("https://www.wowhead.com/npc="+tpl.entry).get();
						//Document wowheadDoc = Jsoup.connect("https://hytale.com/news").get();
						//org.jsoup.Connection.Response r = Jsoup.connect("https://www.wowhead.com/npc="+tpl.entry).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0").followRedirects(true).ignoreHttpErrors(true).execute();
						
						// PART 1
						/*
						File f = new File("creatures/"+tpl.entry+".html");
						if(!f.exists()) { 
							Logger.info("Download initialized");
							YHThread t = new YHThread(tpl.entry);
							Thread tt = new Thread(t);
							tt.start();
							Thread.sleep(100);
						} else {
							Logger.info("Skip");
						}*/

						// PART 2

						File input = new File("creatures/"+tpl.entry+".html");
						Document wowheadDoc = Jsoup.parse(input, "UTF-8");

						//Logger.debug(wowheadDoc.getElementById("infobox-contents-0").parent().selectFirst("script").html());
						if(wowheadDoc.getElementsByClass("database-detail-page-not-found-message").size()>0){
							Logger.debug("This creature does not exist.");
							continue;
						}
						//tryFindLevels(wowheadDoc,tpl);
						tryFindModelsInfo(wowheadDoc,tpl,creaturesModelInfo);
					} catch (Exception e) {
						Logger.error(e);
					}
				}
				
			} catch (SQLException e1) {
				Logger.error(e1.getStackTrace());
			}
			
			try {
				MySQLConnection.close();
				Logger.debug("Closing MySQL connection...");
			} catch (SQLException e) {
				Logger.error(e.getStackTrace());
			}
			Logger.info("Done!");
		} else {
			System.exit(1);
		}
	}
	
	public static void downloadPage(int entry) throws Exception {
		//System.out.println("okk");
        //Response response = Jsoup.connect("https://www.wowhead.com/npc="+entry).execute();
        //Document doc = response.parse();
        
        
        try {
            // get URL content
            String a="https://www.wowhead.com/npc="+entry;
            URL url = new URL(a);
            URLConnection conn = url.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(
                               new InputStreamReader(conn.getInputStream()));

            String inputLine;
            String alldoc = "";
            while ((inputLine = br.readLine()) != null) {
                    alldoc+=inputLine;
            }
            br.close();

            File f = new File("creatures/"+entry+".html");
            
            FileUtils.writeStringToFile(f, alldoc, StandardCharsets.UTF_8);
            
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        

    }
	
	private static boolean tryFindLevels(Document wowheadDoc, CreatureTemplate tpl) {
		try {
			String possibleLevelStringHtml = wowheadDoc.getElementById("infobox-contents-0").parent().selectFirst("script").html();
			
			// Level exist?
			Pattern p1 = Pattern.compile("Level: ");
			Matcher m1 = p1.matcher(possibleLevelStringHtml);
			boolean levelSectionfound=false;
			while(m1.find()) {
				levelSectionfound=true;
			}
			if(!levelSectionfound){
				Logger.debug("No level information for this creature");
				return false;
			}
			
			
			// Double level?
			Pattern p2 = Pattern.compile("Level: ([0-9]+) - ([0-9]+)");
			Matcher m2 = p2.matcher(possibleLevelStringHtml);
			boolean minmaxfound=false;
			while(m2.find()) {
				//Logger.debug("minlevel="+m2.group(1)+", maxlevel="+m2.group(2));
				minmaxfound=true;
			}
			if(minmaxfound) { return true;}

			// Simple level?
			Pattern p3 = Pattern.compile("Level: ([0-9]+)");
			Matcher m3 = p3.matcher(possibleLevelStringHtml);
			boolean simplelevelfound=false;
			while(m3.find()) {
				//Logger.debug("minlevel=maxlevel="+m3.group(1));
				simplelevelfound=true;
			}
			if(simplelevelfound) { return true;}
			
			Logger.info(possibleLevelStringHtml);
			return false;
			
		} catch(NullPointerException nullptr) {
			Logger.error("Node for creature informations not found !");
			return false;
		}
	}
	
	private static boolean tryFindModelsInfo(Document wowheadDoc, CreatureTemplate tpl, HashMap<String, Boolean> creaturesModelInfo) {
		try {
			
			// Principal displayid for wowhead
			String displayid = wowheadDoc.getElementById("wh-mv-view-in-3d-button").attr("data-mv-display-id");
			//Logger.debug("displayid="+displayid);
			
			// Additionnal models : 
			boolean modelInfoFinded=false;
			ArrayList<Integer> modelids = new ArrayList<Integer>();
			modelids.add(Integer.parseInt(displayid));
			for(Element el : wowheadDoc.getElementsByTag("script")) {
				String possibleModelsJs = el.html();
				Pattern p1 = Pattern.compile("\"npcmodel\":([0-9]+)");
				Matcher m1 = p1.matcher(possibleModelsJs);
				while(m1.find()) {
					//Logger.debug("modelid="+m1.group(1));
					if(!displayid.equals(m1.group(1))) {
						modelids.add(Integer.parseInt(m1.group(1)));
					}
				}
				if(modelInfoFinded) {
					break;
				}
			}
			
			if(modelids.size()==0) {
				Logger.error("modelids is empty !!!");
			}
			
			Logger.tag("SQL").info("DELETE FROM creature_template_model WHERE CreatureID={};", tpl.entry);
			int i=0;
			for(int modelid : modelids) {
				Logger.tag("SQL").info("INSERT INTO creature_template_model VALUES({},{},{},{},{},{});", tpl.entry,i,modelid,1,1,0);
				if(!creaturesModelInfo.containsKey(Integer.toString(modelid))) {
					Logger.debug("Model info "+modelid+" not exist, create it.");
					Logger.tag("SQL").info("INSERT INTO creature_model_info VALUES({},{},{},{},{});", modelid, 0, 0, 0, 0);
					creaturesModelInfo.put(Integer.toString(modelid), true);
				}
				i++;
			}
			
			return true;
			
		} catch(NullPointerException nullptr) {
			Logger.error("Node for displayid informations not found !");
		}
		return false;
	}
	
	private static boolean parsingOptions(String[] args) {
		Options YoupHeadCliOptions = new Options();
		
		Option MysqlServerAddr = new Option("mserv","mysql-server", true, "MySQL server addr");
		MysqlServerAddr.setRequired(true);
		MysqlServerAddr.setArgName("IP/Domain");
		YoupHeadCliOptions.addOption(MysqlServerAddr);
		
		Option MysqlServerPort = new Option("mport","mysql-port", true, "MySQL server port (Default: 3306)");
		MysqlServerPort.setRequired(false);
		MysqlServerPort.setArgName("Port");
		YoupHeadCliOptions.addOption(MysqlServerPort);
		
		Option MysqlServerUser = new Option("muser","mysql-user", true, "MySQL username");
		MysqlServerUser.setRequired(true);
		MysqlServerUser.setArgName("Username");
		YoupHeadCliOptions.addOption(MysqlServerUser);
		
		Option MysqlServerPassword = new Option("mpass","mysql-password", true, "MySQL password");
		MysqlServerPassword.setRequired(false);
		MysqlServerPassword.setArgName("Password");
		YoupHeadCliOptions.addOption(MysqlServerPassword);
		
		Option MysqlServerDatabase = new Option("mdb","mysql-database", true, "MySQL server dbname");
		MysqlServerDatabase.setRequired(true);
		MysqlServerDatabase.setArgName("dbname");
		YoupHeadCliOptions.addOption(MysqlServerDatabase);
		
		CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();


        try {
        	YoupHeadCli = parser.parse(YoupHeadCliOptions, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("YoupHead", YoupHeadCliOptions);
            return false;
        }
        
        return true;
	}
	
	private static boolean mysqlSetup(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch ( ClassNotFoundException e ) {
            Logger.error("MySQL driver not found !");
            return false;
        }
        
        
        String mysqlUrl="jdbc:mysql://"+YoupHeadCli.getOptionValue("mserv")+":"+YoupHeadCli.getOptionValue("mport","3306")+"/"+YoupHeadCli.getOptionValue("mdb");
        try {
        	MySQLConnection = DriverManager.getConnection(mysqlUrl, YoupHeadCli.getOptionValue("muser"), YoupHeadCli.getOptionValue("mpass",""));
        	Logger.debug("MySQL Connection OK");

        } catch (SQLException e) {
        	Logger.error("Database connection Error!");
			Logger.error(e.getStackTrace());
			return false;
		}
        
        return true;
	}

}
