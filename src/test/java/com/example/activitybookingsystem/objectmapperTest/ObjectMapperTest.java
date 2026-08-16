package com.example.activitybookingsystem.objectmapperTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class ObjectMapperTest {
    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
//        User user = new User("pg13", 13);
//        String pg13Json = mapper.writeValueAsString(user);
//        System.out.println(pg13Json);
//
//        String packJson = "{\"name\":\"pack\",\"age\":\"13\"}";
//        User packUser = mapper.readValue(packJson, User.class);
//        System.out.println(packUser.toString());

        String json = "[{\"name\":\"pack\",\"age\":\"13\"}, {\"name\":\"pack\",\"age\":\"13\"}]";
        List<User> users = mapper.readValue(json, new TypeReference<List<User>>() {});
        System.out.println(users);


    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class User {
        private String name;
        private int age;
    }
}
