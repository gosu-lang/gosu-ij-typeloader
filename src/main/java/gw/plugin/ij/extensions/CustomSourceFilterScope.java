package gw.plugin.ij.extensions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import gw.lang.reflect.TypeSystem;
import gw.plugin.ij.filesystem.IDEAFile;
import gw.plugin.ij.util.FileUtil;
import org.jetbrains.annotations.NotNull;

/**
 */
public class CustomSourceFilterScope extends DelegatingGlobalSearchScope
{
  private static final Logger LOG = Logger.getInstance( gw.gosu.ij.psi.search.GosuSourceFilterScope.class );

  CustomSourceFilterScope( @NotNull final GlobalSearchScope delegate )
  {
    super( delegate );
  }

  @Override
  public boolean contains( @NotNull final VirtualFile file )
  {
    if( file.isDirectory() )
    {
      return true;
    }

    String[] types = TypeSystem.getTypesForFile( TypeSystem.getCurrentModule(), (IDEAFile)FileUtil.toIResource( file ) );
    return types != null && types.length > 0;
  }

}