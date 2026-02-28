package dev.zawarudo.holo.modules.anime;

import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;

import java.util.List;

public interface MediaSearchProvider {

    MediaPlatform platform();

    List<AnimeResult> searchAnime(String query, int limit) throws APIException, InvalidRequestException;

    List<MangaResult> searchManga(String query, int limit) throws APIException, InvalidRequestException;
}
