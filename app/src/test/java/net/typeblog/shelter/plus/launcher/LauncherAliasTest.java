package net.typeblog.shelter.plus.launcher;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LauncherAliasTest {
    @Test public void canonicalComponentNameMatchesManifestAlias() throws Exception {
        String manifest = new String(Files.readAllBytes(
                new File("src/main/AndroidManifest.xml").toPath()), StandardCharsets.UTF_8);
        Matcher alias = Pattern.compile("<activity-alias\\b([^>]*)>", Pattern.DOTALL)
                .matcher(manifest);
        int matches = 0;
        while (alias.find()) {
            Matcher name = Pattern.compile("android:name=\"([^\"]+)\"").matcher(alias.group(1));
            if (name.find() && name.group(1).equals(LauncherAlias.CLASS_NAME)) matches++;
        }
        assertEquals("exactly one manifest alias must match the component used at runtime", 1, matches);
        assertTrue(manifest.contains("android:targetActivity=\".plus.launcher.LauncherActivity\""));
    }
}
