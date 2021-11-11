package spp.example.operate;

import com.github.javafaker.Faker;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import spp.example.webapp.Booter;
import spp.example.webapp.model.User;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class WebappOperator {

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Faker faker = Faker.instance();

    public static void main(String[] args) {
        //start webapp
        Booter.main(args);

        //create 5 users a second
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("http://localhost:8888/users")
                    .queryParam("firstName", faker.name().firstName())
                    .queryParam("lastName", faker.name().lastName())
                    .build()
                    .toUri();
            restTemplate.exchange(uri, HttpMethod.POST, null, User.class);
        }, 0, 200, TimeUnit.MILLISECONDS);

        //get users 10 times a second
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            restTemplate.getForEntity("http://localhost:8888/users", User[].class);
        }, 0, 100, TimeUnit.MILLISECONDS);

        //get specific user 100 times a second
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                restTemplate.getForEntity("http://localhost:8888/users/" + ThreadLocalRandom.current().nextInt(1000), User.class);
            } catch (RestClientException ignore) {
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

        //get specific user 1 time a second
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                restTemplate.getForEntity("http://localhost:8888/throws-exception", User.class);
            } catch (RestClientException ignore) {
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }
}
