package io.izzel.arclight.installer;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

class Util {

    public static String mavenToPath(String maven) {
        String fileType;
        if (maven.matches(".*@\\w+$")) {
            int i = maven.lastIndexOf('@');
            fileType = maven.substring(i + 1);
            maven = maven.substring(0, i);
        } else {
            fileType = "jar";
        }
        String[] coordinateParts = maven.split(":");
        if (coordinateParts.length == 3) {
            String packagePath = coordinateParts[0].replace('.', '/');
            return String.format("%s/%s/%s/%s-%s.%s", packagePath, coordinateParts[1], coordinateParts[2], coordinateParts[1], coordinateParts[2], fileType);
        } else if (coordinateParts.length == 4) {
            String packagePath = coordinateParts[0].replace('.', '/');
            return String.format("%s/%s/%s/%s-%s-%s.%s", packagePath, coordinateParts[1], coordinateParts[2], coordinateParts[1], coordinateParts[2], coordinateParts[3], fileType);
        } else throw new RuntimeException("Wrong maven coordinate " + maven);
    }

    private static final String SHA_PAD = String.format("%040d", 0);

    public static String hash(String filePath) throws Exception {
        return hash(new File(filePath).toPath());
    }

    public static String hash(File filePath) throws Exception {
        return hash(filePath.toPath());
    }

    public static String hash(Path filePath) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        String hash = new BigInteger(1, messageDigest.digest(Files.readAllBytes(filePath))).toString(16);
        return (SHA_PAD + hash).substring(hash.length());
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void throwException(Throwable e) throws E {
        throw (E) e;
    }
}
