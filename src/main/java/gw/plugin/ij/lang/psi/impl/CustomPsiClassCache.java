/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.plugin.ij.lang.psi.impl;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import gw.lang.GosuShop;
import gw.lang.parser.IFileRepositoryBasedType;
import gw.lang.reflect.AbstractTypeSystemListener;
import gw.lang.reflect.IType;
import gw.lang.reflect.RefreshRequest;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.java.IJavaClassInfo;
import gw.lang.reflect.java.IJavaType;
import gw.lang.reflect.module.IModule;
import gw.plugin.ij.custom.JavaFacadePsiClass;
import gw.plugin.ij.util.FileUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CustomPsiClassCache extends AbstractTypeSystemListener
{
  private static final CustomPsiClassCache INSTANCE = new CustomPsiClassCache();

  @NotNull
  public static CustomPsiClassCache instance()
  {
    return INSTANCE;
  }

  private final ConcurrentHashMap<String, JavaFacadePsiClass> _psi2Class = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<IModule, Map<String, JavaFacadePsiClass>> _type2Class = new ConcurrentHashMap<>();

  private CustomPsiClassCache()
  {
    TypeSystem.addTypeLoaderListenerAsWeakRef( this );
  }

  public JavaFacadePsiClass getPsiClass( @NotNull IType type )
  {
    List<VirtualFile> typeResourceFiles = FileUtil.getTypeResourceFiles( type );
    if( typeResourceFiles.isEmpty() )
    {
      return null;
    }

    IModule module = type.getTypeLoader().getModule();
    String name = type.getName();
    Map<String, JavaFacadePsiClass> map = _type2Class.computeIfAbsent( module, k -> new ConcurrentHashMap<>() );

    JavaFacadePsiClass psiFacadeClass = map.get( name );
    if( psiFacadeClass == null || !psiFacadeClass.isValid() )
    {
      PsiClass delegate = createPsiClass( type );
      psiFacadeClass = new JavaFacadePsiClass( delegate, type );
      map.put( name, psiFacadeClass );
      _psi2Class.put( type.getSourceFiles()[0].getPath().getPathString(), psiFacadeClass );
    }
    return psiFacadeClass;
  }

  @NotNull
  private PsiClass createPsiClass( @NotNull IType type )
  {
    PsiManager manager = PsiManagerImpl.getInstance( (Project)type.getTypeLoader().getModule().getExecutionEnvironment().getProject().getNativeProject() );
    String source = generateSource( type );
    final PsiJavaFile aFile = createDummyJavaFile( type, manager, source );
    final PsiClass[] classes = aFile.getClasses();
    return classes[0];
  }

  private PsiJavaFile createDummyJavaFile( IType type, PsiManager manager, @NonNls final String text )
  {
    final FileType fileType = JavaFileType.INSTANCE;
    return (PsiJavaFile)PsiFileFactory.getInstance( manager.getProject() ).createFileFromText( type.getName() + '.' + JavaFileType.INSTANCE.getDefaultExtension(), fileType, text );
  }

  private String generateSource( IType type )
  {
    String source = null;
    if( type instanceof IJavaType )
    {
      if( ((IJavaType)type).getBackingClassInfo().isCompilable() )
      {
        source = ((IJavaType)type).getSourceFileHandle().getSource().getSource();
      }
    }
    else
    {
      IJavaClassInfo classInfo = TypeSystem.getJavaClassInfo( type.getName(), TypeSystem.getCurrentModule() );
      if( classInfo != null )
      {
        source = classInfo.getSourceFileHandle().getSource().getSource();
      }
      else if( type instanceof IFileRepositoryBasedType )
      {
        source = GosuShop.genJavaStub( (IFileRepositoryBasedType)type );
      }
    }

    return source;
  }

  @Override
  public void refreshedTypes( RefreshRequest request )
  {
    Map<String, JavaFacadePsiClass> map = _type2Class.get( request.module );
    if( map != null )
    {
      for( String type : request.types )
      {
        map.remove( type );
      }
    }
    if( request.file != null )
    {
      String pathString = request.file.getPath().getPathString();
      JavaFacadePsiClass removedFacade = _psi2Class.remove( pathString );
      if( removedFacade != null )
      {
        ((PsiModificationTrackerImpl)removedFacade.getManager().getModificationTracker()).incCounter();
      }
    }
  }

  @Override
  public void refreshed()
  {
    _psi2Class.clear();
    _type2Class.clear();
  }
}
