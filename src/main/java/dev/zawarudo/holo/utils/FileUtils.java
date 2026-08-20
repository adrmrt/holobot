package dev.zawarudo.holo.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Utility class for working with files and directories.
 */
public final class FileUtils {

    private FileUtils() {
    }

    /**
     * Fetches all files inside the given directory path.
     *
     * @param directoryPath The path to the directory from which the files should be fetched.
     * @return All files inside the given directory path, including those inside subdirectories.
     */
    public static List<File> getAllFiles(String directoryPath) {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            return paths.filter(Files::isRegularFile)
                .map(Path::toFile)
                .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to walk through directory.", e);
        }
    }

    public static List<String> getAllResourcePaths(String root, String... extensions) throws IOException {
        // root example: "image/8ball"
        URL url = FileUtils.class.getClassLoader().getResource(root);
        if (url == null) {
            throw new IOException("Resource root not found on classpath: " + root);
        }

        if ("file".equals(url.getProtocol())) {
            return listFromFileSystem(new File(url.getPath()), root, extensions);
        } else if ("jar".equals(url.getProtocol())) {
            return listFromJar(url, root, extensions);
        }

        throw new IOException("Unsupported resource protocol: " + url.getProtocol());
    }

    private static List<String> listFromFileSystem(File dir, String classpathRoot, String... extensions) {
        List<String> out = new ArrayList<>();
        walk(dir, classpathRoot, out, extensions);
        return out;
    }

    private static void walk(File f, String classpathRoot, List<String> out, String... extensions) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children == null) return;
            for (File c : children) walk(c, classpathRoot, out, extensions);
            return;
        }

        String nameLower = f.getName().toLowerCase();
        if (!hasAllowedExtension(nameLower, extensions)) return;

        // Convert absolute filesystem path -> classpath path
        String full = f.getPath().replace("\\", "/");
        int idx = full.lastIndexOf(classpathRoot);
        if (idx >= 0) {
            out.add(full.substring(idx));
        }
    }

    private static List<String> listFromJar(URL rootUrl, String classpathRoot, String... extensions) throws IOException {
        List<String> out = new ArrayList<>();
        JarURLConnection conn = (JarURLConnection) rootUrl.openConnection();

        try (JarFile jar = conn.getJarFile()) {
            jar.stream()
                .filter(e -> !e.isDirectory())
                .map(e -> e.getName())
                .filter(name -> name.startsWith(classpathRoot + "/"))
                .filter(name -> hasAllowedExtension(name.toLowerCase(), extensions))
                .forEach(out::add);
        }

        return out;
    }

    private static boolean hasAllowedExtension(String nameLower, String... extensions) {
        if (extensions == null || extensions.length == 0) return true;
        for (String ext : extensions) {
            String e = ext.startsWith(".") ? ext : "." + ext;
            if (nameLower.endsWith(e)) return true;
        }
        return false;
    }
}
