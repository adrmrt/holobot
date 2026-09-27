package dev.zawarudo.holo.modules;

import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DogCeoClientIT {

    @Test
    void testRandomImage() throws APIException {
        String url = DogCeoClient.getRandomImage();
        assertNotNull(url, "URL should not be null");
    }

    @Test
    void testRandomBreedImage() throws APIException, InvalidRequestException {
        String url = DogCeoClient.getRandomBreedImage("hound");
        assertNotNull(url, "URL should not be null");
    }

    @Test
    void testRandomSubBreedImage() throws APIException, InvalidRequestException {
        String url = DogCeoClient.getRandomSubBreedImage("hound", "afghan");
        assertNotNull(url, "URL should not be null");
    }

    @Test
    void testBreedImages() throws APIException, InvalidRequestException {
        List<String> images = DogCeoClient.getBreedImages("hound");
        assertNotNull(images, "List should not be null");
    }

    @Test
    void testSubBreedImages() throws APIException, InvalidRequestException {
        List<String> images = DogCeoClient.getSubBreedImages("hound", "afghan");
        assertFalse(images.isEmpty(), "List should not be empty");
    }

    @Test
    void testAllBreeds() throws APIException {
        List<DogCeoClient.Breed> breeds = DogCeoClient.getBreedList();
        assertFalse(breeds.isEmpty(), "List should not be empty");
    }

    @Test
    void testHoundSubBreeds() throws InvalidRequestException, APIException {
        List<String> subBreeds = DogCeoClient.getSubBreeds("hound");
        assertFalse(subBreeds.isEmpty(), "List should not be empty");
    }

    @Test
    void testHasSubBreeds() throws APIException {
        List<DogCeoClient.Breed> breeds = DogCeoClient.getBreedList();

        for (DogCeoClient.Breed breed : breeds) {
            // Hound has different sub-breeds
            if (breed.name().equals("hound")) {
                assertTrue(breed.hasSubBreeds(), "Breed should have sub-breeds");
            }
            // API doesn't have any Chihuahua sub-breeds
            if (breed.name().equals("chihuahua")) {
                assertFalse(breed.hasSubBreeds(), "Breed should not have sub-breeds");
            }
        }
    }

    @Test
    void testInvalidBreed() {
        assertThrows(InvalidRequestException.class, () -> DogCeoClient.getSubBreeds("nonsense"), "Method should throw an exception");
        assertThrows(InvalidRequestException.class, () -> DogCeoClient.getRandomBreedImage("nonsense"), "Method should throw an exception");
        assertThrows(InvalidRequestException.class, () -> DogCeoClient.getBreedImages("nonsense"), "Method should throw an exception");
    }

    @Test
    void testInvalidSubBreed() {
        assertThrows(InvalidRequestException.class, () -> DogCeoClient.getRandomSubBreedImage("hound", "nonsense"), "Method should throw an exception");
        assertThrows(InvalidRequestException.class, () -> DogCeoClient.getRandomSubBreedImage("nonsense", "afghan"), "Method should throw an exception");

        assertThrows(InvalidRequestException.class, () -> DogCeoClient.getSubBreedImages("hound", "nonsense"), "Method should throw an exception");
        assertThrows(InvalidRequestException.class, () -> DogCeoClient.getSubBreedImages("nonsense", "afghan"), "Method should throw an exception");
    }
}