package spp.example.webapp.service;

import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.stereotype.Service;
import spp.example.webapp.model.User;
import spp.example.webapp.repository.UserStorage;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserStorage userStorage;

    public UserServiceImpl(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    @Trace
    @Override
    public Iterable<User> getUsers() {
        return userStorage.findAll();
    }

    @Trace
    @Override
    public Optional<User> getUser(long userId) {
        return userStorage.findById(userId);
    }

    @Override
    public User createUser(User user) {
        return userStorage.save(user);
    }

    @Override
    public List<User> getUsersByFirstName(String firstName) {
        return userStorage.findByFirstname(firstName);
    }

    @Override
    public List<User> getUsersByLastName(String lastName) {
        return userStorage.findByLastname(lastName);
    }
}
