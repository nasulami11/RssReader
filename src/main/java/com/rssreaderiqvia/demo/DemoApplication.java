package com.rssreaderiqvia.demo;



import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.xml.splitter.XPathMessageSplitter;
import org.springframework.messaging.Message;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@SpringBootApplication
public class DemoApplication {






    public static void main(String[] args) {

            new SpringApplicationBuilder(DemoApplication.class)

                    .run(args);
        }



@Value("${FileLocation}")
private String FileLocation;



        @Bean
        public IntegrationFlow feedFlow() throws IOException {
            return IntegrationFlows.from("URLChannel")
                    .split(new XPathMessageSplitter("/rss/channel/item"))
                    .channel("MessageToFileChannel")
                    .get();

        }



    @Bean
    public AbstractMessageSplitter xpathSplitter() {
        return new XPathMessageSplitter("/rss/channel/item");
    }


    @InboundChannelAdapter(value= "URLChannel", poller = @Poller(fixedDelay = "1000000000000"))
    public String getData() throws IOException {

            String Reslut = null;

            try {


                String Url = "https://www.aljazeera.net/aljazeerarss/a7c186be-1baa-4bd4-9d80-a84db769f779/73d0e1b4-532f-45ef-b135-bfdff8b8cab9";
                URL obj = new URL(Url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;

                StringBuffer buffer = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    buffer.append(line);

                }

                Reslut = buffer.toString();
            }catch (Exception e){
                System.out.println("something went wrong when calling URL");
            }


        return Reslut;

    }





@ServiceActivator(inputChannel = "MessageToFileChannel")
    public void MessageToFileChannel(Message<?> message) throws IOException, XPathExpressionException, ParseException {


    String publishedDate = parser (message.getPayload().toString(),"pubDate");
    String category = parser (message.getPayload().toString(),"category");
    String guid = parser (message.getPayload().toString(),"guid");
    String link = parser (message.getPayload().toString(),"link");


    if (category == "" )
        category = "NoCategory";



    SimpleDateFormat prevuoisFormat = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z");
    Date currentdate=prevuoisFormat.parse(publishedDate);
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    String format =formatter.format(currentdate);



        String MainfileName = FileLocation+format;
        String SubfileName =  FileLocation+format+"/"+category;

        Path path = Paths.get(MainfileName);

       if (!Files.exists(path)) {

            Files.createDirectory(path);
            Path Spath = Paths.get(SubfileName);

            if (!Files.exists(Spath)) {
                Files.createDirectory(Spath);
            }

       } else {

            Path Spath = Paths.get(SubfileName);

            if (!Files.exists(Spath)) {
                Files.createDirectory(Spath);
            }
        }



        String fileName = FilenameUtils.getName(guid);
        File file = new File(SubfileName);
        FileWriter Write = new FileWriter(file+"/"+fileName+".xml");
        BufferedWriter out = new BufferedWriter(Write);
        out.write(message.getPayload().toString());
        out.close();

        logLinks(link);



    }


    public String parser(String message ,String element) throws  XPathExpressionException {


        InputSource source = new InputSource(new StringReader(message));
        XPath xpath = XPathFactory.newInstance()
                .newXPath();
        Object item = xpath.evaluate("/item", source, XPathConstants.NODE);
        String parsedElement = null;

        parsedElement = xpath.evaluate(element, item);


        return parsedElement;

    }


public void logLinks (String link) throws IOException {

    FileWriter Write = new FileWriter(FileLocation+"/logs" , true);

    BufferedWriter out = new BufferedWriter(Write);

    out.write(link);
    out.newLine();
    out.close();

}

}

