// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.compiled;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.impl.source.JavaFileElementType;
import com.intellij.psi.stubs.BinaryFileStubBuilder;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.stubs.Stub;
import com.intellij.util.cls.ClsFormatException;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

import static com.intellij.psi.compiled.ClassFileDecompilers.Full;

public class ClassFileStubBuilder implements BinaryFileStubBuilder.CompositeBinaryFileStubBuilder<ClassFileDecompilers.Decompiler> {
  private static final Logger LOG = Logger.getInstance(ClassFileStubBuilder.class);

  public static final int STUB_VERSION = 25 + JavaFileElementType.STUB_VERSION;

  @Override
  public boolean acceptsFile(@NotNull VirtualFile file) {
    return true;
  }

  @Override
  public @NotNull Stream<ClassFileDecompilers.Decompiler> getAllSubBuilders() {
    return ClassFileDecompilers.getInstance().EP_NAME.extensions().filter(decompiler -> decompiler instanceof Full);
  }

  @Override
  public @Nullable ClassFileDecompilers.Decompiler getSubBuilder(@NotNull FileContent fileContent) {
    return fileContent.getFile()
      .computeWithPreloadedContentHint(fileContent.getContent(), () -> ClassFileDecompilers.getInstance().find(fileContent.getFile()));
  }

  @Override
  public @NotNull String getSubBuilderVersion(@Nullable ClassFileDecompilers.Decompiler decompiler) {
    if (decompiler == null) return "default";
    int version = decompiler instanceof Full ? ((Full)decompiler).getStubBuilder().getStubVersion() : 0;
    return decompiler.getClass().getName() + ":" + version;
  }

  @Override
  public @Nullable Stub buildStubTree(@NotNull FileContent fileContent, @Nullable ClassFileDecompilers.Decompiler decompiler) {
    return fileContent.getFile().computeWithPreloadedContentHint(fileContent.getContent(), () -> {
      VirtualFile file = fileContent.getFile();
      try {
        if (decompiler instanceof Full) {
          return ((Full)decompiler).getStubBuilder().buildFileStub(fileContent);
        }
      }
      catch (ClsFormatException e) {
        if (LOG.isDebugEnabled()) LOG.debug(file.getPath(), e);
        else LOG.info(file.getPath() + ": " + e.getMessage());
      }

      try {
        PsiFileStub<?> stub = ClsFileImpl.buildFileStub(file, fileContent.getContent());
        if (stub == null && fileContent.getFileName().indexOf('$') < 0) {
          LOG.info("No stub built for the file " + fileContent);
        }
        return stub;
      }
      catch (ClsFormatException e) {
        if (LOG.isDebugEnabled()) LOG.debug(file.getPath(), e);
        else LOG.info(file.getPath() + ": " + e.getMessage());
      }

      return null;
    });
  }

  @Override
  public int getStubVersion() {
    return STUB_VERSION;
  }
}