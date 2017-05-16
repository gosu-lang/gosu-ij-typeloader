/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.plugin.ij.core;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import gw.config.IExtensionFolderLocator;
import gw.lang.reflect.TypeSystem;
import manifold.api.service.BaseService;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class IDEAExtensionFolderLocator extends BaseService implements IExtensionFolderLocator
{
  @Nullable
  @Override
  public File getExtensionFolderPath() {
    final PluginId id = PluginManager.getPluginByClassName(TypeSystem.class.getName());
    return id != null ? new File(PluginManager.getPlugin(id).getPath(), "extlib") : null;
  }
}
