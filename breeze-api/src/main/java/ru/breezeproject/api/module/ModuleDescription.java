package ru.breezeproject.api.module;

import java.util.List;

public record ModuleDescription(
    String name,
    String version,
    String main,
    String apiVersion,
    List<String> depends) {
}
