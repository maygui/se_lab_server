package ch.uzh.ifi.hase.soprafs22.service;

import ch.uzh.ifi.hase.soprafs22.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs22.entity.User;
import ch.uzh.ifi.hase.soprafs22.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(@Qualifier("userRepository") UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers() {
        return this.userRepository.findAll();
    }

    /**
     * This method retrieves a User by ID
     *
     * @param userId ID of the user
     * @throws org.springframework.web.server.ResponseStatusException throws 404 if user ID not found
     */
    public User getUser(Long userId) {

        Optional<User> userById = userRepository.findById(userId);

        // if at this point the User object userById is still empty we throw a ResponseStatusException
        // with status code 404 (not found)
        if (userById.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        return userById.get();
    }

    /**
     * This method updates a User profile
     *
     * @param userID ID of the user
     * @param toBeUpdated // TODO
     */
    public void updateUser(Long userID, User toBeUpdated){
        // get user by ID (throws exception if user id does not exist)
        User user = getUser(userID);


        // if a new name is specified change the old name, otherwise leave as is
        if (toBeUpdated.getUsername() != null && !toBeUpdated.getUsername().isEmpty()){
            checkIfUserExists(toBeUpdated);
            user.setUsername(toBeUpdated.getUsername());
        }

        // if a new username is specified change the old username, otherwise leave as is
        if (toBeUpdated.getBirthday() != null){
            user.setBirthday(toBeUpdated.getBirthday());
        }

        // saves the given entity but data is only persisted in the database once
        // flush() is called
        userRepository.save(user);
        userRepository.flush();
    }

    /**
     * This method creates a new User (registration)
     *
     * @param newUser User object
     */
    public User createUser(User newUser) {
        newUser.setToken(UUID.randomUUID().toString());
        newUser.setStatus(UserStatus.OFFLINE);

        checkIfUserExists(newUser);
        newUser.setStatus(UserStatus.ONLINE);

        // saves the given entity but data is only persisted in the database once
        // flush() is called
        newUser = userRepository.save(newUser);
        userRepository.flush();

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    /**
     * This method checks if the posted credentials are valid
     *
     * @param userInput User object
     * @throws ResponseStatusException
     */
    public User checkCredentials(User userInput) {
        // create empty User object
        User userByUsername = userRepository.findByUsernameAndPassword(userInput.getUsername(), userInput.getPassword());

        // if userByUsername is still empty, the posted username was invalid, and we throw 403
        if (userByUsername == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong username or password!");
        }

        // set status to ONLINE
        userByUsername.setStatus(UserStatus.ONLINE);

        // save status
        userByUsername = userRepository.save(userByUsername);
        userRepository.flush();

        return userByUsername;
    }

    /**
     * This method sets the status of a logged out user to OFFLINE
     *
     * @param loggedIn logged in user
     */
    public void logOut(User loggedIn) {
        // get user
        Optional<User> userById = userRepository.findById(loggedIn.getId());

        if (userById.isPresent()) {
            User user = userById.get();

            // set status to OFFLINE
            user.setStatus(UserStatus.OFFLINE);

            // save status
            userRepository.save(user);
            userRepository.flush();

        }
    }

    /**
     * This is a helper method that will check the uniqueness criteria of the
     * username and the name
     * defined in the User entity. The method will do nothing if the input is unique
     * and throw an error otherwise.
     *
     * @param userToBeCreated
     * @throws org.springframework.web.server.ResponseStatusException
     * @see User
     */
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
        // if username is taken throw error with status code 409
        if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The username provided is not unique. Therefore, the user could not be created!");
        }
    }
}
