package gw.plugin.ij.extensions;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import gw.lang.parser.IFileRepositoryBasedType;
import gw.lang.reflect.INamespaceType;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.module.IExecutionEnvironment;
import gw.lang.reflect.module.IModule;
import gw.plugin.ij.lang.psi.impl.CustomPsiClassCache;
import gw.plugin.ij.util.GosuModuleUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import manifold.api.sourceprod.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 */
public class GosuTypeFinder extends PsiElementFinder
{
  private static GosuTypeFinder INSTANCE = null;

  public GosuTypeFinder()
  {
    INSTANCE = this;
  }

  @Nullable
  @Override
  public PsiClass findClass( @NotNull String s, @NotNull GlobalSearchScope globalSearchScope )
  {
    IModule module = findModule( globalSearchScope );
    TypeSystem.pushModule( module );
    try
    {
      IType type = TypeSystem.getByFullNameIfValid( s );
      if( acceptType( type ) )
      {
        return CustomPsiClassCache.instance().getPsiClass( type );
      }
      return null;
    }
    finally
    {
      TypeSystem.popModule( module );
    }
  }

  public static IModule findModule( @NotNull GlobalSearchScope globalSearchScope )
  {
    IModule module;
    if( globalSearchScope instanceof ModuleWithDependenciesScope )
    {
      module = GosuModuleUtil.getModule( ((ModuleWithDependenciesScope)globalSearchScope).getModule() );
    }
    else
    {
      IExecutionEnvironment execEnv = TypeSystem.getExecutionEnvironment( globalSearchScope.getProject() );
      module = execEnv.getGlobalModule();
    }
    return module;
  }

  @NotNull
  @Override
  public PsiClass[] getClasses( @NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope )
  {
    IModule module = findModule( scope );
    TypeSystem.pushModule( module );
    try
    {
      INamespaceType namespace = TypeSystem.getNamespace( psiPackage.getQualifiedName() );
      if( namespace != null )
      {
        //PsiManager manager = PsiManagerImpl.getInstance( (Project)namespace.getModule().getExecutionEnvironment().getProject().getNativeProject() );
        Map<String, PsiClass> types = new HashMap<>();
        Set<TypeName> children = namespace.getChildren( null );
        for( TypeName tn : children )
        {
          IType type = TypeSystem.getByFullNameIfValid( tn.name );
          if( acceptType( type ) )
          {
            if( !types.containsKey( tn.name ) )
            {
              PsiClass psiClass = CustomPsiClassCache.instance().getPsiClass( type );
              if( psiClass != null )
              {
                types.put( tn.name, psiClass );
              }
            }
          }
        }
        return types.values().toArray( new PsiClass[types.size()] );
      }
      return new PsiClass[0];
    }
    finally
    {
      TypeSystem.popModule( module );
    }
  }

  @NotNull
  @Override
  public PsiClass[] getClasses( @Nullable String className, @NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope )
  {
    return super.getClasses( className, psiPackage, scope );
  }

  @NotNull
  @Override
  public PsiPackage[] getSubPackages( @NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope )
  {
    IModule module = findModule( scope );
    TypeSystem.pushModule( module );
    try
    {
      String parentPackage = psiPackage.getQualifiedName();
      INamespaceType namespace = TypeSystem.getNamespace( parentPackage );
      if( namespace != null )
      {
        PsiManager manager = PsiManagerImpl.getInstance( (Project)namespace.getModule().getExecutionEnvironment().getProject().getNativeProject() );
        Set<PsiPackage> children = new HashSet<>();
        for( TypeName child: namespace.getChildren( null ) )
        {
          if( child.kind == TypeName.Kind.NAMESPACE )
          {
            children.add( new NonDirectoryPackage( manager, parentPackage + '.' + child.name ) );
          }
        }
        return children.toArray( new PsiPackage[children.size()] );
      }
    }
    finally
    {
      TypeSystem.popModule( module );
    }
    return super.getSubPackages( psiPackage, scope );
  }

  @Override
  public boolean processPackageDirectories( @NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope, @NotNull Processor<PsiDirectory> consumer )
  {
    return super.processPackageDirectories( psiPackage, scope, consumer );
  }

  @Nullable
  @Override
  public PsiPackage findPackage( @NotNull String qualifiedName )
  {
//    PsiPackage pkg = JavaPsiFacadeUtil.findPackage( (Project)TypeSystem.getGlobalModule().getExecutionEnvironment().getProject().getNativeProject(), qualifiedName );
//    if( pkg != null )
//    {
//      return null;
//    }

    TypeSystem.pushGlobalModule();
    try
    {
      INamespaceType namespace = TypeSystem.getNamespace( qualifiedName );
      if( namespace != null )
      {
        // If the namespace comes from a non-default typeloader, we assume it is a "virtual" namespace
        // and that it does not reflect a directory tree like a normal java package, otherwise it would
        // be resolved by the DefaultTypeloader

        PsiManager manager = PsiManagerImpl.getInstance( (Project)namespace.getModule().getExecutionEnvironment().getProject().getNativeProject() );
        return new NonDirectoryPackage( manager, namespace.getName() );
      }
    }
    finally
    {
      TypeSystem.popGlobalModule();
    }

    return null;
  }


  @NotNull
  @Override
  public PsiClass[] findClasses( @NotNull String s, @NotNull GlobalSearchScope globalSearchScope )
  {
    PsiClass gsType = findClass( s, globalSearchScope );
    if( gsType != null ) {
      return new PsiClass[] {gsType};
    }
    return new PsiClass[0];
  }

  private boolean acceptType( IType type )
  {
    if( type == null )
    {
      return false;
    }

    if( type instanceof IFileRepositoryBasedType )
    {
      IFileRepositoryBasedType ftype = (IFileRepositoryBasedType)type;
      if( ftype instanceof IGosuClass )
      {
        return !isGosuPluginEnabled() || isSourceProducer( ftype );
      }
      return isSourceProducer( ftype );
    }

    return true;
  }

  private boolean isSourceProducer( IFileRepositoryBasedType ftype )
  {
    return ftype.getSourceFileHandle() != null && ftype.getSourceFileHandle().getSourceProducer() != null;
  }

  private boolean isGosuPluginEnabled()
  {
    IdeaPluginDescriptor gosuPlugin = PluginManager.getPlugin( PluginId.getId( "com.guidewire.gosu" ) );
    return gosuPlugin != null && gosuPlugin.isEnabled();
  }
  public static GosuTypeFinder instance()
  {
    return INSTANCE;
  }
}
