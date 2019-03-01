package com.sourceplusplus.plugin;

import com.sourceplusplus.api.model.artifact.SourceArtifact;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.0
 * @since 0.1.0
 */
public class PluginSourceFile implements MessageCodec<PluginSourceFile, PluginSourceFile> {

    private File file;
    private String qualifiedClassName;
    private final Map<String, SourceArtifact> sourceMethods = new ConcurrentHashMap<>();

    public PluginSourceFile() {
        //ONLY USE WHEN REGISTERING CODEC
    }

    public PluginSourceFile(@NotNull File file, @NotNull String qualifiedClassName) {
        this.file = file;
        this.qualifiedClassName = qualifiedClassName;
    }

    @NotNull
    public File getFile() {
        return file;
    }

    @NotNull
    public String getQualifiedClassName() {
        return qualifiedClassName;
    }

    @NotNull
    public List<SourceArtifact> getSourceMethods() {
        return new ArrayList<>(sourceMethods.values());
    }

    @Nullable
    public SourceArtifact getSourceMethod(@NotNull String methodQualifiedName) {
        return sourceMethods.get(methodQualifiedName);
    }

    public void addSourceMethod(@NotNull SourceArtifact sourceMethod) {
        sourceMethods.put(sourceMethod.artifactQualifiedName(), sourceMethod);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginSourceFile that = (PluginSourceFile) o;
        return Objects.equals(file, that.file) &&
                Objects.equals(qualifiedClassName, that.qualifiedClassName) &&
                Objects.equals(sourceMethods, that.sourceMethods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, qualifiedClassName, sourceMethods);
    }

    @Override
    public void encodeToWire(Buffer buffer, PluginSourceFile pluginSourceFile) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PluginSourceFile decodeFromWire(int pos, Buffer buffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PluginSourceFile transform(PluginSourceFile pluginSourceFile) {
        return pluginSourceFile;
    }

    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
