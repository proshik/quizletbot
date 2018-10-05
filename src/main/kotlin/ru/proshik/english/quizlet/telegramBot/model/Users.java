package ru.proshik.english.quizlet.telegramBot.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Id;
import ru.proshik.english.quizlet.telegramBot.service.vo.ModeType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

//@Entity
//@Table(name = "users")
//@TypeDefs({
//        @TypeDef(name = "string-array", typeClass = StringArrayType.class),
//        @TypeDef(name = "int-array", typeClass = IntArrayType.class),
//        @TypeDef(name = "json", typeClass = JsonStringType.class),
//        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class),
//        @TypeDef(name = "jsonb-node", typeClass = JsonNodeBinaryType.class),
//        @TypeDef(name = "json-node", typeClass = JsonNodeStringType.class)})
public class Users {

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
//    @GeneratedValue(strategy = SEQUENCE, generator = "users_id_seq")
//    @GenericGenerator(name = "users_id_seq",
//            strategy = "enhanced-sequence",
//            parameters = @Parameter(name = SEQUENCE_PARAM, value = "users_id_seq"))
    private Long id;

//    private LocalDateTime createdDate;

    //    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    private String login;

    private String accessToken;

//    @Column(name = "enabled_modes")
//    private String enabledModes;

//    @Type(type = "jsonb")
//    @Column(columnDefinition = "json")
//    private String operationData;

//    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
//    private Set<Notification> notifications = new HashSet<>();

    public Users() {
    }

    public Users(Long chatId, String login, String accessToken) {
//        this.createdDate = createdDate;
        this.chatId = chatId;
        this.login = login;
        this.accessToken = accessToken;
    }

    public Long getId() {
        return id;
    }

//    public LocalDateTime getCreatedDate() {
//        return createdDate;
//    }

    public Long getChatId() {
        return chatId;
    }

    public String getLogin() {
        return login;
    }

    public String getAccessToken() {
        return accessToken;
    }

//    public Set<Notification> getNotifications() {
//        return notifications;
//    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

//    public void setNotifications(Set<Notification> notifications) {
//        this.notifications = notifications;
//    }

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
