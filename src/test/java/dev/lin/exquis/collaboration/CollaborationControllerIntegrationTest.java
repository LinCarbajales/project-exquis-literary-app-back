package dev.lin.exquis.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lin.exquis.collaboration.dtos.CollaborationRequestDTO;
import dev.lin.exquis.role.RoleEntity;
import dev.lin.exquis.role.RoleRepository;
import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.story.StoryRepository;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CollaborationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollaborationRepository collaborationRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity testUser;
    private StoryEntity testStory;
    private RoleEntity userRole;

    @BeforeEach
    void setUp() {
        // Limpiar datos previos
        collaborationRepository.deleteAll();
        userRepository.deleteAll();
        storyRepository.deleteAll();

        // Crear rol si no existe (sin Builder porque RoleEntity no lo tiene)
        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    RoleEntity role = new RoleEntity();
                    role.setName("USER");
                    return roleRepository.save(role);
                });

        // Crear usuario de prueba
        testUser = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("Test")
                .surname("User")
                .roles(Set.of(userRole))
                .build();
        testUser = userRepository.save(testUser);

        // Crear historia de prueba (usando los campos correctos)
        testStory = StoryEntity.builder()
                .extension(10)              // maxCollaborations -> extension
                .finished(false)
                .visibility("public")
                .createdAt(LocalDateTime.now())
                .build();
        testStory = storyRepository.save(testStory);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldCreateCollaboration() throws Exception {
        CollaborationRequestDTO request = CollaborationRequestDTO.builder()
                .text("Érase una vez en un lugar muy lejano...")
                .storyId(testStory.getId())
                .build();

        mockMvc.perform(post("/api/v1/collaborations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", is(request.getText())))
                .andExpect(jsonPath("$.orderNumber", is(1)))
                .andExpect(jsonPath("$.storyId", is(testStory.getId().intValue())))
                .andExpect(jsonPath("$.userId", is(testUser.getId().intValue())));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldGetCollaborationsByStory() throws Exception {
        // Crear algunas colaboraciones
        CollaborationEntity collab1 = CollaborationEntity.builder()
                .text("Primera colaboración")
                .orderNumber(1)
                .createdAt(LocalDateTime.now())
                .story(testStory)
                .user(testUser)
                .build();
        collaborationRepository.save(collab1);

        CollaborationEntity collab2 = CollaborationEntity.builder()
                .text("Segunda colaboración")
                .orderNumber(2)
                .createdAt(LocalDateTime.now())
                .story(testStory)
                .user(testUser)
                .build();
        collaborationRepository.save(collab2);

        mockMvc.perform(get("/api/v1/collaborations/story/" + testStory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].orderNumber", is(1)))
                .andExpect(jsonPath("$[1].orderNumber", is(2)));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldFailWithNullStoryId() throws Exception {
        String requestJson = "{\"text\":\"Texto de prueba\",\"storyId\":null}";

        mockMvc.perform(post("/api/v1/collaborations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldFailWithInvalidStoryId() throws Exception {
        CollaborationRequestDTO request = CollaborationRequestDTO.builder()
                .text("Texto de prueba")
                .storyId(99999L)  // ID que no existe
                .build();

        mockMvc.perform(post("/api/v1/collaborations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        CollaborationRequestDTO request = CollaborationRequestDTO.builder()
                .text("Texto de prueba")
                .storyId(testStory.getId())
                .build();

        mockMvc.perform(post("/api/v1/collaborations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldIncrementOrderNumber() throws Exception {
        // Crear primera colaboración
        CollaborationRequestDTO request1 = CollaborationRequestDTO.builder()
                .text("Primera colaboración")
                .storyId(testStory.getId())
                .build();

        mockMvc.perform(post("/api/v1/collaborations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber", is(1)));

        // Crear segunda colaboración
        CollaborationRequestDTO request2 = CollaborationRequestDTO.builder()
                .text("Segunda colaboración")
                .storyId(testStory.getId())
                .build();

        mockMvc.perform(post("/api/v1/collaborations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber", is(2)));
    }
}