package com.example.profile;

import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final AccountRepository accounts;

    public ProfileController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    // Principal gives us the username that Spring Security authenticated.
    @PostMapping
    public ResponseEntity<Profile> create(@Valid @RequestBody Profile profile, Principal user) {
        if (!accounts.createProfile(user.getName(), profile)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profile already exists");
        }
        return ResponseEntity.created(URI.create("/profile")).body(profile);
    }

    @GetMapping
    public Profile read(Principal user) {
        return accounts.findProfile(user.getName()).orElseThrow(this::profileNotFound);
    }

    @PutMapping
    public Profile update(@Valid @RequestBody Profile profile, Principal user) {
        if (!accounts.updateProfile(user.getName(), profile)) {
            throw profileNotFound();
        }
        return profile;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal user) {
        if (!accounts.deleteProfile(user.getName())) {
            throw profileNotFound();
        }
    }

    private ResponseStatusException profileNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile has not been created");
    }
}
