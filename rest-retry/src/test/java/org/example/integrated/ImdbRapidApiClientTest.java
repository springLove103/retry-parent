package org.example.integrated;

import org.example.client.ImdbRapidApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ImdbRapidApiClientTest {

    @Autowired
    private ImdbRapidApiClient  imdbRapidApiClient;

    @Test
    public void getImdbTitle() {
        String imdbTitle = imdbRapidApiClient.getImdbTitle();
        System.out.println(imdbTitle);
    }
}
