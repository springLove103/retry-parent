package org.simple.example.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
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

         try {
             String result = post(restTemplate,
                     URL,
                     entity,
                     new ParameterizedTypeReference<String>() {
                     });
             log.info("=====================记录数据库-成功操作=====================");
             //TODO
             return result;
         } catch (Exception e) {
             // 回调异常记录数据库
//             log.info(e.getMessage());
             log.info("=====================记录数据库-失败操作=====================");
             //TODO
         }
         return null;
    }
}
