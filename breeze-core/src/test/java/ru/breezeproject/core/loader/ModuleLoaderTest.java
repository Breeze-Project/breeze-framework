package ru.breezeproject.core.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.breezeproject.core.loader.fixtures.RecordingTestModule;
import ru.breezeproject.core.service.SimpleServiceRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleLoaderTest {

    private static final String MODULE_CLASS = "ru.breezeproject.core.loader.fixtures.RecordingTestModule";

    @TempDir
    Path tempDir;

    @BeforeEach
    void resetCounters() {
        RecordingTestModule.resetCounters();
    }

    private ModuleLoader newLoader(Path pluginDataFolder) {
        return new ModuleLoader(pluginDataFolder.toFile(), new SimpleServiceRegistry(), Logger.getLogger("test"));
    }

    private File buildModuleJar(Path dir, String jarName, String moduleName, String apiVersion) throws IOException {
        File jarFile = dir.resolve(jarName).toFile();

        String moduleYml = """
                name: %s
                version: '1.0.0'
                main: %s
                api-version: '%s'
                depends: []
                """.formatted(moduleName, MODULE_CLASS, apiVersion);

        String classResourcePath = MODULE_CLASS.replace('.', '/') + ".class";

        try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(jarFile))) {
            jarOut.putNextEntry(new JarEntry("module.yml"));
            jarOut.write(moduleYml.getBytes());
            jarOut.closeEntry();

            jarOut.putNextEntry(new JarEntry(classResourcePath));
            try (InputStream classBytes = getClass().getClassLoader().getResourceAsStream(classResourcePath)) {
                if (classBytes == null) {
                    throw new IOException("Could not locate compiled fixture class on test classpath: " + classResourcePath);
                }
                classBytes.transferTo(jarOut);
            }
            jarOut.closeEntry();
        }
        return jarFile;
    }

    @Test
    void loadAllEnablesEveryValidModuleInTheDirectory() throws IOException {
        Path modulesDir = tempDir.resolve("modules");
        modulesDir.toFile().mkdirs();
        buildModuleJar(modulesDir, "test-module.jar", "TestModule", "1.0.0");

        ModuleLoader loader = newLoader(tempDir);
        loader.loadAll();

        assertTrue(loader.getLoadedModules().containsKey("TestModule"));
        assertEquals(1, RecordingTestModule.ENABLE_COUNT.get());
    }

    @Test
    void incompatibleApiVersionIsSkipped() throws IOException {
        Path modulesDir = tempDir.resolve("modules");
        modulesDir.toFile().mkdirs();
        buildModuleJar(modulesDir, "future-module.jar", "FutureModule", "9.9.9");

        ModuleLoader loader = newLoader(tempDir);
        loader.loadAll();

        assertFalse(loader.getLoadedModules().containsKey("FutureModule"));
        assertEquals(0, RecordingTestModule.ENABLE_COUNT.get());
    }

    @Test
    void reloadDisablesAndReEnablesOnlyThatModule() throws IOException {
        Path modulesDir = tempDir.resolve("modules");
        modulesDir.toFile().mkdirs();
        buildModuleJar(modulesDir, "test-module.jar", "TestModule", "1.0.0");

        ModuleLoader loader = newLoader(tempDir);
        loader.loadAll();
        assertEquals(1, RecordingTestModule.ENABLE_COUNT.get());

        boolean reloaded = loader.reload("TestModule");

        assertTrue(reloaded);
        assertEquals(1, RecordingTestModule.DISABLE_COUNT.get());
        assertEquals(2, RecordingTestModule.ENABLE_COUNT.get());
        assertTrue(loader.getLoadedModules().containsKey("TestModule"));
    }

    @Test
    void reloadOfUnknownModuleReturnsFalse() {
        ModuleLoader loader = newLoader(tempDir);
        assertFalse(loader.reload("DoesNotExist"));
    }

    @Test
    void unloadAllDisablesEveryModule() throws IOException {
        Path modulesDir = tempDir.resolve("modules");
        modulesDir.toFile().mkdirs();
        buildModuleJar(modulesDir, "test-module.jar", "TestModule", "1.0.0");

        ModuleLoader loader = newLoader(tempDir);
        loader.loadAll();
        loader.unloadAll();

        assertTrue(loader.getLoadedModules().isEmpty());
        assertEquals(1, RecordingTestModule.DISABLE_COUNT.get());
    }

    @Test
    void duplicateModuleNamesAreRejected() throws IOException {
        Path modulesDir = tempDir.resolve("modules");
        modulesDir.toFile().mkdirs();
        buildModuleJar(modulesDir, "module-a.jar", "SameName", "1.0.0");
        buildModuleJar(modulesDir, "module-b.jar", "SameName", "1.0.0");

        ModuleLoader loader = newLoader(tempDir);
        loader.loadAll();

        assertEquals(1, RecordingTestModule.ENABLE_COUNT.get());
        assertEquals(1, loader.getLoadedModules().size());
    }
}
