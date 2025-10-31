package dev.lin.exquis.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import dev.lin.exquis.user.exceptions.UserNotFoundException;

@Service
public class JpaUserDetailsService implements UserDetailsService {

        private UserRepository userRepository;

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

        @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {


        return userRepository.findByEmail(email)
                .map(SecurityUser::new)
                .orElseThrow(() -> new UserNotFoundException("User not found with this email"));

    }

    public UserDetails loadUserById(Long id) {
    UserEntity user = userRepository.findById(id)
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));
    return new SecurityUser(user);
}
}
