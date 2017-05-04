package gw.plugin.ij.extensions;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import gw.lang.reflect.SourcePosition;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.module.IModule;
import gw.plugin.ij.custom.JavaFacadePsiClass;
import gw.plugin.ij.util.GosuModuleUtil;
import gw.plugin.ij.util.JavaPsiFacadeUtil;
import org.jetbrains.annotations.Nullable;


/**
 */
public class GosuGotoDeclarationHandler extends GotoDeclarationHandlerBase
{
  @Nullable
  @Override
  public PsiElement getGotoDeclarationTarget( @Nullable PsiElement sourceElement, Editor editor )
  {
    PsiElement parent = sourceElement.getParent();
    if( parent != null )
    {
      PsiReference ref = parent.getReference();
      if( ref != null )
      {
        PsiElement resolve = ref.resolve();
        if( resolve != null )
        {
          PsiFile file = resolve.getContainingFile();
          if( file != null )
          {
            JavaFacadePsiClass facade = file.getUserData( JavaFacadePsiClass.KEY_JAVAFACADE );
            if( facade != null )
            {
              PsiAnnotation[] annotations = ((PsiModifierListOwner)resolve).getModifierList().getAnnotations();
              if( annotations != null && annotations.length > 0 &&
                  annotations[0].getQualifiedName().equals( SourcePosition.class.getName() ) )
              {
                return findTargetFeature( annotations[0], facade );
              }
            }
          }
        }
      }
    }
    return null;
  }

  private PsiElement findTargetFeature( PsiAnnotation psiAnnotation, JavaFacadePsiClass facade )
  {
    PsiAnnotationMemberValue value = psiAnnotation.findAttributeValue( "originFeatureSignature" );
    String featureName = StringUtil.unquoteString( value.getText() );
    value = psiAnnotation.findAttributeValue( "originTypeName" );
//    if( value != null )
//    {
//      String ownersType = StringUtil.unquoteString( value.getText() );
//      if( ownersType != null )
//      {
//        PsiElement target = findIndirectTarget( ownersType, featureName, facade.getRawFile().getProject() );
//        if( target != null )
//        {
//          return target;
//        }
//      }
//    }

    int iOffset = Integer.parseInt( psiAnnotation.findAttributeValue( "offset" ).getText() );
    int iLength = Integer.parseInt( psiAnnotation.findAttributeValue( "length" ).getText() );

    PsiFile sourceFile = facade.getRawFile();
    IModule module = GosuModuleUtil.findModuleForPsiElement( sourceFile );
    TypeSystem.pushModule( module );
    try
    {
      if( iOffset >= 0 )
      {
        //PsiElement target = sourceFile.findElementAt( iOffset );
        return new FakeTargetElement( sourceFile, iOffset, iLength >= 0 ? iLength : 1, featureName );
      }
    }
    finally
    {
      TypeSystem.popModule( module );
    }
    return facade;
  }

  private PsiElement findIndirectTarget( String ownersType, String featureName, Project project )
  {
    PsiClass psiClass = JavaPsiFacadeUtil.findClass( project, ownersType, GlobalSearchScope.allScope( project ) );
    PsiMethod[] methods = psiClass.findMethodsByName( featureName, false );
    if( methods != null && methods.length > 0 )
    {
      return methods[0].getNameIdentifier();
    }
    else
    {
      methods = psiClass.findMethodsByName( "get" + featureName, false );
      if( methods != null && methods.length > 0 )
      {
        return methods[0].getNameIdentifier();
      }
    }
    return null;
  }
}
