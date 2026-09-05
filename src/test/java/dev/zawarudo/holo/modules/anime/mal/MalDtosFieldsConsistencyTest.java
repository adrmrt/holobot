package dev.zawarudo.holo.modules.anime.mal;

import com.google.gson.annotations.SerializedName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against {@code MalApiClient.ANIME_FIELDS}/{@code MANGA_FIELDS} drifting from
 * {@link MalDtos.Anime}/{@link MalDtos.Manga}'s actual shape - e.g. someone adds a component to
 * the record but forgets to add its wire key to the requested fields, so it comes back silently
 * null forever. Reflection is fine here: it runs at build/test time and fails loudly, unlike the
 * request-building path in production which should stay a plain, readable constant.
 */
class MalDtosFieldsConsistencyTest {

    @Test
    void animeFieldsCoverEveryDtoComponent() {
        assertRequestedFieldsCoverDto(MalDtos.Anime.class, MalApiClient.ANIME_FIELDS);
    }

    @Test
    void mangaFieldsCoverEveryDtoComponent() {
        assertRequestedFieldsCoverDto(MalDtos.Manga.class, MalApiClient.MANGA_FIELDS);
    }

    private void assertRequestedFieldsCoverDto(Class<? extends Record> dto, String requestedFields) {
        Set<String> requestedKeys = Arrays.stream(requestedFields.split(","))
            .map(spec -> spec.contains("{") ? spec.substring(0, spec.indexOf('{')) : spec)
            .collect(Collectors.toSet());

        for (RecordComponent component : dto.getRecordComponents()) {
            String wireKey = wireKeyOf(dto, component);
            assertTrue(
                requestedKeys.contains(wireKey),
                dto.getSimpleName() + "." + component.getName() + " (wire key \"" + wireKey
                    + "\") is not requested by the fields= constant - it will always come back null"
            );
        }
    }

    private String wireKeyOf(Class<?> dto, RecordComponent component) {
        try {
            Field field = dto.getDeclaredField(component.getName());
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            return serializedName != null ? serializedName.value() : component.getName();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
