package ru.platik777.backauth.dto.response;

import ru.platik777.backauth.entity.types.ItemType;

public record PermissionResponse(
            String itemId,
            ItemType itemType,
            Short permissions
    ) {
        public boolean canRead() {
            return (permissions & 1) != 0;
        }

        public boolean canWrite() {
            return (permissions & 2) != 0;
        }

        public boolean canExecute() {
            return (permissions & 4) != 0;
        }

        public String getPermissionsString() {
            StringBuilder sb = new StringBuilder();
            if (canRead()) sb.append("R");
            if (canWrite()) sb.append("W");
            if (canExecute()) sb.append("X");
            return !sb.isEmpty() ? sb.toString() : "NONE";
        }
    }