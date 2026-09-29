package fr.mosaique.accounts;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {
    protected Admin() { }

    public Admin(User user) {
        super(user.getDisplayName(), user.getEmail(), user.getPasswordHash());
        setStatus(user.getStatus());
    }
}