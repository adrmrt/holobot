package dev.zawarudo.holo.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.HttpStatusException;
import dev.zawarudo.holo.utils.exceptions.HttpTransportException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for <a href="https://thecatapi.com">The Cat API</a>.
 */
public final class TheCatApiClient {

    private static final String BASE_URL = "https://api.thecatapi.com/v1";

    private TheCatApiClient() {
    }

    /**
     * Fetches a random cat image URL.
     *
     * @throws APIException if the API call fails
     */
    @NotNull
    public static String getRandomImage() throws APIException {
        JsonArray array = fetchArrayOrThrow(BASE_URL + "/images/search");
        return array.get(0).getAsJsonObject().get("url").getAsString();
    }

    /**
     * Fetches a random cat image URL for the given breed ID.
     *
     * @param breedId the Cat API breed identifier (e.g. {@code "abys"})
     * @throws APIException            if the API call fails
     * @throws InvalidRequestException if no images are found for the breed
     */
    @NotNull
    public static String getRandomBreedImage(@NotNull String breedId) throws APIException, InvalidRequestException {
        JsonArray array = fetchArrayOrThrow(BASE_URL + "/images/search?breed_ids=" + breedId);
        if (array.isEmpty()) {
            throw new InvalidRequestException("No images found for breed: " + breedId);
        }
        return array.get(0).getAsJsonObject().get("url").getAsString();
    }

    /**
     * Returns all known cat breeds.
     *
     * @throws APIException if the API call fails
     */
    @NotNull
    public static List<CatBreed> getBreeds() throws APIException {
        JsonArray array = fetchArrayOrThrow(BASE_URL + "/breeds");
        List<CatBreed> breeds = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            String id = obj.get("id").getAsString();
            String name = obj.get("name").getAsString();
            breeds.add(new CatBreed(id, name));
        }
        return breeds;
    }

    private static JsonArray fetchArrayOrThrow(String url) throws APIException {
        try {
            return HoloHttp.getJsonArray(url);
        } catch (HttpStatusException e) {
            throw new APIException("API error (" + e.getStatusCode() + "): " + url, e);
        } catch (HttpTransportException e) {
            throw new APIException("I/O error while contacting The Cat API: " + url, e);
        }
    }

    /** A cat breed with its API identifier and display name. */
    public record CatBreed(@NotNull String id, @NotNull String name) {
    }
}
