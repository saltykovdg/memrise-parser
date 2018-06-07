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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ParserService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final RestTemplate restTemplate;
    private final String sessionId;
    private final List<String> memriseLessonUrls;
    private final List<String> memriseLessonParts;
    private final List<String> memriseLessonTitles;

    @Autowired
    public ParserService(@Value("${memrise.session.id}") String sessionId,
                         @Value("#{'${memrise.lesson.urls}'.split(';')}") List<String> memriseLessonUrls,
                         @Value("#{'${memrise.lesson.parts}'.split(';')}") List<String> memriseLessonParts,
                         @Value("#{'${memrise.lesson.titles}'.split(';')}") List<String> memriseLessonTitles) {
        this.restTemplate = new RestTemplate();
        this.sessionId = sessionId;
        this.memriseLessonUrls = memriseLessonUrls;
        this.memriseLessonParts = memriseLessonParts;
        this.memriseLessonTitles = memriseLessonTitles;
    }

    @PostConstruct
    private void init() {
        log.info("Start memrise parser");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Cookie", "sessionid_2=" + sessionId);
        HttpEntity requestEntity = new HttpEntity(httpHeaders);

        for (int urlCounter = 0; urlCounter < memriseLessonUrls.size(); urlCounter++) {
            String url = memriseLessonUrls.get(urlCounter);

            for (int partCounter = 0; partCounter < Integer.parseInt(memriseLessonParts.get(urlCounter)); partCounter++) {
                log.info("Parse (url, part): {}, {}", url, partCounter + 1);

                ResponseEntity responseEntity = restTemplate.exchange(url + (partCounter + 1), HttpMethod.GET, requestEntity, String.class);
                String responseBody = (String) responseEntity.getBody();
                Document doc = Jsoup.parse(responseBody);

                String title = doc.getElementsByClass("progress-box-title").text();
                List<Element> elementsA = doc.getElementsByClass("col_a");
                List<Element> elementsB = doc.getElementsByClass("col_b");

                List<String> content = new ArrayList<>();
                content.add("title\t" + title);
                content.add("eng\trus");
                for (int i = 0; i < elementsA.size(); i++) {
                    content.add(elementsA.get(i).text() + "\t" + elementsB.get(i).text());
                }

                PrintWriter writer;
                try {
                    writer = new PrintWriter(memriseLessonTitles.get(urlCounter) + "_" + (partCounter < 9 ? "0" : "") + (partCounter + 1) + ".csv", "UTF-8");
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
}
