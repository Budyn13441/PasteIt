package com.pdwww.pasteit.backend.api.model;

import java.time.ZonedDateTime;
import java.util.Map;

public record StashMeta(
                ZonedDateTime expirationDate,
                boolean readOnly, Map<String, NodeCategory> nodeCategories) {
}
