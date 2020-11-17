package spp.example.webapp.controller;

import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spp.example.webapp.model.User;
import spp.example.webapp.service.UserService;

@RestController
public class WebappController {

    private final UserService userService;

    public WebappController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public ResponseEntity<Iterable<User>> userList() {
        ActiveSpan.info("Getting user list");
        return ResponseEntity.ok(userService.getUsers());
    }

    @RequestMapping(value = "/users/{id}", method = RequestMethod.GET)
    public ResponseEntity<User> getUser(@PathVariable long id) {
        ActiveSpan.info(String.format("Getting user: %d", id));
        return ResponseEntity.of(userService.getUser(id));
    }

    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public ResponseEntity<User> createUser(@RequestParam String firstName, @RequestParam String lastName) {
        ActiveSpan.tag("firstName", firstName);
        ActiveSpan.tag("lastName", lastName);
        ActiveSpan.info(String.format("Creating user: %s %s", firstName, lastName));
        User newUser = new User();
        newUser.setFirstname(firstName);
        newUser.setLastname(lastName);
        return ResponseEntity.ok(userService.createUser(newUser));
    }

    @RequestMapping(value = "/throws-exception", method = RequestMethod.GET)
    public void throwsException() {
        ActiveSpan.error("Throwing exception");
        try {
            caughtException();
        } catch (Exception ex) {
            ActiveSpan.error(ex);
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
