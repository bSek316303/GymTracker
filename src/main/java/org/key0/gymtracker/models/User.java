package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String username; // to wpisuje w formularzu

    @Column(unique = true, nullable = false, length = 64)
    private String email;

    @Column(nullable = false, length = 80)
    private String password; // to wpisuje w formularzu (musi być zahaszowane w bazie!)

    // --- Metody z interfejsu UserDetails (Spring ich wymaga) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // na razie pusta lista ról
    }

    @Override
    public String getPassword() { return this.password; }

    @Override
    public String getUsername() { return this.username; }

    public String getEmail() { return this.email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
