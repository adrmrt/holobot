package dev.zawarudo.holo.modules.emotes;

import dev.zawarudo.holo.database.dao.EmoteDao;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.internal.entities.emoji.CustomEmojiImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages operations related to custom emotes, including integrity checks, insertion, and retrieval.
 * This class ensures unique naming and maintains data consistency in the emote database.
 */
public class EmoteManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmoteManager.class);

    private static final Pattern SUFFIX_PATTERN = Pattern.compile("^(.*?)(-\\d+)?$");

    private final EmoteDao emoteDao;

    /**
     * Initializes the EmoteManager and performs an integrity check on the emote database.
     */
    public EmoteManager(EmoteDao emoteDao) {
        this.emoteDao = emoteDao;

        try {
            performIntegrityCheck();
        } catch (SQLException ex) {
            LOGGER.error("Emote integrity check failed.", ex);
        }
    }

    /**
     * Performs an integrity check on the Emotes table, ensuring all names are unique.
     */
    private void performIntegrityCheck() throws SQLException {
        Map<String, List<EmoteDao.EmoteNameRow>> duplicateGroups = emoteDao.findDuplicateNameGroups();

        if (duplicateGroups.isEmpty()) {
            LOGGER.info("Emote data integrity check passed.");
            return;
        }

        List<EmoteDao.EmoteRename> updates = resolveDuplicates(duplicateGroups);

        if (!updates.isEmpty()) {
            emoteDao.updateNames(updates);
            LOGGER.info("Resolved {} duplicate emotes names.", updates.size());
        }
    }

    /**
     * Inserts one or more custom emotes into the database.
     * Ensures that each emote has a unique name and ID.
     *
     * @param emotes An array of {@link CustomEmoji} objects to insert.
     */
    public synchronized void insertEmotes(@NotNull CustomEmoji... emotes) throws SQLException {
        if (emotes.length == 0) return;

        List<EmoteDao.EmoteRow> toInsert = new ArrayList<>(emotes.length);

        // Track names for emotes of this batch
        Set<String> reservedLower = new HashSet<>();

        for (CustomEmoji emote : emotes) {
            // Emote already stored in the database
            if (emote == null || emoteDao.existsById(emote.getIdLong())) {
                continue;
            }

            String uniqueName = generateUniqueName(emote.getName(), reservedLower);

            toInsert.add(new EmoteDao.EmoteRow(
                emote.getIdLong(),
                uniqueName,
                emote.isAnimated(),
                emote.getTimeCreated().toString(),
                emote.getImageUrl()
            ));
        }

        if (!toInsert.isEmpty()) {
            emoteDao.insertAll(toInsert);
        }
    }

    /**
     * Searches emotes with names partially matching the search term.
     */
    public List<CustomEmoji> searchEmotesByName(@NotNull String name) throws SQLException {
        if (name.isBlank()) return List.of();

        List<EmoteDao.EmoteLite> rows = emoteDao.searchByNameLike(name.trim());
        List<CustomEmoji> result = new ArrayList<>(rows.size());

        for (EmoteDao.EmoteLite row : rows) {
            result.add(toJdaEmoji(row));
        }

        return result;
    }

    /**
     * Retrieves an emote by its exact name.
     */
    public Optional<CustomEmoji> getEmoteByName(@NotNull String name) throws SQLException {
        if (name.isBlank()) return Optional.empty();
        return emoteDao.findByExactNameIgnoreCase(name.trim()).map(this::toJdaEmoji);
    }

    /**
     * Renames the emote. If newName exists, swaps names.
     */
    public void renameEmote(@NotNull String emote, @NotNull String newName) throws SQLException {
        if (emote.isBlank()) {
            throw new IllegalArgumentException("emote must be non-blank");
        }
        if (newName.isBlank()) {
            throw new IllegalArgumentException("newName must be non-blank");
        }

        emoteDao.renameOrSwap(emote.trim(), newName.trim());
    }

    /**
     * Returns a list of ids of the emotes that are stored in the database.
     */
    public Set<Long> getEmoteIds() throws SQLException {
        List<EmoteDao.EmoteLite> emotes = emoteDao.findAll();
        Set<Long> ids = HashSet.newHashSet(emotes.size());

        for (EmoteDao.EmoteLite e : emotes) {
            ids.add(e.id());
        }

        return ids;
    }

    private List<EmoteDao.EmoteRename> resolveDuplicates(Map<String, List<EmoteDao.EmoteNameRow>> duplicateGroups) {
        List<EmoteDao.EmoteRename> updates = new ArrayList<>();

        for (Map.Entry<String, List<EmoteDao.EmoteNameRow>> entry : duplicateGroups.entrySet()) {
            String baseName = entry.getKey();

            List<EmoteDao.EmoteNameRow> duplicates = new ArrayList<>(entry.getValue());
            duplicates.sort(Comparator.comparingLong(EmoteDao.EmoteNameRow::emoteId));

            boolean first = true;
            int suffix = 1;

            for (EmoteDao.EmoteNameRow row : duplicates) {
                String newName = first ? baseName : baseName + "-" + suffix++;
                first = false;

                if (!Objects.equals(row.emoteName(), newName)) {
                    updates.add(new EmoteDao.EmoteRename(row.emoteId(), newName));
                }
            }
        }

        return updates;
    }

    /**
     * Generates a unique name for the emote, ensuring no duplicates in the database.
     */
    private String generateUniqueName(String baseName, Set<String> reservedLower) throws SQLException {
        String strippedName = stripSuffix(baseName).trim();
        if (strippedName.isEmpty()) strippedName = "emote";

        Set<String> existingNames = emoteDao.findNamesStartingWithIgnoreCase(strippedName);

        // Compare case-insensitively
        Set<String> lower = new HashSet<>(reservedLower);
        for (String s : existingNames) {
            if (s != null) lower.add(s.toLowerCase(Locale.ROOT));
        }

        String unique = strippedName;
        int suffix = 1;

        while (lower.contains(unique.toLowerCase(Locale.ROOT))) {
            unique = strippedName + "-" + suffix++;
        }

        reservedLower.add(unique.toLowerCase(Locale.ROOT));
        return unique;
    }

    /**
     * Strips the numeric suffix from a name, if present.
     */
    private String stripSuffix(String name) {
        Matcher matcher = SUFFIX_PATTERN.matcher(name);
        return matcher.matches() ? matcher.group(1) : name;
    }

    private CustomEmoji toJdaEmoji(EmoteDao.EmoteLite row) {
        return new CustomEmojiImpl(row.name(), row.id(), row.animated());
    }
}
