package memrise.parser.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ParserService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final RestTemplate restTemplate;
    private final List<String> memriseLessonUrls;
    private final List<String> memriseLessonParts;
    private final List<String> memriseLessonTitles;
    private final String memriseExportType;

    @Autowired
    public ParserService(@Value("#{'${memrise.lesson.urls}'.split(';')}") List<String> memriseLessonUrls,
                         @Value("#{'${memrise.lesson.parts}'.split(';')}") List<String> memriseLessonParts,
                         @Value("#{'${memrise.lesson.titles}'.split(';')}") List<String> memriseLessonTitles,
                         @Value("${memrise.export.type}") String memriseExportType) {
        this.restTemplate = new RestTemplate();
        this.memriseLessonUrls = memriseLessonUrls;
        this.memriseLessonParts = memriseLessonParts;
        this.memriseLessonTitles = memriseLessonTitles;
        this.memriseExportType = memriseExportType;
    }

    @PostConstruct
    private void init() {
        log.info("Start memrise parser");

        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity requestEntity = new HttpEntity(httpHeaders);

        for (int urlCounter = 0; urlCounter < memriseLessonUrls.size(); urlCounter++) {
            String url = memriseLessonUrls.get(urlCounter);

            for (int partCounter = 0; partCounter < Integer.parseInt(memriseLessonParts.get(urlCounter)); partCounter++) {
                log.info("Parse (url, part): {}, {}", url, partCounter + 1);

                ResponseEntity responseEntity = restTemplate.exchange(url + (partCounter + 1), HttpMethod.GET, requestEntity, String.class);
                String responseBody = (String) responseEntity.getBody();
                Document doc = Jsoup.parse(responseBody);

                String titleCourse = memriseLessonTitles.get(urlCounter);
                String titleCoursePart = (partCounter < 9 ? "0" : "") + (partCounter + 1);
                String title = doc.getElementsByClass("progress-box-title").text();
                List<Element> elementsA = doc.getElementsByClass("col_a");
                List<Element> elementsB = doc.getElementsByClass("col_b");

                List<String> content;
                if (memriseExportType.equals("csv")) {
                    content = prepareCSVContent(title, elementsA, elementsB);
                } else {
                    content = prepareHTMLContent(titleCourse, titleCoursePart, title, elementsA, elementsB);
                }

                PrintWriter writer;
                try {
                    writer = new PrintWriter(titleCourse + "_" + titleCoursePart + "." + memriseExportType, "UTF-8");
                    for (String str : content) {
                        writer.println(str);
                    }
                    writer.close();
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        log.info("Finished memrise parser");
    }

    private List<String> prepareCSVContent(String title, List<Element> elementsA, List<Element> elementsB) {
        List<String> content = new ArrayList<>();
        content.add("title\t" + title);
        content.add("eng\trus");
        for (int i = 0; i < elementsA.size(); i++) {
            content.add(elementsA.get(i).text() + "\t" + elementsB.get(i).text());
        }
        return content;
    }

    private List<String> prepareHTMLContent(String titleCourse, String titleCoursePart, String title, List<Element> elementsA, List<Element> elementsB) {
        List<String> content = new ArrayList<>();
        content.add("<!DOCTYPE html><html><body><table border=\"1\" width=\"100%\" style=\"font-size:24px;\">");
        content.add("<caption><h2>" + titleCourse + " - part [" + titleCoursePart + "] - " + title + "</h2></caption>");
        content.add("<tr><th>ENG</th><th>RUS</th></tr>");
        for (int i = 0; i < elementsA.size(); i++) {
            String en = elementsA.get(i).text();
            String ru = elementsB.get(i).text();
            content.add("<tr><td width=\"50%\">" + en + "</td><td width=\"50%\">" + ru + "</td></tr>");
        }
        content.add("</table></body></html>");
        return content;
    }
}
