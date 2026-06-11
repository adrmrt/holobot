package dev.zawarudo.holo.commands.image;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.ParsingUtils;
import dev.zawarudo.holo.utils.Reader;
import dev.zawarudo.holo.utils.TypeTokenUtils;
import dev.zawarudo.holo.utils.Writer;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.exceptions.HttpStatusException;
import dev.zawarudo.holo.utils.exceptions.HttpTransportException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Class of the Action command. By calling this command, the user can get a random
 * reaction or action GIF.
 */
@CommandInfo(name = "action",
    description = "Sends an action GIF. For directed actions you can either mention an user or reply to a " +
        "message to direct the action towards them.",
    usage = "[<action> | list]",
    example = "blush",
    embedColor = EmbedColor.LIGHT_GRAY,
    category = CommandCategory.IMAGE)
public class ActionCmd extends AbstractCommand implements ExecutableCommand {

    private static final String PERSISTED_PATH = "./data/actions.json";
    private static final String RESOURCE_PATH = "data/actions.json";

    private final Map<String, Action> actions;
    private static final Random RANDOM = new Random();

    public ActionCmd() {
        actions = new HashMap<>();
        try {
            initializeActions();
        } catch (IOException ex) {
            throw new IllegalStateException("Something went wrong while initializing the actions!", ex);
        }
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        // Show a list of available actions
        if (!ctx.hasArgs() || "list".equals(ctx.args().getFirst())) {
            showList(ctx);
            return;
        }

        String sub = ctx.args().getFirst().toLowerCase(Locale.ROOT);
        List<String> rest = ctx.args().subList(1, ctx.args().size());

        // Create new action
        if ("create".equals(sub) && ctx.isBotOwner()) {
            createNewAction(ctx, rest);
            return;
        }

        // Add image or API to action
        if ("add".equals(sub) && ctx.isBotOwner()) {
            addImageToAction(ctx, rest);
            return;
        }

        // Call specific action
        if (isAction(sub)) {
            displayAction(ctx, actions.get(sub), rest);
            return;
        }

        // Unknown action
        ctx.reply().errorEmbed("Couldn't find this action. Use `" + ctx.prefix().orElse("") + "action list` to see all available actions.");
    }

    private void showList(CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("List of Actions")
            .setDescription(getActionsAsString())
            .setColor(getEmbedColor());

        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));

        ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
    }

    private void createNewAction(CommandContext ctx, List<String> restArgs) {
        // E.g. <action create <name> <is_api> <text>
        if (restArgs.size() < 3) {
            ctx.reply().errorEmbed("Insufficient argument: Expected `<name> <is_api> <sentence>`");
            return;
        }

        String name = restArgs.get(0);
        boolean isApi = ParsingUtils.isBoolean(restArgs.get(1)) && Boolean.parseBoolean(restArgs.get(1));
        int start = ParsingUtils.isBoolean(restArgs.get(1)) ? 2 : 1;
        String sentence = String.join(" ", restArgs.subList(start, restArgs.size()));

        if (isAction(name)) {
            ctx.reply().errorEmbed("This action already exists!");
            return;
        }

        if (!sentence.contains("{s}")) {
            ctx.reply().errorEmbed("Sentence needs a subject (`{s}`)!");
            return;
        }

        ctx.invocation().deleteInvokeIfPossible();
        actions.put(name, new Action(name, sentence, isApi));

        try {
            writeActionsToFile();
            ctx.channel().sendMessage(String.format("Successfully created action: `%s`", name)).queue();
        } catch (IOException e) {
            ctx.reply().errorEmbed("Something went wrong while storing the updated actions: " + e.getMessage());
            logger.error("Something went wrong while storing the new action.", e);
        }
    }

    private void addImageToAction(CommandContext ctx, List<String> restArgs) {
        // E.g. <action add <name> <url>
        if (restArgs.size() < 2) {
            ctx.reply().errorEmbed("Insufficient arguments: Expected `<name> <url>`");
            return;
        }

        String name = restArgs.get(0);
        String url = restArgs.get(1);

        if (!isAction(name)) {
            ctx.reply().errorEmbed("Not a valid action: " + name);
            return;
        }

        if (!ParsingUtils.isValidUrl(url)) {
            ctx.reply().errorEmbed("Not a valid URL: " + url);
            return;
        }

        ctx.invocation().deleteInvokeIfPossible();
        getAction(name).addNewUrl(url);

        try {
            writeActionsToFile();
            ctx.channel().sendMessage(String.format("Added new link to action: `%s`", name)).queue();
        } catch (IOException e) {
            ctx.reply().errorEmbed("Something went wrong while storing the updated actions: " + e.getMessage());
            logger.error("Something went wrong while storing the action with the new url.", e);
        }
    }

    /**
     * Displays the action GIF or image in an embed and sends it.
     */
    public void displayAction(CommandContext ctx, Action action, List<String> directedArgs) {
        Member member = ctx.member().orElseThrow();

        ctx.invocation().deleteInvokeIfPossible();

        Optional<String> result = fetchActionUrl(action);
        if (result.isEmpty()) {
            ctx.reply().errorEmbed("Something went wrong while fetching an image. Please try again later.");
            return;
        }

        String mention = determineMention(ctx, directedArgs);
        String title = action.getSentence()
            .replace("{s}", member.getEffectiveName())
            .replace("{u}", mention);

        sendActionEmbed(ctx, result.get(), title);
    }

    private Optional<String> fetchActionUrl(Action action) {
        return action.getRandomUrl().flatMap(url -> action.isApi() ? fetchUrlFromApi(url) : Optional.of(url));
    }

    private Optional<String> fetchUrlFromApi(String url) {
        try {
            JsonObject obj = HoloHttp.getJsonObject(url);
            String imageUrl = obj.getAsJsonArray("results").get(0).getAsJsonObject().get("url").getAsString();
            return (imageUrl == null || imageUrl.isBlank()) ? Optional.empty() : Optional.of(imageUrl);
        } catch (HttpStatusException ex) {
            logger.warn("Action API HTTP error: status={} url={}", ex.getStatusCode(), url);
            return Optional.empty();
        } catch (HttpTransportException ex) {
            logger.warn("Action API transport error: url={}", url, ex);
            return Optional.empty();
        }
    }

    private String determineMention(CommandContext ctx, List<String> directedArgs) {
        Message repliedTo = ctx.message().orElseThrow().getReferencedMessage();
        if (repliedTo != null) {
            return "you";
        }

        if (!directedArgs.isEmpty()) {
            List<Member> mentioned = ctx.invocation().mentionedMembers();
            return mentioned.isEmpty() ?
                String.join(" ", directedArgs) :
                mentioned.getFirst().getEffectiveName();
        }

        return "nothing";
    }

    private void sendActionEmbed(CommandContext ctx, String url, String title) {
        EmbedBuilder builder = new EmbedBuilder()
            .setImage(url)
            .setTitle(title)
            .setColor(getEmbedColor());

        Message repliedTo = ctx.message().orElseThrow().getReferencedMessage();
        if (repliedTo != null) {
            repliedTo.replyEmbeds(builder.build()).queue();
        } else {
            ctx.channel().sendMessageEmbeds(builder.build()).queue();
        }
    }

    /**
     * Checks if a given string is an action.
     */
    public boolean isAction(String name) {
        return actions.containsKey(name);
    }

    /**
     * Returns the action associated with the given name.
     */
    public Action getAction(String name) {
        return actions.get(name);
    }

    /**
     * Initializes all actions using a JSON file containing information for each action.
     */
    private void initializeActions() throws IOException {
        JsonArray array;

        if (Reader.exists(PERSISTED_PATH)) {
            array = Reader.readJsonArray((PERSISTED_PATH));
        } else {
            array = Reader.readJsonArrayResource(RESOURCE_PATH);
            // Write to persisted path so new actions can be written into it
            Writer.writeToFile(array.toString(), PERSISTED_PATH);
        }

        Type listType = TypeTokenUtils.getListTypeToken(Action.class);
        List<Action> actionList = new Gson().fromJson(array, listType);
        actionList.forEach(action -> actions.put(action.getName(), action));
    }

    /**
     * Writes the current actions to the file. Used to store updated actions.
     */
    private void writeActionsToFile() throws IOException {
        List<Action> actionList = new ArrayList<>(actions.values());
        Collections.sort(actionList);
        String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(actionList);
        Writer.writeToFile(jsonString, PERSISTED_PATH);
    }

    private String getActionsAsString() {
        String actionsList = actions.keySet().stream()
            .sorted()
            .collect(Collectors.joining(", "));

        return Formatter.asCodeBlock(actionsList);
    }

    /**
     * Class representing an action. Serves as a container for all the information.
     */
    public static class Action implements Comparable<Action> {
        @SerializedName("name")
        private String name;
        @SerializedName("sentence")
        private String sentence;
        @SerializedName("api")
        private boolean api;
        @SerializedName("urls")
        private List<String> urls;

        public Action(String name, String sentence, boolean api) {
            this.name = name;
            this.sentence = sentence;
            this.api = api;
            urls = new ArrayList<>();
        }

        /**
         * Gets the name of the action.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the sentence associated with the action.
         */
        public String getSentence() {
            return sentence;
        }

        /**
         * Checks whether the images or GIFs are fetched from an API.
         */
        public boolean isApi() {
            return api;
        }

        /**
         * Gets a random URL to a GIF or image of this action.
         */
        public Optional<String> getRandomUrl() {
            if (urls.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(urls.get(RANDOM.nextInt(urls.size())));
        }

        /**
         * Adds a new image or GIF to this action. Make sure to store the updated action as well.
         */
        public void addNewUrl(String url) {
            urls.add(url);
        }

        @Override
        public int compareTo(@NotNull Action o) {
            return this.name.compareTo(o.name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Action action && name.equals(action.name);
        }
    }
}
