package spp.example.webapp.service;

import spp.example.webapp.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Iterable<User> getUsers();

    Optional<User> getUser(long userId);

    User createUser(User user);

    List<User> getUsersByFirstName(String firstName);

    List<User> getUsersByLastName(String firstLast);

//    UserSession getUserSession(User user);
}
