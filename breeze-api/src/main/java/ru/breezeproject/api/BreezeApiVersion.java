package ru.breezeproject.api;

public final class BreezeApiVersion {

    public static final String CURRENT = "1.0.0";

    private BreezeApiVersion() {
    }

    public static boolean isCompatible(String moduleApiVersion) {
        if (moduleApiVersion == null || moduleApiVersion.isBlank()) {
            return false;
        }
        int[] declared = parse(moduleApiVersion);
        int[] current = parse(CURRENT);
        if (declared[0] != current[0]) {
            return false;
        }
        if (declared[1] != current[1]) {
            return declared[1] < current[1];
        }
        return declared[2] <= current[2];
    }

    private static int[] parse(String version) {
        String[] parts = version.trim().split("\\.");
        int[] result = new int[3];
        for (int i = 0; i < 3 && i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }
}
