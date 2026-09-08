package com.loupsolitaire.backend.response;

import java.util.List;

public record EffetResponse(String type, Integer valeur, List<CondResponse> conditions) {
}