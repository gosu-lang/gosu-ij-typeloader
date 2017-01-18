package gw.plugin.ij;

import com.intellij.lang.LanguageASTFactory;
import com.intellij.testFramework.ParsingTestCase;
import gw.gosu.ij.GosuLanguage;
import gw.gosu.ij.psi.impl.source.tree.GosuASTFactory;
import gw.gosu.ij.GosuParserDefinition;

import java.nio.file.Paths;

public class XMLParsingTest extends ParsingTestCase {
  
  public XMLParsingTest() {
    super("", "gs", new GosuParserDefinition());
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    addExplicitExtension(LanguageASTFactory.INSTANCE, GosuLanguage.INSTANCE, new GosuASTFactory());
  }
  
  @Override
  protected String getTestDataPath() {
    return Paths.get(System.getProperty("user.dir"), "src", "test", "resources").toString();
  }

  public void testSimpleGosuClass() {
    doTest(true);
  }
  
  public void testGosuClassWithSimpleXmlType() {
    doTest(true);
  }

  @Override
  protected boolean includeRanges() {
    return true; //include offsets like [n,m] in parse results 
  }
}
