package com.example.springfile.dto;

// Lombok annotations can simplify this, but using plain getters/setters for clarity
public class SearchQueryDto {
    private String query;
    private int nResults = 5; // Default value, matches FastAPI

    // Constructors
    public SearchQueryDto() {
    }

    public SearchQueryDto(String query, int nResults) {
        this.query = query;
        this.nResults = (nResults > 0) ? nResults : 5; // Ensure positive or default
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getNResults() {
        return nResults;
    }

    public void setNResults(int nResults) {
        this.nResults = (nResults > 0) ? nResults : 5; // Ensure positive or default on set
    }

    @Override
    public String toString() {
        return "SearchQueryDto{" +
               "query='" + query + '\'' +
               ", nResults=" + nResults +
               '}';
    }
}
