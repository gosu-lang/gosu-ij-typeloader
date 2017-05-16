package gw.plugin.ij.extensions;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.Processor;
import com.intellij.util.containers.HashSet;
import gw.lang.reflect.IType;
import gw.lang.reflect.SimpleTypeLoader;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.GosuClassTypeLoader;
import gw.lang.reflect.module.IModule;
import gw.plugin.ij.custom.JavaFacadePsiClass;
import gw.plugin.ij.lang.psi.impl.CustomPsiClassCache;
import java.util.Set;
import manifold.api.sourceprod.ISourceProducer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 */
public class CustomPsiShortNamesCache extends PsiShortNamesCache
{
  private final PsiManagerEx _manager;

  public CustomPsiShortNamesCache( PsiManagerEx manager )
  {
    _manager = manager;
  }

  @NotNull
  @Override
  public PsiClass[] getClassesByName( @NotNull @NonNls String name, @NotNull GlobalSearchScope scope )
  {
    IModule module = GosuTypeFinder.findModule( scope );
    TypeSystem.pushModule( module );
    try
    {
      IType type = TypeSystem.getByFullNameIfValid( name, module );
      if( type != null )
      {
        JavaFacadePsiClass psiClass = CustomPsiClassCache.instance().getPsiClass( type );
        if( psiClass != null )
        {
          return new PsiClass[]{psiClass};
        }
      }
    }
    finally
    {
      TypeSystem.popModule( module );
    }
    return new PsiClass[0];
  }

  @NotNull
  @Override
  public String[] getAllClassNames()
  {
    Set<String> types = getAll();
    return types.toArray( new String[types.size()] );
  }

  @Override
  public void getAllClassNames( @NotNull HashSet<String> dest )
  {
    dest.addAll( getAll() );
  }

  private Set<String> getAll()
  {
    HashSet<String> all = new HashSet<>();
    for( IModule module : TypeSystem.getGlobalModule().getModuleTraversalList() )
    {
      SimpleTypeLoader javaLoader = (SimpleTypeLoader)module.getModuleTypeLoader().getDefaultTypeLoader();
      for( ISourceProducer sp : javaLoader.getSourceProducers() )
      {
        all.addAll( sp.getAllTypeNames() );
      }

      GosuClassTypeLoader gosuLoader = module.getModuleTypeLoader().getTypeLoader( GosuClassTypeLoader.class );
      all.addAll( gosuLoader.getAllTypeNames() );
    }
    return all;
  }

  @NotNull
  @Override
  public PsiMethod[] getMethodsByName( @NonNls @NotNull String name, @NotNull GlobalSearchScope scope )
  {
    return new PsiMethod[0];
  }

  @NotNull
  @Override
  public PsiMethod[] getMethodsByNameIfNotMoreThan( @NonNls @NotNull String name, @NotNull GlobalSearchScope scope, int maxCount )
  {
    return new PsiMethod[0];
  }

  @NotNull
  @Override
  public PsiField[] getFieldsByNameIfNotMoreThan( @NonNls @NotNull String name, @NotNull GlobalSearchScope scope, int maxCount )
  {
    return new PsiField[0];
  }

  @Override
  public boolean processMethodsWithName( @NonNls @NotNull String name, @NotNull GlobalSearchScope scope, @NotNull Processor<PsiMethod> processor )
  {
    return false;
  }

  @NotNull
  @Override
  public String[] getAllMethodNames()
  {
    return new String[0];
  }

  @Override
  public void getAllMethodNames( @NotNull HashSet<String> set )
  {

  }

  @NotNull
  @Override
  public PsiField[] getFieldsByName( @NotNull @NonNls String name, @NotNull GlobalSearchScope scope )
  {
    return new PsiField[0];
  }

  @NotNull
  @Override
  public String[] getAllFieldNames()
  {
    return new String[0];
  }

  @Override
  public void getAllFieldNames( @NotNull HashSet<String> set )
  {

  }
}