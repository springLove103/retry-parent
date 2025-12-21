package org.example.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
public class ImdbRapidApiClient extends BaseRetryableApiClient {

    private static final String URL = "http://localhost:8001/demo";

    @Autowired
    private RestTemplate restTemplate;


    public String getImdbTitle() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String,Object> reqMap = Map.of("token","test-token");
        HttpEntity<Map<String,Object>> entity =
                new HttpEntity<>(reqMap, headers);

        return post(restTemplate,
                URL,
                entity,
                new ParameterizedTypeReference<String>() {});
    }
}
