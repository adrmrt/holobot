package dev.zawarudo.holo.modules.pokemon;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * An in-memory Discord for the Pokemon tests: one guild whose text channels keep the messages the
 * bot sends, edits and deletes. JDA types are interface proxies that answer only the calls the
 * Pokemon code makes.
 */
final class FakeDiscord {

    /** Returned by an {@link Answer} to make a fluent call return the proxy itself. */
    static final Object SELF = new Object();

    @FunctionalInterface
    interface Answer {
        Object answer(Object[] args) throws Throwable;
    }

    /** A message the bot sent, as it currently looks. */
    static final class FakeMessage {
        final long id;
        final long channelId;
        MessageEmbed embed;
        List<String> buttonIds = List.of();
        boolean edited;
        boolean deleted;
        /** Delay of a scheduled deletion, or -1 if none is scheduled. */
        long deleteAfterSeconds = -1;

        FakeMessage(long id, long channelId) {
            this.id = id;
            this.channelId = channelId;
        }
    }

    final long guildId;
    final Guild guild;
    final JDA jda;
    /** Every message sent, oldest first. */
    final List<FakeMessage> messages = new ArrayList<>();

    private final Map<Long, TextChannel> channels = new HashMap<>();
    private long nextMessageId = 1000;

    FakeDiscord(long guildId) {
        this.guildId = guildId;
        this.guild = fake(Guild.class, Map.of("getIdLong", _ -> guildId));
        this.jda = fake(JDA.class, Map.of(
            "getTextChannelById", a -> channels.get(toLong(a[0])),
            "getGuildById", a -> toLong(a[0]) == guildId ? guild : null
        ));
    }

    TextChannel addChannel(long channelId) {
        TextChannel channel = fake(TextChannel.class, Map.of(
            "getIdLong", _ -> channelId,
            "getGuild", _ -> guild,
            "sendFiles", _ -> createAction(channelId),
            "editMessageById", a -> action(MessageEditAction.class, () -> edit(toLong(a[0]), (MessageEditData) a[1])),
            "deleteMessageById", a -> action(AuditableRestAction.class, () -> find(toLong(a[0])).deleted = true),
            "retrieveMessageById", a -> action(RestAction.class, () -> asMessage(find(toLong(a[0]))))
        ));
        channels.put(channelId, channel);
        return channel;
    }

    /** Adds a message as if the bot had sent it before a restart. */
    FakeMessage postEarlier(long channelId) {
        FakeMessage message = new FakeMessage(nextMessageId++, channelId);
        messages.add(message);
        return message;
    }

    FakeMessage message(long messageId) {
        return messages.stream().filter(m -> m.id == messageId).findFirst().orElseThrow();
    }

    List<FakeMessage> messages(long channelId) {
        return messages.stream().filter(m -> m.channelId == channelId).toList();
    }

    static User user(long id, String name) {
        return fake(User.class, Map.of(
            "getIdLong", _ -> id,
            "getName", _ -> name,
            "getEffectiveAvatarUrl", _ -> "https://cdn.discordapp.com/embed/avatars/0.png"
        ));
    }

    /**
     * Creates a proxy of {@code type} that answers the given methods by name. Other default methods
     * run their normal code; other methods return the proxy itself where it fits (fluent setters),
     * otherwise null, 0 or false.
     */
    static <T> T fake(Class<T> type, Map<String, Answer> answers) {
        InvocationHandler handler = (self, method, args) -> {
            Object[] a = args == null ? new Object[0] : args;
            Answer answer = answers.get(method.getName());
            if (answer != null) {
                Object result = answer.answer(a);
                return result == SELF ? self : result;
            }
            return switch (method.getName()) {
                case "equals" -> self == a[0];
                case "hashCode" -> System.identityHashCode(self);
                case "toString" -> "Fake" + type.getSimpleName();
                default -> {
                    if (method.isDefault()) {
                        yield InvocationHandler.invokeDefault(self, method, args);
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType != Object.class && returnType.isInstance(self)) {
                        yield self;
                    }
                    yield returnType.isPrimitive() && returnType != void.class ? Array.get(Array.newInstance(returnType, 1), 0) : null;
                }
            };
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private MessageCreateAction createAction(long channelId) {
        List<MessageEmbed> embeds = new ArrayList<>();
        List<String> buttonIds = new ArrayList<>();
        return fake(MessageCreateAction.class, Map.of(
            "setEmbeds", a -> {
                items(a[0]).forEach(e -> embeds.add((MessageEmbed) e));
                return SELF;
            },
            "addComponents", a -> {
                buttonIds.addAll(buttonIds(items(a[0])));
                return SELF;
            },
            "complete", _ -> {
                FakeMessage message = postEarlier(channelId);
                message.embed = embeds.getFirst();
                message.buttonIds = List.copyOf(buttonIds);
                return asMessage(message);
            }
        ));
    }

    private Message edit(long messageId, MessageEditData data) {
        FakeMessage message = find(messageId);
        message.embed = data.getEmbeds().getFirst();
        message.buttonIds = buttonIds(data.getComponents());
        message.edited = true;
        return asMessage(message);
    }

    private static Message asMessage(FakeMessage message) {
        return fake(Message.class, Map.of(
            "getIdLong", _ -> message.id,
            "delete", _ -> fake(AuditableRestAction.class, Map.of(
                "queue", _ -> message.deleted = true,
                "queueAfter", a -> {
                    message.deleteAfterSeconds = ((TimeUnit) a[1]).toSeconds((long) a[0]);
                    return null;
                }
            ))
        ));
    }

    /** Returns the message, failing like Discord does if it doesn't exist (anymore). */
    private FakeMessage find(long messageId) {
        return messages.stream()
            .filter(m -> m.id == messageId && !m.deleted)
            .findFirst()
            .orElseThrow(() -> ErrorResponseException.create(ErrorResponse.UNKNOWN_MESSAGE,
                new Response(new IllegalStateException("Unknown Message"), Set.of())));
    }

    /** A rest action that runs {@code request} when it is completed or queued. */
    @SuppressWarnings("unchecked")
    private static <T> T action(Class<T> type, Callable<?> request) {
        return fake(type, Map.of(
            "complete", _ -> request.call(),
            "queue", a -> {
                Consumer<Object> success = a.length > 0 ? (Consumer<Object>) a[0] : null;
                Consumer<Throwable> failure = a.length > 1 ? (Consumer<Throwable>) a[1] : null;
                Object result;
                try {
                    result = request.call();
                } catch (ErrorResponseException e) {
                    if (failure == null) {
                        throw e;
                    }
                    failure.accept(e);
                    return null;
                }
                if (success != null) {
                    success.accept(result);
                }
                return null;
            }
        ));
    }

    private static List<String> buttonIds(Collection<?> components) {
        return components.stream()
            .flatMap(c -> ((ActionRow) c).getButtons().stream())
            .map(Button::getCustomId)
            .toList();
    }

    /** Varargs arrive as an array, collection overloads as a collection. */
    private static List<?> items(Object arg) {
        return arg instanceof Object[] array ? List.of(array) : List.copyOf((Collection<?>) arg);
    }

    private static long toLong(Object id) {
        return id instanceof Number n ? n.longValue() : Long.parseLong(id.toString());
    }
}
