package org.wso2.asgardeo.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AsgardeoPatchRoleOperationInfo {
    @SerializedName("Operations")
    private List<Operation> operations;

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    /**
     * Represents the Operation component of the patch role operation payload.
     */
    public static class Operation {

        @SerializedName("op")
        private String op;

        @SerializedName("path")
        private String path;

        @SerializedName("value")
        private Value value;

        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Value getValue() {
            return value;
        }

        public void setValue(Value value) {
            this.value = value;
        }
    }

    /**
     * Represents the Value component of the patch role operation payload.
     */
    public static class Value {

        @SerializedName("permissions")
        private List<Permission> permissions;

        public List<Permission> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<Permission> permissions) {
            this.permissions = permissions;
        }
    }

    /**
     * Represents the Permission component of the patch role operation payload.
     */
    public static class Permission {

        @SerializedName("value")
        private String value;

        @SerializedName("display")
        private String display;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDisplay() {
            return display;
        }

        public void setDisplay(String display) {
            this.display = display;
        }
    }
}
