package ru.proshik.english.quizlet.telegramBot.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType;

import javax.persistence.*;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static javax.persistence.GenerationType.SEQUENCE;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM;

@Entity
@Table(name = "account")
public class Account {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static Map<ModeType, Boolean> DEFAULT_MODE_TYPES = new HashMap<>();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        DEFAULT_MODE_TYPES.put(ModeType.LEARN, true);
        DEFAULT_MODE_TYPES.put(ModeType.FLASHCARDS, true);
        DEFAULT_MODE_TYPES.put(ModeType.WRITE, true);
        DEFAULT_MODE_TYPES.put(ModeType.SPELL, true);
        DEFAULT_MODE_TYPES.put(ModeType.TEST, true);
        DEFAULT_MODE_TYPES.put(ModeType.MATCH, true);
        DEFAULT_MODE_TYPES.put(ModeType.GRAVITY, false);
    }

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "account_id_seq")
    @GenericGenerator(name = "account_id_seq",
            strategy = "enhanced-sequence",
            parameters = @Parameter(name = SEQUENCE_PARAM, value = "account_id_seq"))
    private Long id;

    private ZonedDateTime createdDate;

    private String login;

    private String accessToken;

    @Column(name = "enabled_modes")
    private String enabledModes;

    @OneToMany(mappedBy = "account", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Notification> notifications = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Account() {
    }

    public Account(ZonedDateTime createdDate, String login, String accessToken) {
        this.createdDate = createdDate;
        this.login = login;
        this.accessToken = accessToken;
        this.enabledModes = getDefaultModeTypes();
    }

    public Long getId() {
        return id;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public String getLogin() {
        return login;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEnabledModes() {
        return enabledModes;
    }

    public void setEnabledModes(String enabledModes) {
        this.enabledModes = enabledModes;
    }

    public Set<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<Notification> notifications) {
        this.notifications = notifications;
    }

    private static String getDefaultModeTypes() {
        return safeWriteModeTypes(DEFAULT_MODE_TYPES);
    }

    public static String writeEnabledModeTypes(Map<ModeType, Boolean> enabledModeTypes) {
        return safeWriteModeTypes(enabledModeTypes);
    }

    public static Map<ModeType, Boolean> readEnablesModeTypes(String modeTypes) {
        try {
            return OBJECT_MAPPER.readValue(modeTypes, new TypeReference<HashMap<ModeType, Boolean>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("deserialize modeTypes to map");
        }
    }

    private static String safeWriteModeTypes(Map<ModeType, Boolean> modeTypes) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(modeTypes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("unexpected behavior");
        }
    }
}
