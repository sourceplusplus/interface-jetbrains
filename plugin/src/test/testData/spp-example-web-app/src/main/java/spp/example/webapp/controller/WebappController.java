package spp.example.webapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spp.example.webapp.model.User;
import spp.example.webapp.service.UserService;

@RestController
public class WebappController {

    private static final Logger log = LoggerFactory.getLogger(WebappController.class);

    private final UserService userService;

    public WebappController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public ResponseEntity<Iterable<User>> userList() {
        log.info("Getting user list");
        return ResponseEntity.ok(userService.getUsers());
    }

    @RequestMapping(value = "/users/{id}", method = RequestMethod.GET)
    public ResponseEntity<User> getUser(@PathVariable long id) {
        log.info("Getting user: {}", id);
        return ResponseEntity.of(userService.getUser(id));
    }

    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public ResponseEntity<User> createUser(@RequestParam String firstName, @RequestParam String lastName) {
        log.info("Creating user: {} {}", firstName, lastName);
        User newUser = new User();
        newUser.setFirstname(firstName);
        newUser.setLastname(lastName);
        return ResponseEntity.ok(userService.createUser(newUser));
    }

    @RequestMapping(value = "/throws-exception", method = RequestMethod.GET)
    public void throwsException() {
        log.error("Throwing exception");
        try {
            caughtException();
        } catch (Exception ex) {
            log.error("Threw error", ex);
        }

        uncaughtException();
    }

    private void caughtException() {
        "test".substring(10);
    }

    private void uncaughtException() {
        throw new RuntimeException("Something bad happened");
    }
}
