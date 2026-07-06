package ru.breezeproject.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreezeApiVersionTest {

    @Test
    void sameVersionIsCompatible() {
        assertTrue(BreezeApiVersion.isCompatible(BreezeApiVersion.CURRENT));
    }

    @Test
    void olderPatchIsCompatible() {
        assertTrue(BreezeApiVersion.isCompatible("1.0.0"));
    }

    @Test
    void differentMajorIsIncompatible() {
        assertFalse(BreezeApiVersion.isCompatible("2.0.0"));
        assertFalse(BreezeApiVersion.isCompatible("0.9.9"));
    }

    @Test
    void newerMinorThanCurrentIsIncompatible() {
        assertFalse(BreezeApiVersion.isCompatible("1.5.0"));
    }

    @Test
    void blankOrNullIsIncompatible() {
        assertFalse(BreezeApiVersion.isCompatible(null));
        assertFalse(BreezeApiVersion.isCompatible(""));
        assertFalse(BreezeApiVersion.isCompatible("   "));
    }

    @Test
    void malformedPatchSegmentIsTreatedAsZero() {
        assertTrue(BreezeApiVersion.isCompatible("1.0.x"));
    }
}
