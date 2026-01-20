package org.wso2.asgardeo.client.model;

import java.util.List;

public class AsgardeoApplicationsResponse {
    private int totalResults;
    private int startIndex;
    private int count;
    private java.util.List<App> applications;

    public static class App {
        private String id;
        private String clientId;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<App> getApplications() {
        return applications;
    }

    public void setApplications(List<App> applications) {
        this.applications = applications;
    }

}
