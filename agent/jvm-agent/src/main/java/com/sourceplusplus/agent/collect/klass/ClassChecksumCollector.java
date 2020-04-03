package com.sourceplusplus.agent.collect.klass;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.1.0
 */
public class ClassChecksumCollector {

    private static final Map<String, String> classHashMap = new ConcurrentHashMap<>();

    public static void logClassHash(String qualifiedClassName, InputStream compiledClassInputStream) {
        try {
            String hash = ByteSource.wrap(ByteStreams.toByteArray(compiledClassInputStream))
                    .hash(Hashing.sha256()).toString();
            logClassHash(qualifiedClassName, hash);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void logClassHash(String qualifiedClassName, String bytecodeChecksum) {
        classHashMap.put(qualifiedClassName, bytecodeChecksum);
    }

    @Nullable
    public static String getLoggedClassHash(String qualifiedClassName) {
        return classHashMap.get(qualifiedClassName);
    }
}
