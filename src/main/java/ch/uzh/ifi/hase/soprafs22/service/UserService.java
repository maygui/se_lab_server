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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
        // create empty User object
        User userById = null;

        // list all users
        List<User> allUsers = getUsers();

        // iterate through users and check if the requested ID matches with an existing user ID,
        // if so we set the User object userById to this user
        for (User user : allUsers) {
            if (user.getId().equals(userId)) {
                userById = user;
                break;
            }
        }

        // if at this point the User object userById is still empty we throw a ResponseStatusException
        // with status code 404 (not found)
        if (userById == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        return userById;
    }

    /**
     * This method updates a User profile
     *
     * @param userID ID of the user
     * @param newName set new name, if null keep old name
     * @param newUsername set new username, if null keep old username
     */
    public void updateUser(Long userID, String newName, String newUsername){
        // get user by ID (throws exception if user id does not exist)
        User user = getUser(userID);

        // if a new name is specified change the old name, otherwise leave as is
        if (newName != null){
            user.setName(newName);
        }

        // if a new username is specified change the old username, otherwise leave as is
        if (newUsername != null){
            user.setUsername(newUsername);
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
        User userByUsername = null;

        // list all users
        List<User> allUsers = getUsers();

        // iterate through users and check if the requested username matches with an existing username,
        // if so we check if the passwords match (if not throw exception 403), else throw exception 403
        for (User user : allUsers) {
            // check if any user matches the requested username
            if (Objects.equals(user.getUsername(), userInput.getUsername())) {
                userByUsername = user;
                break;
            }
        }

        // if userByUsername is still empty, the posted username was invalid, and we throw 403
        if (userByUsername == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong username!");
        }

        // check if passwords match, else throw 403
        if (!Objects.equals(userByUsername.getPassword(), userInput.getPassword())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong password!");
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
    public User logOut(User loggedIn) {
        // get user
        User user = userRepository.findByUsername(loggedIn.getUsername());

        // set status to OFFLINE
        user.setStatus(UserStatus.OFFLINE);

        // save status
        user = userRepository.save(user);
        userRepository.flush();

        return user;
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
