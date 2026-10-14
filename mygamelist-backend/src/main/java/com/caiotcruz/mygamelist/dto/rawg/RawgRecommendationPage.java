package com.caiotcruz.mygamelist.dto.rawg;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
public record RawgRecommendationPage(List<Candidate> results) {
    public record Candidate(Long id, String name, @JsonProperty("background_image") String cover,
        Double rating, @JsonProperty("ratings_count") Integer ratingsCount, List<RawgGenre> genres) {}
}
